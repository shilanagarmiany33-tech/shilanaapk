package com.example.ui.components

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadPrescriptionSheet(
    onDismiss: () -> Unit,
    onCameraCapture: (Bitmap) -> Unit,
    onGalleryPick: (Uri) -> Unit,
    onDemoSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            onCameraCapture(bitmap)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            onGalleryPick(uri)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "گرتن و بارکردنی وێنەی ڕاچێتە",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "مۆدێلی Gemini وێنەکە دەخوێنێتەوە و زانیارییەکان جیا دەکاتەوە",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "داخستن"
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: Live Camera
            OptionTile(
                icon = Icons.Default.CameraAlt,
                iconBg = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "کامێرای ڕاستەوخۆ",
                subtitle = "بە کامێرای مۆبایلەکەت وێنەی ڕاچێتەی پزیشک بگرە",
                testTag = "camera_option_button",
                onClick = {
                    cameraLauncher.launch()
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Gallery Upload
            OptionTile(
                icon = Icons.Default.PhotoLibrary,
                iconBg = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "بارکردن لە گەلەری",
                subtitle = "وێنەی پاشەکەوتکراوی ڕاچێتەکە لە مۆبایلەکەت هەڵبژێرە",
                testTag = "gallery_option_button",
                onClick = {
                    galleryLauncher.launch("image/*")
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 3: Quick Preset Demo
            OptionTile(
                icon = Icons.Default.AutoAwesome,
                iconBg = Color(0xFFFEF3C7),
                iconTint = Color(0xFFD97706),
                title = "تاقیکردنەوە بە وێنەی نموونە (Demo)",
                subtitle = "ڕاستەوخۆ بە وێنەی ئامادەکراوی ڕاچێتە شیکەرەوەکە تاقی بکەرەوە",
                testTag = "demo_option_button",
                onClick = {
                    onDemoSelect(0)
                }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun OptionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
