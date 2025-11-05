package com.example.noa

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.OutlinedTextField

class PhoneAuthActivity : ComponentActivity() {
    private lateinit var auth: FirebaseAuth
    private var verificationId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        setContent {
            PhoneAuthScreen(
                onSendCode = { phone ->
                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(phone)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(this)
                        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                signInWithCredential(credential)
                            }

                            override fun onVerificationFailed(e: FirebaseException) {
                                Toast.makeText(
                                    this@PhoneAuthActivity,
                                    "Помилка: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }

                            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                                this@PhoneAuthActivity.verificationId = verificationId
                                Toast.makeText(this@PhoneAuthActivity, "Код надіслано!", Toast.LENGTH_SHORT).show()
                            }
                        })
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                },
                onVerifyCode = { code ->
                    val credential = verificationId?.let {
                        PhoneAuthProvider.getCredential(it, code)
                    }
                    if (credential != null) {
                        signInWithCredential(credential)
                    } else {
                        Toast.makeText(this, "Спочатку надішліть код!", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Успішний вхід ✅", Toast.LENGTH_SHORT).show()
                println("✅ DEBUG: Вхід успішний, запускаємо HomeActivity")

                try {
                    val intent = android.content.Intent(this@PhoneAuthActivity, HomeActivity::class.java)
                    intent.flags =
                        android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    println("✅ DEBUG: startActivity викликано!")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "❌ Помилка при переході: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneAuthScreen(
    onSendCode: (String) -> Unit,
    onVerifyCode: (String) -> Unit
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    val neonPurple = Color(0xFF8A2BE2)
    val neonBlue = Color(0xFF00FFFF)
    val darkBg = Color(0xFF0D0D1A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                text = "Sign in to Noa",
                color = neonBlue,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Номер телефону (+380...)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = neonBlue,
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = neonPurple
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSendCode(phone) },
                colors = ButtonDefaults.buttonColors(containerColor = neonPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Надіслати код", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Код підтвердження") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = neonBlue,
                    unfocusedIndicatorColor = Color.Gray,
                    cursorColor = neonPurple
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onVerifyCode(code) },
                colors = ButtonDefaults.buttonColors(containerColor = neonBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Підтвердити", color = darkBg, fontSize = 16.sp)
            }
        }
    }
}
