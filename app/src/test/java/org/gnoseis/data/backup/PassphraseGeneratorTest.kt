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

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.MessageDigest

/** The EFF word list as shipped, and the 20-word generator (#66). */
@RunWith(AndroidJUnit4::class)
class PassphraseGeneratorTest {
    private val words = EffWordList.load(ApplicationProvider.getApplicationContext())

    @Test
    fun wordList_isEffsFileUnchanged() {
        val file = File("src/main/res/raw/eff_large_wordlist.txt")
        val sha256 = MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }

        assertEquals("addd35536511597a02fa0a9ff1e5284677b8883b83e986e43f15a3db996b903e", sha256)
    }

    @Test
    fun wordList_has7776DistinctLowerCaseWords() {
        assertEquals(7_776, words.size)
        assertEquals(7_776, words.toSet().size)
        assertTrue(words.all { it.matches(Regex("[a-z-]+")) })
        assertEquals("abacus", words.first())
        assertEquals("zoom", words.last())
    }

    @Test
    fun generated_isTwentyWordsFromTheList() {
        val list = words.toSet()
        repeat(200) {
            val parts = String(PassphraseGenerator(words).generate()).split(' ')
            assertEquals(20, parts.size)
            assertTrue(parts.all { it in list })
        }
    }

    @Test
    fun generated_isAlreadyNormalised_andLongEnough() {
        repeat(200) {
            val passphrase = PassphraseGenerator(words).generate()
            val normalised = BackupPassphrase.normalise(passphrase) as BackupPassphrase.Normalised

            assertArrayEquals(String(passphrase).toByteArray(Charsets.UTF_8), normalised.bytes)
            assertEquals(BackupPassphrase.OwnCheck.OK, BackupPassphrase.checkOwn(passphrase))
        }
    }

    @Test
    fun generated_differsEachTime() {
        val generator = PassphraseGenerator(words)
        assertNotEquals(String(generator.generate()), String(generator.generate()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun generator_needsTheWholeList() {
        PassphraseGenerator(words.drop(1))
    }
}
