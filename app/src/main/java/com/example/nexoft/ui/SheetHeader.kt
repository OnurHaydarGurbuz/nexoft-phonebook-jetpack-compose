package com.example.nexoft.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SheetHeader(
    leftLabel: String,
    onLeft: () -> Unit,
    title: String,
    rightContent: @Composable () -> Unit = {}
) {
    // Create/Edit’tekiyle aynı boşluk
    Spacer(Modifier.height(30.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically   // ← yukarıda durma sorunu çözüldü
    ) {
        Text(
            leftLabel,
            color = Color(0xFF0075FF),                    // ← sabit mavi (#0075FF)
            style = MaterialTheme.typography.bodyMedium,  // ← Create ile aynı tipografi
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .padding(vertical = 2.dp)                 // küçük dengeleme
                .clickable { onLeft() }
        )

        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF202020)
        )

        // Sağ alan: en az 24dp yer ayır (ikon/menü hizası bozulmasın)
        Box(Modifier.sizeIn(minWidth = 24.dp, minHeight = 24.dp)) {
            rightContent()
        }
    }
}
