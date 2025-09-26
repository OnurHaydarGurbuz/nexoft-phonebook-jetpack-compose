package com.example.nexoft.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt

@Composable
fun SwipeActionRow(
    isOpen: Boolean,
    onOpen: () -> Unit,
    onClose: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    actionHeight: Int = 64,
    shape: Shape = RoundedCornerShape(0.dp),   // ← yeni
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val buttonsWidthPx = with(density) { (56.dp * 2).toPx() }

    var dragOffset by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isOpen) {
        dragOffset = if (isOpen) -buttonsWidthPx else 0f
    }

    val animX by animateFloatAsState(
        targetValue = dragOffset.coerceIn(-buttonsWidthPx, 0f),
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "swipe-offset"
    )

    // Tüm satırı aynı shape ile kırp
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(actionHeight.dp)
            .clip(shape)                       // ← kritik: taşmayı engeller
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        val next = dragOffset + dragAmount
                        dragOffset = if (next < 0f) next.coerceIn(-buttonsWidthPx, 0f) else 0f
                    },
                    onDragEnd = {
                        if (dragOffset <= -buttonsWidthPx / 2f) {
                            dragOffset = -buttonsWidthPx; onOpen()
                        } else {
                            dragOffset = 0f; onClose()
                        }
                    },
                    onDragCancel = { dragOffset = 0f; onClose() }
                )
            }
    ) {
        // Arkadaki aksiyonlar: sağa sabit, shape ile beraber kırpılır
        Row(
            modifier = Modifier
                .fillMaxHeight()
                .width(112.dp)
                .align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .background(Color(0xFF1EA7FF))
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Edit, contentDescription = "Edit", tint = Color.White) }

            Box(
                modifier = Modifier
                    .width(56.dp)
                    .fillMaxHeight()
                    .background(Color(0xFFFF0000))
                    .clickable { onDelete() },
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.White) }
        }

        // Öndeki içerik
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset { IntOffset(animX.roundToInt(), 0) }
                .zIndex(1f)
        ) {
            content()
        }
    }
}
