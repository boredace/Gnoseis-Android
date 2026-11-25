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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package org.gnoseis.ui.note

import org.gnoseis.ui.components.PrefillableTextField
import org.gnoseis.ui.record.saveRecord
import org.gnoseis.ui.record.RecordEditScaffold
import org.gnoseis.ui.record.RecordEditNavigation
import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.constants.TITLE_LENGTH_NOTE
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.theme.GnoseisTheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.ZoneOffset

/**
 * Material's DatePicker works in UTC: a picked date is UTC midnight of that date, in epoch ms.
 * The note's local day goes in and comes out through UTC, never through the device's zone,
 * or the day would be off by one west or east of UTC (#68).
 */
internal fun datePickerMillis(day: LocalDate): Long = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/** The day picked in Material's DatePicker, from its UTC [millis]. */
internal fun datePickerDay(millis: Long): LocalDate =
    java.time.Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditPage(
    pageMode: RecordEditPageMode,
    pageViewModel: NoteEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigation: RecordEditNavigation,
) {
    val pageState = pageViewModel.pageState.value
    val editState = pageViewModel.editState.value
    val coroutineScope = rememberCoroutineScope()

    RecordEditScaffold(
        recordType = RecordType.Note,
        isNew = pageMode != RecordEditPageMode.EDIT,
        linkSource = pageState.linkSource,
        saveEnabled = editState.isValid,
        onSave = {
            coroutineScope.saveRecord(
                save = pageViewModel::onSave,
                linksNew = pageMode == RecordEditPageMode.NEWLINK,
                navigation = navigation,
            )
        },
        onBack = navigation.back,
    ) {
        KeyboardAware {
            NoteEditBody(
                editState = editState,
                onDateChanged = { pageViewModel.updateDate(it) },
                onTimeChanged = { pageViewModel.updateTime(it) },
                onTextContentChange = { pageViewModel.updateTextContent(it) },
                onTitleChange = { pageViewModel.updateTitle(it) },
                titlePrefilled = pageState.namePrefilled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditBody(
    editState: NoteEditViewModel.EditState,
    onDateChanged: (LocalDate) -> Unit,
    onTimeChanged: (LocalTime) -> Unit,
    onTextContentChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    titlePrefilled: Boolean = false,
    ){
    val title = editState.editTitle
    val textContent = editState.editTextContent
    val showDatePicker = rememberSaveable { mutableStateOf(false) }
    val showTimePicker = rememberSaveable { mutableStateOf(false) }

    Column(
       modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
    ){
        PrefillableTextField(
            modifier = Modifier.fillMaxWidth(),
            label = {Text(text="Title")},
            focusOnStart = titlePrefilled,
            value = title ?:"",
            onValueChange = {
                if(it.length <= TITLE_LENGTH_NOTE) {
                    onTitleChange(it)
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrect = true,
                keyboardType = KeyboardType.Text
            ),
        )
        Spacer(modifier = Modifier.height(10.dp))

        // #68: the note's local day and time, the time in the device's 12- or 24-hour setting
        val is24Hour = is24HourClock()
        Row(
            modifier = Modifier.fillMaxWidth(),
        ){
            OutlinedButton(
                onClick = { showDatePicker.value = true },
            ) {
                Text(text = editState.editDay.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
            }
            Spacer(modifier = Modifier.width(10.dp))
            OutlinedButton(
                onClick = { showTimePicker.value = true }
            ){
                Text(text = noteTimeText(editState.editTime, is24Hour))
            }
        }


        if(showDatePicker.value){
            MyDatePickerDialog(
                datePickerState = rememberDatePickerState(initialSelectedDateMillis = datePickerMillis(editState.editDay)),
                onDateSelected = { millis ->
                    millis?.let { onDateChanged(datePickerDay(it)) }
                    showDatePicker.value = false
                },
                onDismiss = {
                    showDatePicker.value = false
                }
            )
        }

        if(showTimePicker.value){
            // Made each time the dialog opens, so it starts at the time on the button
            // (Cancel leaves nothing behind)
            val timePickerState = rememberTimePickerState(
                initialHour = editState.editTime.hour,
                initialMinute = editState.editTime.minute,
                is24Hour = is24Hour,
            )
            MyTimePickerDialog(
                timePickerState = timePickerState,
                onConfirm = {
                    onTimeChanged(LocalTime.of(timePickerState.hour, timePickerState.minute))
                    showTimePicker.value = false
                },
                onDismiss = { showTimePicker.value = false },
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            label = {Text(text="Note Text")},
            value = textContent ?: "",
            onValueChange = {
                onTextContentChange(it)
            },
            singleLine = false,
            minLines = 10,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                autoCorrect = true,
                keyboardType = KeyboardType.Text
            ),
        )
    }
}
@Composable
fun KeyboardAware(
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.imePadding()) {
        content()
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyDatePickerDialog(
    datePickerState: DatePickerState,
    onDateSelected: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
/*
    val datePickerState = rememberDatePickerState(selectableDates = object : SelectableDates {
        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
            return utcTimeMillis <= System.currentTimeMillis()
        }
    })
*/
    DatePickerDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = {
                onDateSelected(datePickerState.selectedDateMillis)
                onDismiss()
            }

            ) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            Button(onClick = {
                onDismiss()
            }) {
                Text(text = "Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState,

        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTimePickerDialog(
    timePickerState: TimePickerState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    DatePickerDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(text = "Cancel")
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            TimePicker(
                state = timePickerState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NoteEditPagePreview() {
    GnoseisTheme {
        Surface {
            RecordEditScaffold(
                recordType = RecordType.Note,
                isNew = true,
                linkSource = null,
                saveEnabled = true,
                onSave = {},
                onBack = {},
            ) {
                NoteEditBody(
                    editState = NoteEditViewModel.EditState(isValid = true),
                    onDateChanged = {},
                    onTimeChanged = {},
                    onTextContentChange = {},
                    onTitleChange = {},
                )
            }
        }
    }
}
