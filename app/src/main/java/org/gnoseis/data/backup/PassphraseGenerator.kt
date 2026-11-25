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

import android.content.Context
import org.gnoseis.R
import java.io.InputStream
import java.security.SecureRandom

/**
 * The EFF large word list (CC BY 4.0, https://www.eff.org/dice): 7,776 words, about 12.9
 * bits each. The raw resource is EFF's file unchanged, a dice number and a word on each line.
 */
object EffWordList {
    const val SIZE = 7_776

    fun load(context: Context): List<String> =
        context.resources.openRawResource(R.raw.eff_large_wordlist).use(::parse)

    internal fun parse(input: InputStream): List<String> =
        input.bufferedReader(Charsets.US_ASCII).lineSequence()
            .filter { it.isNotBlank() }
            .map { it.substringAfter('\t').trimEnd('\r') }
            .toList()
            .also { check(it.size == SIZE) { "Word list has ${it.size} words" } }
}

/**
 * Generates a backup passphrase (#66): always [WORDS] words from the EFF large word list,
 * drawn with the system's secure random generator (258 bits), lower case, one space apart.
 * The order of the words is part of the passphrase.
 */
class PassphraseGenerator(
    private val words: List<String>,
    private val random: SecureRandom = SecureRandom(),
) {
    init {
        require(words.size == EffWordList.SIZE)
    }

    /** A new passphrase. The caller clears it when done. */
    fun generate(): CharArray {
        // nextInt(bound) rejects out-of-range draws, so every word is equally likely
        val picked = IntArray(WORDS) { random.nextInt(words.size) }
        val passphrase = CharArray(picked.sumOf { words[it].length } + WORDS - 1)
        var position = 0
        picked.forEachIndexed { n, index ->
            if (n > 0) passphrase[position++] = ' '
            words[index].toCharArray(passphrase, position)
            position += words[index].length
        }
        picked.fill(0)
        return passphrase
    }

    companion object {
        const val WORDS = 20
    }
}
