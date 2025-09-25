package com.example.nexoft.feature.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.nexoft.feature.contacts.isInDeviceContacts
import com.example.nexoft.feature.contacts.saveToDeviceContacts
import com.example.nexoft.ui.SheetHeader
import com.example.nexoft.ui.SheetScaffold
import kotlinx.coroutines.withContext

data class ContactUi(
    val id: String,
    val first: String,
    val last: String,
    val phone: String,
    val photo: Uri?,
    val inDevice: Boolean
)

@Composable
fun ContactProfileScreen(
    contact: ContactUi,
    onBack: () -> Unit,
    onEdit: (ContactUi) -> Unit,
    onDelete: (String) -> Unit,
    onMarkedDeviceSaved: (String) -> Unit
) {
    val ctx = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    var savedInDevice by remember { mutableStateOf(contact.inDevice) }
    var showToast by remember { mutableStateOf(false) }

    fun hasRead() = androidx.core.content.ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.READ_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(contact.id) {
        if (!savedInDevice && hasRead()) {
            savedInDevice = withContext(kotlinx.coroutines.Dispatchers.IO) {
                isInDeviceContacts(ctx, contact.phone)
            }
        }
    }


    SheetScaffold {
        SheetHeader(
            leftLabel = "Back",
            onLeft = onBack,
            title = "Profile",
            rightContent = {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color(0xFF3D3D3D))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = {
                        menuOpen = false; onEdit(contact)
                    })
                    DropdownMenuItem(text = { Text("Delete", color = Color.Red) }, onClick = {
                        menuOpen = false; onDelete(contact.id)
                    })
                }
            }
        )

        Spacer(Modifier.height(33.dp))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(96.dp).clip(CircleShape).background(Color(0xFFD1D1D1)),
                contentAlignment = Alignment.Center
            ) { AsyncImage(model = contact.photo, contentDescription = "photo") }
            Spacer(Modifier.height(8.dp))
            Text("Change Photo", color = Color(0xFF0075FF), fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(32.dp))
        DisabledField(contact.first)
        Spacer(Modifier.height(12.dp))
        DisabledField(contact.last)
        Spacer(Modifier.height(12.dp))
        DisabledField(contact.phone)

        Spacer(Modifier.height(24.dp))
        SaveToPhoneButton(
            contact = contact,
            savedInDevice = savedInDevice,
            onSaved = {
                savedInDevice = true
                onMarkedDeviceSaved(contact.id)
                showToast = true
            }
        )

        Spacer(Modifier.height(12.dp))
        if (savedInDevice) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = Color(0xFF6D6D6D))
                Spacer(Modifier.width(8.dp))
                Text(
                    "This contact is already saved your phone.",
                    color = Color(0xFF6D6D6D),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showToast) {
        BottomGreenToast("User is added to your phone!") { showToast = false }
    }
}

@Composable
private fun DisabledField(text: String) {
    OutlinedTextField(
        value = text,
        onValueChange = {},
        enabled = false,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = Color(0xFF202020),
            disabledContainerColor = Color.White,
            disabledBorderColor = Color(0xFFE7E7E7),
        )
    )
}

@Composable
private fun SaveToPhoneButton(
    contact: ContactUi,
    savedInDevice: Boolean,
    onSaved: () -> Unit
) {
    val ctx = LocalContext.current
    fun hasWrite() = androidx.core.content.ContextCompat.checkSelfPermission(
        ctx, Manifest.permission.WRITE_CONTACTS
    ) == PackageManager.PERMISSION_GRANTED

    val permLauncher = rememberLauncherForActivityResult(RequestMultiplePermissions()) { res ->
        if (res[Manifest.permission.WRITE_CONTACTS] == true) {
            if (saveToDeviceContacts(ctx, contact.first, contact.last, contact.phone, contact.photo)) {
                onSaved()
            }
        }
    }

    val outline = if (savedInDevice) Color(0xFFE7E7E7) else Color(0xFF202020)
    val textCol = if (savedInDevice) Color(0xFFD1D1D1) else Color(0xFF202020)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(64.dp))
            .border(1.dp, outline, RoundedCornerShape(64.dp))
            .clickable(enabled = !savedInDevice) {
                if (hasWrite()) {
                    if (saveToDeviceContacts(ctx, contact.first, contact.last, contact.phone, contact.photo)) {
                        onSaved()
                    }
                } else {
                    permLauncher.launch(arrayOf(
                        Manifest.permission.WRITE_CONTACTS,
                        Manifest.permission.READ_CONTACTS
                    ))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Text("Save to My Phone Contact", color = textCol, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BottomGreenToast(text: String, onGone: () -> Unit) {
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(1600); onGone() }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Surface(
            color = Color.White, tonalElevation = 6.dp, shadowElevation = 6.dp,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(bottom = 24.dp).fillMaxWidth(0.9f)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF00C853))
                Spacer(Modifier.width(8.dp))
                Text(text, color = Color(0xFF00C853), fontWeight = FontWeight.Medium)
            }
        }
    }
}
