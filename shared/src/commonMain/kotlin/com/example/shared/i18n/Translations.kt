package com.example.shared.i18n

import com.example.shared.data.model.TranslateRequest
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Language code -> English label used as the translate API target.
private val LANG_LABEL = mapOf(
    "en" to "English", "ja" to "Japanese", "ko" to "Korean", "fr" to "French",
    "de" to "German", "es" to "Spanish", "hi" to "Hindi",
)

// Static UI dictionary (ported verbatim from the Android LanguageManager / website LanguageContext).
private val TRANSLATIONS: Map<String, Map<String, String>> = mapOf(
    "Explore" to mapOf("ja" to "探索", "ko" to "탐색", "fr" to "Explorer", "de" to "Entdecken", "es" to "Explorar", "hi" to "खोजें"),
    "Feed" to mapOf("ja" to "フィード", "ko" to "피드", "fr" to "Fil", "de" to "Feed", "es" to "Inicio", "hi" to "फ़ीड"),
    "Communities" to mapOf("ja" to "コミュニティ", "ko" to "커뮤니티", "fr" to "Communautés", "de" to "Communities", "es" to "Comunidades", "hi" to "समुदाय"),
    "Reviews" to mapOf("ja" to "レビュー", "ko" to "리뷰", "fr" to "Avis", "de" to "Bewertungen", "es" to "Reseñas", "hi" to "समीक्षाएं"),
    "Alerts" to mapOf("ja" to "通知", "ko" to "알림", "fr" to "Alertes", "de" to "Benachrichtigungen", "es" to "Alertas", "hi" to "अलर्ट"),
    "Profile" to mapOf("ja" to "プロフィール", "ko" to "프로필", "fr" to "Profil", "de" to "Profil", "es" to "Perfil", "hi" to "प्रोफ़ाइल"),
    "Notifications" to mapOf("ja" to "通知", "ko" to "알림", "fr" to "Notifications", "de" to "Benachrichtigungen", "es" to "Notificaciones", "hi" to "सूचनाएं"),
    "SUGGESTED ARTISTS" to mapOf("ja" to "おすすめアーティスト", "ko" to "추천 아티스트", "fr" to "ARTISTES SUGGÉRÉS", "de" to "VORGESCHLAGENE KÜNSTLER", "es" to "ARTISTAS SUGERIDOS", "hi" to "सुझाए गए कलाकार"),
    "REVIEWS" to mapOf("ja" to "レビュー", "ko" to "리뷰", "fr" to "AVIS", "de" to "BEWERTUNGEN", "es" to "RESEÑAS", "hi" to "समीक्षाएं"),
    "DISCOVER BY GENRE" to mapOf("ja" to "ジャンルで探す", "ko" to "장르별 탐색", "fr" to "DÉCOUVRIR PAR GENRE", "de" to "NACH GENRE ENTDECKEN", "es" to "DESCUBRIR POR GÉNERO", "hi" to "शैली के अनुसार खोजें"),
    "See More" to mapOf("ja" to "もっと見る", "ko" to "더 보기", "fr" to "Voir plus", "de" to "Mehr sehen", "es" to "Ver más", "hi" to "और देखें"),
    "See All" to mapOf("ja" to "すべて見る", "ko" to "전체 보기", "fr" to "Tout voir", "de" to "Alle anzeigen", "es" to "Ver todo", "hi" to "सब देखें"),
    "POSTS" to mapOf("ja" to "投稿", "ko" to "게시물", "fr" to "PUBLICATIONS", "de" to "BEITRÄGE", "es" to "PUBLICACIONES", "hi" to "पोस्ट"),
    "MY COMMUNITIES" to mapOf("ja" to "マイコミュニティ", "ko" to "내 커뮤니티", "fr" to "MES COMMUNAUTÉS", "de" to "MEINE COMMUNITIES", "es" to "MIS COMUNIDADES", "hi" to "मेरे समुदाय"),
    "NOTIFICATIONS" to mapOf("ja" to "通知", "ko" to "알림", "fr" to "NOTIFICATIONS", "de" to "BENACHRICHTIGUNGEN", "es" to "NOTIFICACIONES", "hi" to "सूचनाएं"),
    "PROFILE" to mapOf("ja" to "プロフィール", "ko" to "프로필", "fr" to "PROFIL", "de" to "PROFIL", "es" to "PERFIL", "hi" to "प्रोफ़ाइल"),
    "Today" to mapOf("ja" to "今日", "ko" to "오늘", "fr" to "Aujourd'hui", "de" to "Heute", "es" to "Hoy", "hi" to "आज"),
    "This Week" to mapOf("ja" to "今週", "ko" to "이번 주", "fr" to "Cette semaine", "de" to "Diese Woche", "es" to "Esta semana", "hi" to "इस सप्ताह"),
    "Earlier" to mapOf("ja" to "以前", "ko" to "이전", "fr" to "Plus tôt", "de" to "Früher", "es" to "Antes", "hi" to "पहले"),
    "No notifications" to mapOf("ja" to "通知なし", "ko" to "알림 없음", "fr" to "Aucune notification", "de" to "Keine Benachrichtigungen", "es" to "Sin notificaciones", "hi" to "कोई सूचना नहीं"),
    "Activity" to mapOf("ja" to "アクティビティ", "ko" to "활동", "fr" to "Activité", "de" to "Aktivität", "es" to "Actividad", "hi" to "गतिविधि"),
    "Lists" to mapOf("ja" to "リスト", "ko" to "목록", "fr" to "Listes", "de" to "Listen", "es" to "Listas", "hi" to "सूचियां"),
    "Journal" to mapOf("ja" to "ジャーナル", "ko" to "저널", "fr" to "Journal", "de" to "Tagebuch", "es" to "Diario", "hi" to "पत्रिका"),
    "Edit" to mapOf("ja" to "編集", "ko" to "편집", "fr" to "Modifier", "de" to "Bearbeiten", "es" to "Editar", "hi" to "संपादित करें"),
    "Logout" to mapOf("ja" to "ログアウト", "ko" to "로그아웃", "fr" to "Déconnexion", "de" to "Abmelden", "es" to "Cerrar sesión", "hi" to "लॉग आउट"),
    "Cancel" to mapOf("ja" to "キャンセル", "ko" to "취소", "fr" to "Annuler", "de" to "Abbrechen", "es" to "Cancelar", "hi" to "रद्द करें"),
    "Save Changes" to mapOf("ja" to "変更を保存", "ko" to "변경 사항 저장", "fr" to "Enregistrer", "de" to "Speichern", "es" to "Guardar cambios", "hi" to "परिवर्तन सहेजें"),
    "My Subscriptions" to mapOf("ja" to "サブスクリプション", "ko" to "내 구독", "fr" to "Mes abonnements", "de" to "Meine Abonnements", "es" to "Mis suscripciones", "hi" to "मेरी सदस्यताएं"),
    "Add to List" to mapOf("ja" to "リストに追加", "ko" to "목록에 추가", "fr" to "Ajouter à la liste", "de" to "Zur Liste hinzufügen", "es" to "Añadir a la lista", "hi" to "सूची में जोड़ें"),
    "Submit Review" to mapOf("ja" to "レビューを投稿", "ko" to "리뷰 작성", "fr" to "Soumettre un avis", "de" to "Bewertung einreichen", "es" to "Enviar reseña", "hi" to "समीक्षा सबमिट करें"),
    "Rate this song" to mapOf("ja" to "この曲を評価", "ko" to "이 곡 평가", "fr" to "Noter cette chanson", "de" to "Bewerte diesen Song", "es" to "Califica esta canción", "hi" to "इस गाने को रेट करें"),
    "Write a review" to mapOf("ja" to "レビューを書く", "ko" to "리뷰 작성", "fr" to "Écrire un avis", "de" to "Bewertung schreiben", "es" to "Escribe una reseña", "hi" to "समीक्षा लिखें"),
    "All Reviews" to mapOf("ja" to "すべてのレビュー", "ko" to "모든 리뷰", "fr" to "Tous les avis", "de" to "Alle Bewertungen", "es" to "Todas las reseñas", "hi" to "सभी समीक्षाएं"),
    "Create New List" to mapOf("ja" to "新しいリストを作成", "ko" to "새 목록 만들기", "fr" to "Créer une liste", "de" to "Neue Liste erstellen", "es" to "Crear lista", "hi" to "नई सूची बनाएं"),
    "Login" to mapOf("ja" to "ログイン", "ko" to "로그인", "fr" to "Connexion", "de" to "Anmelden", "es" to "Iniciar sesión", "hi" to "लॉग इन"),
    "Sign Up" to mapOf("ja" to "登録", "ko" to "가입", "fr" to "S'inscrire", "de" to "Registrieren", "es" to "Registrarse", "hi" to "साइन अप"),
)

/** Synchronous static-dictionary lookup. Returns the original text when there is no entry. */
object StaticTranslations {
    fun translate(text: String, lang: String): String {
        if (lang == "en") return text
        return TRANSLATIONS[text]?.get(lang) ?: text
    }
}

/**
 * Dynamic (Gemini-backed) translator for text not in the static dictionary — review bodies,
 * bios, post content. Results cached in-memory keyed by "lang::text". English passes through.
 */
class DynamicTranslator(private val api: SoundSpireApi) {
    private val cache = mutableMapOf<String, String>()
    private val mutex = Mutex()

    private fun key(text: String, lang: String) = "$lang::$text"

    suspend fun translate(text: String, lang: String): String {
        if (lang == "en" || text.isBlank()) return text
        // Static dictionary wins (free + instant).
        val staticHit = StaticTranslations.translate(text, lang)
        if (staticHit != text) return staticHit

        mutex.withLock { cache[key(text, lang)] }?.let { return it }
        val translated = try {
            val label = LANG_LABEL[lang] ?: lang
            api.translate(TranslateRequest(texts = listOf(text), targetLang = label))
                .translations.firstOrNull()?.takeIf { it.isNotBlank() } ?: text
        } catch (_: Exception) { text }
        mutex.withLock { cache[key(text, lang)] = translated }
        return translated
    }
}
