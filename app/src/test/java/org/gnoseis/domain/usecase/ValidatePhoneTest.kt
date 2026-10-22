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

package org.gnoseis.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.ParameterizedRobolectricTestRunner.Parameters

/**
 * ValidatePhone relies on android.util.Patterns, so it needs Robolectric.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class ValidatePhoneTest(
    private val input: String,
    private val expectedSuccess: Boolean,
    private val expectedMessage: String?,
) {
    @Test
    fun execute() {
        val result = ValidatePhone().execute(input)

        assertEquals("successful for '$input'", expectedSuccess, result.successful)
        assertEquals("errorMessage for '$input'", expectedMessage, result.errorMessage)
    }

    companion object {
        private const val BLANK = "The phone number can't be blank"
        private const val INVALID = "That's not a valid phone number"

        @JvmStatic
        @Parameters(name = "\"{0}\" -> {1}")
        fun cases() = listOf(
            // Blank
            arrayOf("", false, BLANK),
            arrayOf("   ", false, BLANK),
            // Valid
            arrayOf("123", true, null),
            arrayOf("5551234", true, null),
            arrayOf("555-1234", true, null),
            arrayOf("555.123.4567", true, null),
            arrayOf("(555) 123-4567", true, null),
            arrayOf("+1 555 123 4567", true, null),
            arrayOf("+44 (20) 7946.0958", true, null),
            // No length limit (documented)
            arrayOf("12345678901234567890", true, null),
            // Invalid
            arrayOf("1", false, INVALID),
            arrayOf("12", false, INVALID),
            arrayOf("abc", false, INVALID),
            arrayOf("555-CALL", false, INVALID),
            arrayOf("+", false, INVALID),
            arrayOf("123-", false, INVALID),
            arrayOf("-123", false, INVALID),
            arrayOf("()123", false, INVALID),
            arrayOf("555 1234 ext. 5", false, INVALID),
        )
    }
}
