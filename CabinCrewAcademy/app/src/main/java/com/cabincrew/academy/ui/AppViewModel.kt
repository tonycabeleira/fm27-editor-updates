package com.cabincrew.academy.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.cabincrew.academy.AppContainer
import com.cabincrew.academy.CabinCrewApplication

/** Creates a ViewModel scoped to the current navigation destination with access to the [AppContainer]. */
@Composable
inline fun <reified VM : ViewModel> appViewModel(
    crossinline create: (AppContainer, SavedStateHandle) -> VM,
): VM {
    val container = (LocalContext.current.applicationContext as CabinCrewApplication).container
    return viewModel(factory = viewModelFactory { initializer { create(container, createSavedStateHandle()) } })
}
