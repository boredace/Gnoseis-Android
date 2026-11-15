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

import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.file.Files
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * #68: schema version 4 rewrites every note's times. Before Room upgrades a live database from
 * an older version, the file is copied as it is, with its `-wal` and `-shm` files, next to it as
 * `<name>.before-v4-<yyyy-MM-dd_HH-mm-ss>` (the device's local time). The copy is never deleted
 * by the app.
 *
 * The live files are only read, never opened as a database: the version comes from the
 * database header. A version change can still be in the `-wal` file after the app was stopped
 * before SQLite moved it into the database, so when there is one, the copy is made first and
 * opened to read the real version, and dropped if that is already 4 or later.
 */
internal object BeforeV4Copy {
    private const val UPGRADED_VERSION = 4
    private val STAMP: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    private val SIDE_FILES = listOf("-wal", "-shm")
    private val HEADER = "SQLite format 3\u0000".toByteArray(Charsets.US_ASCII)
    private const val USER_VERSION_OFFSET = 60

    /**
     * Copies [database] if it exists and its version is from 1 to 3, and returns the copy of the
     * main file; returns null when no copy is needed. Throws if the copy can't be made or read,
     * so the caller doesn't open (and upgrade) the database without a copy. Partial copies are
     * removed before throwing.
     */
    fun keep(database: File, now: LocalDateTime = LocalDateTime.now()): File? {
        if (!database.exists()) return null
        // Not an SQLite file: nothing Room could upgrade
        val headerVersion = headerVersion(database) ?: return null
        val wal = File(database.path + "-wal").let { it.exists() && it.length() > 0 }
        if (headerVersion >= UPGRADED_VERSION || (headerVersion == 0 && !wal)) return null

        val copy = File(database.parentFile, "${database.name}.before-v$UPGRADED_VERSION-${now.format(STAMP)}")
        val pairs = listOf(database to copy) + SIDE_FILES.map { File(database.path + it) to File(copy.path + it) }
        val made = mutableListOf<File>()
        try {
            for ((from, to) in pairs) {
                if (!from.exists()) continue
                Files.copy(from.toPath(), to.toPath()) // fails if the target already exists
                made += to
            }
            if (wal) {
                val version = try {
                    SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
                } catch (e: Exception) {
                    throw IOException("Couldn't read the copy's version", e)
                }
                if (version !in 1 until UPGRADED_VERSION) {
                    made.forEach { it.delete() }
                    return null
                }
            }
        } catch (e: IOException) {
            made.forEach { it.delete() }
            throw IOException("Couldn't keep a copy of ${database.name} before upgrading it", e)
        }
        return copy
    }

    /** PRAGMA user_version as stored in the header of [database], or null if it isn't SQLite. */
    private fun headerVersion(database: File): Int? {
        val header = ByteArray(USER_VERSION_OFFSET + 4)
        val read = database.inputStream().use { input ->
            var total = 0
            while (total < header.size) {
                val n = input.read(header, total, header.size - total)
                if (n < 0) break
                total += n
            }
            total
        }
        if (read < header.size || !header.copyOf(HEADER.size).contentEquals(HEADER)) return null
        return ByteBuffer.wrap(header, USER_VERSION_OFFSET, 4).int
    }
}
