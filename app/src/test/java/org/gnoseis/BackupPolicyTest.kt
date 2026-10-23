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

package org.gnoseis

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

/**
 * #33: Android Auto Backup and device-to-device transfer must not copy the user's data.
 * The only backup is the app's own Export.
 */
@RunWith(AndroidJUnit4::class)
class BackupPolicyTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun manifest_disallowsBackup() {
        assertEquals(0, context.applicationInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP)
    }

    /** Device transfer on API 31+ ignores allowBackup, so the rules must exclude everything. */
    @Test
    fun dataExtractionRules_excludeEveryDomain_inCloudBackupAndDeviceTransfer() {
        val excluded = mutableMapOf<String, MutableSet<String>>()
        val included = mutableListOf<String>()
        var section: String? = null
        context.resources.getXml(R.xml.data_extraction_rules).use { xml ->
            while (xml.next() != XmlPullParser.END_DOCUMENT) {
                if (xml.eventType != XmlPullParser.START_TAG) continue
                when (xml.name) {
                    "cloud-backup", "device-transfer" -> section = xml.name
                    "include" -> included += "$section/${xml.getAttributeValue(null, "domain")}"
                    "exclude" -> if (xml.getAttributeValue(null, "path") == ".") {
                        excluded.getOrPut(section!!) { mutableSetOf() } += xml.getAttributeValue(null, "domain")
                    }
                }
            }
        }

        assertTrue("Unexpected <include>: $included", included.isEmpty())
        assertEquals(ALL_DOMAINS, excluded["cloud-backup"])
        assertEquals(ALL_DOMAINS, excluded["device-transfer"])
    }

    private companion object {
        val ALL_DOMAINS = setOf(
            "root", "file", "database", "sharedpref", "external",
            "device_root", "device_file", "device_database", "device_sharedpref",
        )
    }
}
