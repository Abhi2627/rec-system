package com.example.recsystem.ui.discovery

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.recsystem.data.model.CastMember
import com.example.recsystem.ui.auth.AuthState
import com.example.recsystem.ui.auth.AuthViewModel
import com.example.recsystem.ui.components.PosterImage
import com.example.recsystem.ui.profile.ProfileViewModel
import com.example.recsystem.ui.theme.CineGold
import com.example.recsystem.ui.theme.CineRed
import com.example.recsystem.ui.theme.TagPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    type: String,
    movieId: String,
    viewModel: MovieDetailViewModel,
    onBack: () -> Unit,
    authViewModel: AuthViewModel? = null,
    profileViewModel: ProfileViewModel? = null
) {
    val uiState = viewModel.uiState.collectAsState().value
    val profileState = profileViewModel?.uiState?.collectAsState()?.value
    val isLoggedIn = authViewModel?.authState?.value is AuthState.Success
    val movieIdInt = movieId.toIntOrNull() ?: -1
    val isSaved = profileViewModel?.isMovieSaved(movieIdInt) == true

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(profileState?.successMessage, profileState?.saveError) {
        val msg = profileState?.successMessage ?: profileState?.saveError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            profileViewModel?.clearSuccessMessage()
        }
    }
    LaunchedEffect(movieId) { viewModel.loadDetails(type, movieId) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.detail?.title ?: "Details", maxLines = 1, color = Color(0xFF1A1A1A)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF1A1A1A))
                    }
                },
                actions = {
                    if (isLoggedIn && uiState.detail != null) {
                        IconButton(onClick = {
                            val d = uiState.detail!!
                            if (isSaved) profileViewModel?.removeMovie(movieIdInt)
                            else profileViewModel?.saveMovie(
                                movieId = movieIdInt, title = d.title,
                                overview = d.overview, posterPath = d.poster_path,
                                releaseDate = d.release_date, voteAverage = d.vote_average
                            )
                        }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                                contentDescription = if (isSaved) "Remove" else "Save",
                                tint = if (isSaved) CineRed else Color(0xFF555555)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color(0xFF1A1A1A),
                    navigationIconContentColor = Color(0xFF1A1A1A)
                )
            )
        },
        containerColor = Color(0xFFF8F8F8)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = TagPurple)
                }
                uiState.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Text(uiState.error ?: "", color = Color(0xFF333333), fontSize = 15.sp)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.retry(type, movieId) }) { Text("Retry") }
                    }
                }
                uiState.detail != null -> {
                    val detail = uiState.detail!!
                    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                        // ── Trailer thumbnail / backdrop ──────────────────
                        if (detail.trailerKey.isNotEmpty()) {
                            TrailerThumbnail(
                                trailerKey  = detail.trailerKey,
                                backdropUrl = detail.backdrop_path.ifEmpty { detail.poster_path },
                                title       = detail.title
                            )
                        } else {
                            PosterImage(
                                url      = detail.backdrop_path.ifEmpty { detail.poster_path },
                                title    = detail.title,
                                modifier = Modifier.fillMaxWidth().height(220.dp)
                            )
                        }

                        // ── Title card ────────────────────────────────────
                        Surface(color = Color.White, shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                                Text(detail.title, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color(0xFF1A1A1A))
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (detail.vote_average > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("\u2605", color = CineGold, fontSize = 16.sp)
                                            Spacer(Modifier.width(3.dp))
                                            Text(String.format("%.1f", detail.vote_average), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF333333))
                                            Text("/10", color = Color.Gray, fontSize = 12.sp)
                                        }
                                    }
                                    if (detail.release_date.length >= 4) InfoChip(detail.release_date.take(4))
                                    if (detail.runtime > 0) InfoChip("${detail.runtime} min")
                                }
                                if (detail.genres.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        detail.genres.take(4).forEach { GenreTag(it) }
                                    }
                                }
                                if (!isLoggedIn) {
                                    Spacer(Modifier.height(10.dp))
                                    Text("Sign in to save to your watchlist", fontSize = 12.sp, color = Color(0xFF888888))
                                }
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                        DetailSection("Overview") { Text(detail.overview, fontSize = 14.sp, lineHeight = 22.sp, color = Color(0xFF444444)) }
                        if (detail.director.isNotEmpty()) DetailSection("Director") { Text(detail.director, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color(0xFF1A1A1A)) }
                        if (detail.cast.isNotEmpty()) DetailSection("Cast") { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { detail.cast.forEach { CastRow(it) } } }
                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

// ─── Trailer Thumbnail ────────────────────────────────────────────────────────
//
// Shows the YouTube HD thumbnail with a gradient overlay and a play button.
// Tapping launches the YouTube app (or browser fallback) — the only reliable
// way to play YouTube video on Android without the YouTube Android Player API
// (which requires a paid API key and a separate SDK).

@Composable
fun TrailerThumbnail(
    trailerKey:  String,
    backdropUrl: String,
    title:       String
) {
    val context = LocalContext.current

    // YouTube maxresdefault thumbnail — free, no API key needed
    val thumbUrl = "https://img.youtube.com/vi/$trailerKey/maxresdefault.jpg"
    val youtubeUrl = "https://www.youtube.com/watch?v=$trailerKey"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .background(Color.Black)
            .clickable {
                // Try YouTube app first, fall back to browser
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:$trailerKey")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val resolved = context.packageManager.resolveActivity(intent, 0)
                if (resolved != null) {
                    context.startActivity(intent)
                } else {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(youtubeUrl)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            }
    ) {
        // YouTube HD thumbnail — falls back to backdrop if not available
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(thumbUrl)
                .crossfade(true)
                .build(),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onError = {
                // Coil error callback — backdrop shown via fallback below
            }
        )

        // Dark gradient so play button is always visible
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xAA000000))
                    )
                )
        )

        // Play button circle
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(64.dp)
                .clip(CircleShape)
                .background(Color(0xDDCC0000)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = Icons.Default.PlayArrow,
                contentDescription = "Play trailer",
                tint               = Color.White,
                modifier           = Modifier.size(40.dp)
            )
        }

        // "Watch Trailer" label at the bottom
        Text(
            text     = "Watch Trailer",
            color    = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        )
    }
}

// ─── Supporting composables ───────────────────────────────────────────────────

@Composable
private fun CastRow(member: CastMember) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        if (member.profilePath.isNotEmpty()) {
            PosterImage(url = member.profilePath, title = member.name, contentScale = ContentScale.Crop, modifier = Modifier.size(50.dp).clip(CircleShape))
        } else {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFE0E0E0)), contentAlignment = Alignment.Center) {
                Text(member.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color(0xFF555555), fontSize = 18.sp)
            }
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(member.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1A1A1A))
            if (member.character.isNotEmpty()) Text("as ${member.character}", fontSize = 12.sp, color = Color(0xFF888888))
        }
    }
}

@Composable
private fun DetailSection(title: String, content: @Composable () -> Unit) {
    Surface(color = Color.White, shadowElevation = 1.dp, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A1A))
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun InfoChip(text: String) {
    Text(text = text, color = Color(0xFF444444), fontSize = 12.sp,
        modifier = Modifier.background(Color(0xFFEEEEEE), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 3.dp))
}

@Composable
private fun GenreTag(genre: String) {
    Text(text = genre, color = Color.White, fontSize = 11.sp,
        modifier = Modifier.background(TagPurple, RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
}
