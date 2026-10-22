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

package org.gnoseis.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * #18: every schema exported to app/schemas must open with the current GnoseisDatabase using
 * only GnoseisDatabase.MIGRATIONS. Room checks the migrated tables against the current
 * entities when it opens the database, so a schema change without a matching Migration fails
 * here instead of on users' devices.
 *
 * MigrationTestHelper reads schemas from instrumentation assets, which Robolectric unit tests
 * cannot see, so each exported schema is recreated from its JSON directly.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    // Set from app/build.gradle.kts
    private val schemaDir = File(System.getProperty("room.schemaLocation")!!, GnoseisDatabase::class.java.name)

    private val exportedVersions: List<Int> =
        schemaDir.listFiles()!!.map { it.nameWithoutExtension.toInt() }.sorted()

    // @Database is not retained at runtime, so ask a freshly created database.
    private val currentVersion: Int =
        Room.inMemoryDatabaseBuilder(context, GnoseisDatabase::class.java).build().run {
            openHelper.writableDatabase.version.also { close() }
        }

    @After
    fun tearDown() {
        exportedVersions.forEach { context.deleteDatabase(dbName(it)) }
    }

    @Test
    fun currentSchema_isExported() {
        assertEquals(currentVersion, exportedVersions.last())
    }

    @Test
    fun everyExportedVersion_opensWithCurrentDatabase() {
        for (version in exportedVersions) {
            val path = context.getDatabasePath(dbName(version)).apply { parentFile!!.mkdirs() }
            createFromSchema(path, version)

            Room.databaseBuilder(context, GnoseisDatabase::class.java, dbName(version))
                .addMigrations(*GnoseisDatabase.MIGRATIONS)
                .build()
                .apply { openHelper.writableDatabase } // runs migrations, then validates
                .close()
        }
    }

    private fun dbName(version: Int) = "migration-from-$version"

    /** Creates a database exactly as Room would have created it at [version]. */
    private fun createFromSchema(path: File, version: Int) {
        val schema = JSONObject(File(schemaDir, "$version.json").readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            schema.getJSONArray("entities").forEachObject { entity ->
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", table))
                entity.optJSONArray("indices")?.forEachObject { index ->
                    db.execSQL(index.getString("createSql").replace("\${TABLE_NAME}", table))
                }
            }
            schema.optJSONArray("views")?.forEachObject { view ->
                db.execSQL(view.getString("createSql").replace("\${VIEW_NAME}", view.getString("viewName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) db.execSQL(setup.getString(i))
            db.version = version
        }
    }

    private fun JSONArray.forEachObject(action: (JSONObject) -> Unit) {
        for (i in 0 until length()) action(getJSONObject(i))
    }
}
