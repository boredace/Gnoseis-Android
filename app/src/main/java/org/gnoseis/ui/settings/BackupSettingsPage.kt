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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Serializable
data class BackupSettingsPageRoute(
        val dummy: Boolean? = false
)


@Composable
fun BackupSettingsPage(
    pageViewModel: BackupSettingsViewModel = viewModel(factory = AppViewModelProvider.Factory),
    onNavMenuClick: () -> Unit,
    onImportClicked: () -> Unit,
    onSetUpPassphrase: () -> Unit = {},
    ) {

    // #66: back from the passphrase setup, encryption may have been turned on or changed
    LifecycleResumeEffect(Unit) {
        pageViewModel.refresh()
        onPauseOrDispose { }
    }

    // Whether the save dialog that is open was started by "Back up without encryption"
    var exportPlain by rememberSaveable { mutableStateOf(false) }

    // #32: the user chooses the folder and file name of the backup
    val exportLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        // uri is null when the user closes the file picker without choosing a location
        uri?.let { pageViewModel.exportDatabase(it, plain = exportPlain) }
    }

    // Create a file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        // uri is null when the user closes the file picker without choosing a file
        uri?.let { pageViewModel.importDatabase(it) }
    }

    fun export(plain: Boolean) {
        exportPlain = plain
        val dateStamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        exportLocationLauncher.launch(pageViewModel.suggestedFileName(dateStamp, plain))
    }

    BackupSettingsScaffold(
        onNavMenuClick = onNavMenuClick,
        encryption = pageViewModel.encryption.value,
        busy = pageViewModel.busy.value,
        onExportClicked = { export(plain = false) },
        onPlainExportConfirmed = { export(plain = true) },
        onImportClicked = {
            filePickerLauncher.launch(arrayOf("*/*")) // Use a MIME type like "image/*" for specific files
        },
        onSetUpPassphrase = onSetUpPassphrase,
        onTurnOffConfirmed = { pageViewModel.turnOffEncryption() },
    )

    pageViewModel.message.value?.let { message ->
        BackupMessageDialog(message, onConfirm = { pageViewModel.dismissResult() })
    }
    pageViewModel.passphrasePrompt.value?.let { prompt ->
        RestorePassphraseDialog(
            prompt = prompt,
            onRestore = pageViewModel::submitPassphrase,
            onCancel = pageViewModel::cancelPassphrase,
        )
    }
    if (pageViewModel.keepOfferWaiting) {
        AlertDialog(
            onDismissRequest = {},
            icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
            title = { Text("Keep encrypting backups?") },
            text = { Text("Keep encrypting backups on this device with this passphrase. You can change or turn it off in this page.") },
            confirmButton = { TextButton(onClick = { pageViewModel.keepEncrypting() }) { Text("Keep encrypting") } },
            dismissButton = { TextButton(onClick = { pageViewModel.declineKeepOffer() }) { Text("Not now") } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsScaffold(
    onNavMenuClick: () -> Unit,
    encryption: BackupEncryption,
    busy: Boolean,
    onExportClicked: () -> Unit,
    onPlainExportConfirmed: () -> Unit,
    onImportClicked: () -> Unit,
    onSetUpPassphrase: () -> Unit,
    onTurnOffConfirmed: () -> Unit,
    ) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Import / Export Database") },
                navigationIcon = {
                    IconButton(onClick = { onNavMenuClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {},
        content = { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
            ) {
                BackupSettingsBody(
                    encryption = encryption,
                    busy = busy,
                    onExportClicked = onExportClicked,
                    onPlainExportConfirmed = onPlainExportConfirmed,
                    onImportClicked = onImportClicked,
                    onSetUpPassphrase = onSetUpPassphrase,
                    onTurnOffConfirmed = onTurnOffConfirmed,
                )
            }
        }
    )
}


@Composable
fun BackupSettingsBody(
    encryption: BackupEncryption,
    busy: Boolean,
    onExportClicked: () -> Unit,
    onPlainExportConfirmed: () -> Unit,
    onImportClicked: () -> Unit,
    onSetUpPassphrase: () -> Unit,
    onTurnOffConfirmed: () -> Unit,
    ) {
    var confirmTurnOff by remember { mutableStateOf(false) }
    var confirmPlainExport by remember { mutableStateOf(false) }
    val encrypting = encryption != BackupEncryption.OFF

    if (confirmTurnOff) {
        AlertDialog(
            onDismissRequest = { confirmTurnOff = false },
            title = { Text("Turn off encryption?") },
            text = { Text("New backups will be plain files that anyone who gets them can read. Encrypted backups you've already made still need their passphrase.") },
            confirmButton = {
                TextButton(onClick = { confirmTurnOff = false; onTurnOffConfirmed() }) { Text("Turn off") }
            },
            dismissButton = { TextButton(onClick = { confirmTurnOff = false }) { Text("Cancel") } },
        )
    }
    if (confirmPlainExport) {
        AlertDialog(
            onDismissRequest = { confirmPlainExport = false },
            icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null) },
            title = { Text("Back up without encryption?") },
            text = { Text("Anyone who gets this file can read all your data.") },
            confirmButton = {
                TextButton(onClick = { confirmPlainExport = false; onPlainExportConfirmed() }) {
                    Text("Back up without encryption")
                }
            },
            dismissButton = { TextButton(onClick = { confirmPlainExport = false }) { Text("Cancel") } },
        )
    }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(8.dp)
    ) {
        if (busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))

        // #66: backup encryption
        Row(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(text = "Encryption", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "Encrypt backups", modifier = Modifier.weight(1f))
            Switch(
                checked = encrypting,
                onCheckedChange = { on -> if (on) onSetUpPassphrase() else confirmTurnOff = true },
            )
        }
        Row(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(
                text = when (encryption) {
                    BackupEncryption.OFF -> "Backups are plain files. Turn this on to encrypt them with a backup passphrase, so they can only be opened with it."
                    else -> "Backups are encrypted with AES-256 and can only be opened with your backup passphrase."
                }
            )
        }
        if (encryption == BackupEncryption.UNREADABLE) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                Text(
                    text = "The backup key on this device can't be opened, so encrypted backups can't be made. Set the passphrase again to keep encrypting.",
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        if (encrypting) {
            Row(Modifier.fillMaxWidth()) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSetUpPassphrase,
                ) {
                    Text(if (encryption == BackupEncryption.UNREADABLE) "Set passphrase again" else "Change passphrase")
                }
            }
        }

        Spacer(modifier = Modifier.padding(12.dp))

        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "Export Database", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = if (encrypting) {
                    "Pressing Export will let you choose where to save an encrypted copy of your data."
                } else {
                    "Pressing Export will let you choose where to save a copy of the current SQLite application database. The backup file is not encrypted, so anyone with access to that location can read your data."
                }
            )
        }


        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onExportClicked() }
            ) { Text(text = "Export") }
        }
        if (encrypting) {
            Row(Modifier.fillMaxWidth()) {
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { confirmPlainExport = true },
                ) { Text(text = "Back up without encryption") }
            }
        }


        Spacer(modifier = Modifier.padding(12.dp))

        Row(
            modifier = Modifier
                .padding(
                    top = 12.dp,
                    bottom = 12.dp
                )
        ) {
            Text(text = "Import Database", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "You can import existing backup file to replace database currently used by the application. An encrypted backup asks for its passphrase if it was made with another one.")
        }

        Row(
            modifier = Modifier
                .padding(bottom = 12.dp)
        ) {
            Text(text = "Warning: All existing data in your application will be replaced with the data from imported file. Ensure you have exported existing database using the Export function above before proceeding.", color = Color.Red)
        }
        Row(Modifier.fillMaxWidth()) {
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onImportClicked() }
            ) { Text(text = "Import") }
        }

    }
}


/** The result of the last export or import. */
@Composable
fun BackupMessageDialog(
    message: BackupMessage,
    onConfirm: () -> Unit,
) {
    val unchanged = "Your existing data was not changed."
    val (title, text) = when (message) {
        BackupMessage.EXPORTED_PLAIN ->
            "Export Successful" to "Database file exported to the selected location. It is not encrypted."
        BackupMessage.EXPORTED_ENCRYPTED ->
            "Export Successful" to "Database file exported to the selected location. The backup is encrypted with your backup passphrase."
        BackupMessage.EXPORT_FAILED ->
            "Export Failed" to "The backup file could not be written to the selected location."
        BackupMessage.KEY_UNREADABLE ->
            "Export Failed" to "The backup key on this device can't be opened, so no backup was written. Set the passphrase again, or use Back up without encryption."
        BackupMessage.RESTORED ->
            "Import Successful" to "Database file imported and replaced the old data."
        BackupMessage.WRONG_PASSPHRASE_OR_DAMAGED ->
            "Import Failed" to "Wrong passphrase, or the file is damaged. $unchanged"
        BackupMessage.NEWER_FORMAT ->
            "Import Failed" to "Made by a newer version of Gnoseis. Update the app to restore it. $unchanged"
        BackupMessage.NOT_A_BACKUP ->
            "Import Failed" to "This isn't a Gnoseis backup. $unchanged"
        BackupMessage.READ_FAILED ->
            "Import Failed" to "The selected file could not be read. $unchanged"
    }
    val success = message in setOf(BackupMessage.EXPORTED_PLAIN, BackupMessage.EXPORTED_ENCRYPTED, BackupMessage.RESTORED)
    AlertDialog(
        text = { Text(text = text) },
        onDismissRequest = { if (!success) onConfirm() },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) { Text(text = "OK") }
        },
        title = { Text(text = title) },
        icon = {
            Icon(if (success) Icons.Outlined.CheckCircleOutline else Icons.Outlined.ErrorOutline, contentDescription = null)
        },
    )
}

/** Asks for the passphrase of an encrypted backup (#66); kept out of screenshots. */
@Composable
fun RestorePassphraseDialog(
    prompt: PassphrasePrompt,
    onRestore: (CharArray) -> Unit,
    onCancel: () -> Unit,
) {
    var passphrase by remember { mutableStateOf("") }
    AlertDialog(
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        onDismissRequest = { if (!prompt.busy) onCancel() },
        icon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
        title = { Text("This backup is encrypted") },
        text = {
            Column {
                Text("Enter the passphrase it was made with.")
                PassphraseField(
                    value = passphrase,
                    onValueChange = { passphrase = it },
                    label = "Backup passphrase",
                    isError = prompt.wrongPassphrase,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
                if (prompt.wrongPassphrase) {
                    Text(
                        "Wrong passphrase, or the file is damaged.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (prompt.busy) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
        },
        confirmButton = {
            TextButton(
                enabled = !prompt.busy,
                onClick = {
                    onRestore(passphrase.toCharArray())
                    passphrase = ""
                },
            ) { Text("Restore") }
        },
        dismissButton = { TextButton(enabled = !prompt.busy, onClick = onCancel) { Text("Cancel") } },
    )
}


@Preview
@Composable
fun BackupSettingsPagePreview(
) {
    BackupSettingsScaffold(
        onNavMenuClick = {},
        encryption = BackupEncryption.ON,
        busy = false,
        onExportClicked = {},
        onPlainExportConfirmed = {},
        onImportClicked = {},
        onSetUpPassphrase = {},
        onTurnOffConfirmed = {},
    )
}
