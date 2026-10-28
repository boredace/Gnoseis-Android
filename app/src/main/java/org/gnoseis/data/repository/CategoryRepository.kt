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

package org.gnoseis.data.repository

import kotlinx.coroutines.flow.Flow
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.category.CategoryDao

/**
 * [now] gives the system time (UTC epoch ms) stamped on inserts and updates (#50).
 */
class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun getCategory(id: String): Category = categoryDao.getCategory(id)
    fun getCategoryStream(id: String): Flow<Category> = categoryDao.getCategoryFlow(id)
    fun getCategoriesStream(): Flow<List<Category>> = categoryDao.getCategoriesFlow()
    suspend fun getCategoryByRowId (rowId: Long) : Category = categoryDao.getCategoryByRowId(rowId)
    suspend fun addCategory(category: Category): Long =
        now().let { categoryDao.addCategory(category.copy(createdAt = it, updatedAt = it)) }
    /** Saves the category's editable fields. Returns false if nothing changed. */
    suspend fun updateCategory(category: Category): Boolean = categoryDao.updateCategory(
        id = category.id,
        categoryName = category.categoryName,
        comments = category.comments,
        now = now(),
    ) > 0
    suspend fun deleteCategoryById(categoryId: String) = categoryDao.deleteCategoryById(categoryId)
}