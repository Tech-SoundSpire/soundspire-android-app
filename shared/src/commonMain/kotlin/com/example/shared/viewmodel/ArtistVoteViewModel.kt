package com.example.shared.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shared.data.model.ArtistVoteRequest
import com.example.shared.network.SoundSpireApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

data class ArtistVoteUiState(
    val loading: Boolean = true,
    val name: String? = null,
    val imageUrl: String? = null,
    val biography: String? = null,
    val voteCount: Int = 0,
    val userVoted: Boolean = false,
    val voting: Boolean = false,
)

// Port of ArtistVoteScreen.kt: vote for an off-platform (SoundCharts) artist to join SoundSpire.
class ArtistVoteViewModel(private val api: SoundSpireApi, private val uuid: String) : ViewModel() {

    private val _state = MutableStateFlow(ArtistVoteUiState())
    val state: StateFlow<ArtistVoteUiState> = _state.asStateFlow()
    private var userId: String? = null

    init { load() }

    private fun load() {
        viewModelScope.launch {
            userId = runCatching { api.getSession().user?.id }.getOrNull()
            val obj = runCatching { api.getArtistByUuid(uuid) }.getOrNull()
            val name = obj?.get("name")?.jsonPrimitive?.contentOrNull
            val imageUrl = obj?.get("imageUrl")?.jsonPrimitive?.contentOrNull
            val bio = obj?.get("biography")?.jsonPrimitive?.contentOrNull
            val vote = runCatching { api.getArtistVote(uuid, userId) }.getOrNull()
            _state.value = _state.value.copy(
                loading = false, name = name, imageUrl = imageUrl, biography = bio,
                voteCount = vote?.count ?: 0, userVoted = vote?.userVoted ?: false,
            )
        }
    }

    fun vote() {
        val uid = userId ?: return
        if (_state.value.userVoted || _state.value.voting) return
        _state.value = _state.value.copy(voting = true)
        viewModelScope.launch {
            val result = runCatching {
                api.castArtistVote(ArtistVoteRequest(
                    soundcharts_uuid = uuid, artist_name = _state.value.name ?: "",
                    image_url = _state.value.imageUrl, userId = uid,
                ))
            }.getOrNull()
            _state.value = _state.value.copy(
                voting = false,
                voteCount = result?.count ?: _state.value.voteCount,
                userVoted = result != null || _state.value.userVoted,
            )
        }
    }
}
