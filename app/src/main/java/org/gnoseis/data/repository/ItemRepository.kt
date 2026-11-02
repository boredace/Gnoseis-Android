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
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.item.ItemDao

/**
 * [now] gives the system time (UTC epoch ms) stamped on inserts and updates (#50).
 */
class ItemRepository(
    private val itemDao: ItemDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    suspend fun getItem(id: String): Item = itemDao.getItem(id)
    fun getItemStream(id: String): Flow<Item> = itemDao.getItemFlow(id)
    fun getItemsStream(): Flow<List<Item>> = itemDao.getItemsFlow()
    suspend fun getItemByRowId(id: Long) : Item = itemDao.getItemByRowId(id)

    /** Saves the item's editable fields. Returns false if nothing changed. */
    suspend fun updateItem(item: Item): Boolean = itemDao.updateItem(
        id = item.id,
        itemName = item.itemName,
        comments = item.comments,
        now = now(),
    ) > 0
    suspend fun addItem(item: Item): Long =
        now().let { itemDao.addItem(item.copy(createdAt = it, updatedAt = it)) }
    suspend fun deleteItemById(recordId: String) = itemDao.deleteItemById(recordId)
}