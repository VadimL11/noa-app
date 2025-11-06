package com.example.noa

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.net.toUri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class HomeActivity : ComponentActivity() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private lateinit var dbRef: DatabaseReference

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Адаптація під різні версії Android
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        } else {
            window.setDecorFitsSystemWindows(false)
        }

        // 🔹 Перевіряємо користувача
        val currentUser = auth.currentUser
        if (currentUser == null) {
            finish()
            return
        }

        // 🔹 Firebase reference до глобального чату
        dbRef = FirebaseDatabase.getInstance().getReference("chats/global")

        // 🔹 Запускаємо інтерфейс чату
        setContent {
            ChatScreen(dbRef, currentUser.uid)
        }

        // 🌟 Перевірка дозволу на оверлей (Noa поверх екрана)
        if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, NoaOverlayService::class.java))
        } else {
            Toast.makeText(
                this,
                "Дозволь Noa з’являтись поверх екрана 💫",
                Toast.LENGTH_LONG
            ).show()

            val permissionIntent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivity(permissionIntent)
        }
    }

    // ✅ Коли активність у фокусі — Noa переходить у "focusable"
    override fun onResume() {
        super.onResume()
        sendBroadcast(
            Intent("com.example.noa.TOGGLE_NOA_FOCUS")
                .setPackage(packageName)
                .putExtra("mode", "focusable")
        )
    }

    // ✅ Коли активність згорт. — Noa знову "not_focusable"
    override fun onPause() {
        super.onPause()
        sendBroadcast(
            Intent("com.example.noa.TOGGLE_NOA_FOCUS")
                .setPackage(packageName)
                .putExtra("mode", "not_focusable")
        )
    }
}
