package com.example.nexoft.feature.contacts


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import coil.compose.AsyncImage
import kotlinx.coroutines.withContext


@Composable
fun ContactsScreen(
    onCreateNew: () -> Unit,
    onOpenProfile: (id: String) -> Unit,   // <<< YENİ
    vm: ContactsViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold { inner ->
        Column(
            modifier = Modifier
                .padding(inner)
                .fillMaxSize()
                .background(Color(0xFFF6F6F6))
        ) {

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Contacts",
                    color = Color(0xFF202020),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                BlueCircleAddButton(onClick = onCreateNew)
            }

            // Search
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { vm.onEvent(ContactsEvent.OnSearchChanged(it)) },
                    leadingIcon = {
                        Icon(Icons.Outlined.Search, contentDescription = null, tint = Color(0xFFB0B0B0))
                    },
                    placeholder = {
                        Text(
                            "Search contacts by name",
                            color = Color(0xFFB0B0B0),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color(0xFFE7E7E7),
                        focusedBorderColor = Color(0xFF0075FF),
                        cursorColor = Color(0xFF0075FF)
                    ),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp)
                )
            }

            Spacer(Modifier.height(16.dp))

            // --- CONTENT ---
            val filtered = state.contacts
                .filter {
                    val q = state.searchQuery.trim().lowercase()
                    if (q.isBlank()) true else {
                        val name = (it.firstName + " " + it.lastName).trim().lowercase()
                        name.contains(q) || it.phone.lowercase().contains(q)
                    }
                }
                .sortedWith(
                    compareBy {
                        (it.firstName + " " + it.lastName).trim().ifBlank { it.phone }.lowercase()
                    }
                )

            if (filtered.isEmpty()) {
                EmptyState(onCreateNew)
            } else {
                val groups = filtered.groupBy { displayInitial(it) }
                    .toSortedMap() // A..Z sıralı

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    groups.forEach { (letter, contacts) ->
                        // Section Header (sticky)
                        stickyHeader {
                            SectionHeader(letter = letter)
                        }

                        itemsIndexed(contacts) { index, contact ->
                            val isFirst = index == 0
                            val isLast  = index == contacts.lastIndex
                            val shape = when {
                                isFirst && isLast -> RoundedCornerShape(8.dp)
                                isFirst -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
                                isLast  -> RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                else    -> RoundedCornerShape(0.dp)
                            }

                            ContactRow(
                                contact = contact,
                                shape = shape,
                                onClick = { onOpenProfile(contact.id) }
                            )

                            if (isLast) Spacer(Modifier.height(16.dp)) // grup arası boşluk
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BlueCircleAddButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Color(0xFF0075FF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Filled.Add, contentDescription = "Add", tint = Color.White)
    }
}

@Composable
private fun SectionHeader(letter: Char) {
    // Figma'daki 32dp yükseklik, beyaz zemin
    Surface(
        color = Color.White,
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = letter.toString(),
                color = Color(0xFFB0B0B0),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ContactRow(
    contact: com.example.nexoft.core.model.Contact,
    shape: RoundedCornerShape,
    onClick: () -> Unit
) {
    Surface(
        color = Color.White,
        shape = shape,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(contact)

            Spacer(Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName(contact),
                    color = Color(0xFF3D3D3D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Text(
                    text = contact.phone,
                    color = Color(0xFF6D6D6D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}


private fun displayName(c: com.example.nexoft.core.model.Contact): String {
    val name = listOf(c.firstName, c.lastName).filter { it.isNotBlank() }.joinToString(" ").trim()
    return name.ifBlank { c.phone }
}

private fun displayInitial(c: com.example.nexoft.core.model.Contact): Char {
    val name = displayName(c)
    return name.firstOrNull()?.uppercaseChar() ?: '#'
}

@Composable
private fun EmptyState(onCreateNew: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 33.dp)
            .offset(y = (-180).dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFFD1D1D1))
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No Contacts",
            color = Color(0xFF202020),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Contacts you’ve added will appear here.",
            color = Color(0xFF3D3D3D),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Create New Contact",
            color = Color(0xFF0075FF),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.clickable { onCreateNew() }
        )
    }
}

@Composable
private fun Avatar(contact: com.example.nexoft.core.model.Contact) {
    val ctx = androidx.compose.ui.platform.LocalContext.current

    // Başlangıç durumu: ViewModel'den gelen flag
    var showBadge by remember(contact.id, contact.isInDevice) {
        mutableStateOf(contact.isInDevice)
    }

    // READ izni varsa cihazda var mı diye sessizce kontrol et (UI'ı bloklamadan)
    LaunchedEffect(contact.id, contact.phone) {
        if (!showBadge &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                ctx, android.Manifest.permission.READ_CONTACTS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            val exists = withContext(kotlinx.coroutines.Dispatchers.IO) {
                isInDeviceContacts(ctx, contact.phone)
            }
            if (exists) showBadge = true
        }
    }


    // Dış container CLIP YOK → rozet kesilmez
    Box(
        modifier = Modifier.size(40.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Fotoğrafı clip’le
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(Color(0xFFEDFAFF)),
            contentAlignment = Alignment.Center
        ) {
            val photo = contact.photoUrl
            if (!photo.isNullOrBlank()) {
                AsyncImage(
                    model = photo,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Text(
                    text = displayInitial(contact).toString(),
                    color = Color(0xFF0075FF),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Sağ-alt mavi telefon rozeti
        if (showBadge) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF0075FF))
                    .border(1.dp, Color.White, CircleShape)
                    .align(Alignment.BottomEnd) // zaten Box'ın alignment'ı bu ama net olsun
                ,
                contentAlignment = Alignment.Center
            ) {
                // Telefon ikonu istersen ADD yerine PHONE kullan
                // import: androidx.compose.material.icons.filled.Phone
                Icon(
                    imageVector = Icons.Filled.Phone,
                    contentDescription = "Saved in phone",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize().padding(2.dp)
                )
            }
        }
    }
}
