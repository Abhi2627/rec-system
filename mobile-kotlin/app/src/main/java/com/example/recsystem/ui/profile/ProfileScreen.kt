package com.example.recsystem.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recsystem.data.model.Movie
import com.example.recsystem.data.model.PersonalizationProfile
import com.example.recsystem.data.model.UserAccount
import com.example.recsystem.ui.theme.CineCream
import com.example.recsystem.ui.theme.CineNavy
import com.example.recsystem.ui.theme.CineRed
import com.example.recsystem.ui.theme.TagPurple

// ── Genre list (mirrors OnboardingScreen) ─────────────────────────────────────
private val GENRE_OPTIONS = listOf(
    "Action", "Adventure", "Animation", "Comedy", "Crime",
    "Documentary", "Drama", "Family", "Fantasy", "History",
    "Horror", "Music", "Mystery", "Romance", "Sci-Fi",
    "Thriller", "War", "Western"
)

@Composable
fun FullProfileScreen(
    user: UserAccount,
    viewModel: ProfileViewModel,
    onMovieClick: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
        containerColor = Color(0xFFF5F5F5)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp)
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
                        modifier = Modifier.size(80.dp).clip(CircleShape).background(CineCream),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, null, tint = CineNavy, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(user.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(user.email, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                }
            }

            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CineRed)
                    }
                }
                return@LazyColumn
            }

            val profile = uiState.profile

            // ── Watchlist ─────────────────────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("My Watchlist", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A1A), modifier = Modifier.weight(1f))
                    if ((profile?.savedMovies?.size ?: 0) > 0)
                        Text("${profile!!.savedMovies.size}", fontSize = 13.sp, color = Color(0xFF666666))
                }
            }
            if (profile?.savedMovies.isNullOrEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                        Text("Movies you save will appear here", color = Color(0xFF888888), fontSize = 14.sp)
                    }
                }
            } else {
                items(profile!!.savedMovies) { movie ->
                    SavedMovieRow(movie, onClick = { onMovieClick("movie", movie.id.toString()) }, onRemove = { viewModel.removeMovie(movie.id) })
                }
            }

            // ── Recent searches ───────────────────────────────────────────
            if (!profile?.recentSearches.isNullOrEmpty()) {
                item {
                    Text("Recent Searches", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A1A), modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(profile!!.recentSearches) { q ->
                            Text(
                                q, fontSize = 12.sp, color = TagPurple,
                                modifier = Modifier
                                    .background(TagPurple.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
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
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(50.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFFCC3333))
                ) { Text("Logout", color = Color.White, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

// ─── Saved movie row ──────────────────────────────────────────────────────────

@Composable
private fun SavedMovieRow(movie: Movie, onClick: () -> Unit, onRemove: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).clickable { onClick() },
        shape     = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = movie.posterPath,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(48.dp).height(72.dp).clip(RoundedCornerShape(6.dp)).background(Color(0xFFEEEEEE))
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(movie.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color(0xFF1A1A1A))
                if (movie.releaseDate.length >= 4)
                    Text(movie.releaseDate.take(4), fontSize = 12.sp, color = Color(0xFF666666))
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, "Remove", tint = Color(0xFF888888))
            }
        }
    }
}

// ─── Preferences editor ───────────────────────────────────────────────────────

@Composable
private fun PreferencesEditor(
    profile: PersonalizationProfile?,
    isSaving: Boolean,
    onSave: (String, Int?, List<String>, List<String>, List<String>, List<String>, List<String>) -> Unit
) {
    var displayName by remember(profile?.displayName) { mutableStateOf(profile?.displayName ?: "") }
    var ageText     by remember(profile?.age) { mutableStateOf(profile?.age?.toString() ?: "") }
    // Genres — tap to toggle from a fixed grid
    var genres      by remember(profile?.favoriteGenres) { mutableStateOf((profile?.favoriteGenres ?: emptyList()).toSet()) }
    // Free-form lists
    var keywords    by remember(profile?.favoriteKeywords) { mutableStateOf(profile?.favoriteKeywords ?: emptyList()) }
    var actors      by remember(profile?.favoriteActors) { mutableStateOf(profile?.favoriteActors ?: emptyList()) }
    var actresses   by remember(profile?.favoriteActresses) { mutableStateOf(profile?.favoriteActresses ?: emptyList()) }
    var directors   by remember(profile?.favoriteDirectors) { mutableStateOf(profile?.favoriteDirectors ?: emptyList()) }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(16.dp),
        shape     = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Preferences", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1A1A1A), modifier = Modifier.padding(bottom = 14.dp))

            // Display name
            ProfileTextField(value = displayName, onValueChange = { displayName = it }, label = "Display name")
            Spacer(Modifier.height(10.dp))

            // Age
            ProfileTextField(
                value = ageText,
                onValueChange = { ageText = it.filter { c -> c.isDigit() }.take(3) },
                label = "Age (optional)",
                keyboardType = KeyboardType.Number
            )
            Spacer(Modifier.height(14.dp))

            // Genres — tap to toggle grid
            Text("Favourite genres", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
            Spacer(Modifier.height(8.dp))
            // 3-column wrap grid using FlowRow emulation with LazyVerticalGrid in fixed height
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp),
                userScrollEnabled = false
            ) {
                items(GENRE_OPTIONS) { genre ->
                    val selected = genre in genres
                    Text(
                        text     = genre,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color    = if (selected) Color.White else Color(0xFF333333),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, if (selected) CineRed else Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                            .background(if (selected) CineRed else Color.White)
                            .clickable {
                                genres = if (selected) genres - genre else genres + genre
                            }
                            .padding(horizontal = 6.dp, vertical = 10.dp)
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // Keywords / themes
            ChipInputField("Themes (e.g. space, heist)", keywords, { if (it.isNotBlank()) keywords = keywords + it.trim() }, { keywords = keywords - it })
            // Favourite actors
            ChipInputField("Favourite actors", actors, { if (it.isNotBlank()) actors = actors + it.trim() }, { actors = actors - it })
            // Favourite actresses
            ChipInputField("Favourite actresses", actresses, { if (it.isNotBlank()) actresses = actresses + it.trim() }, { actresses = actresses - it })
            // Directors
            ChipInputField("Favourite directors", directors, { if (it.isNotBlank()) directors = directors + it.trim() }, { directors = directors - it })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick  = { onSave(displayName, ageText.toIntOrNull(), genres.toList(), keywords, actors, actresses, directors) },
                enabled  = !isSaving,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CineRed)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Save preferences", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValueChange,
        label         = { Text(label, color = Color(0xFF666666), fontSize = 13.sp) },
        singleLine    = true,
        textStyle     = TextStyle(color = Color(0xFF1A1A1A), fontSize = 14.sp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier      = Modifier.fillMaxWidth(),
        colors        = OutlinedTextFieldDefaults.colors(
            focusedTextColor        = Color(0xFF1A1A1A),
            unfocusedTextColor      = Color(0xFF1A1A1A),
            focusedBorderColor      = CineNavy,
            unfocusedBorderColor    = Color(0xFFCCCCCC),
            focusedContainerColor   = Color.White,
            unfocusedContainerColor = Color.White,
            cursorColor             = CineNavy
        )
    )
}

@Composable
private fun ChipInputField(
    label: String,
    chips: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333), modifier = Modifier.padding(bottom = 6.dp))
    if (chips.isNotEmpty()) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
            items(chips) { chip ->
                InputChip(
                    selected = false, onClick = {},
                    label = { Text(chip, fontSize = 12.sp, color = Color(0xFF1A1A1A)) },
                    trailingIcon = {
                        Icon(Icons.Default.Close, "Remove $chip",
                            modifier = Modifier.size(14.dp).clickable { onRemove(chip) },
                            tint = Color(0xFF666666))
                    }
                )
            }
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = input, onValueChange = { input = it },
            singleLine = true,
            placeholder = { Text("Add...", fontSize = 13.sp, color = Color(0xFF888888)) },
            textStyle = TextStyle(color = Color(0xFF1A1A1A), fontSize = 13.sp),
            modifier = Modifier.weight(1f),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF1A1A1A), unfocusedTextColor = Color(0xFF1A1A1A),
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                cursorColor = CineNavy
            )
        )
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = { onAdd(input); input = "" }, enabled = input.isNotBlank()) {
            Icon(Icons.Default.Add, "Add", tint = if (input.isNotBlank()) CineRed else Color(0xFFCCCCCC))
        }
    }
    Spacer(Modifier.height(10.dp))
}
