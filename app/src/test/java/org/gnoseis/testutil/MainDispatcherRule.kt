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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Replaces [Dispatchers.Main] with a [TestDispatcher] for the duration of a test, so that
 * viewModelScope and the debounce jobs launched on Dispatchers.Main run on the test scheduler.
 *
 * Create ViewModels through [viewModel] and call [clearViewModels] in @After before closing
 * the database. Room runs queries on its own threads and resumes the caller on
 * Dispatchers.Main from there, even after viewModelScope was cancelled, if the query was
 * already running. If that happens while Main is being swapped, the test fails with
 * "Dispatchers.Main is used concurrently with setting it" (#25).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    private val viewModelStore = ViewModelStore()
    private val viewModels = mutableListOf<ViewModel>()

    /** Creates a ViewModel that [clearViewModels] clears, cancelling its viewModelScope. */
    fun <T : ViewModel> viewModel(newViewModel: () -> T): T {
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <VM : ViewModel> create(modelClass: Class<VM>): VM = newViewModel() as VM
        }
        @Suppress("UNCHECKED_CAST")
        return (ViewModelProvider(viewModelStore, factory)["vm${viewModels.size}", ViewModel::class.java] as T)
            .also { viewModels += it }
    }

    /**
     * Clears every ViewModel created by [viewModel], then waits until the work in their
     * viewModelScopes has actually stopped, including queries Room was still running.
     */
    fun clearViewModels() {
        // Read before clearing; viewModelScope is created on first access, so not at construction
        // time, when Dispatchers.Main may not be set yet.
        val jobs = viewModels.map { it.viewModelScope.coroutineContext.job }
        viewModels.clear()
        viewModelStore.clear()
        testDispatcher.scheduler.advanceUntilIdle()
        runBlocking {
            withTimeoutOrNull(5_000) { jobs.joinAll() }
                ?: throw AssertionError("ViewModel work was still running 5 s after the ViewModels were cleared")
        }
    }

    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        clearViewModels() // in case @After did not, so nothing uses Main while it is reset
        Dispatchers.resetMain()
    }
}
