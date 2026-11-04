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

package org.gnoseis.ui.category

import org.gnoseis.data.constants.TITLE_LENGTH_CATEGORY
import org.gnoseis.ui.record.prefillName
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType
import org.gnoseis.data.repository.CategoryRepository
import org.gnoseis.data.repository.LinkedRecordRepository
import org.gnoseis.domain.usecase.GetLinkSourceUseCase
import org.gnoseis.domain.usecase.LinkSource

class CategoryEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository,
    private val linkedRecordRepository: LinkedRecordRepository,
    private val getLinkSource: GetLinkSourceUseCase,

    ) : ViewModel(){

    private val categoryId : String? = savedStateHandle.toRoute<CategoryEditRoute>().categoryId

    private val _editState = mutableStateOf(
        EditState()
    )
    val editState: State<EditState> = _editState

    private val _pageState = mutableStateOf(
        PageState(
            isNew = true,
            pageMode = checkNotNull(savedStateHandle.toRoute<CategoryEditRoute>().pageMode),
            categoryId =  null,
            linkFromType = savedStateHandle.toRoute<CategoryEditRoute>().linkFromType,
            linkFromId = savedStateHandle.toRoute<CategoryEditRoute>().linkFromId,
            category = null
        )
    )
    val pageState: State<PageState> = _pageState

    init {
        val route = savedStateHandle.toRoute<CategoryEditRoute>()
        if (_pageState.value.pageMode != RecordEditPageMode.EDIT) {
            prefillName(route.prefillName, TITLE_LENGTH_CATEGORY)?.let { name ->
                _editState.value = _editState.value.copy(editCategoryName = name, isValid = validateInput(name))
                _pageState.value = _pageState.value.copy(namePrefilled = true)
            }
        }
        val linkFromType = _pageState.value.linkFromType
        val linkFromId = _pageState.value.linkFromId
        if (_pageState.value.pageMode == RecordEditPageMode.NEWLINK && linkFromType != null && linkFromId != null) {
            viewModelScope.launch {
                _pageState.value = _pageState.value.copy(linkSource = getLinkSource(linkFromType, linkFromId))
            }
        }
        if (!categoryId.isNullOrEmpty()) {
            viewModelScope.launch {
                categoryRepository.getCategory(categoryId).let {

                    _pageState.value = _pageState.value.copy(
                        isNew = false,
                        pageMode = checkNotNull(savedStateHandle.toRoute<CategoryEditRoute>().pageMode),
                        categoryId = it.id,
                        linkFromType = savedStateHandle.toRoute<CategoryEditRoute>().linkFromType,
                        linkFromId = savedStateHandle.toRoute<CategoryEditRoute>().linkFromId,
                        category = it
                    )

                    _editState.value = _editState.value.copy(
                        isValid = true,
                        editCategoryName = it.categoryName,
                        editCategoryDescription = it.comments,
                    )
                }
            }
        }
    }

    suspend fun onSave(): String? {
        if(
        // Add new Note
            _editState.value.isValid &&
            _pageState.value.isNew
        ) {
            val addedRecordRowId = categoryRepository.addCategory(
                Category(
                    ownerDbId = "db1",
                    categoryName = _editState.value.editCategoryName!!, // assuming cannot be valid if null
                    comments =_editState.value.editCategoryDescription,
                )
            )
            val addedRecordId = categoryRepository.getCategoryByRowId(addedRecordRowId).id
            if (
            // Also link to source record
                _pageState.value.pageMode == RecordEditPageMode.NEWLINK &&
                _pageState.value.linkFromId != null &&
                _pageState.value.linkFromType != null
            ){
                linkedRecordRepository.addLinkedRecord(
                    LinkedRecord(
                        ownerDbId = "db1",
                        record1Id = _pageState.value.linkFromId!!,
                        record2Id = addedRecordId,
                        record1TypeId = _pageState.value.linkFromType!!.recordTypeId,
                        record2TypeId = RecordType.Category.recordTypeId
                    )
                )
            }
            return addedRecordId
        } else if (
        // Update Existing Category
            _editState.value.isValid &&
            !_pageState.value.isNew
        ) {
            categoryRepository.updateCategory(_pageState.value.category!!.copy(
                categoryName = _editState.value.editCategoryName!!,  // assuming cannot be valid if null
                comments = _editState.value.editCategoryDescription,
            ))
            return null
        } else {
            return null
        }
    }

    private var debounceJob: Job? = null

    fun updateCategoryName(it: String) {
        // update editCategoryName only immediately
        _editState.value = _editState.value.copy(editCategoryName = it)
        debounceJob?.cancel()
        // perform validations and update only when stopped typing
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            _editState.value = _editState.value.copy(
                isValid = validateInput(it),
                editCategoryName = it,
            )
        }
    }

    fun updateCategoryDescription(it: String) {
        // update editCategoryDescription only immediately
        _editState.value = _editState.value.copy(editCategoryDescription = it)
        debounceJob?.cancel()
        // perform validations and update only when stopped typing
        debounceJob = CoroutineScope(Dispatchers.Main).launch {
            delay(500)
            _editState.value = _editState.value.copy(
                isValid = validateInput(_editState.value.editCategoryName),
                editCategoryDescription = it,
            )
        }
    }

    private fun validateInput(categoryName: String?):Boolean {
        var result = false
        if(!categoryName.isNullOrEmpty()) result = true
        return result
    }

    data class PageState(
        val isNew: Boolean = true,
        val pageMode: RecordEditPageMode? = null,
        val categoryId: String? = null,
        val linkFromType: RecordType? = null,
        val linkFromId: String? = null,
        val linkSource: LinkSource? = null,
        /** The name came from a search (#10): the page starts in its field. */
        val namePrefilled: Boolean = false,
        val category: Category? = Category(),
    )

    data class EditState(
        val isValid: Boolean = false,
        val editCategoryName: String? = null,
        val editCategoryDescription: String? = null,
    )


    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}