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

package org.gnoseis.data.entity.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.gnoseis.data.database.GnoseisDatabase
import org.gnoseis.testutil.inMemoryDatabase
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsDaoTest {
    private val db: GnoseisDatabase = inMemoryDatabase()
    private val dao = db.settingsDao()

    @After
    fun tearDown() = db.close()

    @Test
    fun missingRow_readsAsNull() = runTest {
        assertNull(dao.getValueInt(SettingKey.LAST_BACKUP_AT.id))
    }

    @Test
    fun upsert_insertsThenReplaces() = runTest {
        dao.upsert(SettingKey.LAST_BACKUP_AT.row(1))
        dao.upsert(SettingKey.LAST_BACKUP_AT.row(2))

        assertEquals(2L, dao.getValueInt(SettingKey.LAST_BACKUP_AT.id))
        assertNull(dao.getValueInt(SettingKey.LAST_DELETION_AT.id))
        val rows = db.openHelper.writableDatabase.query("SELECT settingId, settingName FROM Settings").use {
            buildList { while (it.moveToNext()) add(it.getInt(0) to it.getString(1)) }
        }
        assertEquals(listOf(2 to "lastBackupAt"), rows)
    }

    @Test
    fun flow_emitsNullThenEachNewValue() = runTest {
        dao.getValueIntFlow(SettingKey.LAST_DELETION_AT.id).test {
            assertNull(awaitItem())
            dao.upsert(SettingKey.LAST_DELETION_AT.row(7))
            assertEquals(7L, awaitItem())
        }
    }

    @Test
    fun keys_haveTheFixedIds() {
        assertEquals(
            listOf(1 to "appId", 2 to "lastBackupAt", 3 to "lastDeletionAt"),
            SettingKey.entries.map { it.id to it.settingName },
        )
    }
}
