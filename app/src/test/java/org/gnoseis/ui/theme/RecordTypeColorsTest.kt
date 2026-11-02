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

package org.gnoseis.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class RecordTypeColorsTest {
    private val light = mapOf(
        "Note" to NoteColorsLight,
        "Contact" to ContactColorsLight,
        "Organization" to OrganizationColorsLight,
        "Category" to CategoryColorsLight,
        "Item" to ItemColorsLight,
    )
    private val dark = mapOf(
        "Note" to NoteColorsDark,
        "Contact" to ContactColorsDark,
        "Organization" to OrganizationColorsDark,
        "Category" to CategoryColorsDark,
        "Item" to ItemColorsDark,
    )

    /** Each role's colour per type: light outline, light fill, dark outline, dark fill. */
    private val roles = mapOf(
        "light outline" to light.mapValues { it.value.outline },
        "light fill" to light.mapValues { it.value.fill },
        "dark outline" to dark.mapValues { it.value.outline },
        "dark fill" to dark.mapValues { it.value.fill },
    )

    /** WCAG contrast ratio between two colours. */
    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
    }

    @Test
    fun everyOutline_hasEnoughContrastOnItsFill() {
        (light.mapKeys { "${it.key} light" } + dark.mapKeys { "${it.key} dark" }).forEach { (name, colors) ->
            val ratio = contrast(colors.outline, colors.fill)
            assertTrue("$name: $ratio", ratio >= 4.5f)
        }
    }

    @Test
    fun everyTwoTypes_stayApart() {
        assertEveryPairApart(minimum = 15.0) { it }
    }

    @Test
    fun everyTwoTypes_stayApartWithNightLight() {
        assertEveryPairApart(minimum = 10.0, ::withNightLight)
    }

    @Test
    fun ciede2000_matchesReferenceValues() {
        // Pairs 1 and 7 from Sharma, Wu and Dalal's CIEDE2000 test data.
        assertEquals(2.0425, ciede2000(doubleArrayOf(50.0, 2.6772, -79.7751), doubleArrayOf(50.0, 0.0, -82.7485)), 1e-4)
        assertEquals(2.3669, ciede2000(doubleArrayOf(50.0, 0.0, 0.0), doubleArrayOf(50.0, -1.0, 2.0)), 1e-4)
    }

    private fun assertEveryPairApart(minimum: Double, view: (DoubleArray) -> DoubleArray) {
        roles.forEach { (role, colors) ->
            val types = colors.keys.toList()
            for (i in types.indices) for (j in i + 1 until types.size) {
                val a = view(srgb(colors.getValue(types[i])))
                val b = view(srgb(colors.getValue(types[j])))
                val difference = ciede2000(lab(a), lab(b))
                assertTrue("${types[i]}/${types[j]} $role: $difference", difference >= minimum)
            }
        }
    }

    private fun srgb(color: Color) = doubleArrayOf(color.red.toDouble(), color.green.toDouble(), color.blue.toDouble())

    private fun toLinear(c: Double) = if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

    private fun toSrgb(c: Double) = if (c <= 0.0031308) c * 12.92 else 1.055 * c.pow(1 / 2.4) - 0.055

    /** Night light, simulated: the blue channel cut to 40% in linear light. */
    private fun withNightLight(rgb: DoubleArray): DoubleArray =
        doubleArrayOf(rgb[0], rgb[1], toSrgb(toLinear(rgb[2]) * 0.4))

    /** sRGB to CIELAB, D65 white. */
    private fun lab(rgb: DoubleArray): DoubleArray {
        val (r, g, b) = rgb.map(::toLinear)
        val x = (0.4124 * r + 0.3576 * g + 0.1805 * b) / 0.95047
        val y = 0.2126 * r + 0.7152 * g + 0.0722 * b
        val z = (0.0193 * r + 0.1192 * g + 0.9505 * b) / 1.08883
        fun f(t: Double) = if (t > 216.0 / 24389) cbrt(t) else (24389.0 / 27 * t + 16) / 116
        return doubleArrayOf(116 * f(y) - 16, 500 * (f(x) - f(y)), 200 * (f(y) - f(z)))
    }

    /** CIEDE2000 colour difference between two CIELAB colours. */
    private fun ciede2000(lab1: DoubleArray, lab2: DoubleArray): Double {
        fun rad(deg: Double) = deg * PI / 180
        fun deg(rad: Double) = rad * 180 / PI
        val (l1, a1, b1) = lab1.toList()
        val (l2, a2, b2) = lab2.toList()
        val cMean = (hypot(a1, b1) + hypot(a2, b2)) / 2
        val g = 0.5 * (1 - sqrt(cMean.pow(7) / (cMean.pow(7) + 25.0.pow(7))))
        val a1p = (1 + g) * a1
        val a2p = (1 + g) * a2
        val c1p = hypot(a1p, b1)
        val c2p = hypot(a2p, b2)
        val h1p = if (c1p == 0.0) 0.0 else (deg(atan2(b1, a1p)) + 360) % 360
        val h2p = if (c2p == 0.0) 0.0 else (deg(atan2(b2, a2p)) + 360) % 360

        val dLp = l2 - l1
        val dCp = c2p - c1p
        var dhp = h2p - h1p
        if (c1p * c2p == 0.0) dhp = 0.0 else if (dhp > 180) dhp -= 360 else if (dhp < -180) dhp += 360
        val dHp = 2 * sqrt(c1p * c2p) * sin(rad(dhp / 2))

        val lpMean = (l1 + l2) / 2
        val cpMean = (c1p + c2p) / 2
        val hpMean = when {
            c1p * c2p == 0.0 -> h1p + h2p
            abs(h1p - h2p) <= 180 -> (h1p + h2p) / 2
            h1p + h2p < 360 -> (h1p + h2p + 360) / 2
            else -> (h1p + h2p - 360) / 2
        }
        val t = 1 - 0.17 * cos(rad(hpMean - 30)) + 0.24 * cos(rad(2 * hpMean)) +
            0.32 * cos(rad(3 * hpMean + 6)) - 0.20 * cos(rad(4 * hpMean - 63))
        val sl = 1 + 0.015 * (lpMean - 50).pow(2) / sqrt(20 + (lpMean - 50).pow(2))
        val sc = 1 + 0.045 * cpMean
        val sh = 1 + 0.015 * cpMean * t
        val rt = -2 * sqrt(cpMean.pow(7) / (cpMean.pow(7) + 25.0.pow(7))) *
            sin(rad(60 * exp(-((hpMean - 275) / 25).pow(2))))
        return sqrt(
            (dLp / sl).pow(2) + (dCp / sc).pow(2) + (dHp / sh).pow(2) + rt * (dCp / sc) * (dHp / sh)
        )
    }
}
