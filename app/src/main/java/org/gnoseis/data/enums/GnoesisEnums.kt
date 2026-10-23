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

package org.gnoseis.data.enums


enum class IconTextPosition {
    Right, Bottom
}
enum class RecordType(val recordTypeId: Int) {
    Note(1),
    Contact(2),
    Organization(3),
    Category(4),
    Item(5),
    LinkedRecord(5001);

    companion object {
        fun from(findValue: Int): RecordType = RecordType.values().first {it.recordTypeId == findValue}
    }
}

enum class CategoryEditPageMode {
    NEW,
    EDIT,
    NEWLINK
}

enum class ContactEditPageMode {
    NEW,
    EDIT,
    NEWLINK
}

enum class ItemEditPageMode {
    NEW,
    EDIT,
    NEWLINK
}

enum class NoteEditPageMode {
    NEW,
    EDIT,
    NEWLINK
}

enum class OrganizationEditPageMode {
    NEW,
    EDIT,
    NEWLINK
}