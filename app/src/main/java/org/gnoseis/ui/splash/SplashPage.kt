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

package org.gnoseis.ui.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import kotlin.math.min
import org.gnoseis.BuildConfig
import org.gnoseis.R

// Brand colours, matching gnoseis.org
private val SplashBackground = Color(0xFF0B0D14)
private val SplashText = Color(0xFFE9EAF0)
private val SplashFaint = Color(0xFF6B7189)

private val Fraunces = FontFamily(Font(R.font.fraunces_semibold, FontWeight.SemiBold))

// The splash is laid out on the mockup's 248 x 508 screen and scaled to fit
// the device, so it keeps the same proportions on every phone.
private val MockupWidth = 248.dp
private val MockupHeight = 508.dp

/**
 * Branded screen shown after the system splash: logo, name and tagline in
 * the centre, version and distribution channel at the bottom.
 */
@Composable
fun SplashPage(modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(SplashBackground)
            .navigationBarsPadding()
    ) {
        val scale = min(maxWidth / MockupWidth, maxHeight / MockupHeight)
        // Sizes in mockup pixels, scaled; text ignores the font scale so it keeps its proportions
        val density = LocalDensity.current
        val px = { value: Float -> (value * scale).dp }
        val textPx = { value: Float -> with(density) { px(value).toSp() } }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = px(28f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.gnoseis_logo),
                contentDescription = null,
                modifier = Modifier.size(px(96f))
            )
            Spacer(Modifier.height(px(16f)))
            Text(
                text = stringResource(R.string.app_name),
                style = TextStyle(
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = textPx(36f),
                    lineHeight = textPx(43f),
                    color = SplashText
                )
            )
            Spacer(Modifier.height(px(8f)))
            Text(
                text = stringResource(R.string.app_tagline),
                style = TextStyle(
                    fontSize = textPx(13f),
                    lineHeight = textPx(19.5f),
                    color = SplashFaint,
                    textAlign = TextAlign.Center
                )
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = px(24f)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "v${BuildConfig.VERSION_NAME}",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = textPx(12f),
                    lineHeight = textPx(17f),
                    color = SplashFaint
                )
            )
            Spacer(Modifier.height(px(2f)))
            Text(
                text = stringResource(R.string.app_channel),
                style = TextStyle(
                    fontSize = textPx(11f),
                    lineHeight = textPx(15.5f),
                    color = SplashFaint
                )
            )
        }
    }
}

@Preview
@Composable
fun SplashPagePreview() {
    SplashPage()
}
