/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024-2026 Gnoseis.org
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
*   https://github.com/boredace/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/boredace/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
*
*   By contributing to Gnoseis, you agree that your contributions will be
*   licensed under both the open-source license in the LICENSE file and the
*   commercial license described in the COMMERCIAL_LICENSE file.
*/

package org.gnoseis.ui.settings

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.gnoseis.data.backup.StoredBackupKey
import org.gnoseis.data.repository.DatabaseRepository
import org.gnoseis.data.repository.ExportResult
import org.gnoseis.data.repository.ImportResult
import org.gnoseis.data.repository.KeepOffer

/** Whether backups are encrypted on this device (#66). */
enum class BackupEncryption { OFF, ON, UNREADABLE }

/** The result dialog shown after an export or import. */
enum class BackupMessage {
    EXPORTED_PLAIN,
    EXPORTED_ENCRYPTED,
    EXPORT_FAILED,
    KEY_UNREADABLE,
    RESTORED,
    WRONG_PASSPHRASE_OR_DAMAGED,
    NEWER_FORMAT,
    NOT_A_BACKUP,
    READ_FAILED,
}

/** The passphrase dialog for an encrypted backup the stored key doesn't open (#66). */
data class PassphrasePrompt(val file: Uri, val wrongPassphrase: Boolean = false, val busy: Boolean = false)

class BackupSettingsViewModel(
    private val databaseRepository: DatabaseRepository,
    private val backupKey: StoredBackupKey,
) : ViewModel() {

    private val _encryption = mutableStateOf(BackupEncryption.OFF)
    val encryption: State<BackupEncryption> = _encryption

    private val _message = mutableStateOf<BackupMessage?>(null)
    val message: State<BackupMessage?> = _message

    private val _busy = mutableStateOf(false)
    val busy: State<Boolean> = _busy

    private val _passphrasePrompt = mutableStateOf<PassphrasePrompt?>(null)
    val passphrasePrompt: State<PassphrasePrompt?> = _passphrasePrompt

    /** Set after a passphrase restore on a device without a usable key of its own. */
    private val _keepOffer = mutableStateOf<KeepOffer?>(null)

    /** The offer to keep encrypting, shown once the restore message is dismissed. */
    val keepOfferWaiting: Boolean get() = _keepOffer.value != null && _message.value == null

    init {
        refresh()
    }

    /** Reads whether encryption is on; called again when coming back from the passphrase setup. */
    fun refresh() {
        viewModelScope.launch {
            _encryption.value = withContext(Dispatchers.IO) {
                when (backupKey.state()) {
                    StoredBackupKey.Off -> BackupEncryption.OFF
                    StoredBackupKey.Unreadable -> BackupEncryption.UNREADABLE
                    is StoredBackupKey.On -> BackupEncryption.ON
                }
            }
        }
    }

    /** The file name the save dialog suggests; an encrypted backup says so before the extension. */
    fun suggestedFileName(dateStamp: String, plain: Boolean = false): String =
        if (plain || encryption.value == BackupEncryption.OFF) "gnoseis_data_$dateStamp.backup"
        else "gnoseis_data_$dateStamp.encrypted.backup"

    /** Backs up to [destination]: encrypted while encryption is on, unless [plain]. */
    fun exportDatabase(destination: Uri, plain: Boolean = false) {
        viewModelScope.launch {
            _busy.value = true
            _message.value = when (databaseRepository.exportDatabase(destination, plain)) {
                ExportResult.Plain -> BackupMessage.EXPORTED_PLAIN
                ExportResult.Encrypted -> BackupMessage.EXPORTED_ENCRYPTED
                ExportResult.KeyUnreadable -> BackupMessage.KEY_UNREADABLE
                ExportResult.Failed -> BackupMessage.EXPORT_FAILED
            }
            _busy.value = false
        }
    }

    fun importDatabase(file: Uri) {
        viewModelScope.launch {
            _busy.value = true
            handle(file, databaseRepository.importDatabase(file), fromPrompt = false)
            _busy.value = false
        }
    }

    /** Tries the encrypted backup again with [passphrase], which is cleared afterwards. */
    fun submitPassphrase(passphrase: CharArray) {
        val prompt = _passphrasePrompt.value
        if (prompt == null || prompt.busy) {
            passphrase.fill('\u0000')
            return
        }
        _passphrasePrompt.value = prompt.copy(busy = true)
        viewModelScope.launch {
            val result = try {
                databaseRepository.importDatabase(prompt.file, passphrase)
            } finally {
                passphrase.fill('\u0000')
            }
            handle(prompt.file, result, fromPrompt = true)
        }
    }

    fun cancelPassphrase() {
        _passphrasePrompt.value = null
    }

    private fun handle(file: Uri, result: ImportResult, fromPrompt: Boolean) {
        when {
            result == ImportResult.NeedsPassphrase -> {
                _passphrasePrompt.value = PassphrasePrompt(file)
                return
            }
            // Stay in the dialog so the passphrase can be typed again
            result == ImportResult.WrongPassphraseOrDamaged && fromPrompt -> {
                _passphrasePrompt.value = PassphrasePrompt(file, wrongPassphrase = true)
                return
            }
        }
        _passphrasePrompt.value = null
        _message.value = when (result) {
            is ImportResult.Restored -> {
                result.keepOffer?.let { _keepOffer.value = it }
                BackupMessage.RESTORED
            }
            ImportResult.WrongPassphraseOrDamaged -> BackupMessage.WRONG_PASSPHRASE_OR_DAMAGED
            ImportResult.NewerFormat -> BackupMessage.NEWER_FORMAT
            ImportResult.NotABackup -> BackupMessage.NOT_A_BACKUP
            ImportResult.Failed, ImportResult.NeedsPassphrase -> BackupMessage.READ_FAILED
        }
    }

    /** Keeps the restored backup's passphrase for backups made on this device from now on. */
    fun keepEncrypting() {
        val offer = _keepOffer.value ?: return
        _keepOffer.value = null
        viewModelScope.launch {
            databaseRepository.keepEncrypting(offer)
            refresh()
        }
    }

    fun declineKeepOffer() {
        _keepOffer.value?.clear()
        _keepOffer.value = null
    }

    /** Turns backup encryption off on this device; encrypted files already made are untouched. */
    fun turnOffEncryption() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { backupKey.turnOff() }
            refresh()
        }
    }

    /** Hides the result dialog of the last export or import. */
    fun dismissResult() {
        _message.value = null
    }

    override fun onCleared() {
        declineKeepOffer()
    }
}
