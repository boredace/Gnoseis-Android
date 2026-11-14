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

package org.gnoseis.ui.mostlinked

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.gnoseis.data.entity.home.LinkedRecordCount
import org.gnoseis.data.entity.home.TypeLinkCount
import org.gnoseis.data.repository.HomeRepository

/**
 * The full Most linked list (#52): every linked contact, organization, category and item,
 * ranked as on Home but without Home's top-10 limit. It starts on the type in the route and has
 * its own type picker, so changing the type here leaves Home's filter alone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MostLinkedViewModel(
    savedStateHandle: SavedStateHandle,
    homeRepository: HomeRepository,
) : ViewModel() {

    /** The type picked on this page, or null for all types. Starts on the route's type. */
    private val typeId = MutableStateFlow(savedStateHandle.toRoute<MostLinkedRoute>().recordTypeId)

    fun setTypeId(typeId: Int?) {
        this.typeId.value = typeId
    }

    val pageState: StateFlow<MostLinkedPageState> =
        combine(
            typeId.flatMapLatest { typeId ->
                homeRepository.getMostLinkedAllStream(typeId).map { records -> typeId to records }
            },
            homeRepository.getLinksPerTypeStream(),
        ) { (typeId, records), types ->
            MostLinkedPageState(typeId = typeId, types = types, records = records, isLoaded = true)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            MostLinkedPageState(typeId = typeId.value),
        )

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class MostLinkedPageState(
    /** The type [records] is filtered to, or null for all types. */
    val typeId: Int? = null,
    /** The types offered in the type picker: those with links, with their link counts. */
    val types: List<TypeLinkCount> = emptyList(),
    /** Every linked record of [typeId] (or of all types), most linked first. */
    val records: List<LinkedRecordCount> = emptyList(),
    /** False until the first values arrive, so the page doesn't flash an empty state. */
    val isLoaded: Boolean = false,
) {
    /** Nothing is linked at all: "Link records to see them here". */
    val isNothingLinked: Boolean get() = isLoaded && types.isEmpty()

    /** A type is picked but has no linked records (its last link was removed): "No linked <type> yet". */
    val isPickedTypeEmpty: Boolean get() = isLoaded && typeId != null && records.isEmpty() && types.isNotEmpty()
}
