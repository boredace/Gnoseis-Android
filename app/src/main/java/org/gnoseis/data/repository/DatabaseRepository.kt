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
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.gnoseis.data.database.GnoseisDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class DatabaseRepository (
    private val context: Context,
){
    suspend fun exportDatabase(destinationPath: String) : Boolean {
        val dbFile = context.getDatabasePath("gnoseis_data")
        val destFile = File(destinationPath)
        val db = GnoseisDatabase.getDatabase(context)

        return try {
            withContext(Dispatchers.IO) {
                db.closeDatabase()

                FileInputStream(dbFile).use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output) // Copy the file contents
                    }
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    suspend fun importDatabase(selectedFile: Uri) : Boolean{
        val dbFile = context.getDatabasePath("gnoseis_data")

        // Open the input stream from the Uri
        val contentResolver = context.contentResolver
        val inputStream = contentResolver.openInputStream(selectedFile)

        // Ensure the input stream was opened successfully
        if (inputStream == null) {
            return false
        }

        return try {
            withContext(Dispatchers.IO) {
                // Close the database if needed
                val db = GnoseisDatabase.getDatabase(context)
                db.closeDatabase()

                // Create the output stream to the app's internal storage
                FileOutputStream(dbFile).use { output ->
                    inputStream.use { input ->
                        input.copyTo(output) // Copy content from input to output
                    }
                }
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
 }