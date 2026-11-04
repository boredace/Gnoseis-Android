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

package org.gnoseis.ui.record

import kotlinx.serialization.Serializable
import org.gnoseis.data.enums.RecordEditPageMode
import org.gnoseis.data.enums.RecordType

/** The Details page of the record [recordId] of [recordType]. */
@Serializable
data class RecordDetailsRoute(
    val recordType: RecordType,
    val recordId: String,
)

/**
 * The Edit page of a record of [recordType]: the record [recordId] in [RecordEditPageMode.EDIT]
 * mode, or a new record (no id) in NEW or NEWLINK mode. In NEWLINK mode the new record is linked
 * to [linkFromId] of [linkFromType].
 */
@Serializable
data class RecordEditRoute(
    val recordType: RecordType,
    val recordId: String?,
    val pageMode: RecordEditPageMode,
    val linkFromType: RecordType? = null,
    val linkFromId: String? = null,
    /** #10: a new record's name, from the search it's created from. Ignored when editing. */
    val prefillName: String? = null,
    /**
     * In NEWLINK mode: records picked on Link Records before creating this one, linked to
     * [linkFromId] as well when the new record is saved. [alsoLinkTypeIds] holds their type ids,
     * in the same order.
     */
    val alsoLinkIds: List<String> = emptyList(),
    val alsoLinkTypeIds: List<Int> = emptyList(),
)

/** The records to link to the source as well, with their types. */
fun RecordEditRoute.alsoLinkRecords(): List<Pair<String, RecordType>> =
    alsoLinkIds.zip(alsoLinkTypeIds).map { (id, typeId) -> id to RecordType.from(typeId) }
