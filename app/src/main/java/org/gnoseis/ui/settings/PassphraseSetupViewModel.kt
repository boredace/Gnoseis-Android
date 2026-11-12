/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024 Gnoseis.org
*
*   This program is free software: you can redistribute it and/or modify
*   it under the terms of the GNU General Public License as published by
*   the Free Software Foundation version 3 of the License.
*
*   This program is distributed in the hope that it will be useful,
*   but WITHOUT ANY WARRANTY; without even the implied warranty of
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*   GNU General Public License for more details.
*
*   You should have received a copy of the GNU General Public License
*   along with this program. If not, see
*   https://github.com/gnoseis.org/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/gnoseis.org/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
*
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.ui.settings

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.gnoseis.data.backup.BackupPassphrase
import org.gnoseis.data.backup.PassphraseGenerator
import org.gnoseis.data.backup.StoredBackupKey
import java.security.MessageDigest

enum class SetupStep { CHOOSE, GENERATED, OWN, CONFIRM, SAVING, DONE }

/**
 * Sets or changes the backup passphrase (#66): choose a generated or an own passphrase, enter
 * it once more, then the key is derived and stored. Every own passphrase needs the extra
 * "Use it anyway" confirmation, because no strength estimate is available.
 *
 * Text fields hold Strings, which can't be cleared; the passphrase arrays held here are.
 */
class PassphraseSetupViewModel(
    private val backupKey: StoredBackupKey,
    private val words: () -> List<String>,
) : ViewModel() {

    private val _step = mutableStateOf(SetupStep.CHOOSE)
    val step: State<SetupStep> = _step

    /** True when a passphrase is already set (or its key can't be opened): this is a change. */
    private val _isChange = mutableStateOf(false)
    val isChange: State<Boolean> = _isChange

    private val _generatedWords = mutableStateOf<List<String>>(emptyList())
    val generatedWords: State<List<String>> = _generatedWords

    private val _own = mutableStateOf("")
    val own: State<String> = _own

    private val _ownCheck = mutableStateOf(BackupPassphrase.OwnCheck.TOO_SHORT)
    val ownCheck: State<BackupPassphrase.OwnCheck> = _ownCheck

    /** Code points after normalising, for the "n of 12 characters" line. */
    private val _ownLength = mutableStateOf(0)
    val ownLength: State<Int> = _ownLength

    private val _showOwnError = mutableStateOf(false)
    val showOwnError: State<Boolean> = _showOwnError

    private val _showUseAnyway = mutableStateOf(false)
    val showUseAnyway: State<Boolean> = _showUseAnyway

    private val _confirm = mutableStateOf("")
    val confirm: State<String> = _confirm

    private val _confirmMismatch = mutableStateOf(false)
    val confirmMismatch: State<Boolean> = _confirmMismatch

    private val _saveFailed = mutableStateOf(false)
    val saveFailed: State<Boolean> = _saveFailed

    /** The passphrase chosen in the step before CONFIRM. */
    private var chosen: CharArray? = null
    private var chosenStep = SetupStep.GENERATED

    init {
        viewModelScope.launch {
            _isChange.value = withContext(Dispatchers.IO) { backupKey.state() != StoredBackupKey.Off }
        }
    }

    fun generate() {
        viewModelScope.launch {
            val list = withContext(Dispatchers.IO) { words() }
            choose(PassphraseGenerator(list).generate(), SetupStep.GENERATED)
            _generatedWords.value = String(chosen!!).split(' ')
            _step.value = SetupStep.GENERATED
        }
    }

    /** The generated passphrase without the numbers, for Copy. */
    fun generatedText(): String = _generatedWords.value.joinToString(" ")

    fun useOwn() {
        _step.value = SetupStep.OWN
    }

    fun onOwnChange(text: String) {
        _own.value = text
        _showOwnError.value = false
        val chars = text.toCharArray()
        _ownCheck.value = BackupPassphrase.checkOwn(chars)
        _ownLength.value = when (val normalised = BackupPassphrase.normalise(chars)) {
            is BackupPassphrase.Normalised -> normalised.codePoints.also { normalised.bytes.fill(0) }
            BackupPassphrase.Refused -> 0
        }
        chars.fill('\u0000')
    }

    /** Next on the own passphrase: too short or unusable shows why; otherwise "Use it anyway". */
    fun ownNext() {
        if (_ownCheck.value != BackupPassphrase.OwnCheck.OK) {
            _showOwnError.value = true
            return
        }
        _showUseAnyway.value = true
    }

    fun dismissUseAnyway() {
        _showUseAnyway.value = false
    }

    fun useAnyway() {
        _showUseAnyway.value = false
        choose(_own.value.toCharArray(), SetupStep.OWN)
        _step.value = SetupStep.CONFIRM
    }

    fun generatedNext() {
        _step.value = SetupStep.CONFIRM
    }

    fun onConfirmChange(text: String) {
        _confirm.value = text
        _confirmMismatch.value = false
    }

    /** Checks the passphrase entered once more, then derives and stores the key. */
    fun turnOn() {
        val passphrase = chosen ?: return
        val expected = BackupPassphrase.normalise(passphrase) as? BackupPassphrase.Normalised ?: return
        val typedChars = _confirm.value.toCharArray()
        val typed = BackupPassphrase.normalise(typedChars)
        typedChars.fill('\u0000')
        val matches = typed is BackupPassphrase.Normalised && MessageDigest.isEqual(expected.bytes, typed.bytes)
        if (typed is BackupPassphrase.Normalised) typed.bytes.fill(0)
        if (!matches) {
            expected.bytes.fill(0)
            _confirmMismatch.value = true
            return
        }
        _step.value = SetupStep.SAVING
        _saveFailed.value = false
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { backupKey.setPassphrase(expected.bytes) }
                clearChosen()
                _confirm.value = ""
                _own.value = ""
                _step.value = SetupStep.DONE
            } catch (e: Exception) {
                e.printStackTrace()
                _saveFailed.value = true
                _step.value = SetupStep.CONFIRM
            } finally {
                expected.bytes.fill(0)
            }
        }
    }

    /** One step back; false when the setup should close. */
    fun back(): Boolean {
        when (_step.value) {
            SetupStep.CHOOSE, SetupStep.DONE -> return false
            SetupStep.GENERATED, SetupStep.OWN -> _step.value = SetupStep.CHOOSE
            SetupStep.CONFIRM -> {
                _confirm.value = ""
                _confirmMismatch.value = false
                _step.value = chosenStep
            }
            SetupStep.SAVING -> Unit
        }
        return true
    }

    private fun choose(passphrase: CharArray, step: SetupStep) {
        clearChosen()
        chosen = passphrase
        chosenStep = step
    }

    private fun clearChosen() {
        chosen?.fill('\u0000')
        chosen = null
    }

    override fun onCleared() {
        clearChosen()
    }
}
