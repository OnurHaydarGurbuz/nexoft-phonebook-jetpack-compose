package com.example.nexoft.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.style.TextAlign

// ---- 1. Primitive ----
@Composable
fun BottomToast(
    text: String,
    onGone: () -> Unit,
    durationMs: Int = 1600,
    accent: Color = Color(0xFF12B76A),     // Figma’daki yeşil
    icon: @Composable () -> Unit = {
        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White)
    }
) {
    LaunchedEffect(text) {                   // aynı anda iki toast gelirse sıradakini tetikler
        kotlinx.coroutines.delay(durationMs.toLong())
        onGone()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .padding(bottom = 24.dp)
                .fillMaxWidth(0.9f)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) { icon() }
                Spacer(Modifier.width(8.dp))
                Text(text, color = accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ---- 2. HostState ----
data class ToastData(val message: String, val accent: Color = Color(0xFF12B76A), val token: Long = System.nanoTime())

class ToastHostState {
    var current by mutableStateOf<ToastData?>(null)
        internal set
    fun show(message: String, accent: Color = Color(0xFF12B76A)) {
        current = ToastData(message, accent) // token farklı olduğu için LaunchedEffect tetiklenir
    }
}

@Composable
fun rememberToastHostState() = remember { ToastHostState() }

// ---- 3. Host (ekrana yerleştireceğin tek composable) ----
@Composable
fun AppToastHost(hostState: ToastHostState) {
    hostState.current?.let { data ->
        BottomToast(
            text = data.message,
            onGone = { hostState.current = null },
            accent = data.accent
        )
    }
}


@SuppressLint("ConfigurationScreenWidthHeight")
@Composable
fun DeleteContactSheet(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val cfg = LocalConfiguration.current
    // İstediğin panel yüksekliği (ör: 240.dp). Butonların alttan görünmesini sağlar.
    val sheetHeight = 180.dp
    val topPad = (cfg.screenHeightDp.dp - sheetHeight).coerceAtLeast(0.dp)

    SheetScaffold(
        topPadding = topPad,                                 // << kısa sheet
        scrimColor = Color.Black.copy(alpha = 0.85f),
        blockOutsideClicks = true
    ) {
        // Header yok
        Spacer(Modifier.height(12.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                "Delete Contact",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF202020)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Are you sure you want to delete this contact?",
            color = Color(0xFF3D3D3D),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp)
            ) { Text("No") }

            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF202020))
            ) { Text("Yes") }
        }
        Spacer(Modifier.navigationBarsPadding().height(12.dp))
    }
}

