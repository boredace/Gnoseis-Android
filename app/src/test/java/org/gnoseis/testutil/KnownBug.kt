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

package org.gnoseis.testutil

const val ISSUES_URL = "https://github.com/boredace/Gnoseis-Android/issues"

/**
 * Marks a test that currently fails because of the bug tracked in GitHub [issue]. If [block]
 * fails, the test report shows the issue link and [explanation] in plain words first, followed
 * by the raw assertion or exception. Once the bug is fixed the test passes; remove the wrapper
 * in the fixing commit.
 */
inline fun <T> knownBug(issue: Int, explanation: String, block: () -> T): T =
    try {
        block()
    } catch (e: Throwable) {
        throw AssertionError(
            "KNOWN BUG #$issue: $explanation\n    $ISSUES_URL/$issue\n    Raw failure: ${e.message ?: e}",
            e,
        )
    }

