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

package org.gnoseis.ui.item

import org.gnoseis.ui.record.saveRecord
import org.gnoseis.ui.record.RecordEditScaffold
import org.gnoseis.ui.record.RecordEditNavigation
import android.annotation.SuppressLint
import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.serialization.Serializable
import org.gnoseis.AppViewModelProvider
import org.gnoseis.data.constants.TITLE_LENGTH_ITEM
import org.gnoseis.data.enums.ItemEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.ui.theme.GnoseisTheme

@Serializable
data class ItemEditRoute(
    val itemId: String?,
    val pageMode: ItemEditPageMode,
    val linkFromType: RecordType? = null,
    val linkFromId: String? = null
)

const val TAG = "ItemEditPage"
@Composable
fun ItemEditPage(
    pageMode: ItemEditPageMode,
    pageViewModel: ItemEditViewModel = viewModel(factory = AppViewModelProvider.Factory),
    navigation: RecordEditNavigation,
) {
    val pageState = pageViewModel.pageState.value
    val editState = pageViewModel.editState.value
    val coroutineScope = rememberCoroutineScope()

    RecordEditScaffold(
        recordType = RecordType.Item,
        isNew = pageMode != ItemEditPageMode.EDIT,
        linkSource = pageState.linkSource,
        saveEnabled = editState.isValid,
        onSave = {
            coroutineScope.saveRecord(
                save = pageViewModel::onSave,
                linksNew = pageMode == ItemEditPageMode.NEWLINK,
                navigation = navigation,
            )
        },
        onBack = navigation.back,
    ) {
        ItemEditBody(
            editState = editState,
            onEvent = { pageViewModel.onEvent(it) },
        )
    }
}

@SuppressLint("SuspiciousIndentation")
@Composable
fun ItemEditBody(
    editState: ItemEditViewModel.EditState,
    onEvent: (ItemEditPageEvent) -> Unit,
){
    Column(
       modifier = Modifier
           .padding(start = 5.dp, end = 5.dp, bottom = 5.dp)
           .verticalScroll(rememberScrollState())
    ){

        ItemTextField(
            label = "Item Name",
            value = editState.editItemName?:"" ,
            onValueChanged = {
                if(it.length <= TITLE_LENGTH_ITEM)
                    onEvent(ItemEditPageEvent.ItemNameChanged(it))},
            type = TextFieldType.WORD)

        Spacer(modifier = Modifier.height(10.dp))

         OutlinedTextField(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(),
            label = {Text(text="Item Description")},
            value = editState.editItemDescription?:"",
            onValueChange = {
                onEvent(ItemEditPageEvent.ItemDescriptionChanged(it))
            },
            singleLine = false,
            minLines = 10
        )

    }
}

enum class TextFieldType {
    WORD,
    EMAIL,
    PHONE
}
@Composable
fun ItemTextField(
    label: String,
    value: String,
    onValueChanged: (String) -> Unit,
    type: TextFieldType
){
    OutlinedTextField(
        modifier = Modifier.fillMaxWidth(),
        label = {Text(text=label)},
        value = value,
        onValueChange = {onValueChanged(it) },
        singleLine = true,
        keyboardOptions =
        when (type) {
            TextFieldType.WORD -> {
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Text
                )
            }
            TextFieldType.EMAIL -> {
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Email
                )
            }
            TextFieldType.PHONE -> {
                KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrect = false,
                    keyboardType = KeyboardType.Phone
                )
            }
        },
//            isError = pageState.phoneMainValidationResult?.successful  != null
    )
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ItemEditPagePreview() {
    GnoseisTheme {
        Surface {
            RecordEditScaffold(
                recordType = RecordType.Item,
                isNew = true,
                linkSource = null,
                saveEnabled = true,
                onSave = {},
                onBack = {},
            ) {
                ItemEditBody(
                    editState = ItemEditViewModel.EditState(isValid = false, editItemName = "this is fun"),
                    onEvent = {},
                )
            }
        }
    }
}
