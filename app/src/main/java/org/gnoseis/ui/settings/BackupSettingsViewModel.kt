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
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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
import kotlinx.coroutines.launch
import org.gnoseis.data.repository.DatabaseRepository
import org.gnoseis.data.repository.ExportResult
import org.gnoseis.data.repository.ImportResult

class BackupSettingsViewModel(
    private val databaseRepository: DatabaseRepository
) : ViewModel() {

    private val _exportSuccess = mutableStateOf(false)
    val exportuccess: State<Boolean> = _exportSuccess

    private val _importSuccess = mutableStateOf(false)
    val importuccess: State<Boolean> = _importSuccess

    private val _exportFailed = mutableStateOf(false)
    val exportFailed: State<Boolean> = _exportFailed

    private val _importFailed = mutableStateOf(false)
    val importFailed: State<Boolean> = _importFailed


    fun exportDatabase(destination: Uri): Boolean {
        viewModelScope.launch {
            // Until the encryption screens arrive (#66), any written backup counts as success
            val written = databaseRepository.exportDatabase(destination).let {
                it == ExportResult.Plain || it == ExportResult.Encrypted
            }
            _exportSuccess.value = written
            _exportFailed.value = !written
        }
        return false
    }

    fun importDatabase(path: Uri): Boolean {
        viewModelScope.launch {
            val restored = databaseRepository.importDatabase(path).let { result ->
                (result as? ImportResult.Restored)?.keepOffer?.clear()
                result is ImportResult.Restored
            }
            _importSuccess.value = restored
            _importFailed.value = !restored
        }
        return false
    }

    /** Hides the result dialog of the last export or import. */
    fun dismissResult() {
        _exportSuccess.value = false
        _importSuccess.value = false
        _exportFailed.value = false
        _importFailed.value = false
    }
}