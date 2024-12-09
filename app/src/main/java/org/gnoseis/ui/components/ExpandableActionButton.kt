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

package org.gnoseis.ui.components

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLink
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.TextSnippet
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp


// Custom fab that allows for displaying extended content
@Composable
fun ExpandableActionButton(
//    expandable: Boolean,
    isExpanded: Boolean = false,
    fabIcon: ImageVector,
    fabText: String,
    onFabClick: () -> Unit,
    fab1Icon: ImageVector,
    fab1Text: String,
    onFab1Click: () -> Unit,
    fab2Icon: ImageVector? = null,
    fab2Text: String? = null,
    onFab2Click: () -> Unit,
) {
//    var isExpanded by remember { mutableStateOf(false) }
//    if (!expandable) { // Close the expanded fab if you change to non expandable nav destination
//        isExpanded = false
//    }

    val fabSize = 64.dp
    var expandedBoxHeight = 84.dp
    val expandedFabWidth by animateDpAsState(
        targetValue = if (isExpanded) 200.dp else fabSize,
        animationSpec = spring(dampingRatio = 3f)
    )
    val expandedFabHeight by animateDpAsState(
        targetValue = if (isExpanded) 58.dp else fabSize,
        animationSpec = spring(dampingRatio = 3f)
    )
    var fabCount: Int = 1

    if (fab2Icon != null && fab2Text != null) {
        fabCount = 2
        expandedBoxHeight = 140.dp

    }

    Column {

        // ExpandedBox over the FAB
        Box(
            modifier = Modifier
                .offset(y = (25).dp)
                .size(
                    width = expandedFabWidth,
                    height = (animateDpAsState(if (isExpanded) expandedBoxHeight else 0.dp, animationSpec = spring(dampingRatio = 4f))).value)
                .alpha(0.8f)
                .background(
                    color = Color.Black,
                    shape = RoundedCornerShape(18.dp)
                )
        ) {
            // Customize the content of the expanded box as needed

            Column() {

                // FAB 1 - REQUIRED
                FloatingActionButton(
                    onClick = {
                        onFab1Click()
                    },
                    modifier = Modifier
                        .width(expandedFabWidth)
                        .height(expandedFabHeight),
                    shape = RoundedCornerShape(18.dp)

                ) {

                    Icon(
                        imageVector = fab1Icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp)
                            .offset(
                                x = animateDpAsState(
                                    if (isExpanded) -70.dp else 0.dp,
                                    animationSpec = spring(dampingRatio = 3f)
                                ).value
                            )
                    )

                    Text(
                        text = fab1Text,
                        softWrap = false,
                        modifier = Modifier
                            .offset(
                                x = animateDpAsState(
                                    if (isExpanded) 10.dp else 50.dp,
                                    animationSpec = spring(dampingRatio = 3f)
                                ).value
                            )
                            .alpha(
                                animateFloatAsState(
                                    targetValue = if (isExpanded) 1f else 0f,
                                    animationSpec = tween(
                                        durationMillis = if (isExpanded) 350 else 100,
                                        delayMillis = if (isExpanded) 100 else 0,
                                        easing = EaseIn
                                    )
                                ).value
                            )
                    )

                }

                // FAB 2 - OPTIONAL
                if (fabCount == 2) {
                    FloatingActionButton(
                        onClick = {
                            onFab2Click()
                        },
                        modifier = Modifier
                            .width(expandedFabWidth)
                            .height(expandedFabHeight),
                        shape = RoundedCornerShape(18.dp)

                    ) {

                        Icon(
                            imageVector = fab2Icon!!,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .offset(
                                    x = animateDpAsState(
                                        if (isExpanded) -70.dp else 0.dp,
                                        animationSpec = spring(dampingRatio = 3f)
                                    ).value
                                )
                        )

                        Text(
                            text = fab2Text!!,
                            softWrap = false,
                            modifier = Modifier
                                .offset(
                                    x = animateDpAsState(
                                        if (isExpanded) 10.dp else 50.dp,
                                        animationSpec = spring(dampingRatio = 3f)
                                    ).value
                                )
                                .alpha(
                                    animateFloatAsState(
                                        targetValue = if (isExpanded) 1f else 0f,
                                        animationSpec = tween(
                                            durationMillis = if (isExpanded) 350 else 100,
                                            delayMillis = if (isExpanded) 100 else 0,
                                            easing = EaseIn
                                        )
                                    ).value
                                )
                        )

                    }
                }
            }
        }



        FloatingActionButton(
            onClick = {
                onFabClick()
            },
            modifier = Modifier
                .width(expandedFabWidth)
                .height(expandedFabHeight),
            shape = RoundedCornerShape(18.dp)

        ) {

            Icon(
                imageVector = fabIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .offset(x = animateDpAsState(if (isExpanded) -70.dp else 0.dp, animationSpec = spring(dampingRatio = 3f)).value)
            )

            Text(
                text = fabText,
                softWrap = false,
                modifier = Modifier
                    .offset(x = animateDpAsState(if (isExpanded) 10.dp else 50.dp, animationSpec = spring(dampingRatio = 3f)).value)
                    .alpha(
                        animateFloatAsState(
                            targetValue = if (isExpanded) 1f else 0f,
                            animationSpec = tween(
                                durationMillis = if (isExpanded) 350 else 100,
                                delayMillis = if (isExpanded) 100 else 0,
                                easing = EaseIn)).value)
            )

        }
    }
}


@Composable
@Preview
fun ExpandableActionButtonPreview() {
    ExpandableActionButton(
        isExpanded =  false,
        fabIcon = Icons.Filled.AddLink,
        onFabClick = {},
        fabText = "Link Records",
        fab1Icon = Icons.Outlined.TextSnippet,
        fab1Text = "New Note",
        onFab1Click = {},
        fab2Icon = Icons.Outlined.Contacts,
        fab2Text = "New Contact",
        onFab2Click = {}
    )
}