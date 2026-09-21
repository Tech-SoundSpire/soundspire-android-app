package com.example.shared

// Sanity check for the KMP wiring. Real shared code lands here in Phase 2 onward.
expect fun platformName(): String

object SharedInfo {
    fun describe(): String = "SoundSpire shared module on ${platformName()}"
}
