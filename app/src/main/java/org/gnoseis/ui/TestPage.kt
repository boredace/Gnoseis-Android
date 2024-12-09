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


package org.gnoseis.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.gnoseis.ui.navigation.NavigationDestination

object TestPageDestination : NavigationDestination {
    override val route = "test_page"
    override val titleRes = -9
}
const val TAG = "test_page"

@Composable
fun TestPage() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .border(4.dp, Color.Red)
    ) {
        val (text, setText) = remember { mutableStateOf("") }

        TextField(
            value = text,
            onValueChange = setText,
            label = {},
            modifier = Modifier
                .fillMaxWidth()
        )

        for (i in 0..100) {
            Text("Item #$i")
        }
    }
}

@Composable
fun KeyboardAware(
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .imePadding()
    ) {
        content()
    }
}


@Preview
@Composable
fun TestPagePreview() {
    TestPage()
}