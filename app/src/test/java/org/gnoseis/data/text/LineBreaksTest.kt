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

package org.gnoseis.data.text

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class LineBreaksTest {
    @Test
    fun loneCarriageReturn_becomesOneLineFeed() = assertEquals("a\nb", normalizeLineBreaks("a\rb"))

    @Test
    fun carriageReturnLineFeed_becomesOneLineFeed() = assertEquals("a\nb", normalizeLineBreaks("a\r\nb"))

    @Test
    fun lineFeed_staysAsItIs() = assertEquals("a\nb", normalizeLineBreaks("a\nb"))

    @Test
    fun twoCarriageReturns_areTwoLineBreaks() = assertEquals("a\n\nb", normalizeLineBreaks("a\r\rb"))

    @Test
    fun twoCarriageReturnLineFeeds_areTwoLineBreaks() = assertEquals("a\n\nb", normalizeLineBreaks("a\r\n\r\nb"))

    @Test
    fun mixedLineBreaks_eachBecomeOneLineFeed() = assertEquals("a\nb\nc\nd", normalizeLineBreaks("a\r\nb\rc\nd"))

    @Test
    fun trailingCarriageReturn_becomesALineFeed() = assertEquals("a\n", normalizeLineBreaks("a\r"))

    @Test
    fun lineFeedThenCarriageReturn_areTwoLineBreaks() = assertEquals("a\n\nb", normalizeLineBreaks("a\n\rb"))

    @Test
    fun textWithoutLineBreaks_isUnchanged() = assertEquals(" a b\t", normalizeLineBreaks(" a b\t"))

    @Test
    fun otherSeparators_areLeftAlone() =
        assertEquals("a\u2028b\u2029c\u0085d", normalizeLineBreaks("a\u2028b\u2029c\u0085d"))

    @Test
    fun emptyString_staysEmpty() = assertEquals("", normalizeLineBreaks(""))

    @Test
    fun save_editedText_hasLineFeedsOnly() = assertEquals("a\nb\nc", lineBreaksToSave("a\r\nb\rc", "old"))

    @Test
    fun save_textEqualOnceNormalised_keepsTheStoredString() =
        assertSame("a\rb\r\nc", lineBreaksToSave("a\nb\nc", "a\rb\r\nc"))

    @Test
    fun save_newRecord_hasLineFeedsOnly() = assertEquals("a\nb", lineBreaksToSave("a\r\nb", null))

    @Test
    fun save_noText_staysNull() {
        assertNull(lineBreaksToSave(null, null))
        assertNull(lineBreaksToSave(null, "a\rb")) // cleared, which is a change
    }
}
