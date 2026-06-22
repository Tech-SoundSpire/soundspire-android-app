package com.example.util

import com.example.data.remote.SoundSpireService
import com.example.data.remote.TranslateRequest

/**
 * Translates dynamic (server-sourced) text — review bodies, bios, post content —
 * via the Gemini-backed /api/translate endpoint. Results are cached in-memory
 * keyed by "lang::text" so the same string isn't re-translated.
 */
object DynamicTranslator {
    private val cache = mutableMapOf<String, String>()

    private fun key(text: String, lang: String) = "$lang::$text"

    /** Translate a batch of texts. Returns a map of original -> translated. English passes through unchanged. */
    suspend fun translateBatch(api: SoundSpireService, texts: List<String>, lang: String): Map<String, String> {
        if (lang == "en") return texts.associateWith { it }

        val result = mutableMapOf<String, String>()
        val uncached = mutableListOf<String>()

        for (t in texts.distinct()) {
            if (t.isBlank()) { result[t] = t; continue }
            val cached = cache[key(t, lang)]
            if (cached != null) result[t] = cached else uncached.add(t)
        }

        if (uncached.isNotEmpty()) {
            try {
                val langLabel = SUPPORTED_LANGUAGES.firstOrNull { it.code == lang }?.label ?: lang
                val response = api.translate(TranslateRequest(texts = uncached, targetLang = langLabel))
                uncached.forEachIndexed { i, original ->
                    val translated = response.translations.getOrNull(i)?.takeIf { it.isNotBlank() } ?: original
                    cache[key(original, lang)] = translated
                    result[original] = translated
                }
            } catch (_: Exception) {
                // On failure, fall back to original text
                uncached.forEach { result[it] = it }
            }
        }

        return result
    }
}
