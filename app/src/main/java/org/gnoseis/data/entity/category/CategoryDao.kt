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

package org.gnoseis.data.entity.category

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM Category WHERE id=:id")
    suspend fun getCategory(id: String) : Category
    @Query("SELECT * FROM Category WHERE id=:id")
    fun getCategoryFlow(id: String) : Flow<Category>
    @Query("SELECT * FROM Category ORDER BY categoryName ASC")
    fun getCategoriesFlow(): Flow<List<Category>>
    @Query("SELECT * FROM Category WHERE _rowid_ = :rowId")
    suspend fun getCategoryByRowId(rowId: Long) : Category
    @Insert
    suspend fun addCategory(item: Category): Long
    @Update
    suspend fun updateCategory(category: Category)
    @Query (
        "DELETE FROM Category WHERE id = :categoryId"
    )
    suspend fun deleteCategoryById(categoryId: String)
}
