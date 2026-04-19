package com.example.recsystem

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Physical device (S23): backend is running on Mac at 192.168.0.167:8000
        // To switch to emulator, change this to "http://10.0.2.2:8000/"
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
            val authState by authViewModel.authState

            LaunchedEffect(authState) {
                val token  = (authState as? AuthState.Success)?.token
                val bearer = token?.let { "Bearer $it" }
                discoveryViewModel.loadPersonalizedRow(bearer)
                if (bearer != null) {
                    profileViewModel.loadProfile(bearer)
                } else {
                    profileViewModel.clearProfile()
                }
            }

            RecSystemTheme {
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
