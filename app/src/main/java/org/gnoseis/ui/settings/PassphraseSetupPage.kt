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

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.PersistableBundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.backup.BackupPassphrase

@Serializable
data class PassphraseSetupRoute(
    val dummy: Boolean? = false
)

/** Sets or changes the backup passphrase (#66). Kept out of screenshots and recent apps. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassphraseSetupPage(
    onClose: () -> Unit,
    viewModel: PassphraseSetupViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    SecureWindow()
    val step by viewModel.step
    LaunchedEffect(step) { if (step == SetupStep.DONE) onClose() }
    val goBack = { if (!viewModel.back()) onClose() }
    BackHandler(onBack = goBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (step) {
                            SetupStep.CHOOSE -> if (viewModel.isChange.value) "Change passphrase" else "Encrypt backups"
                            SetupStep.GENERATED, SetupStep.OWN -> "Your passphrase"
                            else -> "Confirm passphrase"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = goBack) {
                        if (step == SetupStep.CHOOSE) Icon(Icons.Filled.Close, contentDescription = "Close")
                        else Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (step) {
                SetupStep.CHOOSE -> ChooseStep(viewModel)
                SetupStep.GENERATED -> GeneratedStep(viewModel)
                SetupStep.OWN -> OwnStep(viewModel)
                SetupStep.CONFIRM, SetupStep.SAVING, SetupStep.DONE -> ConfirmStep(viewModel)
            }
        }
    }
}

@Composable
private fun ChooseStep(viewModel: PassphraseSetupViewModel) {
    Text("Choose a backup passphrase. You need it to restore an encrypted backup, on this or any other device.")
    if (viewModel.isChange.value) {
        Text("Backups you've already made keep the passphrase they were made with.")
    }
    OutlinedCard(
        onClick = { viewModel.generate() },
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Generate one (recommended)", fontWeight = FontWeight.Bold)
            Text("20 random words. As strong as the encryption itself.")
        }
    }
    OutlinedCard(onClick = { viewModel.useOwn() }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Use my own", fontWeight = FontWeight.Bold)
            Text("At least ${BackupPassphrase.MINIMUM_LENGTH} characters. Your backups are exactly as strong as it is.")
        }
    }
    NoRecoveryNotice()
}

@Composable
private fun GeneratedStep(viewModel: PassphraseSetupViewModel) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }
    val words = viewModel.generatedWords.value
    Text("Write the words down in this order.")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        words.chunked(4).forEachIndexed { row, rowWords ->
            Row(Modifier.fillMaxWidth()) {
                rowWords.forEachIndexed { column, word ->
                    Text(
                        text = "${row * 4 + column + 1} $word",
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    OutlinedButton(
        onClick = {
            copySensitive(context, viewModel.generatedText())
            copied = true
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(Icons.Outlined.ContentCopy, contentDescription = null)
        Text(if (copied) "  Copied" else "  Copy")
    }
    Text(
        "Copy copies the words without their numbers. Keep the passphrase somewhere safe, away from your backups.",
        style = MaterialTheme.typography.bodySmall,
    )
    Button(onClick = { viewModel.generatedNext() }, modifier = Modifier.fillMaxWidth()) { Text("Next") }
}

@Composable
private fun OwnStep(viewModel: PassphraseSetupViewModel) {
    val check = viewModel.ownCheck.value
    PassphraseField(
        value = viewModel.own.value,
        onValueChange = viewModel::onOwnChange,
        label = "Backup passphrase",
        isError = viewModel.showOwnError.value,
    )
    val length = viewModel.ownLength.value
    Text(
        text = when {
            viewModel.showOwnError.value && check == BackupPassphrase.OwnCheck.UNUSABLE_CHARACTER ->
                "This passphrase contains a character that can't be used."
            viewModel.showOwnError.value -> "Use at least ${BackupPassphrase.MINIMUM_LENGTH} characters."
            length < BackupPassphrase.MINIMUM_LENGTH -> "$length of ${BackupPassphrase.MINIMUM_LENGTH} characters"
            else -> "$length characters"
        },
        color = if (viewModel.showOwnError.value) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodySmall,
    )
    Text("Your backups are exactly as strong as this passphrase. Gnoseis can't estimate its strength on this device.")
    Button(onClick = { viewModel.ownNext() }, modifier = Modifier.fillMaxWidth()) { Text("Next") }

    if (viewModel.showUseAnyway.value) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUseAnyway() },
            title = { Text("Strength can't be checked") },
            text = { Text("Gnoseis can't estimate how hard this passphrase is to guess. Use one only you know, that you haven't used anywhere else.") },
            confirmButton = { TextButton(onClick = { viewModel.useAnyway() }) { Text("Use it anyway") } },
            dismissButton = { TextButton(onClick = { viewModel.dismissUseAnyway() }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ConfirmStep(viewModel: PassphraseSetupViewModel) {
    val saving = viewModel.step.value != SetupStep.CONFIRM
    Text("Enter the passphrase once more, typed or pasted from where you keep it.")
    PassphraseField(
        value = viewModel.confirm.value,
        onValueChange = viewModel::onConfirmChange,
        label = "Backup passphrase",
        isError = viewModel.confirmMismatch.value,
    )
    if (viewModel.confirmMismatch.value) {
        Text(
            "That doesn't match. Check the words and their order.",
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (viewModel.saveFailed.value) {
        Text("Couldn't turn on encryption. Try again.", color = MaterialTheme.colorScheme.error)
    }
    NoRecoveryNotice()
    if (saving) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator(modifier = Modifier.padding(4.dp))
            Text("Setting up…")
        }
    } else {
        Button(onClick = { viewModel.turnOn() }, modifier = Modifier.fillMaxWidth()) {
            Text(if (viewModel.isChange.value) "Change passphrase" else "Turn on encryption")
        }
    }
}

@Composable
private fun NoRecoveryNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            "If you lose the passphrase, your encrypted backups can't be opened. Gnoseis can't reset or recover it.",
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(12.dp),
        )
    }
}

/** A passphrase text field: no suggestions or learning by the keyboard, hidden until shown. */
@Composable
fun PassphraseField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isError: Boolean,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                if (visible) Icon(Icons.Outlined.VisibilityOff, contentDescription = "Hide passphrase")
                else Icon(Icons.Outlined.Visibility, contentDescription = "Show passphrase")
            }
        },
        modifier = modifier,
    )
}

/** Keeps the current screen out of screenshots and the recent-apps preview while shown. */
@Composable
fun SecureWindow() {
    val context = LocalContext.current
    DisposableEffect(context) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}

private fun Context.findActivity(): Activity? =
    generateSequence(this) { (it as? ContextWrapper)?.baseContext }.filterIsInstance<Activity>().firstOrNull()

/** Copies [text] and marks it sensitive, so the system doesn't show it in its clipboard preview. */
private fun copySensitive(context: Context, text: String) {
    val clip = ClipData.newPlainText("Backup passphrase", text)
    // ClipDescription.EXTRA_IS_SENSITIVE (API 33); the key is read the same way on older versions
    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}
