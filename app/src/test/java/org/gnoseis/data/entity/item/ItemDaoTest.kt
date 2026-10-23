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

package org.gnoseis.data.entity.item

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.testutil.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ItemDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.itemDao()

    @After
    fun tearDown() = db.close()

    @Test
    fun add_get_update_delete() = runTest {
        val item = Item(itemName = "Laptop", comments = "c", parentId = "p")
        val rowId = dao.addItem(item)
        assertEquals(item, dao.getItem(item.id))
        assertEquals(item, dao.getItemByRowId(rowId))

        dao.updateItem(item.copy(itemName = "Desktop"))
        assertEquals("Desktop", dao.getItemFlow(item.id).first().itemName)

        dao.deleteItemById(item.id)
        val deleted: Item? = dao.getItem(item.id)
        assertNull(deleted)
    }

    @Test
    fun list_isSortedByName() = runTest {
        listOf("b", "c", "a").forEach { dao.addItem(Item(itemName = it)) }
        assertEquals(listOf("a", "b", "c"), dao.getItemsFlow().first().map { it.itemName })
    }

    @Test
    fun listFlow_emitsOnChanges() = runTest {
        val item = Item(itemName = "x")
        dao.getItemsFlow().test {
            assertEquals(emptyList<Item>(), awaitItem())
            dao.addItem(item)
            assertEquals(listOf(item), awaitItem())
            dao.deleteItemById(item.id)
            assertEquals(emptyList<Item>(), awaitItem())
        }
    }
}
