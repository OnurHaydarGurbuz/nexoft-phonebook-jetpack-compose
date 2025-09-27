package com.example.nexoft.feature.create

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.nexoft.feature.contacts.PersonIcon
import com.example.nexoft.ui.SheetHeader
import com.example.nexoft.ui.SheetScaffold
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@RequiresApi(Build.VERSION_CODES.R)
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
    val context = LocalContext.current
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    val doneEnabled = first.isNotBlank() && phone.isNotBlank()
    var showSuccess by remember { mutableStateOf(false) }
    var showCropper by remember { mutableStateOf(false) }
    var pendingSource by remember { mutableStateOf<Uri?>(null) }

    // Düzeltilmiş createImageUri fonksiyonu
    fun createImageUri(ctx: Context): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}.jpg"
        val storageDir = ctx.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        storageDir?.mkdirs()
        val imageFile = File(storageDir, imageFileName)
        return FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", imageFile)
    }




    val takePictureLauncher = rememberLauncherForActivityResult(TakePicture()) { success ->
        if (success && pendingCameraUri != null) {
            pendingSource = pendingCameraUri
            showCropper = true
        }
        pendingCameraUri = null
        showPickerSheet = false
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) {
            try {
                val uri = createImageUri(context)
                pendingCameraUri = uri
                takePictureLauncher.launch(uri)
            } catch (e: Exception) {
                e.printStackTrace()
                showPickerSheet = false
            }
        } else {
            showPickerSheet = false
        }
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(PickVisualMedia()) { uri ->
        if (uri != null) {
            pendingSource = uri
            showCropper = true
        }
        showPickerSheet = false
    }

    SheetScaffold(topPadding = 42.dp) {
        SheetHeader(
            leftLabel = "Cancel",
            onLeft = onCancel,
            title = "New Contact",
            rightContent = {
                Text(
                    text = "Done",
                    color = if (doneEnabled) Color(0xFF0075FF) else Color(0xFFD1D1D1),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 2.dp)
                        .clickable(enabled = doneEnabled) {
                            showSuccess = true
                        }
                )
            }
        )

        // Avatar + "Add Photo"
        Spacer(Modifier.height(33.dp))
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD1D1D1)),
                contentAlignment = Alignment.Center
            ) {
                if (photoUri == null) {
                    PersonIcon(size = 96.dp, color = Color(0xFFD1D1D1))
                } else {
                    AsyncImage(
                        model = photoUri,
                        contentDescription = "photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
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

        // Form alanları
        Spacer(Modifier.height(32.dp))
        FieldBox(value = first, onChange = { first = it }, placeholder = "First Name")
        Spacer(Modifier.height(12.dp))
        FieldBox(value = last,  onChange = { last = it },  placeholder = "Last Name")
        Spacer(Modifier.height(12.dp))
        FieldBox(
            value = phone,
            onChange = { phone = it },
            placeholder = "Phone Number",
            keyboard = KeyboardOptions(imeAction = ImeAction.Done)
        )
    }

    // Foto picker sheet
    AddPhotoPickerSheet(
        visible = showPickerSheet,
        onDismiss = { showPickerSheet = false },
        onCamera  = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
        onGallery = { pickMediaLauncher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly)) }
    )

    // Düzeltilmiş PhotoCropper - artık tam ekran dialog
    if (showCropper && pendingSource != null) {
        PhotoCropperDialog(
            sourceUri = pendingSource!!,
            onDismiss = {
                showCropper = false
                pendingSource = null
            },
            onCropped = { processed ->
                showCropper = false
                pendingSource = null
                photoUri = processed
            }
        )
    }

    // Başarılı animasyonu
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
                placeholder,
                color = Color(0xFF888888),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(64.dp))
                    .border(1.dp, Color(0xFF202020), RoundedCornerShape(64.dp))
                    .clickable(onClick = onCamera),
                contentAlignment = Alignment.Center
            ) {
                Text("Camera", color = Color(0xFF202020), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(64.dp))
                    .border(1.dp, Color(0xFF202020), RoundedCornerShape(64.dp))
                    .clickable(onClick = onGallery),
                contentAlignment = Alignment.Center
            ) {
                Text("Gallery", color = Color(0xFF202020), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = Color(0xFF0075FF), fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OneShotLottie(
    assetName: String,
    onFinished: () -> Unit
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        focusManager.clearFocus(force = true)
        keyboard?.hide()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .zIndex(10f),
        contentAlignment = Alignment.Center
    ) {
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

@RequiresApi(Build.VERSION_CODES.R)
@OptIn(DelicateCoroutinesApi::class)
@Composable
fun PhotoCropperDialog(
    sourceUri: Uri,
    onDismiss: () -> Unit,
    onCropped: (Uri) -> Unit,
    outSizePx: Int = 512,
    outQuality: Int = 70
) {
    val ctx = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var translation by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }
    var imageSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    // Resmin container içinde FIT boyutlarını veren yardımcı
    fun calculateImageBounds(): androidx.compose.ui.geometry.Rect {
        if (imageSize == androidx.compose.ui.geometry.Size.Zero || containerSize == androidx.compose.ui.geometry.Size.Zero) {
            return androidx.compose.ui.geometry.Rect.Zero
        }
        val containerRatio = containerSize.width / containerSize.height
        val imageRatio = imageSize.width / imageSize.height

        val (fitWidth, fitHeight) = if (imageRatio > containerRatio) {
            containerSize.width to (containerSize.width / imageRatio)
        } else {
            (containerSize.height * imageRatio) to containerSize.height
        }

        val left = (containerSize.width - fitWidth) / 2f
        val top  = (containerSize.height - fitHeight) / 2f
        return androidx.compose.ui.geometry.Rect(
            offset = androidx.compose.ui.geometry.Offset(left, top),
            size = androidx.compose.ui.geometry.Size(fitWidth, fitHeight)
        )
    }

    // Jest: pan/zoom sınırları daireye göre hesaplanır
    val transform = rememberTransformableState { zoomChange, panChange, _ ->
        val bounds = calculateImageBounds()
        if (bounds == androidx.compose.ui.geometry.Rect.Zero) return@rememberTransformableState

        val circleRadius = containerSize.minDimension * 0.4f
        val circleDiameter = circleRadius * 2f

        // daireyi kaplayacak minimum ölçek
        val minScaleX = circleDiameter / bounds.width
        val minScaleY = circleDiameter / bounds.height
        val minScaleNeeded = maxOf(minScaleX, minScaleY)

        val newScale = (scale * zoomChange).coerceIn(minScaleNeeded, 4f)

        val scaledW = bounds.width  * newScale
        val scaledH = bounds.height * newScale

        val maxTransX = ((scaledW - circleDiameter) / 2f).coerceAtLeast(0f)
        val maxTransY = ((scaledH - circleDiameter) / 2f).coerceAtLeast(0f)

        scale = newScale
        translation = androidx.compose.ui.geometry.Offset(
            (translation.x + panChange.x).coerceIn(-maxTransX, maxTransX),
            (translation.y + panChange.y).coerceIn(-maxTransY, maxTransY)
        )
    }

    // İlk açılışta min scale uygula ve ortala
    LaunchedEffect(imageSize, containerSize) {
        val bounds = calculateImageBounds()
        if (bounds != androidx.compose.ui.geometry.Rect.Zero) {
            val circleRadius = containerSize.minDimension * 0.4f
            val circleDiameter = circleRadius * 2f
            val minScaleX = circleDiameter / bounds.width
            val minScaleY = circleDiameter / bounds.height
            val need = maxOf(minScaleX, minScaleY)
            if (need.isFinite() && need > 0f) {
                scale = need
                translation = androidx.compose.ui.geometry.Offset.Zero
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Üst bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .zIndex(1f),
                horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White)
                    }
                    Text("Move and Scale", color = Color.White, fontWeight = FontWeight.Medium)
                    TextButton(
                        onClick = {
                            GlobalScope.launch(Dispatchers.IO) {
                                val uri = cropCircleAndCompress(
                                    ctx, sourceUri, scale, translation,
                                    containerSize,   // 👈 EKLENDİ
                                    imageSize,
                                    outSizePx, outQuality,
                                )
                                withContext(Dispatchers.Main) { onCropped(uri) }
                            }
                        }
                    ) {
                        Text("Use Photo", color = Color(0xFF0075FF), fontWeight = FontWeight.Bold)
                    }
                }

                // Ana crop alanı
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(32.dp)
                        .clipToBounds()
                        .onSizeChanged { size ->
                            containerSize = androidx.compose.ui.geometry.Size(
                                size.width.toFloat(), size.height.toFloat()
                            )
                        }
                        .transformable(transform), // 👈 jest burada
                    contentAlignment = Alignment.Center
                ) {
                    // Resim
                    AsyncImage(
                        model = sourceUri,
                        contentDescription = null,
                        onSuccess = { state ->
                            val d = state.result.drawable
                            imageSize = androidx.compose.ui.geometry.Size(
                                d.intrinsicWidth.toFloat(),
                                d.intrinsicHeight.toFloat()
                            )
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = translation.x,
                                translationY = translation.y
                            ),
                        contentScale = ContentScale.Fit
                    )

                    // Üstte sadece beyaz çember
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val circleRadius = size.minDimension * 0.4f
                        val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                        drawCircle(
                            color = Color.White,
                            radius = circleRadius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }
                }

                // Alt ipucu
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Pinch to zoom, drag to move",
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}




fun cropCircleAndCompress(
    context: Context,
    source: Uri,
    scale: Float,                                   // UI'daki ölçek
    translation: androidx.compose.ui.geometry.Offset, // UI'daki pan (container pikselinde)
    containerSize: androidx.compose.ui.geometry.Size, // UI container boyutu (onSizeChanged ile tuttuğun)
    imageSize: androidx.compose.ui.geometry.Size,     // Drawable intrinsic size (onSuccess'ta aldığın)
    outSizePx: Int,
    outQuality: Int,
    format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
): Uri {
    // 1) Kaynağı oku
    val srcBmp: Bitmap = try {
        if (Build.VERSION.SDK_INT >= 28) {
            val src = ImageDecoder.createSource(context.contentResolver, source)
            ImageDecoder.decodeBitmap(src) { decoder, _, _ ->
                decoder.isMutableRequired = true
            }
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, source)
        }
    } catch (e: Exception) {
        throw RuntimeException("Failed to decode bitmap", e)
    }

    val outBmp = createBitmap(outSizePx, outSizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(outBmp)
    canvas.drawColor(android.graphics.Color.TRANSPARENT)

    val cw = containerSize.width.coerceAtLeast(1f)
    val ch = containerSize.height.coerceAtLeast(1f)

    // 2) UI'daki FIT yerleşimi (AsyncImage ContentScale.Fit ile aynı)
    val srcW = srcBmp.width.toFloat()
    val srcH = srcBmp.height.toFloat()
    val imageRatio = srcW / srcH
    val containerRatio = cw / ch

    val fitW: Float
    val fitH: Float
    if (imageRatio > containerRatio) {
        fitW = cw
        fitH = cw / imageRatio
    } else {
        fitH = ch
        fitW = ch * imageRatio
    }
    val fitLeft = (cw - fitW) / 2f
    val fitTop  = (ch - fitH) / 2f

    // 3) UI matrisini kur: FIT -> merkezde scale -> pan
    val m = android.graphics.Matrix().apply {
        // FIT: src -> container'daki fitRect
        postScale(fitW / srcW, fitH / srcH)
        postTranslate(fitLeft, fitTop)

        // UI scale: container merkezine göre
        postScale(scale, scale, cw / 2f, ch / 2f)

        // UI pan (container pikselinde)
        postTranslate(translation.x, translation.y)
    }

    // 4) Container uzayını output tuvaline eşle (daire ölçeği birebir olsun)
    // UI'daki daire çapı: 0.8 * min(container)
    // Output'taki daire çapı: 0.8 * outSizePx
    // Oran: K = outSizePx / min(container)
    val containerMin = minOf(cw, ch).coerceAtLeast(1f)
    val K = outSizePx / containerMin

    // Container merkezini out canvas merkezine taşı ve ölçekle
    m.postTranslate(-cw / 2f, -ch / 2f)
    m.postScale(K, K)
    m.postTranslate(outSizePx / 2f, outSizePx / 2f)

    // 5) Çiz
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    canvas.drawBitmap(srcBmp, m, paint)

    // 6) Dairesel maske (UI ile aynı oran: 0.40f)
    val mask = createBitmap(outSizePx, outSizePx, Bitmap.Config.ALPHA_8)
    val maskCanvas = android.graphics.Canvas(mask)
    val radius = outSizePx * 0.40f
    val center = outSizePx / 2f
    val maskPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
    }
    maskCanvas.drawColor(android.graphics.Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR)
    maskCanvas.drawCircle(center, center, radius, maskPaint)

    // Xfermode güvenli katmanla uygula
    val save = canvas.saveLayer(0f, 0f, outSizePx.toFloat(), outSizePx.toFloat(), null)
    paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.DST_IN)
    canvas.drawBitmap(mask, 0f, 0f, paint)
    paint.xfermode = null
    canvas.restoreToCount(save)

    // Temizlik
    mask.recycle()
    srcBmp.recycle()

    // 7) Dosyaya yaz
    val dir = File(context.filesDir, "avatars").apply { mkdirs() }
    val ext = when (format) {
        Bitmap.CompressFormat.PNG -> "png"
        Bitmap.CompressFormat.JPEG -> "jpg"
        else -> "webp"
    }
    val outFile = File(dir, "avatar_${System.currentTimeMillis()}.$ext")
    FileOutputStream(outFile).use { fos ->
        outBmp.compress(format, outQuality.coerceIn(70, 100), fos)
    }
    outBmp.recycle()

    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
}

