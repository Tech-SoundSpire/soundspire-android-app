package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.Artist
import com.example.data.model.Comment
import com.example.data.model.Review
import com.example.ui.components.VideoPlayer
import com.example.ui.theme.CardBackground
import com.example.ui.theme.MidnightBlack
import com.example.ui.theme.GoldStar
import com.example.ui.theme.SoundSpireAccentPurple
import com.example.ui.theme.SoundSpireNeonTeal
import com.example.ui.theme.SoundSpireVibrantBlue
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.SoundSpireViewModel

@Composable
fun ProfileScreen(
    viewModel: SoundSpireViewModel,
    modifier: Modifier = Modifier
) {
    val artist by viewModel.activeArtist.collectAsState()
    val reviews by viewModel.activeReviews.collectAsState()
    val comments by viewModel.activeComments.collectAsState()

    var activeTab by remember { mutableStateOf("stream") } // "stream", "reviews", "discussions"

    if (artist == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MidnightBlack),
            contentAlignment = Alignment.Center
        ) {
            Text("Select an artist from discover...", color = TextSecondary)
        }
        return
    }

    val currentArtist = artist!!

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MidnightBlack)
    ) {
        // Slim Header Details
        ProfileHeader(
            artistName = currentArtist.name,
            onBackClick = { viewModel.selectArtist(null) }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hero Banner & Overlap Avatar
            item {
                ArtistCoverHero(artist = currentArtist)
            }

            // Bio description block
            item {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentArtist.name,
                                    color = TextPrimary,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                if (currentArtist.isVerified) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = "Verified Channels",
                                        tint = SoundSpireNeonTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Genres: ${currentArtist.genres}",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Rating badge
                        Row(
                            modifier = Modifier
                                .background(CardBackground, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = GoldStar,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (currentArtist.ratingCount > 0) String.format("%.1f", currentArtist.averageRating) else "0.0",
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = currentArtist.bio,
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Interactive Sections Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                        .padding(2.dp)
                ) {
                    TabPill(
                        title = "Live Stream",
                        isActive = activeTab == "stream",
                        icon = Icons.Default.GraphicEq,
                        onClick = { activeTab = "stream" },
                        modifier = Modifier.weight(1f),
                        testTag = "tab_stream"
                    )
                    TabPill(
                        title = "Fan Reviews (${reviews.size})",
                        isActive = activeTab == "reviews",
                        icon = Icons.Default.RateReview,
                        onClick = { activeTab = "reviews" },
                        modifier = Modifier.weight(1f),
                        testTag = "tab_reviews"
                    )
                    TabPill(
                        title = "Discuss (${comments.size})",
                        isActive = activeTab == "discussions",
                        icon = Icons.AutoMirrored.Filled.Comment,
                        onClick = { activeTab = "discussions" },
                        modifier = Modifier.weight(1f),
                        testTag = "tab_discuss"
                    )
                }
            }

            // Tab Panels render
            when (activeTab) {
                "stream" -> {
                    item {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "ACTIVE LIVE STREAM & PERFORMANCE",
                                color = SoundSpireVibrantBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            VideoPlayer(
                                videoUrl = currentArtist.videoUrl,
                                title = currentArtist.featuredTrackTitle,
                                modifier = Modifier.fillMaxWidth().testTag("active_video_player")
                            )

                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Artist Setup Details",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Feel free to check out reviews or write comments in the tabs. SoundSpire converts uploaded raw tracks to responsive streaming units dynamically.",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                lineHeight = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                "reviews" -> {
                    // Reviews Posting Block
                    item {
                        ReviewsFormPanel(
                            artistId = currentArtist.id,
                            onSubmit = { rating, comment ->
                                viewModel.submitReview(currentArtist.id, rating, comment)
                            }
                        )
                    }

                    item {
                        Text(
                            text = "COMMUNITY FEEDBACK",
                            color = SoundSpireAccentPurple,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp)
                        )
                    }

                    if (reviews.isEmpty()) {
                        item {
                            Text(
                                "No fan reviews launched yet. Be the first to rate standard!",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(32.dp)
                            )
                        }
                    } else {
                        items(reviews) { review ->
                            ReviewItemRow(review = review)
                        }
                    }
                }

                "discussions" -> {
                    // Discussion comments listing with nested replies
                    item {
                        DiscussionsFormPanel(
                            artistId = currentArtist.id,
                            comments = comments,
                            onSubmit = { commentText, parentId ->
                                viewModel.submitComment(currentArtist.id, commentText, parentId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    artistName: String,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBackground)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.testTag("profile_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = SoundSpireNeonTeal
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$artistName Studio",
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun ArtistCoverHero(artist: Artist) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        // Banner images
        AsyncImage(
            model = artist.bannerUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
        )

        // Shade overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))
                    )
                )
        )

        // Overlapping Profile Avatar (Offset strictly aligned downwards)
        AsyncImage(
            model = artist.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 20.dp, y = 20.dp)
                .size(76.dp)
                .clip(CircleShape)
                .border(3.dp, MidnightBlack, CircleShape)
                .border(4.dp, SoundSpireNeonTeal, CircleShape)
        )
    }
    // Vertical Spacer to compensate offset push
    Spacer(modifier = Modifier.height(24.dp))
}

@Composable
private fun TabPill(
    title: String,
    isActive: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isActive) SoundSpireNeonTeal else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) MidnightBlack else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isActive) MidnightBlack else TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ReviewsFormPanel(
    artistId: String,
    onSubmit: (Int, String) -> Unit
) {
    var textReview by remember { mutableStateOf("") }
    var ratingChosen by remember { mutableStateOf(5) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "SUBMIT FAN REVIEW",
                color = SoundSpireVibrantBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // Dynamic stars selector layout
            Row(
                modifier = Modifier.padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (starIndex in 1..5) {
                    val active = starIndex <= ratingChosen
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Star $starIndex",
                        tint = if (active) GoldStar else Color.LightGray.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(28.dp)
                            .clickable { ratingChosen = starIndex }
                            .testTag("star_select_$starIndex")
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = textReview,
                    onValueChange = { textReview = it },
                    placeholder = { Text("Write your concert feedback, synth reviews...", fontSize = 11.sp, color = TextSecondary) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("text_review_field"),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (textReview.isNotBlank()) {
                            onSubmit(ratingChosen, textReview)
                            textReview = ""
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(SoundSpireNeonTeal, RoundedCornerShape(8.dp))
                        .testTag("submit_review_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Submit",
                        tint = MidnightBlack,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReviewItemRow(review: Review) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = review.fanName,
                    color = SoundSpireNeonTeal,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                // Ratings star string
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (i in 1..5) {
                        val color = if (i <= review.rating) GoldStar else Color.Transparent
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (i <= review.rating) GoldStar else Color.LightGray.copy(alpha = 0.2f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = review.comment,
                color = TextPrimary,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}

@Composable
private fun DiscussionsFormPanel(
    artistId: String,
    comments: List<Comment>,
    onSubmit: (String, String?) -> Unit
) {
    var rawTextComment by remember { mutableStateOf("") }
    var focusedParentId: String? by remember { mutableStateOf(null) }
    var focusedParentName: String? by remember { mutableStateOf(null) }

    val topLevelComments = comments.filter { it.parentId == null }

    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        // Nested replies hud indicator if active
        if (focusedParentId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoundSpireAccentPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = null,
                        tint = SoundSpireAccentPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Replying to ${focusedParentName}",
                        color = SoundSpireAccentPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(
                    onClick = {
                        focusedParentId = null
                        focusedParentName = null
                    },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        tint = SoundSpireAccentPurple,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Input form
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 10.dp)
        ) {
            OutlinedTextField(
                value = rawTextComment,
                onValueChange = { rawTextComment = it },
                placeholder = { Text(if (focusedParentId != null) "Replying to note..." else "Join discussions, ask for setups...", fontSize = 11.sp, color = TextSecondary) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("comments_input_field"),
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SoundSpireNeonTeal)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (rawTextComment.isNotBlank()) {
                        onSubmit(rawTextComment, focusedParentId)
                        // Reset replies states
                        rawTextComment = ""
                        focusedParentId = null
                        focusedParentName = null
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(SoundSpireNeonTeal, RoundedCornerShape(8.dp))
                    .testTag("submit_comment_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Submit",
                    tint = MidnightBlack,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Text(
            text = "DISCUSSIONS & REPLIES",
            color = SoundSpireVibrantBlue,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 10.dp)
        )

        if (topLevelComments.isEmpty()) {
            Text(
                "No live chats yet. Setup a discussion!",
                color = TextSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )
        } else {
            // Render comments, including its sub-replies right below
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                for (topComment in topLevelComments) {
                    CommentCard(
                        comment = topComment,
                        onReplyClick = {
                            focusedParentId = topComment.id
                            focusedParentName = topComment.userName
                        }
                    )

                    // Find replies associated with this parent
                    val replies = comments.filter { it.parentId == topComment.id }
                    if (replies.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .padding(start = 24.dp)
                                .border(1.dp, SoundSpireAccentPurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .padding(vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (reply in replies) {
                                CommentCard(
                                    comment = reply,
                                    isReply = true,
                                    onReplyClick = {}
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentCard(
    comment: Comment,
    isReply: Boolean = false,
    onReplyClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (isReply) Color.Transparent else CardBackground),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.userName,
                        color = if (comment.userRole == "artist") SoundSpireNeonTeal else SoundSpireVibrantBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (comment.userRole == "artist") {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = "Online",
                            tint = SoundSpireNeonTeal,
                            modifier = Modifier.size(6.dp)
                        )
                    }
                }

                if (!isReply) {
                    IconButton(
                        onClick = onReplyClick,
                        modifier = Modifier.size(24.dp).testTag("reply_button_${comment.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "Reply",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = comment.commentText,
                color = TextPrimary,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}
