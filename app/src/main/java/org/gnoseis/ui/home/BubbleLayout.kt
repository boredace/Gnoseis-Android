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
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** A bubble's centre and diameter, in dp. */
data class PlacedBubble(val x: Float, val y: Float, val diameter: Float)

/**
 * Places the Most linked bubbles (#45). The largest goes in the middle, and each next one at
 * the free spot nearest the middle, touching one already placed. The same counts and area
 * always give the same layout. If they don't fit, every bubble shrinks until they do.
 */
object BubbleLayout {
    /** Diameter for a record with [links] links, before fitting. */
    fun diameterFor(links: Int): Float = MIN_DIAMETER + 4f * links

    /** Returns one bubble per entry of [linkCounts], in the same order. */
    fun layout(linkCounts: List<Int>, width: Float, height: Float): List<PlacedBubble> {
        if (linkCounts.isEmpty()) return emptyList()
        val maxDiameter = MAX_SHARE * min(width, height)
        val wanted = linkCounts.map { min(diameterFor(it), maxDiameter) }
        val area = wanted.sumOf { (PI * it * it / 4).toDouble() }.toFloat()
        var scale = min(1f, sqrt(FILL * width * height / area))
        repeat(MAX_ATTEMPTS) {
            pack(wanted.map { it * scale }, width, height)?.let { return it }
            scale *= SHRINK
        }
        // Not reached for 10 bubbles in a usable area; overlap rather than leave one out.
        return wanted.map { PlacedBubble(width / 2, height / 2, it * scale) }
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

    const val MIN_DIAMETER = 52f
    private const val GAP = 4f
    private const val MAX_SHARE = 0.48f
    private const val FILL = 0.5f
    private const val SHRINK = 0.92f
    private const val MAX_ATTEMPTS = 30
    private const val ANGLES = 120
    private const val EPSILON = 0.01f
}
