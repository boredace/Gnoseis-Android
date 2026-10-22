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
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.category.CategoryDao
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.contact.ContactDao
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.item.ItemDao
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.entity.links.LinkedRecordDao
import org.gnoseis.data.entity.links.NoteItemDao
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.note.NoteDao
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.entity.organization.OrganizationDao
import org.gnoseis.data.entity.search.SearchDao
import org.gnoseis.data.entity.sharing.Incoming
import org.gnoseis.data.entity.sharing.SharedRecord

@Database(
    entities = [
        Category::class,
        Contact::class,
        org.gnoseis.data.entity.sharing.Database::class,
        Incoming::class,
        Item::class,
        LinkedRecord::class,
        Note::class,
        Organization::class,
        SharedRecord::class
    ],
    version = 2
)
abstract class GnoseisDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun contactDao(): ContactDao
    abstract fun itemDao(): ItemDao
    abstract fun noteDao(): NoteDao
    abstract fun organizationDao(): OrganizationDao
    abstract fun noteItemDao(): NoteItemDao
    abstract fun linkedRecordDao(): LinkedRecordDao
    abstract fun searchDao(): SearchDao
    fun closeDatabase() {
        this.close()
    }


    companion object {
        /**
         * #23: before #13, the same two records could be linked more than once, in either
         * direction. Keeps the first link of each unordered pair and deletes the rest.
         * The schema itself is unchanged.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM LinkedRecord WHERE rowid NOT IN (" +
                            "SELECT MIN(rowid) FROM LinkedRecord " +
                            "GROUP BY MIN(record1Id, record2Id), MAX(record1Id, record2Id))"
                )
            }
        }

        /**
         * Every schema change must bump `version` and add a Migration here. Each version's
         * schema is exported to app/schemas and MigrationTest opens every one of them.
         * Declared after the migrations it lists, which are initialized in order.
         */
        internal val MIGRATIONS: Array<Migration> = arrayOf(
            MIGRATION_1_2,
        )

        @Volatile
        private var Instance: GnoseisDatabase? = null

        fun getDatabase(context: Context): GnoseisDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context, GnoseisDatabase::class.java, "gnoseis_data")
                    /**
                     * No fallbackToDestructiveMigration(): it silently deletes all user data
                     * whenever no migration path exists (#18). Opening a database without a
                     * path now fails instead, leaving the file untouched.
                     */
                    .addMigrations(*MIGRATIONS)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                Instance?.noteDao()?.addNote(
                                    Note(
                                    ownerDbId = "yyy",
                                    title = "This is a default note",
                                    textContents = "This note is created when application was initialized. Feel free to delete or edit it as you please.",
                                    )
                                )
                            }
                        }
                    })
                    .build()
                    .also { Instance = it }
            }
        }

        /**
         * Closes and forgets the singleton so each test starts from a fresh database file.
         */
        @VisibleForTesting
        internal fun resetInstanceForTesting() {
            synchronized(this) {
                Instance?.close()
                Instance = null
            }
        }

    }
}