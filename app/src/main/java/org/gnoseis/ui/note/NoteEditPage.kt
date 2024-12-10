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

@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3Api::class)

package org.gnoseis.ui.note

import android.content.res.Configuration
import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CalendarLocale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.constants.TITLE_LENGTH_NOTE
import org.gnoseis.data.enums.NoteEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.navigation.NavigationDestination
import org.gnoseis.ui.theme.GnoseisTheme
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

@Serializable
data class NoteEditRoute(
    val noteId: String?,
    val pageMode: NoteEditPageMode,
    val linkFromType: RecordType? = null,
    val linkFromId: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditPage(
    pageMode: NoteEditPageMode,
    pageViewModel: NoteEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navMenuClick: () -> Unit,
    navigateToNoteDetailsPage: (String) ->Unit

    ) {
    val pageState = pageViewModel.pageState.value
    val editState = pageViewModel.editState.value

    val coroutineScope = rememberCoroutineScope()
    val datePickerState by pageViewModel.datePickerState.collectAsState()

    NoteEditScaffold(
        pageState = pageState,
        editState = editState,
        datePickerState = datePickerState,
        onDateChanged = {
            pageViewModel.updateDate(it)
        },
        onNavMenuclick = navMenuClick,
        onSave = {
            coroutineScope.launch {
                val addedNoteId = pageViewModel.onSave()
                if(addedNoteId != null) {
                    if(pageMode == NoteEditPageMode.NEWLINK) {
                        navMenuClick()
                    } else {
                        navigateToNoteDetailsPage(addedNoteId)
                    }
                } else {
                    navMenuClick()
                }
            }
         },
        onTextContentChange = { pageViewModel.updateTextContent(it) },
        onTitleChange = {pageViewModel.updateTitle(it) },
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScaffold(
    pageState: NoteEditViewModel.PageState,
    editState: NoteEditViewModel.EditState,
    datePickerState: DatePickerState,
    onDateChanged: (Long) -> Unit,
    onNavMenuclick: () -> Unit,
    onSave: () -> Unit,
    onTextContentChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,


){
    val pullRefreshState = rememberPullToRefreshState()
    if (pullRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            // TODO: Do something
            delay(1500)
            pullRefreshState.endRefresh()
        }
    }
    Scaffold(
        topBar = {

            TopAppBar(
                title = { Text(text="New Note") },
                navigationIcon = {
                    IconButton(onClick = { onNavMenuclick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
//                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onSave() },
//                        enabled = pageState.isValid
                        enabled = editState.isValid

                    ) {
                        when {
                            pageState.isNew -> Text(text = "Add")
                            !pageState.isNew -> Text(text= "Save")
                        }
                    }
                }
            )
        },
        floatingActionButton = {},
        content = { innerPadding ->
            KeyboardAware {
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .nestedScroll(pullRefreshState.nestedScrollConnection)
                ) {
                    NoteEditBody(
                        editState = editState,
                        datePickerState = datePickerState,
                        onDateChanged = onDateChanged,
                        onTextContentChange = onTextContentChange,
                        onTitleChange = onTitleChange,
                    )
                }
            }

        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditBody(
    editState: NoteEditViewModel.EditState,
    datePickerState: DatePickerState,
    onDateChanged: (Long) -> Unit,
    onTextContentChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,

    ){
    val title = editState.editTitle
    val textContent = editState.editTextContent
    val showDatePicker = rememberSaveable { mutableStateOf(false) }

    Column(
       modifier = Modifier.padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
    ){
        OutlinedTextField(
            modifier = Modifier.fillMaxWidth(),
            label = {Text(text="Title")},
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


//        val datePickerState = rememberDatePickerState(
////            initialSelectedDateMillis = LocalDateTime.now().atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()
//            initialSelectedDateMillis = pageState.dateTimeHolder.dateMillis*1000
//        )


//        Log.i("XXX2" , "Edit page  = initialSelectedDateMillis = ${LocalDateTime.now().atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()}")
//        Log.i("XXX2" , "Edit page  = initialSelectedDateMillis = ${pageState.dateTimeHolder.dateMillis*1000}")

//        val timePickerState = rememberTimePickerState(
//            initialHour = pageState.dateTimeHolder?.localHour?:0,
//            initialMinute = pageState.dateTimeHolder?.localMinute?:0
//        )
//        var showTimePicker = rememberSaveable { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
        ){
            OutlinedButton(
                onClick = { showDatePicker.value = true },
            ) {
                Text(text="${Instant.ofEpochMilli(editState.editDate).toString().substring(0,10)}")
            }
            Spacer(modifier = Modifier.width(10.dp))
//            OutlinedButton(
//                onClick = { showTimePicker.value = true }
//            ){
//                Text(text="${timePickerState.hour}:${timePickerState.minute}")
//            }
        }


        if(showDatePicker.value){
            MyDatePickerDialog(
                datePickerState = datePickerState,
                onDateSelected = {
                    Log.i("XXX2", "onDateSelected | it = $it")
                    val selectedDateTime = Instant.ofEpochMilli(it?:0)
                    Log.i("xxx2", "onDateSelected | Instant.ofEpochMillis =  $selectedDateTime")
                    Log.i("xxx2", "onDateSelected | Instant.toEpochMillis =  ${selectedDateTime.toEpochMilli()}")
                    onDateChanged(selectedDateTime.toEpochMilli())
                    showDatePicker.value = false
                },
                onDismiss = {
                    showDatePicker.value = false
                }
            )
        }

//        if(showTimePicker.value){
//            MyTimePickerDialog(timePickerState = timePickerState)
//        }
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
private fun convertMillisToDate(millis: Long): String {
    val formatter = SimpleDateFormat("dd/MM/yyyy")
    return formatter.format(Date(millis))
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
) {
    TimePicker(
        state = timePickerState

    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun NoteEditPagePreview()
{
    GnoseisTheme {
        Surface {
            NoteEditScaffold(
                pageState = NoteEditViewModel.PageState(
                    isNew = true,
                    pageMode = NoteEditPageMode.NEW,
                ),
                editState = NoteEditViewModel.EditState(
                    isValid = true
                ),
                datePickerState = DatePickerState(locale = CalendarLocale.getDefault()),
                onNavMenuclick = {},
                onSave = {},
                onTextContentChange = {},
                onTitleChange = {},
                onDateChanged = {},
            )
        }
    }

}