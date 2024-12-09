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

package org.gnoseis.ui.icons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.gnoseis.R
import org.gnoseis.data.enums.IconTextPosition


@Composable
fun BaseIconImage (
    @DrawableRes drawable: Int,
    text: String?,
    textPosition: IconTextPosition?,
    colorFilter: ColorFilter? = ColorFilter.tint(MaterialTheme.colorScheme.primary),
) {
    var baseImage :@Composable () -> Unit = {
        Image(
            painter = painterResource(id = drawable),
            contentDescription = text,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
            ,
            colorFilter = colorFilter
        )
    }
    if (text.isNullOrEmpty()) {
        baseImage()
    }
    else when (textPosition) {
        IconTextPosition.Bottom -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                baseImage()
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = text, style = MaterialTheme.typography.bodyMedium)
            }
        }
        IconTextPosition.Right -> {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                baseImage()
                Spacer(modifier = Modifier.width(5.dp))
                Text(text = text)
            }
        }
        null -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                baseImage()
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = text)
            }
        }
    }
}
@Composable
fun CategoryIcon(
    text: String? = null,
    textPosition: IconTextPosition? = IconTextPosition.Bottom,
    colorFilter: ColorFilter? = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
    BaseIconImage(
        drawable = R.drawable.outline_label_24,
        text = text,
        textPosition = textPosition,
        colorFilter = colorFilter,
    )
}

@Composable
fun ContactIcon(
    text: String? = null,
    textPosition: IconTextPosition? = IconTextPosition.Bottom,
    colorFilter: ColorFilter? = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
    BaseIconImage(
        drawable = R.drawable.outline_people_24,
        text = text,
        textPosition = textPosition,
        colorFilter = colorFilter,
    )
}

@Composable
fun DeleteIcon(
    colorFilter: ColorFilter? = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
    Image(
        painter = painterResource(id = R.drawable.outline_delete_24),
        contentDescription = "Contact",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
        ,
        colorFilter = colorFilter
    )
}

@Composable
fun EditIcon(
    colorFilter: ColorFilter? = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
    Image(
        painter = painterResource(id = R.drawable.outline_edit_24),
        contentDescription = "Contact",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
        ,
        colorFilter = colorFilter
    )
}

@Composable
fun EmailIcon() {
    Image(
        painter = painterResource(id = R.drawable.outline_email_24),
        contentDescription = "Contact",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
        ,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
}
@Composable
fun FilterIcon() {
    Image(
        painter = painterResource(id = R.drawable.outline_filter_alt_24),
        contentDescription = "Filter Content",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
        ,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    )
}
@Composable
fun ItemIcon(
    text: String? = null,
    textPosition: IconTextPosition? = IconTextPosition.Bottom,
    colorFilter: ColorFilter? = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
    BaseIconImage(
        drawable = R.drawable.outline_deployed_code_24,
        text = text,
        textPosition = textPosition,
        colorFilter = colorFilter,
    )
}

@Composable
fun NoteIcon(
    text: String? = null,
    textPosition: IconTextPosition? = IconTextPosition.Bottom,
    colorFilter: ColorFilter? = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
    BaseIconImage(
        drawable = R.drawable.outline_description_24,
        text = text,
        textPosition = textPosition,
        colorFilter = colorFilter,
    )
}


@Composable
fun OrganizationIcon(
    text: String? = null,
    textPosition: IconTextPosition? = IconTextPosition.Bottom,
    colorFilter: ColorFilter? = ColorFilter.tint(MaterialTheme.colorScheme.primary)
) {
    BaseIconImage(
        drawable = R.drawable.baseline_business_24,
        text = text,
        textPosition = textPosition,
        colorFilter = colorFilter,
    )
}
@Composable
fun PhoneIcon() {
    Image(
        painter = painterResource(id = R.drawable.outline_phone_enabled_24),
        contentDescription = "Phone",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
        ,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
}

@Composable
fun QuestionIcon() {
    Image(
        painter = painterResource(id = R.drawable.outline_question_mark_24),
        contentDescription = "Question Mark",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
        ,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary))
}


@Composable
fun SaveIcon(text: String? = null) {
    Row(
    ){
        Image(
            painter = painterResource(id = R.drawable.outline_save_24),
            contentDescription = "Save",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(24.dp)
                .height(24.dp)
            ,
            colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.surface)
        )
        if(!text.isNullOrEmpty()){
            Box(modifier = Modifier.fillMaxWidth()){
                Text(text = text)
            }

        }
    }

}
@Composable
fun SettingsIcon() {
    Image(
        painter = painterResource(id = R.drawable.outline_settings_24),
        contentDescription = "Settings",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
        ,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    )
}

@Composable
fun SortIcon() {
    Image(
        painter = painterResource(id = R.drawable.outline_sort_24),
        contentDescription = "Sort Content",
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .width(24.dp)
            .height(24.dp)
        ,
        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary)
    )
}