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

import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.HKDFParameters
import org.junit.Assert.assertArrayEquals
import org.junit.Test
import kotlin.random.Random

/**
 * HKDF written on the platform's Mac. The SHA-256 answers are RFC 5869's test cases 1–3; the
 * SHA-512 answers use the same inputs and were computed with Python's hmac module. The backup
 * file key is also checked against Bouncy Castle's own HKDF.
 */
class HkdfKnownAnswerTest {

    private val case1 = Triple(ByteArray(22) { 0x0b }, bytes(0x00, 0x0d), bytes(0xf0, 0xfa))
    private val case2 = Triple(bytes(0x00, 0x50), bytes(0x60, 0xb0), bytes(0xb0, 0x100))
    private val case3 = Triple(ByteArray(22) { 0x0b }, ByteArray(0), ByteArray(0))

    @Test
    fun sha256_rfc5869_case1() = check("HmacSHA256", case1, 42,
        "3cb25f25faacd57a90434f64d0362f2a2d2d0a90cf1a5a4c5db02d56ecc4c5bf34007208d5b887185865")

    @Test
    fun sha256_rfc5869_case2() = check("HmacSHA256", case2, 82,
        "b11e398dc80327a1c8e7f78c596a49344f012eda2d4efad8a050cc4c19afa97c" +
            "59045a99cac7827271cb41c65e590e09da3275600c2f09b8367793a9aca3db71" +
            "cc30c58179ec3e87c14c01d5c1f3434f1d87")

    @Test
    fun sha256_rfc5869_case3_noSaltNoInfo() = check("HmacSHA256", case3, 42,
        "8da4e775a563c18f715f802a063c5a31b8a11f5c5ee1879ec3454e5f3c738d2d9d201395faa4b61a96c8")

    @Test
    fun sha512_case1() = check("HmacSHA512", case1, 42,
        "832390086cda71fb47625bb5ceb168e4c8e26a1a16ed34d9fc7fe92c1481579338da362cb8d9f925d7cb")

    @Test
    fun sha512_case2() = check("HmacSHA512", case2, 82,
        "ce6c97192805b346e6161e821ed165673b84f400a2b514b2fe23d84cd189ddf1" +
            "b695b48cbd1c8388441137b3ce28f16aa64ba33ba466b24df6cfcb021ecff235" +
            "f6a2056ce3af1de44d572097a8505d9e7a93")

    @Test
    fun sha512_case3_noSaltNoInfo() = check("HmacSHA512", case3, 42,
        "f5fa02b18298a72a8c23898a8703472c6eb179dc204c03425c970e3b164bf90fff22d04836d0e2343bac")

    @Test
    fun fileKey_knownAnswer() {
        assertArrayEquals(
            hex("e2619ed8d54741e07efce6adafe2c342d8d6df28833c612f623caca8e778ca9f"),
            BackupKeys.deriveFileKey(bytes(0x00, 0x20), bytes(0x20, 0x40)),
        )
    }

    @Test
    fun fileKey_matchesBouncyCastleHkdf() {
        val random = Random(66)
        repeat(20) {
            val master = random.nextBytes(32)
            val fileSalt = random.nextBytes(32)
            val expected = ByteArray(32)
            HKDFBytesGenerator(SHA512Digest()).apply {
                init(HKDFParameters(master, fileSalt, "org.gnoseis.backup/v1/file-key".toByteArray()))
            }.generateBytes(expected, 0, expected.size)

            assertArrayEquals(expected, BackupKeys.deriveFileKey(master, fileSalt))
        }
    }

    private fun check(algorithm: String, case: Triple<ByteArray, ByteArray, ByteArray>, length: Int, okm: String) =
        assertArrayEquals(hex(okm), BackupKeys.hkdf(algorithm, case.first, case.second, case.third, length))

    /** Bytes from [from] up to, not including, [until]. */
    private fun bytes(from: Int, until: Int) = ByteArray(until - from) { (from + it).toByte() }

    private fun hex(text: String): ByteArray =
        ByteArray(text.length / 2) { text.substring(it * 2, it * 2 + 2).toInt(16).toByte() }
}
