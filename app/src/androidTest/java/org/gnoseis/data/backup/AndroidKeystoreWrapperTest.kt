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

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.SecureRandom

/** The real Android Keystore wrapping the stored backup key (#66). Needs a device or emulator. */
@RunWith(AndroidJUnit4::class)
class AndroidKeystoreWrapperTest {
    private val wrapper = AndroidKeystoreWrapper(alias = "gnoseis_backup_key_test")
    private val associatedData = "test".toByteArray()
    private val plain = ByteArray(76).also(SecureRandom()::nextBytes)

    @After
    fun tearDown() = wrapper.delete()

    @Test
    fun roundTrip() {
        assertArrayEquals(plain, wrapper.unwrap(wrapper.wrap(plain, associatedData), associatedData))
    }

    @Test
    fun wrapped_isNotThePlainBytes_andUsesANewIvEachTime() {
        val first = wrapper.wrap(plain, associatedData)
        val second = wrapper.wrap(plain, associatedData)

        assertEquals(12 + plain.size + 16, first.size)
        assertFalse(first.copyOfRange(12, 12 + plain.size).contentEquals(plain))
        assertFalse(first.copyOf(12).contentEquals(second.copyOf(12)))
    }

    @Test
    fun otherAssociatedData_failsToUnwrap() {
        val wrapped = wrapper.wrap(plain, associatedData)
        assertTrue(runCatching { wrapper.unwrap(wrapped, "other".toByteArray()) }.isFailure)
    }

    @Test
    fun afterDelete_unwrapFails() {
        val wrapped = wrapper.wrap(plain, associatedData)
        wrapper.delete()
        assertTrue(runCatching { wrapper.unwrap(wrapped, associatedData) }.isFailure)
    }

    @Test
    fun storedKey_onTheDevice_roundTripsAndTurnsOff() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val file = File(context.cacheDir, "stored_key_test")
        val stored = StoredBackupKey(file, wrapper)
        val masterKey = ByteArray(32).also(SecureRandom()::nextBytes)
        val kdfSalt = ByteArray(32).also(SecureRandom()::nextBytes)

        stored.keep(masterKey, kdfSalt, BackupFormat.WRITTEN_KDF)
        val state = stored.state() as StoredBackupKey.On
        assertArrayEquals(masterKey, state.masterKey)
        assertArrayEquals(kdfSalt, state.kdfSalt)

        stored.turnOff()
        assertEquals(StoredBackupKey.Off, stored.state())
        assertFalse(file.exists())
    }
}
