package com.example.nexoft.feature.create

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@Composable
fun CreateContactScreen(
    onCancel: () -> Unit,
    // prefer passing values up so the list updates:
    onDone: (first: String, last: String, phone: String) -> Unit
) {
    // state
    var first by rememberSaveable { mutableStateOf("") }
    var last  by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    val doneEnabled = first.isNotBlank() && phone.isNotBlank()

    // Background (Figma: rgba(0,0,0,.45))
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.TopCenter
    ) {
        // The sheet (Figma: top=42px, radius 25px, full width/height)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 42.dp), // space above
            shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 40.dp // Figma shows a strong soft shadow
        ) {
            // Inner content (Figma width 343 => 16dp side paddings on 375)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Header row (top=30)
                Spacer(Modifier.height(30.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cancel",
                        color = Color(0xFF0075FF),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.clickable { onCancel() }
                    )
                    Text(
                        "New Contact",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF202020)
                    )
                    Text(
                        "Done",
                        color = if (doneEnabled) Color(0xFF0075FF) else Color(0xFFD1D1D1),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(enabled = doneEnabled) {
                                onDone(first.trim(), last.trim(), phone.trim())
                            }
                    )
                }

                // Avatar + "Add Photo" (group top=103; gap 32)
                Spacer(Modifier.height(33.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1D1D1)), // var(--200)
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add Photo",
                        color = Color(0xFF0075FF),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            // TODO: open picker (camera/gallery)
                        }
                    )
                }

                // Fields (gap 32)
                Spacer(Modifier.height(32.dp))
                FieldBox(
                    value = first,
                    onChange = { first = it },
                    placeholder = "First Name"
                )
                Spacer(Modifier.height(12.dp))
                FieldBox(
                    value = last,
                    onChange = { last = it },
                    placeholder = "Last Name"
                )
                Spacer(Modifier.height(12.dp))
                FieldBox(
                    value = phone,
                    onChange = { phone = it },
                    placeholder = "Phone Number",
                    keyboard = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        }
    }
}

/**
 * Matches the Figma “white box with 8px radius, 1px #E7E7E7 outline,
 * 40px height, 16px horizontal padding, 10px vertical padding,
 * placeholder color #888888”.
 */
@Composable
private fun FieldBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp), // 40 in Figma is very tight; 48 reads better on Android
        shape = RoundedCornerShape(8.dp),
        placeholder = {
            Text(
                placeholder,
                color = Color(0xFF888888),
                fontWeight = FontWeight.SemiBold
            )
        },
        singleLine = true,
        keyboardOptions = keyboard,
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedBorderColor = Color(0xFFE7E7E7),
            focusedBorderColor = Color(0xFF0075FF),
            cursorColor = Color(0xFF0075FF)
        )
    )
}
