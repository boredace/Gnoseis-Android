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
 * ValidateEmail relies on android.util.Patterns, so it needs Robolectric.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
class ValidateEmailTest(
    private val input: String,
    private val expectedSuccess: Boolean,
    private val expectedMessage: String?,
) {
    @Test
    fun execute() {
        val result = ValidateEmail().execute(input)

        assertEquals("successful for '$input'", expectedSuccess, result.successful)
        assertEquals("errorMessage for '$input'", expectedMessage, result.errorMessage)
    }

    companion object {
        private const val BLANK = "The email can't be blank"
        private const val INVALID = "That's not a valid email"

        @JvmStatic
        @Parameters(name = "\"{0}\" -> {1}")
        fun cases() = listOf(
            // Blank
            arrayOf("", false, BLANK),
            arrayOf("   ", false, BLANK),
            arrayOf("\t\n", false, BLANK),
            // Valid
            arrayOf("a@b.co", true, null),
            arrayOf("john.doe@example.com", true, null),
            arrayOf("user+tag@example.org", true, null),
            arrayOf("first_last@sub.domain.com", true, null),
            arrayOf("UPPER@EXAMPLE.COM", true, null),
            arrayOf("x%y@ex-ample.com", true, null),
            arrayOf("1@2.3", true, null),
            // Lenient behaviour of android.util.Patterns (documented, not asserted as desirable)
            arrayOf("a@b.c", true, null),
            arrayOf("user.@example.com", true, null),
            // Invalid
            arrayOf("plainaddress", false, INVALID),
            arrayOf("@example.com", false, INVALID),
            arrayOf("user@", false, INVALID),
            arrayOf("user@example", false, INVALID),
            arrayOf("user@.com", false, INVALID),
            arrayOf("user@@example.com", false, INVALID),
            arrayOf("user name@example.com", false, INVALID),
            arrayOf(" user@example.com", false, INVALID),
            arrayOf("user@example.com ", false, INVALID),
            arrayOf("user@-example.com", false, INVALID),
            arrayOf("user@example..com", false, INVALID),
            arrayOf("jürgen@example.com", false, INVALID),
        )
    }
}
