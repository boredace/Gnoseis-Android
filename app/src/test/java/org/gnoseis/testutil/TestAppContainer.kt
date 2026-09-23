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

package org.gnoseis.testutil

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.gnoseis.data.AppContainer
import org.gnoseis.data.AppDataContainer
import org.gnoseis.data.database.GnoseisDatabase

/**
 * In-memory [GnoseisDatabase] without the seed-note callback, so every test starts empty.
 */
fun inMemoryDatabase(
    context: Context = ApplicationProvider.getApplicationContext(),
): GnoseisDatabase =
    Room.inMemoryDatabaseBuilder(context, GnoseisDatabase::class.java)
        .allowMainThreadQueries()
        .build()

/**
 * [AppContainer] wired exactly like production, but backed by an in-memory database.
 * Call [close] in @After.
 */
class TestAppContainer(
    context: Context = ApplicationProvider.getApplicationContext(),
    val database: GnoseisDatabase = inMemoryDatabase(context),
) : AppContainer by AppDataContainer(context, { database }) {
    fun close() = database.close()
}
