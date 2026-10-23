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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Polls [condition] in real (wall-clock) time. Room runs queries on its own executor threads,
 * so work launched from a ViewModel's init block completes outside of runTest's virtual clock.
 */
suspend fun waitFor(
    timeoutMillis: Long = 5_000,
    message: String = "condition was not met in time",
    condition: () -> Boolean,
) {
    withContext(Dispatchers.Default) {
        try {
            withTimeout(timeoutMillis) {
                while (!condition()) delay(10)
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw AssertionError(message, e)
        }
    }
}
