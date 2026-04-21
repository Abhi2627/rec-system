package com.example.recsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recsystem.data.api.RecSystemApi
import com.example.recsystem.data.repository.AuthRepository
import com.example.recsystem.data.repository.DiscoveryRepository
import com.example.recsystem.data.repository.PersonalizationRepository
import com.example.recsystem.ui.AppNavigator
import com.example.recsystem.ui.auth.AuthState
import com.example.recsystem.ui.auth.AuthViewModel
import com.example.recsystem.ui.discovery.DiscoveryViewModel
import com.example.recsystem.ui.discovery.MovieDetailViewModel
import com.example.recsystem.ui.profile.ProfileViewModel
import com.example.recsystem.ui.search.SearchViewModel
import com.example.recsystem.ui.theme.RecSystemTheme
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val baseUrl = "http://192.168.0.167:8000/"

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val api                       = retrofit.create(RecSystemApi::class.java)
        val discoveryRepository       = DiscoveryRepository(api, applicationContext)
        val authRepository            = AuthRepository(api)
        val personalizationRepository = PersonalizationRepository(api)

        val discoveryViewModel   = DiscoveryViewModel(discoveryRepository)
        val searchViewModel      = SearchViewModel(discoveryRepository)
        val authViewModel        = AuthViewModel(authRepository)
        val movieDetailViewModel = MovieDetailViewModel(discoveryRepository)
        val profileViewModel     = ProfileViewModel(personalizationRepository)

        setContent {
            RecSystemTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    val authState by authViewModel.authState

                    LaunchedEffect(authState) {
                        val token  = (authState as? AuthState.Success)?.token
                        val bearer = token?.let { "Bearer $it" }
                        discoveryViewModel.loadPersonalizedRow(bearer)
                        if (bearer != null) profileViewModel.loadProfile(bearer)
                        else profileViewModel.clearProfile()
                    }

                    Surface(color = MaterialTheme.colorScheme.background) {
                        AppNavigator(
                            authViewModel        = authViewModel,
                            discoveryViewModel   = discoveryViewModel,
                            searchViewModel      = searchViewModel,
                            movieDetailViewModel = movieDetailViewModel,
                            profileViewModel     = profileViewModel
                        )
                    }
                }
            }
        }
    }
}

// ─── Compose splash screen ────────────────────────────────────────────────────
// Pure Compose — no XML theme, no SplashScreen API, no crash risk.
// Shows navy background + clapperboard emoji + CineRec wordmark.
// Fades in over 400ms, holds for 1s, then calls onFinished.

@Composable
private fun SplashScreen(onFinished: () -> Unit) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.85f) }

    LaunchedEffect(Unit) {
        // Fade + scale in
        alpha.animateTo(1f, animationSpec = tween(400))
        scale.animateTo(1f, animationSpec = tween(400))
        // Hold
        delay(900)
        // Done
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A2E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .alpha(alpha.value)
                .scale(scale.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Clapperboard icon as text emoji — always renders, no resource needed
            Text(
                text      = "\uD83C\uDFAC",
                fontSize  = 72.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text       = "CineRec",
                fontSize   = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = Color(0xFFF7E3C8),
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text     = "Your personal cinema guide",
                fontSize = 13.sp,
                color    = Color(0xFFF7E3C8).copy(alpha = 0.55f),
                letterSpacing = 0.5.sp
            )
        }
    }
}
