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

import java.nio.CharBuffer
import java.text.Normalizer

/**
 * Turns a backup passphrase into the bytes the key is derived from (#66). Every client does
 * exactly this, so a passphrase typed on one device opens a backup made on another:
 * 1. refuse an unpaired surrogate or a noncharacter (U+FDD0–FDEF, any code point ending in
 *    FFFE or FFFF);
 * 2. Unicode NFKC;
 * 3. remove white space at both ends and turn every run inside into one U+0020, where white
 *    space is exactly the 25 code points in [isWhiteSpace]. The platform's own tests
 *    (isWhitespace, trim, strip, regex \s) disagree with that list and with each other;
 * 4. UTF-8.
 * Case is kept.
 *
 * The caller owns the passphrase CharArray and clears it. Intermediate arrays are cleared here;
 * NFKC's result is a String, which can't be cleared, no worse than the text field the
 * passphrase was typed into.
 */
object BackupPassphrase {
    /** Fewest code points, after normalising, of a passphrase the user picks. */
    const val MINIMUM_LENGTH = 12

    sealed interface Result

    /** The UTF-8 [bytes] to derive the key from, and their length in code points. */
    class Normalised(val bytes: ByteArray, val codePoints: Int) : Result

    /** Contains a character that can't be used. On restore this counts as a wrong passphrase. */
    data object Refused : Result

    enum class OwnCheck { OK, TOO_SHORT, UNUSABLE_CHARACTER }

    fun normalise(passphrase: CharArray): Result {
        if (!hasOnlyUsableCharacters(passphrase)) return Refused
        val nfkc = Normalizer.normalize(CharBuffer.wrap(passphrase), Normalizer.Form.NFKC)
        val collapsed = CharArray(nfkc.length)
        var length = 0
        var codePoints = 0
        var spacePending = false
        var i = 0
        while (i < nfkc.length) {
            val codePoint = nfkc.codePointAt(i)
            val charCount = Character.charCount(codePoint)
            if (isWhiteSpace(codePoint)) {
                spacePending = length > 0 // drops white space at the start
            } else {
                if (spacePending) {
                    collapsed[length++] = ' '
                    codePoints++
                    spacePending = false
                }
                for (k in 0 until charCount) collapsed[length++] = nfkc[i + k]
                codePoints++
            }
            i += charCount
        }
        // white space at the end is dropped by never writing the pending space
        val encoded = Charsets.UTF_8.newEncoder().encode(CharBuffer.wrap(collapsed, 0, length))
        collapsed.fill('\u0000')
        val bytes = ByteArray(encoded.remaining()).also { encoded.get(it) }
        if (encoded.hasArray()) encoded.array().fill(0)
        return Normalised(bytes, codePoints)
    }

    /** Checks a passphrase the user picks for themselves. */
    fun checkOwn(passphrase: CharArray): OwnCheck =
        when (val result = normalise(passphrase)) {
            Refused -> OwnCheck.UNUSABLE_CHARACTER
            is Normalised -> {
                result.bytes.fill(0)
                if (result.codePoints < MINIMUM_LENGTH) OwnCheck.TOO_SHORT else OwnCheck.OK
            }
        }

    /** Unicode's White_Space property: exactly these 25 code points, on every client. */
    internal fun isWhiteSpace(codePoint: Int): Boolean = when (codePoint) {
        in 0x0009..0x000D, 0x0020, 0x0085, 0x00A0, 0x1680,
        in 0x2000..0x200A, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000 -> true
        else -> false
    }

    private fun hasOnlyUsableCharacters(text: CharArray): Boolean {
        var i = 0
        while (i < text.size) {
            val char = text[i]
            val codePoint = when {
                Character.isHighSurrogate(char) -> {
                    if (i + 1 >= text.size || !Character.isLowSurrogate(text[i + 1])) return false
                    Character.toCodePoint(char, text[++i])
                }
                Character.isLowSurrogate(char) -> return false
                else -> char.code
            }
            if (codePoint in 0xFDD0..0xFDEF || codePoint and 0xFFFE == 0xFFFE) return false
            i++
        }
        return true
    }
}
