/*
*   Gnoseis is an Android native CRM application and general knowledge manager.
*
*   Copyright (C) 2024-2026 Gnoseis.org
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
*   https://github.com/boredace/Gnoseis-Android/blob/main/LICENSE.
*
*   Dual Licensing Notice:
*
*   Gnoseis can also be licensed under commercial terms. See
*   https://github.com/boredace/Gnoseis-Android/blob/main/COMMERCIAL_LICENSE
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
import java.time.ZoneId

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

    @Test
    fun migration1To2_keepsOneLinkPerPairOfRecords() {
        val path = context.getDatabasePath(dbName(1)).apply { parentFile!!.mkdirs() }
        createFromSchema(path, 1)
        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            fun link(id: String, record1Id: String, record2Id: String) = db.execSQL(
                "INSERT INTO LinkedRecord (id, ownerDbId, record1Id, record2Id, record1TypeId, record2TypeId) " +
                        "VALUES (?, 'db1', ?, ?, 2, 1)",
                arrayOf(id, record1Id, record2Id),
            )
            link("first", "contact", "note")
            link("same-pair", "contact", "note")
            link("reversed", "note", "contact")
            link("other-pair", "contact", "note2")
        }

        val db = Room.databaseBuilder(context, GnoseisDatabase::class.java, dbName(1))
            .addMigrations(*GnoseisDatabase.MIGRATIONS)
            .build()
        val remaining = db.openHelper.writableDatabase.query("SELECT id FROM LinkedRecord ORDER BY id").use {
            buildList { while (it.moveToNext()) add(it.getString(0)) }
        }
        db.close()

        assertEquals(listOf("first", "other-pair"), remaining)
    }

    /** #31: a database from a build before the public repository has an undeclared index. */
    @Test
    fun migration1To2_dropsIndexesNoEntityDeclares() {
        val path = context.getDatabasePath(dbName(1)).apply { parentFile!!.mkdirs() }
        createFromSchema(path, 1)
        SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("CREATE INDEX idx_LinkedRecord ON LinkedRecord (id, record1Id, record2Id)")
        }

        val db = Room.databaseBuilder(context, GnoseisDatabase::class.java, dbName(1))
            .addMigrations(*GnoseisDatabase.MIGRATIONS)
            .build()
        val indexes = db.openHelper.writableDatabase // runs migrations, then validates
            .query("SELECT name FROM sqlite_master WHERE type = 'index' AND sql IS NOT NULL").use {
                buildList { while (it.moveToNext()) add(it.getString(0)) }
            }
        db.close()

        assertEquals(emptyList<String>(), indexes)
    }

    /**
     * #50: rows from before v3 keep their data and get NULL timestamps, from v1 and from v2.
     * #51: the Settings table is created empty.
     */
    @Test
    fun migrationTo3_keepsRows_withNullTimestamps() {
        for (version in listOf(1, 2)) {
            val path = context.getDatabasePath(dbName(version)).apply { parentFile!!.mkdirs() }
            createFromSchema(path, version)
            SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                db.execSQL("INSERT INTO Note (id, ownerDbId, title, createDateTime, createDate) VALUES ('n', 'db1', 'kept', 7, 0)")
                db.execSQL("INSERT INTO Contact (id, ownerDbId, nameLast) VALUES ('c', 'db1', 'Doe')")
                db.execSQL("INSERT INTO Organization (id, ownerDbId, organizationName) VALUES ('o', 'db1', 'Acme')")
                db.execSQL("INSERT INTO Category (id, ownerDbId, categoryName) VALUES ('g', 'db1', 'Work')")
                db.execSQL("INSERT INTO Item (id, ownerDbId, itemName) VALUES ('i', 'db1', 'Laptop')")
                db.execSQL(
                    "INSERT INTO LinkedRecord (id, ownerDbId, record1Id, record2Id, record1TypeId, record2TypeId) " +
                            "VALUES ('l', 'db1', 'c', 'n', 2, 1)"
                )
            }

            val db = Room.databaseBuilder(context, GnoseisDatabase::class.java, dbName(version))
                .addMigrations(*GnoseisDatabase.MIGRATIONS)
                .build()
            val sql = db.openHelper.writableDatabase
            fun nonNullTimestamps(table: String, columns: String) =
                sql.query("SELECT COUNT(*) FROM $table WHERE COALESCE($columns) IS NOT NULL").use {
                    it.moveToFirst(); it.getInt(0)
                }
            val title = sql.query("SELECT title FROM Note WHERE id = 'n'").use { it.moveToFirst(); it.getString(0) }
            val settingsRows = sql.query("SELECT COUNT(*) FROM Settings").use { it.moveToFirst(); it.getInt(0) }
            val counts = listOf("Note", "Contact", "Organization", "Category", "Item")
                .map { nonNullTimestamps(it, "createdAt, updatedAt") } +
                    nonNullTimestamps("LinkedRecord", "createdAt, NULL")
            db.close()

            assertEquals("from v$version", "kept", title)
            assertEquals("from v$version", List(6) { 0 }, counts)
            assertEquals("from v$version", 0, settingsRows)
        }
    }

    /**
     * #68: note times are rewritten from the local clock time stored as UTC to UTC, in the
     * upgrading device's zone (here America/Toronto), from v3 and from v1. Nothing else changes.
     */
    @Test
    fun migrationTo4_convertsNoteTimes_andKeepsEverythingElse() {
        val toronto = ZoneId.of("America/Toronto")
        // v3 createDateTime, v3 createDate, v4 createDateTime, v4 createDate (the shared examples)
        val notes = mapOf(
            "summer" to listOf(1784125800000, 1784073600000, 1784140200000, 1784088000000),
            "winter" to listOf(1768487400000, 1768435200000, 1768505400000, 1768453200000),
            "no-time" to listOf(1794614400000, 1794614400000, 1794632400000, 1794632400000),
            "gap" to listOf(1772937000000, 1772928000000, 1772955000000, 1772946000000),
            "overlap" to listOf(1793496600000, 1793491200000, 1793511000000, 1793505600000),
        )
        for (version in listOf(1, 3)) {
            val path = context.getDatabasePath(dbName(version)).apply { parentFile!!.mkdirs() }
            createFromSchema(path, version)
            SQLiteDatabase.openDatabase(path.path, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                val stamps = if (version >= 3) ", createdAt, updatedAt" else ""
                notes.forEach { (id, t) ->
                    val values = if (version >= 3) ", 11, 22" else ""
                    db.execSQL(
                        "INSERT INTO Note (id, ownerDbId, title, textContents, createDateTime, createDate$stamps) " +
                                "VALUES ('$id', 'db1', 'title $id', 'text $id', ${t[0]}, ${t[1]}$values)"
                    )
                }
                db.execSQL("INSERT INTO Contact (id, ownerDbId, nameLast) VALUES ('c', 'db1', 'Doe')")
                db.execSQL(
                    "INSERT INTO LinkedRecord (id, ownerDbId, record1Id, record2Id, record1TypeId, record2TypeId) " +
                            "VALUES ('l', 'db1', 'c', 'summer', 2, 1)"
                )
                if (version >= 3) db.execSQL("INSERT INTO Settings (settingId, settingName, valueInt) VALUES (1, 'lastBackupAt', 33)")
            }

            val db = Room.databaseBuilder(context, GnoseisDatabase::class.java, dbName(version))
                .addMigrations(*GnoseisDatabase.migrations { toronto })
                .build()
            val sql = db.openHelper.writableDatabase // runs migrations, then validates
            fun rows(query: String) = sql.query(query).use { c ->
                buildList { while (c.moveToNext()) add((0 until c.columnCount).joinToString("|") { c.getString(it) ?: "null" }) }
            }
            val noteRows = rows("SELECT id, ownerDbId, title, textContents, createDateTime, createDate, createdAt, updatedAt FROM Note ORDER BY id")
            val linkRows = rows("SELECT id, ownerDbId, record1Id, record2Id, record1TypeId, record2TypeId, createdAt FROM LinkedRecord")
            val contactRows = rows("SELECT id, nameLast, createdAt, updatedAt FROM Contact")
            val settingsRows = rows("SELECT settingId, settingName, valueInt, valueText FROM Settings")
            val dbVersion = sql.version
            db.close()

            val stamps = if (version >= 3) "11|22" else "null|null"
            assertEquals("from v$version", 4, dbVersion)
            assertEquals(
                "from v$version",
                notes.toSortedMap().map { (id, t) -> "$id|db1|title $id|text $id|${t[2]}|${t[3]}|$stamps" },
                noteRows,
            )
            assertEquals("from v$version", listOf("l|db1|c|summer|2|1|null"), linkRows)
            assertEquals("from v$version", listOf("c|Doe|null|null"), contactRows)
            assertEquals(
                "from v$version",
                if (version >= 3) listOf("1|lastBackupAt|33|null") else emptyList<String>(),
                settingsRows,
            )
        }
    }

    /** #68: the v4 schema has the same tables as v3, so the identity hash is unchanged. */
    @Test
    fun schema4_hasTheSameIdentityHashAsSchema3() {
        fun hash(version: Int) =
            JSONObject(File(schemaDir, "$version.json").readText()).getJSONObject("database").getString("identityHash")
        assertEquals(hash(3), hash(4))
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
