package com.example.noa

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Composable
fun ChatScreen(dbRef: DatabaseReference, userId: String) {
    var messageText by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    // 🔹 слухаємо Firebase
    LaunchedEffect(Unit) {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val temp = mutableListOf<Message>()
                for (child in snapshot.children) {
                    child.getValue(Message::class.java)?.let { temp.add(it) }
                }
                messages = temp.sortedBy { it.timestamp }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🌈 фон у стилі Noa
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A0A14), Color(0xFF2A0046))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            // 🔸 список повідомлень
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                reverseLayout = false
            ) {
                items(messages) { msg ->
                    val isMine = msg.senderId == userId
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        contentAlignment = if (isMine) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Surface(
                            color = if (isMine) Color(0xFF6C63FF) else Color(0xFF2A2A40),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = msg.text.orEmpty(),
                                color = Color.White,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }

            // 🔹 панель вводу
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(60.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text("Напиши повідомлення...", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default,
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1A1A25),
                        unfocusedContainerColor = Color(0xFF1A1A25),
                        cursorColor = Color(0xFF7EE7F3),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                keyboardController?.show() // 🔥 відкриває клавіатуру при тапі
                            }
                        }
                        .background(Color(0xFF1A1A25), RoundedCornerShape(14.dp))
                        .padding(start = 4.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 🔵 неонова кнопка
                val isPressed = remember { mutableStateOf(false) }
                val scale by animateFloatAsState(
                    targetValue = if (isPressed.value) 0.9f else 1.0f,
                    label = ""
                )

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .scale(scale)
                        .shadow(8.dp, CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(Color(0xFF7DF9FF), Color(0xFF6C63FF))
                            ),
                            shape = CircleShape
                        )
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            isPressed.value = true
                            val text = messageText.trim()
                            if (text.isNotEmpty()) {
                                val id = dbRef.push().key ?: return@clickable
                                val msg = Message(id = id, senderId = userId, text = text)
                                dbRef.child(id).setValue(msg)
                                messageText = ""
                            }
                            isPressed.value = false
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("➤", fontSize = 22.sp, color = Color.White)
                }
            }
        }
    }
}
