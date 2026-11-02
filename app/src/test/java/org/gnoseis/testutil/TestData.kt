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

import org.gnoseis.data.entity.category.Category
import org.gnoseis.data.entity.contact.Contact
import org.gnoseis.data.entity.item.Item
import org.gnoseis.data.entity.links.LinkedRecord
import org.gnoseis.data.entity.note.Note
import org.gnoseis.data.entity.organization.Organization
import org.gnoseis.data.enums.RecordType

/** The time [TestAppContainer]'s repositories stamp on every insert and update (#50). */
const val TEST_NOW = 1_700_000_000_000L

/**
 * The record as [TestAppContainer] stores it: created and updated at [TEST_NOW]. Saving a
 * stamped record through the container stores it unchanged, so tests can compare it directly.
 */
fun Note.stamped() = copy(createdAt = TEST_NOW, updatedAt = TEST_NOW)
fun Contact.stamped() = copy(createdAt = TEST_NOW, updatedAt = TEST_NOW)
fun Organization.stamped() = copy(createdAt = TEST_NOW, updatedAt = TEST_NOW)
fun Category.stamped() = copy(createdAt = TEST_NOW, updatedAt = TEST_NOW)
fun Item.stamped() = copy(createdAt = TEST_NOW, updatedAt = TEST_NOW)

fun link(
    record1Id: String,
    record1Type: RecordType,
    record2Id: String,
    record2Type: RecordType,
) = LinkedRecord(
    ownerDbId = "db1",
    record1Id = record1Id,
    record2Id = record2Id,
    record1TypeId = record1Type.recordTypeId,
    record2TypeId = record2Type.recordTypeId,
)
