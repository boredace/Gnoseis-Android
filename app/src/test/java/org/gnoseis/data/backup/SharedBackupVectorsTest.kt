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

package org.gnoseis.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.gnoseis.data.backup.BackupPassphrase.OwnCheck
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.MessageDigest

/**
 * Test vectors shared byte for byte with Gnoseis-Windows, which runs the same files through its
 * own code: `backup/backup-key-vectors-v1.json` (normalising, Argon2id and HKDF together) and
 * `backup/passphrase-normalising-v1.json` (normalising alone, written by hand from design doc 08
 * section 5.1). This repository holds the originals.
 */
class SharedBackupVectorsTest {

    @Test
    fun keyVectors_fileIsUnchanged() =
        assertEquals(KEY_VECTORS_SHA256, sha256Hex(resource(KEY_VECTORS)))

    @Test
    fun normalisingList_fileIsUnchanged() =
        assertEquals(NORMALISING_SHA256, sha256Hex(resource(NORMALISING)))

    @Test
    fun keyVectors_giveTheSameKeys() {
        val vectors = json(KEY_VECTORS)["vectors"]!!.jsonArray.map { it.jsonObject }
        assertEquals(3, vectors.size)
        for (vector in vectors) {
            val name = vector.string("name")
            val normalised = BackupPassphrase.normalise(vector.string("passphrase").toCharArray())
            assertTrue(name, normalised is BackupPassphrase.Normalised)
            val bytes = (normalised as BackupPassphrase.Normalised).bytes
            assertEquals(name, vector.string("normalisedHex"), hex(bytes))

            val kdf = KdfParameters(vector.int("memoryKiB"), vector.int("iterations"), vector.int("parallelism"))
            val master = BackupKeys.deriveMasterKey(bytes, unhex(vector.string("kdfSaltHex")), kdf)
            assertEquals(name, vector.string("masterKeyHex"), hex(master))

            val fileKey = BackupKeys.deriveFileKey(master, unhex(vector.string("fileSaltHex")))
            assertEquals(name, vector.string("fileKeyHex"), hex(fileKey))
        }
    }

    @Test
    fun normalisingList_givesTheExpectedBytes() {
        val entries = json(NORMALISING)["entries"]!!.jsonArray.map { it.jsonObject }
        assertTrue(entries.size >= 30)
        entries.forEachIndexed { index, entry ->
            val input = entry.string("input")
            val label = "entry $index: ${describe(input)}"
            val expected = entry.string("expectedHex")
            val actual = when (val result = BackupPassphrase.normalise(input.toCharArray())) {
                BackupPassphrase.Refused -> "refused"
                is BackupPassphrase.Normalised -> hex(result.bytes)
            }
            assertEquals(label, expected, actual)

            val meetsMinimum = entry["meetsMinimum"]!!.jsonPrimitive.boolean
            assertEquals(label, meetsMinimum, BackupPassphrase.checkOwn(input.toCharArray()) == OwnCheck.OK)
        }
    }

    private fun resource(name: String): ByteArray =
        javaClass.classLoader!!.getResourceAsStream("backup/$name")!!.use { it.readBytes() }

    private fun json(name: String): JsonObject =
        Json.parseToJsonElement(resource(name).toString(Charsets.UTF_8)).jsonObject

    private fun JsonObject.string(key: String) = this[key]!!.jsonPrimitive.content

    private fun JsonObject.int(key: String) = this[key]!!.jsonPrimitive.int

    private fun describe(text: String) = text.map { "U+%04X".format(it.code) }.joinToString(" ")

    private fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it) }

    private fun unhex(text: String) = ByteArray(text.length / 2) { text.substring(2 * it, 2 * it + 2).toInt(16).toByte() }

    private fun sha256Hex(bytes: ByteArray) = hex(MessageDigest.getInstance("SHA-256").digest(bytes))

    companion object {
        const val KEY_VECTORS = "backup-key-vectors-v1.json"
        const val NORMALISING = "passphrase-normalising-v1.json"
        const val KEY_VECTORS_SHA256 = "0c25c32770876e1a1fa42d71cbd5cd4a90a1c6b9f6787a7ec1d956be9cd3787d"
        const val NORMALISING_SHA256 = "7bee1e4ad9fca064e19a0186ddd331d83b5c9953873cdd072c7b5a5a43eebee9"
    }
}
