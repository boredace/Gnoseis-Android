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

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** A bubble's centre and diameter, in dp. */
data class PlacedBubble(val x: Float, val y: Float, val diameter: Float)

/**
 * Places the Most linked bubbles (#45). Sizes are relative: the most linked record gets the
 * largest bubble, bigger when there are fewer bubbles, and each other bubble's area is its
 * share of the top link count. The largest goes in the middle, and each next one at the free
 * spot nearest the middle, touching one already placed. Then the whole group is moved so it's
 * centred in the area. The same counts and area always give the same layout. If they don't
 * fit, every bubble shrinks until they do.
 */
object BubbleLayout {
    /** Diameters for [linkCounts], in the same order, before fitting. */
    fun diametersFor(linkCounts: List<Int>, width: Float, height: Float): List<Float> {
        val share = when (linkCounts.size) {
            1 -> ONE_SHARE
            2 -> TWO_SHARE
            else -> MANY_SHARE
        }
        val largest = share * min(width, height)
        val top = (linkCounts.maxOrNull() ?: 0).coerceAtLeast(1)
        return linkCounts.map { max(MIN_OF_LARGEST * largest, largest * sqrt(it.toFloat() / top)) }
    }

    /** Returns one bubble per entry of [linkCounts], in the same order. */
    fun layout(linkCounts: List<Int>, width: Float, height: Float): List<PlacedBubble> {
        if (linkCounts.isEmpty()) return emptyList()
        val wanted = diametersFor(linkCounts, width, height)
        val area = wanted.sumOf { (PI * it * it / 4).toDouble() }.toFloat()
        var scale = min(1f, sqrt(FILL * width * height / area))
        repeat(MAX_ATTEMPTS) {
            pack(wanted.map { it * scale }, width, height)?.let { return centred(it, width, height) }
            scale *= SHRINK
        }
        // Not reached for 10 bubbles in a usable area; overlap rather than leave one out.
        return wanted.map { PlacedBubble(width / 2, height / 2, it * scale) }
    }

    /** Moves [bubbles] so their bounds are centred. They already fit, so they still do. */
    private fun centred(bubbles: List<PlacedBubble>, width: Float, height: Float): List<PlacedBubble> {
        val left = bubbles.minOf { it.x - it.diameter / 2 }
        val right = bubbles.maxOf { it.x + it.diameter / 2 }
        val top = bubbles.minOf { it.y - it.diameter / 2 }
        val bottom = bubbles.maxOf { it.y + it.diameter / 2 }
        val dx = width / 2 - (left + right) / 2
        val dy = height / 2 - (top + bottom) / 2
        return bubbles.map { it.copy(x = it.x + dx, y = it.y + dy) }
    }

    private fun pack(diameters: List<Float>, width: Float, height: Float): List<PlacedBubble>? {
        val cx = width / 2
        val cy = height / 2
        val order = diameters.indices.sortedByDescending { diameters[it] } // stable for ties
        val placed = arrayOfNulls<PlacedBubble>(diameters.size)
        val done = mutableListOf<PlacedBubble>()

        fun fits(x: Float, y: Float, r: Float) =
            x - r >= 0 && y - r >= 0 && x + r <= width && y + r <= height &&
                    done.all { hypot(it.x - x, it.y - y) >= it.diameter / 2 + r + GAP - EPSILON }

        for (i in order) {
            val r = diameters[i] / 2
            val spot = if (done.isEmpty()) {
                PlacedBubble(cx, cy, diameters[i]).takeIf { fits(cx, cy, r) }
            } else {
                done.flatMap { other ->
                    val distance = other.diameter / 2 + r + GAP
                    (0 until ANGLES).map { step ->
                        val angle = 2 * PI * step / ANGLES
                        PlacedBubble(
                            (other.x + distance * cos(angle)).toFloat(),
                            (other.y + distance * sin(angle)).toFloat(),
                            diameters[i],
                        )
                    }
                }
                    .filter { fits(it.x, it.y, r) }
                    .minByOrNull { hypot(it.x - cx, it.y - cy) }
            } ?: return null
            placed[i] = spot
            done += spot
        }
        return placed.map { it!! }
    }

    // Largest bubble's diameter as a share of the shorter side, for 1, 2 and more bubbles
    private const val ONE_SHARE = 0.62f
    private const val TWO_SHARE = 0.52f
    private const val MANY_SHARE = 0.48f
    /** Smallest bubble's diameter as a share of the largest, so names stay readable. */
    private const val MIN_OF_LARGEST = 0.36f
    private const val GAP = 4f
    private const val FILL = 0.5f
    private const val SHRINK = 0.92f
    private const val MAX_ATTEMPTS = 30
    private const val ANGLES = 120
    private const val EPSILON = 0.01f
}
