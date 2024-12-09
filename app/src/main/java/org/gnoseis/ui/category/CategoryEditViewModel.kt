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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.repository.CategoryRepository

class CategoryEditViewModel(
    savedStateHandle: SavedStateHandle,
    private val categoryRepository: CategoryRepository

) : ViewModel(){
    private var categoryId : String = checkNotNull(savedStateHandle[CategoryEditDestination.categoryIdArg])
    private var isValid = MutableStateFlow(false)
    private var isNew = MutableStateFlow(true)
    private var editCategoryName = MutableStateFlow("")
    private var editCategoryDescription = MutableStateFlow( "")
    private var category = MutableStateFlow(Category(ownerDbId = "new-new", categoryName="new-new"))

    val categoryEditPageState : StateFlow<CategoryEditState> =
        combine(
            isValid,
            isNew,
            editCategoryName,
            editCategoryDescription,
            category
        ){
            isValid, isNew, editCategoryName, editCategoryDescription, newCategory ->
            CategoryEditState(
                isValid = isValid,
                isNew = isNew,
                editCategoryName = editCategoryName,
                editCategoryDescription = editCategoryDescription,
                category = newCategory
            )
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            CategoryEditState()
        )

    init {
        if (categoryId != "new") {
            viewModelScope.launch {
                categoryRepository.getCategory(categoryId).let {
                    isValid.value = true
                    isNew.value = false
                    editCategoryName.value = it.categoryName
                    editCategoryDescription.value = it.comments ?: ""
                    category.value = it
                }
            }
        }
    }
    suspend fun onSave(): String? {
        if(isValid.value && isNew.value) {
            val addedCategoryRowId = categoryRepository.addCategory(
                Category(
                    ownerDbId = "db1",
                    categoryName = editCategoryName.value,
                    comments = editCategoryDescription.value
                )
            )
            return categoryRepository.getCategoryByRowId(addedCategoryRowId).id
        } else if (isValid.value && !isNew.value) {
            categoryRepository.updateCategory(
                category.value.copy(
                    categoryName = editCategoryName.value,
                    comments = editCategoryDescription.value
                )
            )
            return null
        } else {
            return null
        }
    }

    fun updateCategoryName(it: String) {
        editCategoryName.value = it
        isValid.value = validateInput()
    }

    fun updateCategoryDescription(it: String) {
        editCategoryDescription.value = it
        isValid.value = validateInput()
    }

    private fun validateInput():Boolean {
        var tempVal = false
        if(editCategoryName.value.isNotEmpty()) tempVal = true
//        if(editCategoryDescription.value.isNotEmpty()) tempVal = true
        return tempVal
    }

    data class CategoryEditState(
        val isValid: Boolean = false,
        val isNew: Boolean = true,
        val editCategoryName: String? = null,
        val editCategoryDescription: String? = null,
        val category: Category? = Category(
            ownerDbId = "ppp",
            categoryName = "new-name",
            comments = null
        )
    )


    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}