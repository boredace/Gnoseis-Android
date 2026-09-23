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

import android.database.sqlite.SQLiteConstraintException
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.testutil.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.categoryDao()

    @After
    fun tearDown() = db.close()

    @Test
    fun add_thenGetById_andByRowId() = runTest {
        val category = Category(categoryName = "Work", comments = "c")

        val rowId = dao.addCategory(category)

        assertEquals(category, dao.getCategory(category.id))
        assertEquals(category, dao.getCategoryByRowId(rowId))
        assertEquals(category, dao.getCategoryFlow(category.id).first())
    }

    @Test
    fun getById_unknownId_returnsNull_despiteNonNullReturnType() = runTest {
        val result: Category? = dao.getCategory("missing")
        assertNull(result)
        assertNull(dao.getCategoryFlow("missing").first())
    }

    @Test
    fun add_duplicateId_throws() = runTest {
        val category = Category(categoryName = "a")
        dao.addCategory(category)

        assertThrows(SQLiteConstraintException::class.java) {
            kotlinx.coroutines.runBlocking { dao.addCategory(category.copy(categoryName = "b")) }
        }
    }

    @Test
    fun update_changesStoredRecord() = runTest {
        val category = Category(categoryName = "old")
        dao.addCategory(category)

        dao.updateCategory(category.copy(categoryName = "new", comments = "x"))

        assertEquals(category.copy(categoryName = "new", comments = "x"), dao.getCategory(category.id))
    }

    @Test
    fun update_unknownRecord_isNoOp() = runTest {
        dao.updateCategory(Category(categoryName = "ghost"))
        assertEquals(emptyList<Category>(), dao.getCategoriesFlow().first())
    }

    @Test
    fun deleteById_removesOnlyThatRecord() = runTest {
        val a = Category(categoryName = "a")
        val b = Category(categoryName = "b")
        dao.addCategory(a)
        dao.addCategory(b)

        dao.deleteCategoryById(a.id)

        assertEquals(listOf(b), dao.getCategoriesFlow().first())
    }

    @Test
    fun list_isSortedByName_caseSensitively() = runTest {
        // SQLite's default BINARY collation: upper case sorts before lower case.
        listOf("banana", "Cherry", "apple", "Banana").forEach { dao.addCategory(Category(categoryName = it)) }

        val names = dao.getCategoriesFlow().first().map { it.categoryName }

        assertEquals(listOf("Banana", "Cherry", "apple", "banana"), names)
    }

    @Test
    fun listFlow_emitsOnInsertUpdateDelete() = runTest {
        val category = Category(categoryName = "a")
        dao.getCategoriesFlow().test {
            assertEquals(emptyList<Category>(), awaitItem())
            dao.addCategory(category)
            assertEquals(listOf(category), awaitItem())
            dao.updateCategory(category.copy(categoryName = "b"))
            assertEquals(listOf(category.copy(categoryName = "b")), awaitItem())
            dao.deleteCategoryById(category.id)
            assertEquals(emptyList<Category>(), awaitItem())
        }
    }

    @Test
    fun singleRecordFlow_emitsNullAfterDelete() = runTest {
        val category = Category(categoryName = "a")
        dao.addCategory(category)
        dao.getCategoryFlow(category.id).test {
            assertEquals(category, awaitItem())
            dao.deleteCategoryById(category.id)
            assertNull(awaitItem())
        }
    }
}
