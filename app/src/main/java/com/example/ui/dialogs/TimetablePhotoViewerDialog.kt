package com.example.ui.dialogs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.AuraGlassButton
import com.example.ui.components.AuraGlassIconButton
import com.example.ui.theme.LocalAuraColors
import com.example.util.MediaStorageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun TimetablePhotoViewerDialog(
    imagePath: String,
    onReplaceUri: (Uri) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = imagePath) {
        value = withContext(Dispatchers.IO) {
            MediaStorageHelper.loadBitmapSafe(imagePath, maxDimension = 2200)
        }
    }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 4.5f)
        scale = newScale
        if (newScale > 1.02f) {
            offsetX += panChange.x
            offsetY += panChange.y
        } else {
            offsetX = 0f
            offsetY = 0f
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onReplaceUri(uri)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xF5070D19), Color(0xFA0B1324))
                    )
                )
                .testTag("timetable_photo_viewer_dialog")
        ) {
            // Zoomable / Pannable Image Surface
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 80.dp, bottom = 112.dp, start = 12.dp, end = 12.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x18FFFFFF))
                    .border(1.dp, Color(0x36FFFFFF), RoundedCornerShape(22.dp))
                    .transformable(state = transformableState),
                contentAlignment = Alignment.Center
            ) {
                val loaded = bitmap
                if (loaded != null) {
                    Image(
                        bitmap = loaded.asImageBitmap(),
                        contentDescription = "Uploaded Official Timetable Photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )
                } else {
                    Text(
                        text = "Loading high-resolution timetable...",
                        color = LocalAuraColors.current.textSecondary
                    )
                }
            }

            // Top Frosted Bar
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Official Timetable Photo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Pinch to zoom • Drag to inspect rooms & slots",
                        style = MaterialTheme.typography.bodySmall,
                        color = LocalAuraColors.current.secondaryAccent
                    )
                }
                AuraGlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close Timetable Photo Viewer",
                    onClick = onDismiss,
                    size = 48.dp,
                    tint = Color.White,
                    testTag = "viewer_close_timetable_photo_button"
                )
            }

            // Bottom Controls: Quick Zoom + Replace + Remove
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AuraGlassButton(
                        text = "1x Fit",
                        isPrimary = scale <= 1.15f,
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        cornerRadius = 14.dp,
                        testTag = "timetable_zoom_1x"
                    )
                    AuraGlassButton(
                        text = "2x Zoom",
                        icon = Icons.Default.ZoomIn,
                        isPrimary = scale in 1.8f..2.4f,
                        onClick = { scale = 2f },
                        cornerRadius = 14.dp,
                        testTag = "timetable_zoom_2x"
                    )
                    AuraGlassButton(
                        text = "3x Detail",
                        isPrimary = scale >= 2.8f,
                        onClick = { scale = 3f },
                        cornerRadius = 14.dp,
                        testTag = "timetable_zoom_3x"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuraGlassButton(
                        text = "Replace Photo",
                        icon = Icons.Default.PhotoLibrary,
                        isPrimary = true,
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "viewer_replace_timetable_photo_button"
                    )
                    AuraGlassButton(
                        text = "Remove",
                        icon = Icons.Default.Delete,
                        containerColor = LocalAuraColors.current.danger.copy(alpha = 0.85f),
                        textColor = Color.White,
                        onClick = {
                            onRemove()
                            onDismiss()
                        },
                        testTag = "viewer_remove_timetable_photo_button"
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                }
            }
        }
    }
}
