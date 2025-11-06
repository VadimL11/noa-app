package com.example.noa

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth

private const val ACTION_TOGGLE_NOA_FOCUS = "com.example.noa.TOGGLE_NOA_FOCUS"

class NoaOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var noaView: View? = null
    private lateinit var layoutParams: WindowManager.LayoutParams
    private var isFocusable = false
    private var lastTapTime = 0L

    private val doubleTapThreshold = 300L

    override fun onBind(intent: Intent?): IBinder? = null

    // 🔹 BroadcastReceiver — слухає HomeActivity
    private val focusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.getStringExtra("mode")) {
                "focusable" -> updateFocus(true)
                "not_focusable" -> updateFocus(false)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createOverlay()

        // ✅ Безпечна реєстрація ресівера
        val filter = IntentFilter(ACTION_TOGGLE_NOA_FOCUS)
        ContextCompat.registerReceiver(
            this,
            focusReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    // 🔹 Створюємо плаваючого Noa
    private fun createOverlay() {
        noaView = LayoutInflater.from(this).inflate(R.layout.overlay_noa, null, false)
        val noaIcon = noaView!!.findViewById<ImageView>(R.id.noa_icon)

        val layoutType =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 300
        }

        setTouchListener(noaIcon)
        windowManager.addView(noaView, layoutParams)
    }

    // 🔹 Обробка руху і кліків
    private fun setTouchListener(noaIcon: ImageView) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        noaIcon.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager.updateViewLayout(noaView, layoutParams)
                    true
                }

                MotionEvent.ACTION_UP -> {
                    val dx = kotlin.math.abs(event.rawX - initialTouchX)
                    val dy = kotlin.math.abs(event.rawY - initialTouchY)
                    if (dx < 10 && dy < 10) handleTap()
                    true
                }

                else -> false
            }
        }
    }

    // 🔹 Одинарний / подвійний тап
    private fun handleTap() {
        val now = System.currentTimeMillis()
        if (now - lastTapTime < doubleTapThreshold) {
            minimizeApp()
        } else {
            openChat()
        }
        lastTapTime = now
    }

    private fun openChat() {
        val user = FirebaseAuth.getInstance().currentUser
        val intent = if (user != null)
            Intent(this, HomeActivity::class.java)
        else
            Intent(this, PhoneAuthActivity::class.java)

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
    }

    private fun minimizeApp() {
        val homeIntent = Intent(Intent.ACTION_MAIN)
        homeIntent.addCategory(Intent.CATEGORY_HOME)
        homeIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(homeIntent)
    }

    // 🔹 Перемикає між focusable / not_focusable
    private fun updateFocus(focusable: Boolean) {
        if (focusable == isFocusable) return
        isFocusable = focusable

        // Змінюємо тільки прапор і оновлюємо вікно
        layoutParams.flags = if (focusable) {
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        }

        noaView?.let { windowManager.updateViewLayout(it, layoutParams) }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            unregisterReceiver(focusReceiver)
        } catch (_: Exception) {}
        noaView?.let { windowManager.removeView(it) }
    }
}
