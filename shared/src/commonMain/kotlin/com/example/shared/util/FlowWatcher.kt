package com.example.shared.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Minimal bridge so Swift can observe a Kotlin StateFlow without SKIE.
 * Collects on Dispatchers.Main so the callback fires on the iOS main thread (safe to
 * update SwiftUI state). SwiftUI wraps one of these per observed flow in an
 * ObservableObject. Swap for SKIE later if the manual wiring gets tedious.
 */
class FlowWatcher(private val flow: StateFlow<*>) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    val value: Any? get() = flow.value

    fun watch(onChange: (Any?) -> Unit) {
        cancel()
        onChange(flow.value) // emit current value immediately
        job = scope.launch { flow.collect { onChange(it) } }
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
