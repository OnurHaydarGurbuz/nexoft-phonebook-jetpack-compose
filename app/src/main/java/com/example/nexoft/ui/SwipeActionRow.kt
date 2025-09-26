package com.example.nexoft.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Sola kaydırınca sağda 2 aksiyon (Edit & Delete) gösterir.
 * - Tek satır açık mantığı: isOpen dışarıdan veriliyor.
 * - Yumuşak animasyon: spring ile.
 */
@Composable
fun SwipeActionRow(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    actionHeight: Int = 64, // dp
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val actionWidth = 56.dp
    val actionsCount = 2
    val maxRevealPx = with(density) { (actionWidth * actionsCount).toPx() }

    // Çekme anındaki ham offset (0..maxReveal)
    var dragReveal by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    // Açık/kapalı hedefe animasyon
    val target = if (isOpen) maxRevealPx else 0f
    val anim by animateFloatAsState(
        targetValue = target,
        animationSpec = spring(stiffness = 600f),
        label = "rowOffset"
    )

    // Gösterilecek offset: sürükleme varsa dragReveal, yoksa anim
    val shown = if (dragging) dragReveal else anim

    Box(modifier = modifier.height(actionHeight.dp)) {
        // --- Arkadaki aksiyon butonları ---
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 16.dp)     // içeriğiyle hizalı dursun
                .clip(RoundedCornerShape(8.dp))  // kart köşeleriyle uyumlu
                .background(Color.Transparent),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // EDIT (mavi)
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Color(0xFF1EA7FF))
                    .clickable {
                        // tıklandığında kapat + işlem
                        onClose()
                        onEdit()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Color.White)
            }

            // DELETE (kırmızı)
            Box(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight()
                    .background(Color(0xFFFF0000))
                    .clickable {
                        onClose()   // satırı kapat
                        onDelete()  // sheet/scrim'i dışarıda aç
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
            }
        }

        // --- Öndeki kart (içerik) ---
        Surface(
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset(x = -shown.roundToInt(), y = 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragging = true
                            // başka satır açıksa bunu açtıracağız
                            onOpen()
                        },
                        onHorizontalDrag = { change, delta ->
                            // sola çekme: delta negatif => reveal artar
                            val newReveal = (dragReveal - delta).coerceIn(0f, maxRevealPx)
                            dragReveal = newReveal
                            change.consume()
                        },
                        onDragEnd = {
                            dragging = false
                            // eşik: yarıdan fazlaysa açık kalsın
                            if (dragReveal >= maxRevealPx * 0.45f) {
                                onOpen()
                            } else {
                                onClose()
                            }
                        },
                        onDragCancel = {
                            dragging = false
                            onClose()
                        }
                    )
                }
        ) {
            // İçerik: offset 0 iken tıklanabilir olsun
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                content()
            }
        }
    }

    // Dışarıdan isOpen kapatıldıysa, dragReveal'ı da senkronla
    LaunchedEffect(isOpen) {
        dragReveal = if (!isOpen) 0f
        else maxRevealPx
    }
}
