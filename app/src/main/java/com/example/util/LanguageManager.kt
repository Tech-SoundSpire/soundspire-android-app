package com.example.util

import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class SupportedLanguage(val code: String, val label: String, val nativeLabel: String)

val SUPPORTED_LANGUAGES = listOf(
    SupportedLanguage("en", "English", "English"),
    SupportedLanguage("ja", "Japanese", "日本語"),
    SupportedLanguage("ko", "Korean", "한국어"),
    SupportedLanguage("fr", "French", "Français"),
    SupportedLanguage("de", "German", "Deutsch"),
    SupportedLanguage("es", "Spanish", "Español"),
    SupportedLanguage("hi", "Hindi", "हिन्दी"),
)

// Static UI translations (ported from the website's LanguageContext)
private val translations: Map<String, Map<String, String>> = mapOf(
    "Explore" to mapOf("en" to "Explore", "ja" to "探索", "ko" to "탐색", "fr" to "Explorer", "de" to "Entdecken", "es" to "Explorar", "hi" to "खोजें"),
    "Feed" to mapOf("en" to "Feed", "ja" to "フィード", "ko" to "피드", "fr" to "Fil", "de" to "Feed", "es" to "Inicio", "hi" to "फ़ीड"),
    "Communities" to mapOf("en" to "Communities", "ja" to "コミュニティ", "ko" to "커뮤니티", "fr" to "Communautés", "de" to "Communities", "es" to "Comunidades", "hi" to "समुदाय"),
    "Reviews" to mapOf("en" to "Reviews", "ja" to "レビュー", "ko" to "리뷰", "fr" to "Avis", "de" to "Bewertungen", "es" to "Reseñas", "hi" to "समीक्षाएं"),
    "Alerts" to mapOf("en" to "Alerts", "ja" to "通知", "ko" to "알림", "fr" to "Alertes", "de" to "Benachrichtigungen", "es" to "Alertas", "hi" to "अलर्ट"),
    "Profile" to mapOf("en" to "Profile", "ja" to "プロフィール", "ko" to "프로필", "fr" to "Profil", "de" to "Profil", "es" to "Perfil", "hi" to "प्रोफ़ाइल"),
    "Notifications" to mapOf("en" to "Notifications", "ja" to "通知", "ko" to "알림", "fr" to "Notifications", "de" to "Benachrichtigungen", "es" to "Notificaciones", "hi" to "सूचनाएं"),
    "SUGGESTED ARTISTS" to mapOf("en" to "SUGGESTED ARTISTS", "ja" to "おすすめアーティスト", "ko" to "추천 아티스트", "fr" to "ARTISTES SUGGÉRÉS", "de" to "VORGESCHLAGENE KÜNSTLER", "es" to "ARTISTAS SUGERIDOS", "hi" to "सुझाए गए कलाकार"),
    "REVIEWS" to mapOf("en" to "REVIEWS", "ja" to "レビュー", "ko" to "리뷰", "fr" to "AVIS", "de" to "BEWERTUNGEN", "es" to "RESEÑAS", "hi" to "समीक्षाएं"),
    "DISCOVER BY GENRE" to mapOf("en" to "DISCOVER BY GENRE", "ja" to "ジャンルで探す", "ko" to "장르별 탐색", "fr" to "DÉCOUVRIR PAR GENRE", "de" to "NACH GENRE ENTDECKEN", "es" to "DESCUBRIR POR GÉNERO", "hi" to "शैली के अनुसार खोजें"),
    "See More" to mapOf("en" to "See More", "ja" to "もっと見る", "ko" to "더 보기", "fr" to "Voir plus", "de" to "Mehr sehen", "es" to "Ver más", "hi" to "और देखें"),
    "See All" to mapOf("en" to "See All", "ja" to "すべて見る", "ko" to "전체 보기", "fr" to "Tout voir", "de" to "Alle anzeigen", "es" to "Ver todo", "hi" to "सब देखें"),
    "Explore" to mapOf("en" to "Explore", "ja" to "探索", "ko" to "탐색", "fr" to "Explorer", "de" to "Entdecken", "es" to "Explorar", "hi" to "खोजें"),
    "POSTS" to mapOf("en" to "POSTS", "ja" to "投稿", "ko" to "게시물", "fr" to "PUBLICATIONS", "de" to "BEITRÄGE", "es" to "PUBLICACIONES", "hi" to "पोस्ट"),
    "MY COMMUNITIES" to mapOf("en" to "MY COMMUNITIES", "ja" to "マイコミュニティ", "ko" to "내 커뮤니티", "fr" to "MES COMMUNAUTÉS", "de" to "MEINE COMMUNITIES", "es" to "MIS COMUNIDADES", "hi" to "मेरे समुदाय"),
    "NOTIFICATIONS" to mapOf("en" to "NOTIFICATIONS", "ja" to "通知", "ko" to "알림", "fr" to "NOTIFICATIONS", "de" to "BENACHRICHTIGUNGEN", "es" to "NOTIFICACIONES", "hi" to "सूचनाएं"),
    "PROFILE" to mapOf("en" to "PROFILE", "ja" to "プロフィール", "ko" to "프로필", "fr" to "PROFIL", "de" to "PROFIL", "es" to "PERFIL", "hi" to "प्रोफ़ाइल"),
    "Today" to mapOf("en" to "Today", "ja" to "今日", "ko" to "오늘", "fr" to "Aujourd'hui", "de" to "Heute", "es" to "Hoy", "hi" to "आज"),
    "This Week" to mapOf("en" to "This Week", "ja" to "今週", "ko" to "이번 주", "fr" to "Cette semaine", "de" to "Diese Woche", "es" to "Esta semana", "hi" to "इस सप्ताह"),
    "Earlier" to mapOf("en" to "Earlier", "ja" to "以前", "ko" to "이전", "fr" to "Plus tôt", "de" to "Früher", "es" to "Antes", "hi" to "पहले"),
    "No notifications" to mapOf("en" to "No notifications", "ja" to "通知なし", "ko" to "알림 없음", "fr" to "Aucune notification", "de" to "Keine Benachrichtigungen", "es" to "Sin notificaciones", "hi" to "कोई सूचना नहीं"),
    "Activity" to mapOf("en" to "Activity", "ja" to "アクティビティ", "ko" to "활동", "fr" to "Activité", "de" to "Aktivität", "es" to "Actividad", "hi" to "गतिविधि"),
    "Lists" to mapOf("en" to "Lists", "ja" to "リスト", "ko" to "목록", "fr" to "Listes", "de" to "Listen", "es" to "Listas", "hi" to "सूचियां"),
    "Journal" to mapOf("en" to "Journal", "ja" to "ジャーナル", "ko" to "저널", "fr" to "Journal", "de" to "Tagebuch", "es" to "Diario", "hi" to "पत्रिका"),
    "Edit" to mapOf("en" to "Edit", "ja" to "編集", "ko" to "편집", "fr" to "Modifier", "de" to "Bearbeiten", "es" to "Editar", "hi" to "संपादित करें"),
    "Logout" to mapOf("en" to "Logout", "ja" to "ログアウト", "ko" to "로그아웃", "fr" to "Déconnexion", "de" to "Abmelden", "es" to "Cerrar sesión", "hi" to "लॉग आउट"),
    "Cancel" to mapOf("en" to "Cancel", "ja" to "キャンセル", "ko" to "취소", "fr" to "Annuler", "de" to "Abbrechen", "es" to "Cancelar", "hi" to "रद्द करें"),
    "Save Changes" to mapOf("en" to "Save Changes", "ja" to "変更を保存", "ko" to "변경 사항 저장", "fr" to "Enregistrer", "de" to "Speichern", "es" to "Guardar cambios", "hi" to "परिवर्तन सहेजें"),
    "My Subscriptions" to mapOf("en" to "My Subscriptions", "ja" to "サブスクリプション", "ko" to "내 구독", "fr" to "Mes abonnements", "de" to "Meine Abonnements", "es" to "Mis suscripciones", "hi" to "मेरी सदस्यताएं"),
    "Add to List" to mapOf("en" to "Add to List", "ja" to "リストに追加", "ko" to "목록에 추가", "fr" to "Ajouter à la liste", "de" to "Zur Liste hinzufügen", "es" to "Añadir a la lista", "hi" to "सूची में जोड़ें"),
    "Submit Review" to mapOf("en" to "Submit Review", "ja" to "レビューを投稿", "ko" to "리뷰 작성", "fr" to "Soumettre un avis", "de" to "Bewertung einreichen", "es" to "Enviar reseña", "hi" to "समीक्षा सबमिट करें"),
    "Rate this song" to mapOf("en" to "Rate this song", "ja" to "この曲を評価", "ko" to "이 곡 평가", "fr" to "Noter cette chanson", "de" to "Bewerte diesen Song", "es" to "Califica esta canción", "hi" to "इस गाने को रेट करें"),
    "Write a review" to mapOf("en" to "Write a review", "ja" to "レビューを書く", "ko" to "리뷰 작성", "fr" to "Écrire un avis", "de" to "Bewertung schreiben", "es" to "Escribe una reseña", "hi" to "समीक्षा लिखें"),
    "All Reviews" to mapOf("en" to "All Reviews", "ja" to "すべてのレビュー", "ko" to "모든 리뷰", "fr" to "Tous les avis", "de" to "Alle Bewertungen", "es" to "Todas las reseñas", "hi" to "सभी समीक्षाएं"),
    "Create New List" to mapOf("en" to "Create New List", "ja" to "新しいリストを作成", "ko" to "새 목록 만들기", "fr" to "Créer une liste", "de" to "Neue Liste erstellen", "es" to "Crear lista", "hi" to "नई सूची बनाएं"),
    "Login" to mapOf("en" to "Login", "ja" to "ログイン", "ko" to "로그인", "fr" to "Connexion", "de" to "Anmelden", "es" to "Iniciar sesión", "hi" to "लॉग इन"),
    "Sign Up" to mapOf("en" to "Sign Up", "ja" to "登録", "ko" to "가입", "fr" to "S'inscrire", "de" to "Registrieren", "es" to "Registrarse", "hi" to "साइन अप"),
)

object LanguageManager {
    private const val PREFS = "soundspire_lang"
    private const val KEY = "lang"

    private val _lang = MutableStateFlow("en")
    val lang: StateFlow<String> = _lang

    fun init(context: Context) {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "en") ?: "en"
        _lang.value = stored
    }

    fun setLang(context: Context, code: String) {
        _lang.value = code
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, code).apply()
    }

    fun translate(key: String, code: String): String {
        return translations[key]?.get(code) ?: key
    }
}

// Composition local so any composable can read the current language
val LocalLanguage = compositionLocalOf { "en" }
