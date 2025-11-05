package com.example.noa

import android.app.ActivityManager
import android.content.Intent
import androidx.core.net.toUri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.google.firebase.auth.FirebaseAuth

class HomeActivity : ComponentActivity() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HomeScreen(
                onLogout = {
                    auth.signOut()
                    stopService(Intent(this, NoaOverlayService::class.java))
                    finish()
                }
            )
        }

        val overlayIntent = Intent(this, NoaOverlayService::class.java)

        if (Settings.canDrawOverlays(this)) {
            // 🚀 Запускаємо сервіс тільки якщо він ще не працює
            if (!isServiceRunning(NoaOverlayService::class.java)) {
                startService(overlayIntent)
            }

            // 🔽 Мінімізуємо застосунок після запуску Noa
            moveTaskToBack(true)
        } else {
            Toast.makeText(
                this,
                "Дозволь Noa показуватись поверх екрана 💫",
                Toast.LENGTH_LONG
            ).show()

            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(permissionIntent)
        }
    }

    // ✅ Перевірка, чи працює вже сервіс NoaOverlayService
    @Suppress("DEPRECATION")
    private fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        // Якщо користувач дозволив overlay після повернення
        if (Settings.canDrawOverlays(this) && !isServiceRunning(NoaOverlayService::class.java)) {
            startService(Intent(this, NoaOverlayService::class.java))
        }
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit) {
    // 👇 Lottie-анімація Noa
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("noa_glow.json"))
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f
    )

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // 🌈 Неоновий градієнтний фон
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0D1A), Color(0xFF240046))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // ✨ Noa glowing animation
            LottieAnimation(
                composition = composition,
                progress = { progress },
                modifier = Modifier
                    .size(220.dp)
                    .padding(bottom = 16.dp)
            )

            // 👋 Привітальний текст із плавною появою
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Привіт 👋",
                        color = Color(0xFF7DF9FF),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Noa з тобою 💫",
                        color = Color.White,
                        fontSize = 20.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                    )
                }
            }

            Button(onClick = onLogout) {
                Text("Вийти")
            }
        }
    }
}
