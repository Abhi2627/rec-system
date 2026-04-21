package com.example.recsystem.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.recsystem.data.model.Movie

@Composable
fun MovieGridCard(movie: Movie, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f)
        ) {
            PosterImage(
                url = movie.posterPath,
                title = movie.title,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(5.dp))

        Text(
            text = movie.title,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = Color(0xFF1A1A1A),
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 15.sp,
            modifier = Modifier.fillMaxWidth()
        )

        if (movie.voteAverage > 0.0) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "\u2605", color = Color(0xFFFFC107), fontSize = 10.sp)
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = String.format("%.1f", movie.voteAverage),
                    fontSize = 10.sp,
                    color = Color(0xFF444444),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}

/**
 * Reusable poster image with a dark placeholder/fallback.
 * Uses SubcomposeAsyncImage so we can render a custom placeholder
 * while the image loads and a fallback if it fails.
 */
@Composable
fun PosterImage(
    url: String,
    title: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val context = LocalContext.current

    if (url.isEmpty()) {
        PosterPlaceholder(title = title, modifier = modifier)
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build(),
        contentDescription = title,
        contentScale = contentScale,
        modifier = modifier,
        loading = {
            PosterPlaceholder(title = "", modifier = Modifier.fillMaxSize())
        },
        error = {
            PosterPlaceholder(title = title, modifier = Modifier.fillMaxSize())
        }
    )
}

@Composable
fun PosterPlaceholder(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(Color(0xFF2C2C3E)),
        contentAlignment = Alignment.Center
    ) {
        if (title.isNotEmpty()) {
            Text(
                text = title.take(2).uppercase(),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
