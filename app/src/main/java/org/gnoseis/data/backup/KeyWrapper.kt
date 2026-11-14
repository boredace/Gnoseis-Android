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

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Seals small secrets with a key that can't be read out of the device (#66). */
interface KeyWrapper {
    /** Returns [plain] sealed, IV included. */
    fun wrap(plain: ByteArray, associatedData: ByteArray): ByteArray

    /** Opens what [wrap] returned; throws if the key is gone or the bytes were changed. */
    fun unwrap(wrapped: ByteArray, associatedData: ByteArray): ByteArray

    /** Deletes the wrapping key. Anything it wrapped can't be opened any more. */
    fun delete()
}

/**
 * Wraps with an AES-256-GCM key kept in the Android Keystore. The key never leaves the
 * Keystore and has no user authentication (no biometric or lock screen prompt); the Keystore
 * picks a random IV for every wrap.
 */
class AndroidKeystoreWrapper(private val alias: String = ALIAS) : KeyWrapper {

    override fun wrap(plain: ByteArray, associatedData: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.ENCRYPT_MODE, key(create = true))
        cipher.updateAAD(associatedData)
        val sealed = cipher.doFinal(plain)
        val iv = cipher.iv
        check(iv.size == IV_SIZE)
        return ByteBuffer.allocate(IV_SIZE + sealed.size).put(iv).put(sealed).array()
    }

    override fun unwrap(wrapped: ByteArray, associatedData: ByteArray): ByteArray {
        require(wrapped.size > IV_SIZE + TAG_BITS / 8)
        val key = key(create = false) ?: throw IllegalStateException("The wrapping key is gone")
        val cipher = Cipher.getInstance(AES_GCM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, wrapped, 0, IV_SIZE))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(wrapped, IV_SIZE, wrapped.size - IV_SIZE)
    }

    override fun delete() {
        keyStore().run { if (containsAlias(alias)) deleteEntry(alias) }
    }

    private fun key(create: Boolean): SecretKey? {
        (keyStore().getEntry(alias, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        if (!create) return null
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build()
            )
        }.generateKey()
    }

    private fun keyStore() = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    companion object {
        const val ALIAS = "gnoseis_backup_key"
        private const val KEYSTORE = "AndroidKeyStore"
        private const val AES_GCM = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_BITS = 128
    }
}
