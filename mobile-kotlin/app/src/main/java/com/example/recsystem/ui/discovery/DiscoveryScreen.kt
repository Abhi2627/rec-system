package com.example.recsystem.ui.discovery

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.recsystem.data.model.Movie
import com.example.recsystem.ui.components.MovieGridCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel,
    onMovieClick: (String, String) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsState()

    // Reload whenever the screen is entered and content is missing/stale
    LaunchedEffect(Unit) {
        if (uiState.trendingMovies.isEmpty() && !uiState.isLoading) {
            viewModel.loadAllContent()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        when {
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF6200EA))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Loading content...",
                            color = Color(0xFF333333),
                            fontSize = 14.sp
                        )
                    }
                }
            }

            uiState.error != null && uiState.trendingMovies.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text(
                            text = "\uD83D\uDCF6",
                            fontSize = 48.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = uiState.error ?: "Could not load content",
                            color = Color(0xFF333333),
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.loadAllContent() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6200EA)
                            )
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // ── Header ────────────────────────────────────────────
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1A1A2E))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "\uD83C\uDFAC", fontSize = 24.sp)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "CineRec",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                    }

                    // ── Trending carousel ─────────────────────────────────
                    val carouselMovies = uiState.trendingMovies.take(10)
                    if (carouselMovies.isNotEmpty()) {
                        TrendingCarousel(movies = carouselMovies, onMovieClick = onMovieClick)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Personalised row (logged-in only) ─────────────────
                    if (uiState.personalizedMovies.isNotEmpty()) {
                        HorizontalSection(
                            title = "\u2728 Picked for you",
                            movies = uiState.personalizedMovies,
                            onMovieClick = onMovieClick
                        )
                    }

                    HorizontalSection("Trending TV",  uiState.trendingTV,     onMovieClick)
                    HorizontalSection("Action",       uiState.actionMovies,   onMovieClick)
                    HorizontalSection("Comedy",       uiState.comedyMovies,   onMovieClick)
                    HorizontalSection("Thriller",     uiState.thrillerMovies, onMovieClick)
                    HorizontalSection("Sci-Fi",       uiState.scifiMovies,    onMovieClick)

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

// ─── Trending Carousel ────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrendingCarousel(movies: List<Movie>, onMovieClick: (String, String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { movies.size })

    LaunchedEffect(pagerState) {
        while (true) {
            delay(4_000)
            val next = (pagerState.currentPage + 1) % movies.size
            pagerState.animateScrollToPage(next, animationSpec = tween(800))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)   // taller — shows more of portrait poster
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            val movie = movies[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable {
                        val type = if (movie.mediaType == "tv") "tv" else "movie"
                        onMovieClick(type, movie.id.toString())
                    }
            ) {
                // Use TopCenter alignment so face/title art at top of portrait poster is visible
                if (movie.posterPath.isNotEmpty()) {
                    AsyncImage(
                        model = movie.posterPath,
                        contentDescription = movie.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF2A2A3E)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = movie.title,
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
                // Gradient — starts from 40% height so top third is clear
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    0.45f to Color.Transparent,
                                    1.0f to Color(0xF2000000)
                                )
                            )
                        )
                )
                // Title overlay at bottom — constrained width to avoid hitting dots
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 22.dp)
                ) {
                    Text(
                        text = movie.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 22.sp
                    )
                    if (movie.voteAverage > 0.0) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("\u2605", color = Color(0xFFFFC107), fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", movie.voteAverage),
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Page dots — fixed at very bottom, above the title text
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(movies.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 7.dp else 4.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color.White else Color.White.copy(alpha = 0.45f)
                        )
                )
            }
        }
    }
}

// ─── Horizontal section ───────────────────────────────────────────────────────

@Composable
fun HorizontalSection(
    title: String,
    movies: List<Movie>,
    onMovieClick: (String, String) -> Unit
) {
    if (movies.isEmpty()) return

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1A1A1A),   // explicit dark — never inherits a light theme color
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(movies) { movie ->
                MovieGridCard(movie = movie) {
                    val type = if (movie.mediaType == "tv") "tv" else "movie"
                    onMovieClick(type, movie.id.toString())
                }
            }
        }
    }
}
