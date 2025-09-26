package com.example.nexoft.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.clickable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment

@Composable
fun SheetScaffold(
    topPadding: Dp = 42.dp,
    scrimColor: Color = Color.Black.copy(alpha = 0.45f),
    blockOutsideClicks: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val blocker = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scrimColor)
            .let {
                if (blockOutsideClicks) {
                    it.clickable(interactionSource = blocker, indication = null) { /* consume */ }
                } else it
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topPadding),
            shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 40.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                content = content
            )
        }
    }
}
