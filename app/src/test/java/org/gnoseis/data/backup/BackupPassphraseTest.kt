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

import org.gnoseis.data.backup.BackupPassphrase.OwnCheck
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Passphrase normalising and the minimum length, the same on every client (#66). */
class BackupPassphraseTest {

    // ---------- NFKC ----------

    @Test
    fun composedAndDecomposedForms_giveTheSameBytes() {
        assertArrayEquals(bytesOf("café crème"), bytesOf("café crème"))
        assertArrayEquals("café".toByteArray(), bytesOf("café"))
    }

    @Test
    fun fullWidthCharacters_becomeTheirUsualForm() =
        assertNormalises("ABC 123", "ＡＢＣ　１２３")

    @Test
    fun caseIsKept() = assertNormalises("AbC dEf", "AbC dEf")

    @Test
    fun spacingAccent_leavesTheCombiningAccentAfterTrimming() =
        assertArrayEquals(byteArrayOf(0xCC.toByte(), 0x81.toByte(), 0x61, 0x62, 0x63), bytesOf("´abc"))

    // ---------- White space ----------

    @Test
    fun leadingAndTrailingSpaces_areRemoved() = assertNormalises("abc def", "  abc def \t ")

    @Test
    fun doubleSpaces_becomeOne() = assertNormalises("abc def ghi", "abc  def     ghi")

    @Test
    fun lineBreaks_becomeOneSpace() {
        assertNormalises("abc def ghi", "abc\ndef\r\nghi")
        assertNormalises("abc def", "abc \r\n\t def\n")
    }

    @Test
    fun unusualWhiteSpaceInside_countsAsWhiteSpace() {
        for (space in listOf("\u0085", " ", " ", " ", " ", " ", " ", " ", "\u000B", "\u000C")) {
            assertNormalises("a b", "a${space}b")
            assertNormalises("a b", "${space}a $space  b$space")
        }
    }

    @Test
    fun controlAndZeroWidthCharacters_areKept() {
        assertArrayEquals(byteArrayOf(0x61, 0x1F, 0x62), bytesOf("a\u001Fb"))
        assertArrayEquals(byteArrayOf(0x61, 0xE2.toByte(), 0x80.toByte(), 0x8B.toByte(), 0x62), bytesOf("a​b"))
        assertArrayEquals(byteArrayOf(0x1C, 0x61), bytesOf("\u001Ca"))
    }

    @Test
    fun whiteSpace_isExactlyTheFixedList() {
        val expected = (0x09..0x0D) + 0x20 + 0x85 + 0xA0 + 0x1680 + (0x2000..0x200A) +
            0x2028 + 0x2029 + 0x202F + 0x205F + 0x3000
        val found = (0..0x10FFFF).filter(BackupPassphrase::isWhiteSpace)

        assertEquals(25, found.size)
        assertEquals(expected, found)
    }

    @Test
    fun onlyWhiteSpace_isEmpty() {
        val result = BackupPassphrase.normalise(" \t\n ".toCharArray()) as BackupPassphrase.Normalised
        assertEquals(0, result.bytes.size)
        assertEquals(0, result.codePoints)
    }

    // ---------- Refused characters ----------

    @Test
    fun unpairedSurrogates_areRefused() {
        assertRefused("a\uD800b")
        assertRefused("a\uDC00b")
        assertRefused("ab\uD83D")
        assertRefused("\uDE00\uD83D") // low then high
    }

    @Test
    fun noncharacters_areRefused() {
        assertRefused("a￾")
        assertRefused("a￿")
        assertRefused("﷐abc")
        assertRefused("abc﷯")
        assertRefused("a🿾") // U+1FFFE
        assertRefused("a􏿿") // U+10FFFF
    }

    @Test
    fun pairedSurrogates_areKept() =
        assertArrayEquals("a😀".toByteArray(Charsets.UTF_8), bytesOf("a😀"))

    // ---------- Minimum length ----------

    @Test
    fun underTwelveCharacters_isTooShort() {
        assertEquals(OwnCheck.TOO_SHORT, check("abcdefghijk"))
        assertEquals(OwnCheck.TOO_SHORT, check(""))
    }

    @Test
    fun twelveCharacters_isEnough() = assertEquals(OwnCheck.OK, check("abcdefghijkl"))

    @Test
    fun minimum_countsCodePoints_notUtf16Units() {
        assertEquals(OwnCheck.OK, check("abcdefghijk😀"))
        assertEquals(12, (BackupPassphrase.normalise("abcdefghijk😀".toCharArray()) as BackupPassphrase.Normalised).codePoints)
    }

    @Test
    fun minimum_isCountedAfterNormalising() {
        assertEquals(OwnCheck.TOO_SHORT, check(" abcdefghijk "))
        assertEquals(OwnCheck.TOO_SHORT, check("abcde   fghij"))
        assertEquals(OwnCheck.OK, check("abcde   fghijk"))
    }

    @Test
    fun unusableCharacter_isReportedOnSetup() = assertEquals(OwnCheck.UNUSABLE_CHARACTER, check("abcdefghijkl￾"))

    @Test
    fun normalise_leavesTheCallersPassphraseAlone() {
        val passphrase = "  Abc  def  ".toCharArray()
        BackupPassphrase.normalise(passphrase)
        assertTrue(passphrase.contentEquals("  Abc  def  ".toCharArray()))
        assertFalse(passphrase.all { it == '\u0000' })
    }

    // ---------- Helpers ----------

    private fun bytesOf(text: String): ByteArray =
        (BackupPassphrase.normalise(text.toCharArray()) as BackupPassphrase.Normalised).bytes

    private fun assertNormalises(expected: String, text: String) =
        assertEquals(expected, String(bytesOf(text), Charsets.UTF_8))

    private fun assertRefused(text: String) =
        assertEquals(BackupPassphrase.Refused, BackupPassphrase.normalise(text.toCharArray()))

    private fun check(text: String) = BackupPassphrase.checkOwn(text.toCharArray())
}
