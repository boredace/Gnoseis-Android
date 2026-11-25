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

package org.gnoseis.data.backup

import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.bouncycastle.crypto.generators.Argon2BytesGenerator
import org.bouncycastle.crypto.params.Argon2Parameters
import org.junit.Test
import org.junit.runner.RunWith
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Measures Argon2id with the backup's parameters (64 MiB, 3 iterations, parallelism 1)
 * on the device it runs on, and logs the time and the peak Java heap use.
 *
 * Run on a device or emulator, then read the result with `adb logcat -s Argon2idMeasure`:
 * ./gradlew :app:connectedDebugAndroidTest
 *   -Pandroid.testInstrumentationRunnerArguments.class=org.gnoseis.data.backup.Argon2idMeasurementTest
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class Argon2idMeasurementTest {

    @Test
    fun measureArgon2idWithBackupParameters() {
        val runtime = Runtime.getRuntime()
        val salt = ByteArray(32).also { SecureRandom().nextBytes(it) }
        val passphrase = "measurement passphrase, not a real one".toByteArray(Charsets.UTF_8)
        val parameters = Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withMemoryAsKB(65536)
            .withIterations(3)
            .withParallelism(1)
            .withSalt(salt)
            .build()

        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL}, API ${Build.VERSION.SDK_INT}, " +
            "ABIs ${Build.SUPPORTED_ABIS.joinToString()}, max heap ${runtime.maxMemory() / MIB} MiB")

        repeat(RUNS) { run ->
            System.gc()
            val usedBefore = runtime.totalMemory() - runtime.freeMemory()
            val peak = AtomicLong(usedBefore)
            val sampling = AtomicBoolean(true)
            val sampler = Thread {
                while (sampling.get()) {
                    peak.accumulateAndGet(runtime.totalMemory() - runtime.freeMemory(), ::maxOf)
                    Thread.sleep(5)
                }
            }.apply { start() }

            val key = ByteArray(32)
            val started = SystemClock.elapsedRealtime()
            try {
                Argon2BytesGenerator().apply { init(parameters) }.generateBytes(passphrase, key)
                val elapsed = SystemClock.elapsedRealtime() - started
                sampling.set(false)
                sampler.join()
                Log.i(TAG, "Run ${run + 1}: $elapsed ms, peak heap ${peak.get() / MIB} MiB " +
                    "(${(peak.get() - usedBefore) / MIB} MiB above the start)")
            } catch (error: OutOfMemoryError) {
                sampling.set(false)
                sampler.join()
                Log.e(TAG, "Run ${run + 1}: OutOfMemoryError after " +
                    "${SystemClock.elapsedRealtime() - started} ms, peak heap ${peak.get() / MIB} MiB")
                throw error
            }
        }
    }

    private companion object {
        const val TAG = "Argon2idMeasure"
        const val MIB = 1024L * 1024L
        const val RUNS = 3
    }
}
