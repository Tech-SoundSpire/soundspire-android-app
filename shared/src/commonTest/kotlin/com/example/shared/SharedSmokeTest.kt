package com.example.shared

import kotlin.test.Test
import kotlin.test.assertTrue

// Non-network smoke test: proves the shared code runs and the .env -> SharedConfig
// injection produced real values on whichever platform this runs on.
class SharedSmokeTest {

    @Test
    fun platform_name_is_populated() {
        val n = platformName()
        println("SHARED platformName -> $n")
        assertTrue(n.isNotBlank(), "platformName() was blank")
    }

    @Test
    fun config_is_injected_from_env() {
        println("SHARED apiBaseUrl -> ${SharedConfig.API_BASE_URL}")
        assertTrue(SharedConfig.API_BASE_URL.startsWith("http"), "API_BASE_URL not injected from .env")
        assertTrue(SharedConfig.SUPABASE_URL.contains("supabase"), "SUPABASE_URL not injected from .env")
        assertTrue(SharedConfig.SUPABASE_ANON_KEY.isNotBlank(), "SUPABASE_ANON_KEY not injected from .env")
    }
}
