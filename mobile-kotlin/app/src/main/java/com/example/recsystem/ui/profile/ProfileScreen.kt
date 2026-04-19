package com.example.recsystem.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recsystem.data.model.Movie
import com.example.recsystem.data.model.PersonalizationProfile
import com.example.recsystem.data.model.UserAccount
import com.example.recsystem.ui.theme.CineNavy
import com.example.recsystem.ui.theme.CineRed
import com.example.recsystem.ui.theme.CineCream
import com.example.recsystem.ui.theme.NeutralSurface
import com.example.recsystem.ui.theme.TagPurple

@Composable
fun FullProfileScreen(
    user: UserAccount,
    viewModel: ProfileViewModel,
    onMovieClick: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show success / error snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.successMessage, uiState.saveError) {
        val msg = uiState.successMessage ?: uiState.saveError
        if (msg != null) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NeutralSurface
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CineNavy)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(CineCream),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = CineNavy,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text  = user.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text  = user.email,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                }
            }

            // ── Loading shimmer ───────────────────────────────────────────
            if (uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = CineRed) }
                }
                return@LazyColumn
            }

            val profile = uiState.profile

            // ── Saved Movies watchlist ────────────────────────────────────
            item {
                SectionHeader(
                    title = "My Watchlist",
                    count = profile?.savedMovies?.size ?: 0
                )
            }
            if (profile?.savedMovies.isNullOrEmpty()) {
                item {
                    EmptyHint("Movies you save will appear here")
                }
            } else {
                items(profile!!.savedMovies) { movie ->
                    SavedMovieRow(
                        movie = movie,
                        onClick = {
                            onMovieClick("movie", movie.id.toString())
                        },
                        onRemove = { viewModel.removeMovie(movie.id) }
                    )
                }
            }

            // ── Recent searches ───────────────────────────────────────────
            if (!profile?.recentSearches.isNullOrEmpty()) {
                item { SectionHeader(title = "Recent Searches") }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(profile!!.recentSearches) { query ->
                            SearchChip(query)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // ── Preferences editor ────────────────────────────────────────
            item {
                PreferencesEditor(
                    profile  = profile,
                    isSaving = uiState.isSaving,
                    onSave   = { dn, age, genres, kws, actors, actresses, dirs ->
                        viewModel.updatePreferences(dn, age, genres, kws, actors, actresses, dirs)
                    }
                )
            }

            // ── Logout ────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick  = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(50.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Logout") }
            }
        }
    }
}

// ─── Saved movie row ──────────────────────────────────────────────────────────

@Composable
private fun SavedMovieRow(movie: Movie, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape     = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model            = movie.posterPath,
                contentDescription = movie.title,
                contentScale     = ContentScale.Crop,
                modifier         = Modifier
                    .width(48.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEEEEEE))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text      = movie.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize  = 14.sp,
                    maxLines  = 2,
                    overflow  = TextOverflow.Ellipsis
                )
                if (movie.releaseDate.length >= 4) {
                    Text(
                        text  = movie.releaseDate.take(4),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector        = Icons.Default.Close,
                    contentDescription = "Remove",
                    tint               = Color.Gray
                )
            }
        }
    }
}

// ─── Preferences editor ───────────────────────────────────────────────────────

@Composable
private fun PreferencesEditor(
    profile: PersonalizationProfile?,
    isSaving: Boolean,
    onSave: (
        displayName: String,
        age: Int?,
        genres: List<String>,
        keywords: List<String>,
        actors: List<String>,
        actresses: List<String>,
        directors: List<String>
    ) -> Unit
) {
    var displayName  by remember(profile?.displayName)  { mutableStateOf(profile?.displayName ?: "") }
    var ageText      by remember(profile?.age)           { mutableStateOf(profile?.age?.toString() ?: "") }
    var genres       by remember(profile?.favoriteGenres)      { mutableStateOf(profile?.favoriteGenres ?: emptyList()) }
    var keywords     by remember(profile?.favoriteKeywords)    { mutableStateOf(profile?.favoriteKeywords ?: emptyList()) }
    var actors       by remember(profile?.favoriteActors)      { mutableStateOf(profile?.favoriteActors ?: emptyList()) }
    var actresses    by remember(profile?.favoriteActresses)   { mutableStateOf(profile?.favoriteActresses ?: emptyList()) }
    var directors    by remember(profile?.favoriteDirectors)   { mutableStateOf(profile?.favoriteDirectors ?: emptyList()) }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(16.dp),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Preferences",
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp,
                modifier   = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
                value         = displayName,
                onValueChange = { displayName = it },
                label         = { Text("Display name") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            OutlinedTextField(
                value             = ageText,
                onValueChange     = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                label             = { Text("Age (optional)") },
                singleLine        = true,
                keyboardOptions   = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier          = Modifier.fillMaxWidth().padding(bottom = 10.dp)
            )

            ChipInputField(
                label  = "Favourite genres (e.g. Sci-Fi, Drama)",
                chips  = genres,
                onAdd  = { if (it.isNotBlank()) genres = genres + it.trim() },
                onRemove = { genres = genres - it }
            )
            ChipInputField(
                label    = "Themes / keywords (e.g. space, heist)",
                chips    = keywords,
                onAdd    = { if (it.isNotBlank()) keywords = keywords + it.trim() },
                onRemove = { keywords = keywords - it }
            )
            ChipInputField(
                label    = "Favourite actors",
                chips    = actors,
                onAdd    = { if (it.isNotBlank()) actors = actors + it.trim() },
                onRemove = { actors = actors - it }
            )
            ChipInputField(
                label    = "Favourite actresses",
                chips    = actresses,
                onAdd    = { if (it.isNotBlank()) actresses = actresses + it.trim() },
                onRemove = { actresses = actresses - it }
            )
            ChipInputField(
                label    = "Favourite directors",
                chips    = directors,
                onAdd    = { if (it.isNotBlank()) directors = directors + it.trim() },
                onRemove = { directors = directors - it }
            )

            Spacer(Modifier.height(12.dp))
            Button(
                onClick  = {
                    val age = ageText.toIntOrNull()
                    onSave(displayName, age, genres, keywords, actors, actresses, directors)
                },
                enabled  = !isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CineRed)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        color    = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Save preferences", color = Color.White)
                }
            }
        }
    }
}

// ─── Chip input field ─────────────────────────────────────────────────────────

@Composable
private fun ChipInputField(
    label: String,
    chips: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }

    Text(
        text     = label,
        fontSize = 12.sp,
        color    = Color.Gray,
        modifier = Modifier.padding(bottom = 4.dp)
    )

    // Existing chips
    if (chips.isNotEmpty()) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 6.dp)
        ) {
            items(chips) { chip ->
                InputChip(
                    selected = false,
                    onClick  = {},
                    label    = { Text(chip, fontSize = 12.sp) },
                    trailingIcon = {
                        Icon(
                            imageVector        = Icons.Default.Close,
                            contentDescription = "Remove $chip",
                            modifier           = Modifier.size(14.dp).clickable { onRemove(chip) }
                        )
                    }
                )
            }
        }
    }

    // Add new chip
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value         = input,
            onValueChange = { input = it },
            singleLine    = true,
            placeholder   = { Text("Add…", fontSize = 13.sp) },
            modifier      = Modifier.weight(1f),
            textStyle     = LocalTextStyle.current.copy(fontSize = 13.sp)
        )
        Spacer(Modifier.width(6.dp))
        IconButton(
            onClick  = { onAdd(input); input = "" },
            enabled  = input.isNotBlank()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = CineRed)
        }
    }
    Spacer(Modifier.height(8.dp))
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, count: Int? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
        if (count != null && count > 0) {
            Text("$count", fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.Gray, fontSize = 14.sp)
    }
}

@Composable
private fun SearchChip(query: String) {
    Text(
        text     = query,
        fontSize = 12.sp,
        color    = TagPurple,
        modifier = Modifier
            .background(TagPurple.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
