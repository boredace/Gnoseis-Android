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

package org.gnoseis.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stands in for the Android Keystore, which Robolectric doesn't have: AES-256-GCM with a key
 * held in memory. [failUnwrap] makes every unwrap fail, as if the Keystore key were lost.
 */
class FakeKeyWrapper : KeyWrapper {
    private var key: SecretKey? = null
    var failUnwrap = false
    var deleted = 0
        private set

    override fun wrap(plain: ByteArray, associatedData: ByteArray): ByteArray {
        val key = key ?: KeyGenerator.getInstance("AES").apply { init(256) }.generateKey().also { key = it }
        val iv = ByteArray(12).also(SecureRandom()::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        cipher.updateAAD(associatedData)
        return iv + cipher.doFinal(plain)
    }

    override fun unwrap(wrapped: ByteArray, associatedData: ByteArray): ByteArray {
        check(!failUnwrap) { "Unwrap failed" }
        val key = key ?: throw IllegalStateException("The wrapping key is gone")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, wrapped, 0, 12))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(wrapped, 12, wrapped.size - 12)
    }

    override fun delete() {
        key = null
        deleted++
    }
}
