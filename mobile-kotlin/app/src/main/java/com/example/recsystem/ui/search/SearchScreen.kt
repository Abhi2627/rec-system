package com.example.recsystem.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recsystem.data.model.Movie

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onMovieClick: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1A1A2E))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("\uD83D\uDD0D", fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Search",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }

        // ── Search bar — explicit dark text so it's always readable ───────
        OutlinedTextField(
            value = uiState.query,
            onValueChange = { viewModel.onQueryChange(it) },
            placeholder = { Text("Search movies...", color = Color(0xFF888888), fontSize = 15.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF555555)) },
            trailingIcon = {
                if (uiState.query.isNotBlank()) {
                    IconButton(onClick = { viewModel.clearSearch() }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color(0xFF555555))
                    }
                }
            },
            textStyle = TextStyle(color = Color(0xFF1A1A1A), fontSize = 15.sp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    viewModel.search()
                    focusManager.clearFocus()
                }
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .focusRequester(focusRequester),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor   = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor        = Color(0xFF1A1A1A),
                unfocusedTextColor      = Color(0xFF1A1A1A),
                focusedBorderColor      = Color(0xFF1A1A2E),
                unfocusedBorderColor    = Color(0xFFDDDDDD),
                cursorColor             = Color(0xFF1A1A2E)
            )
        )

        // ── Content ───────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color(0xFF6200EA))
                            Spacer(Modifier.height(12.dp))
                            Text("Finding the best matches...", color = Color(0xFF555555), fontSize = 14.sp)
                        }
                    }
                }

                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error ?: "", color = Color(0xFFCC0000), fontSize = 14.sp)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.search() }) { Text("Retry") }
                        }
                    }
                }

                uiState.hasSearched && uiState.results.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No results found for \"${uiState.query}\"",
                            color = Color(0xFF555555),
                            fontSize = 15.sp
                        )
                    }
                }

                // Results after a search — row cards with metadata
                uiState.results.isNotEmpty() -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.results) { movie ->
                            SearchResultCard(movie = movie, onClick = {
                                val type = if (movie.mediaType == "tv") "tv" else "movie"
                                onMovieClick(type, movie.id.toString())
                                focusManager.clearFocus()
                            })
                        }
                    }
                }

                // Empty state — poster grid like Instagram explore
                uiState.trending.isNotEmpty() -> {
                    Column {
                        Text(
                            text = "Trending",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF555555),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            contentPadding = PaddingValues(horizontal = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            items(uiState.trending) { movie ->
                                AsyncImage(
                                    model = movie.posterPath,
                                    contentDescription = movie.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .aspectRatio(2f / 3f)
                                        .clickable {
                                            val type = if (movie.mediaType == "tv") "tv" else "movie"
                                            onMovieClick(type, movie.id.toString())
                                        }
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Trending not loaded yet — show a subtle hint
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Type to search movies", color = Color(0xFF888888), fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

// ─── Search result row card ───────────────────────────────────────────────────

@Composable
private fun SearchResultCard(movie: Movie, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = movie.posterPath,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(60.dp)
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFEEEEEE))
            )
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = movie.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF1A1A1A)
                )
                if (movie.releaseDate.length >= 4) {
                    Spacer(Modifier.height(4.dp))
                    Text(text = movie.releaseDate.take(4), fontSize = 12.sp, color = Color(0xFF666666))
                }
                if (movie.voteAverage > 0.0) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("\u2605", color = Color(0xFFFFC107), fontSize = 12.sp)
                        Spacer(Modifier.width(3.dp))
                        Text(String.format("%.1f", movie.voteAverage), fontSize = 12.sp, color = Color(0xFF444444))
                    }
                }
                if (movie.overview.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = movie.overview,
                        fontSize = 12.sp,
                        color = Color(0xFF666666),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}
