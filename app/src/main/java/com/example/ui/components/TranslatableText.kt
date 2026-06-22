package com.example.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import com.example.data.remote.ApiClient
import com.example.util.DynamicTranslator
import com.example.util.LanguageManager
import com.example.util.LocalLanguage

/**
 * A drop-in replacement for [Text] that auto-translates its content to the
 * currently-selected app language. It first checks the static UI dictionary,
 * then falls back to the dynamic (Gemini-backed) translator with caching.
 * English passes through unchanged with zero overhead.
 */
@Composable
fun TText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    style: TextStyle = LocalTextStyle.current,
) {
    val lang = LocalLanguage.current
    val context = LocalContext.current

    // Static dictionary lookup is synchronous & cheap
    val staticHit = if (lang != "en" && text.isNotBlank()) LanguageManager.translate(text, lang) else text
    val needsDynamic = lang != "en" && text.isNotBlank() && staticHit == text

    // Always call remember/LaunchedEffect unconditionally (Rules of Compose)
    var dynamic by remember(text, lang) { mutableStateOf<String?>(null) }
    LaunchedEffect(text, lang, needsDynamic) {
        if (needsDynamic) {
            val api = ApiClient.getService(context)
            val map = DynamicTranslator.translateBatch(api, listOf(text), lang)
            dynamic = map[text]
        } else {
            dynamic = null
        }
    }

    val display: String = when {
        lang == "en" || text.isBlank() -> text
        staticHit != text -> staticHit
        else -> dynamic ?: text
    }

    Text(
        text = display,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        lineHeight = lineHeight,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow,
        letterSpacing = letterSpacing,
        style = style,
    )
}
