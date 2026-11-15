package com.uzuu.letplay.ui.base

import kotlinx.coroutines.flow.StateFlow

/**
 * Common contract for UI screens to observe loading states and effects.
 * Decouples [BaseScreen] from the requirement to inherit from [BaseViewModel],
 * allowing any ViewModel, component, or state-holder to drive screen chrome.
 */
interface ScreenUiController {
    val loadingKeys: StateFlow<Map<LoadingKey, Int>>
    val effects: StateFlow<List<PendingUiEffect>>
    fun onEffectHandled(id: Long)
}
