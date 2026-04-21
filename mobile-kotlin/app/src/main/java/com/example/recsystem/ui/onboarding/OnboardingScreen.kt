package com.example.recsystem.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recsystem.ui.theme.CineNavy
import com.example.recsystem.ui.theme.CineRed

// All available genres with their emoji icons
private data class GenreOption(val id: String, val name: String, val emoji: String)

private val ALL_GENRES = listOf(
    GenreOption("28",  "Action",      "\uD83D\uDCA5"),
    GenreOption("12",  "Adventure",   "\uD83C\uDFDE\uFE0F"),
    GenreOption("16",  "Animation",   "\uD83C\uDFA8"),
    GenreOption("35",  "Comedy",      "\uD83D\uDE02"),
    GenreOption("80",  "Crime",       "\uD83D\uDD75\uFE0F"),
    GenreOption("99",  "Documentary", "\uD83C\uDFA5"),
    GenreOption("18",  "Drama",       "\uD83C\uDFAD"),
    GenreOption("10751","Family",     "\uD83D\uDC68\u200D\uD83D\uDC69\u200D\uD83D\uDC67"),
    GenreOption("14",  "Fantasy",     "\u2728"),
    GenreOption("36",  "History",     "\uD83C\uDFDB\uFE0F"),
    GenreOption("27",  "Horror",      "\uD83D\uDC7B"),
    GenreOption("10402","Music",      "\uD83C\uDFB5"),
    GenreOption("9648","Mystery",     "\uD83D\uDD0D"),
    GenreOption("10749","Romance",    "\u2764\uFE0F"),
    GenreOption("878", "Sci-Fi",      "\uD83D\uDE80"),
    GenreOption("53",  "Thriller",    "\uD83D\uDDE1\uFE0F"),
    GenreOption("10752","War",        "\uD83E\uDD96"),
    GenreOption("37",  "Western",     "\uD83E\uDD20")
)

@Composable
fun OnboardingScreen(
    onComplete: (selectedGenres: List<String>) -> Unit,
    onSkip: () -> Unit
) {
    var selected by remember { mutableStateOf(setOf<String>()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        // ── Header ────────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CineNavy)
                .padding(horizontal = 24.dp, vertical = 32.dp)
        ) {
            Text(
                text = "\uD83C\uDFAC",
                fontSize = 40.sp
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "What do you love watching?",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                color = Color.White
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Pick at least 3 genres to personalise your recommendations.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.75f),
                lineHeight = 20.sp
            )
        }

        // ── Genre grid ────────────────────────────────────────────────────
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(ALL_GENRES) { genre ->
                val isSelected = genre.name in selected
                GenreChip(
                    genre = genre,
                    isSelected = isSelected,
                    onClick = {
                        selected = if (isSelected) selected - genre.name
                                   else selected + genre.name
                    }
                )
            }
        }

        // ── Bottom bar ────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                text = "${selected.size} selected",
                fontSize = 13.sp,
                color = if (selected.size >= 3) Color(0xFF2E7D32) else Color(0xFF888888),
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Button(
                onClick = { onComplete(selected.toList()) },
                enabled = selected.size >= 3,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CineRed,
                    disabledContainerColor = Color(0xFFCCCCCC)
                )
            ) {
                Text(
                    text = "Get my recommendations",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            TextButton(
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Skip for now",
                    color = Color(0xFF888888),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun GenreChip(
    genre: GenreOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor     = if (isSelected) CineRed else Color.White
    val textColor   = if (isSelected) Color.White else Color(0xFF1A1A1A)
    val borderColor = if (isSelected) CineRed else Color(0xFFDDDDDD)

    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = genre.emoji, fontSize = 26.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(
            text = genre.name,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = textColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}
