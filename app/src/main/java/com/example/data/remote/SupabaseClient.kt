package com.example.data.remote

import com.example.BuildConfig
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.presenceChangeFlow
import io.github.jan.supabase.realtime.PostgresAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Supabase client for All Chat — talks directly to the `forum_posts` table
 * and subscribes to realtime postgres changes, mirroring the web client.
 */
object SupabaseManager {
    val client by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY,
        ) {
            install(Postgrest)
            install(Realtime)
        }
    }

    @Serializable
    data class ForumPostInsert(
        val forum_id: String,
        val user_id: String,
        val content: String,
        val media_type: String,
        val media_urls: List<String> = emptyList(),
        val parent_post_id: String? = null,
    )

    /**
     * A forum_posts row as stored in Supabase. The backend `/messages` endpoint strips
     * `parent_post_id` and `reactions` (its Sequelize model doesn't declare them), so we
     * read directly from Supabase — mirroring the website's All Chat client.
     */
    @Serializable
    data class ForumPostRow(
        val forum_post_id: String,
        val forum_id: String? = null,
        val user_id: String? = null,
        val content: String? = null,
        val media_type: String? = null,
        val media_urls: List<String>? = null,
        val parent_post_id: String? = null,
        val created_at: String? = null,
        val reactions: Map<String, List<String>>? = null,
    )

    /** Read all messages for a forum directly from Supabase (includes parent_post_id + reactions). */
    suspend fun fetchMessages(forumId: String, limit: Long = 200): List<ForumPostRow> {
        return client.from("forum_posts").select {
            filter { eq("forum_id", forumId) }
            order("created_at", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
            limit(limit)
        }.decodeList()
    }

    /** Insert a forum_post row (chat message / comment). */
    suspend fun insertMessage(row: ForumPostInsert) {
        client.from("forum_posts").insert(row)
    }

    /** Delete a message by id. */
    suspend fun deleteMessage(forumPostId: String) {
        client.from("forum_posts").delete {
            filter { eq("forum_post_id", forumPostId) }
        }
    }

    /** Edit a message's content. */
    suspend fun updateMessageContent(forumPostId: String, content: String) {
        client.from("forum_posts").update(buildJsonObject { put("content", content) }) {
            filter { eq("forum_post_id", forumPostId) }
        }
    }

    /**
     * Subscribe to realtime INSERT/UPDATE/DELETE on forum_posts for a forum.
     * Subscribes the channel, returns the action flow, and unsubscribes on completion.
     */
    suspend fun forumChanges(forumId: String): Flow<PostgresAction> {
        val channel = client.channel("forum:$forumId")
        val flow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "forum_posts"
            filter("forum_id", io.github.jan.supabase.postgrest.query.filter.FilterOperator.EQ, forumId)
        }
        channel.subscribe()
        return flow.onCompletion {
            try { channel.unsubscribe() } catch (_: Exception) {}
        }
    }

    data class OnlineUser(val userId: String, val username: String)

    // One long-lived shared presence flow per community. Reused across screen remounts
    // so navigating away and back doesn't tear down + re-subscribe the channel (which
    // raced the async unsubscribe and dropped the count back to 1).
    private val presenceScope = CoroutineScope(Dispatchers.IO)
    private val presenceFlows = mutableMapOf<String, Flow<List<OnlineUser>>>()

    /**
     * Track presence in a community's presence channel and emit the current online
     * users whenever someone joins/leaves. Mirrors the web `useCommunityPresence`.
     *
     * Three things are critical to match the web behavior:
     *  1. Presence is keyed by `userId` (web uses `presence: { key: user.id }`). Without
     *     this, reconnects churn under random keys and the count drifts.
     *  2. The presence-change callback MUST be registered BEFORE `subscribe()`, otherwise
     *     the initial full PRESENCE_STATE sync (which contains users who were already
     *     online — e.g. a fan whose chat was open first) is delivered before we listen
     *     and is lost, leaving the artist stuck at "1 online".
     *  3. The channel is kept alive (shareIn with a stop timeout) so quick screen
     *     remounts reuse the same subscription instead of churning it.
     */
    @Synchronized
    fun communityPresence(communityId: String, userId: String, username: String): Flow<List<OnlineUser>> {
        return presenceFlows.getOrPut(communityId) {
            val upstream = channelFlow {
                val channel = client.channel("community:$communityId:presence") {
                    presence { key = userId }
                }

                // Ref-count presences per user. A user is online while they have >=1 live
                // presence ref. This is what makes a reload survive: the fan's reconnect
                // emits a join (new ref) + a leave (old ref) under the same userId key —
                // removing only the *specific* old ref leaves the new one intact, so the
                // count doesn't drop regardless of which event arrives first.
                val refs = mutableMapOf<String, MutableSet<String>>()
                val names = mutableMapOf<String, String>()

                fun snapshot(): List<OnlineUser> =
                    refs.filterValues { it.isNotEmpty() }.keys.map { OnlineUser(it, names[it] ?: "User") }

                // Start collecting BEFORE subscribing so the initial PRESENCE_STATE is captured.
                val job = launch {
                    channel.presenceChangeFlow().collect { action ->
                        action.leaves.forEach { (key, presence) ->
                            refs[key]?.remove(presence.presenceRef)
                            if (refs[key]?.isEmpty() == true) { refs.remove(key); names.remove(key) }
                        }
                        action.joins.forEach { (key, presence) ->
                            refs.getOrPut(key) { mutableSetOf() }.add(presence.presenceRef)
                            val name = try {
                                presence.state["username"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.content }
                            } catch (_: Exception) { null }
                            if (name != null) names[key] = name
                        }
                        trySend(snapshot())
                    }
                }

                channel.subscribe(blockUntilSubscribed = true)
                try {
                    channel.track(buildJsonObject {
                        put("user_id", userId)
                        put("username", username)
                    })
                } catch (_: Exception) {}

                awaitClose {
                    job.cancel()
                    CoroutineScope(Dispatchers.IO).launch {
                        try { channel.unsubscribe() } catch (_: Exception) {}
                    }
                }
            }
            // Keep the channel subscribed for 30s after the last collector leaves, so a
            // navigate-away-and-back reuses it. replay=1 re-emits the latest count instantly.
            upstream.shareIn(
                scope = presenceScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 30_000),
                replay = 1,
            )
        }
    }
}
