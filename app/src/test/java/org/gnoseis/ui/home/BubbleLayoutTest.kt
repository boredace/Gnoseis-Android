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

package org.gnoseis.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class BubbleLayoutTest {
    private fun assertFitsWithoutOverlap(bubbles: List<PlacedBubble>, width: Float, height: Float) {
        bubbles.forEach {
            val r = it.diameter / 2
            assertTrue("inside: $it", it.x - r >= -0.01f && it.y - r >= -0.01f && it.x + r <= width + 0.01f && it.y + r <= height + 0.01f)
        }
        for (i in bubbles.indices) for (j in i + 1 until bubbles.size) {
            val a = bubbles[i]
            val b = bubbles[j]
            assertTrue("no overlap: $a $b", hypot(a.x - b.x, a.y - b.y) >= (a.diameter + b.diameter) / 2)
        }
    }

    @Test
    fun noRecords_noBubbles() {
        assertEquals(emptyList<PlacedBubble>(), BubbleLayout.layout(emptyList(), 358f, 360f))
    }

    @Test
    fun largestGoesInTheMiddle_andSizesFollowTheCounts() {
        val bubbles = BubbleLayout.layout(listOf(3, 8, 1), 358f, 360f)

        assertEquals(179f, bubbles[1].x, 0.01f)
        assertEquals(180f, bubbles[1].y, 0.01f)
        assertTrue(bubbles[1].diameter > bubbles[0].diameter && bubbles[0].diameter > bubbles[2].diameter)
        assertEquals(BubbleLayout.diameterFor(8), bubbles[1].diameter, 0.01f) // small counts aren't shrunk
    }

    @Test
    fun tenBubbles_fitWithoutOverlap_whateverTheCounts() {
        listOf(
            listOf(21, 16, 14, 11, 9, 8, 7, 6, 5, 4),
            List(10) { 500 },
            listOf(1000, 1, 1, 1, 1, 1, 1, 1, 1, 1),
            List(10) { 1 },
        ).forEach { counts ->
            listOf(358f to 360f, 280f to 360f, 600f to 360f).forEach { (width, height) ->
                val bubbles = BubbleLayout.layout(counts, width, height)
                assertEquals(counts.size, bubbles.size)
                assertFitsWithoutOverlap(bubbles, width, height)
            }
        }
    }

    @Test
    fun sameInput_sameLayout() {
        val counts = listOf(21, 16, 14, 11, 9, 8, 7, 6, 5, 4)
        assertEquals(BubbleLayout.layout(counts, 358f, 360f), BubbleLayout.layout(counts, 358f, 360f))
    }
}
