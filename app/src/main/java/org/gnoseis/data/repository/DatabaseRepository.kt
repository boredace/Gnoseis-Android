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
*   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
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

package org.gnoseis.data.repository

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gnoseis.data.database.GnoseisDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

const val TAG = "Settings"

class DatabaseRepository (
    private val context: Context,
){
    suspend fun copyDatabase(destinationPath: String) : Boolean {
        val dbFile = context.getDatabasePath("gnoseis_data")
        val destFile = File(destinationPath)
        val db = GnoseisDatabase.getDatabase(context)

        return try {
            withContext(Dispatchers.IO) {
                db.closeDatabase()
                Log.i(TAG, "Closed database")

                FileInputStream(dbFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output) // Copy the file contents
                    }
                }
                val db = GnoseisDatabase.getDatabase(context)
            }
            Log.i("Settings", "Maybe Success?")
            true
        } catch (e: IOException) {
            Log.i("Settings", "Maybe Failure?")
            e.printStackTrace()
            false
        }
    }

        fun closeApp() {

        }
 }