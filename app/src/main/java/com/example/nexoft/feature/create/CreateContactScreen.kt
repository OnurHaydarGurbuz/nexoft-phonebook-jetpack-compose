package com.example.nexoft.feature.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.sp
import android.Manifest
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.*
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.zIndex


@Composable
fun CreateContactScreen(
    onCancel: () -> Unit,
    onDone: (first: String, last: String, phone: String, photo: Uri?) -> Unit
) {
    // state
    var first by rememberSaveable { mutableStateOf("") }
    var last  by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var showPickerSheet by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val doneEnabled = first.isNotBlank() && phone.isNotBlank()
    var showSuccess by remember { mutableStateOf(false) }


    fun createImageUri(ctx: Context): Uri {
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dir = File(ctx.cacheDir, "images").apply { mkdirs() }
        val file = File(dir, "IMG_${time}.jpg")
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
    }
    val takePictureLauncher = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success) photoUri = pendingCameraUri
        pendingCameraUri = null
        showPickerSheet = false
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            val uri = createImageUri(context)
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } else {
            showPickerSheet = false
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) photoUri = uri
        showPickerSheet = false
    }


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
        )
        {
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
                        modifier = Modifier.clickable(enabled = doneEnabled) {
                            showSuccess = true
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
                        if (photoUri == null) {
                            Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = Color.White)
                        } else {
                            AsyncImage(model = photoUri, contentDescription = "photo", modifier = Modifier.fillMaxSize())
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Add Photo",
                        color = Color(0xFF0075FF),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showPickerSheet = true }
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
    AddPhotoPickerSheet(
        visible = showPickerSheet,
        onDismiss = { showPickerSheet = false },
        onCamera  = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onGallery = { pickMediaLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
    )
    if (showSuccess) {
        OneShotLottie(assetName = "Done.lottie") {
            showSuccess = false
            onDone(first.trim(), last.trim(), phone.trim(), photoUri)
        }
    }


}


@Composable
internal fun FieldBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboard: KeyboardOptions = KeyboardOptions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        placeholder = {
            Text(
                placeholder,                 // "First", "Last", "Phone"
                color = Color(0xFF888888),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,        // smaller
                    lineHeight = 18.sp,      // avoids bottom clipping
                    fontWeight = FontWeight.SemiBold
                ),
                maxLines = 1
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPhotoPickerSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    onCamera: () -> Unit,
    onGallery: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        scrimColor = Color.Black.copy(alpha = 0.85f),
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
        dragHandle = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // pill: Camera
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(64.dp))
                    .border(1.dp, Color(0xFF202020), RoundedCornerShape(64.dp))
                    .clickable(onClick = onCamera),
                contentAlignment = Alignment.Center
            ) { Text("Camera", color = Color(0xFF202020), fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(8.dp))

            // pill: Gallery
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(64.dp))
                    .border(1.dp, Color(0xFF202020), RoundedCornerShape(64.dp))
                    .clickable(onClick = onGallery),
                contentAlignment = Alignment.Center
            ) { Text("Gallery", color = Color(0xFF202020), fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(12.dp))

            // text button: Cancel (blue)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) { Text("Cancel", color = Color(0xFF0075FF), fontWeight = FontWeight.SemiBold) }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OneShotLottie(
    assetName: String,
    onFinished: () -> Unit
) {
    // Hide keyboard immediately
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }

    // Fullscreen white overlay (no Dialog)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .zIndex(10f),
        contentAlignment = Alignment.Center
    ) {
        // Lottie + texts
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val comp by rememberLottieComposition(LottieCompositionSpec.Asset(assetName))
            val progress by animateLottieCompositionAsState(
                composition = comp,
                iterations = 1,
                restartOnPlay = false
            )
            if (progress >= 0.999f) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(250)
                    onFinished()
                }
            }
            LottieAnimation(
                composition = comp,
                progress = { progress },
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.height(24.dp))
            Text("All Done!", color = Color(0xFF202020), fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("New contact saved 🎉", color = Color(0xFF3D3D3D), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
