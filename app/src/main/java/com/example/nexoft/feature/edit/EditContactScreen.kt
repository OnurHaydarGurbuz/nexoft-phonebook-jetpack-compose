package com.example.nexoft.feature.edit

import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.content.FileProvider
import com.example.nexoft.feature.profile.ContactUi
import com.example.nexoft.feature.create.FieldBox
import com.example.nexoft.feature.create.AddPhotoPickerSheet

@Composable
fun EditContactScreen(
    initial: ContactUi,
    onCancel: () -> Unit,
    onDone: (updated: ContactUi) -> Unit
) {
    var first by rememberSaveable { mutableStateOf(initial.first) }
    var last  by rememberSaveable { mutableStateOf(initial.last) }
    var phone by rememberSaveable { mutableStateOf(initial.phone) }
    var photo by rememberSaveable { mutableStateOf(initial.photo) }
    var showPicker by remember { mutableStateOf(false) }
    val doneEnabled = first.isNotBlank() && phone.isNotBlank()
    val ctx = LocalContext.current

    // camera/gallery aynen create'teki gibi
    fun createImageUri(ctx: Context): Uri {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = File(ctx.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "IMG_${time}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }
    var pendingUri by remember { mutableStateOf<Uri?>(null) }
    val takePictureLauncher = rememberLauncherForActivityResult(TakePicture()) { ok ->
        if (ok) photo = pendingUri
        pendingUri = null; showPicker = false
    }
    val permLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            val u = createImageUri(ctx); pendingUri = u; takePictureLauncher.launch(u)
        } else showPicker = false
    }
    val pickGallery = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) photo = uri; showPicker = false
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 42.dp),
            shape = RoundedCornerShape(topStart = 25.dp, topEnd = 25.dp),
            color = Color.White,
            tonalElevation = 6.dp,
            shadowElevation = 40.dp
        ) {
            Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                Spacer(Modifier.height(30.dp))
                Row(
                    Modifier.fillMaxWidth(),
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
                        "Edit Contact",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF202020)
                    )
                    Text(
                        "Done",
                        color = if (doneEnabled) Color(0xFF0075FF) else Color(0xFFD1D1D1),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable(enabled = doneEnabled) {
                            onDone(
                                initial.copy(
                                    first = first.trim(),
                                    last = last.trim(),
                                    phone = phone.trim(),
                                    photo = photo
                                )
                            )
                        }
                    )
                }

                Spacer(Modifier.height(33.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(96.dp).clip(CircleShape).background(Color(0xFFD1D1D1))
                    ) { AsyncImage(model = photo, contentDescription = null) }
                    Spacer(Modifier.height(8.dp))
                    Text("Change Photo",
                        color = Color(0xFF0075FF),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showPicker = true })
                }

                Spacer(Modifier.height(32.dp))
                FieldBox(value = first, onChange = { first = it }, placeholder = "First Name")
                Spacer(Modifier.height(12.dp))
                FieldBox(value = last,  onChange = { last  = it }, placeholder = "Last Name")
                Spacer(Modifier.height(12.dp))
                FieldBox(
                    value = phone, onChange = { phone = it },
                    placeholder = "+123456789",
                    keyboard = KeyboardOptions(imeAction = ImeAction.Done)
                )
            }
        }
    }

    AddPhotoPickerSheet(
        visible = showPicker,
        onDismiss = { showPicker = false },
        onCamera  = { permLauncher.launch(Manifest.permission.CAMERA) },
        onGallery = { pickGallery.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
    )
}
