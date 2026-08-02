package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PrescriptionJsonParser
import com.example.ui.components.AnalysisLoadingView
import com.example.ui.components.AppTopBar
import com.example.ui.components.PrescriptionCard
import com.example.ui.components.PrescriptionDetailModal
import com.example.ui.components.UploadPrescriptionSheet
import com.example.viewmodel.PrescriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PrescriptionViewModel,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        val prescriptions by viewModel.prescriptions.collectAsStateWithLifecycle()
        val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
        val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()

        val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
        val stepText by viewModel.analyzingStep.collectAsStateWithLifecycle()

        val showUploadSheet by viewModel.showUploadSheet.collectAsStateWithLifecycle()
        val showDetailModal by viewModel.showDetailModal.collectAsStateWithLifecycle()
        val selectedPrescription by viewModel.selectedPrescription.collectAsStateWithLifecycle()

        val totalMedsCount = prescriptions.sumOf { p ->
            PrescriptionJsonParser.fromJson(p.medicationsJson).size
        }
        val favoritesCount = prescriptions.count { it.isFavorite }

        Scaffold(
            topBar = {
                AppTopBar(
                    totalCount = prescriptions.size,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.openUploadSheet() },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    elevation = androidx.compose.material3.FloatingActionButtonDefaults.elevation(8.dp),
                    modifier = Modifier.testTag("scan_prescription_fab")
                ) {
                    Icon(
                        imageVector = Icons.Default.AddAPhoto,
                        contentDescription = "گرتنی ڕاچێتە"
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "گرتنی ڕاچێتەی نوێ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        MetricsBar(
                            totalPrescriptions = prescriptions.size,
                            totalMeds = totalMedsCount,
                            favorites = favoritesCount
                        )
                    }

                    item {
                        FilterChipsRow(
                            selectedTab = selectedTab,
                            onTabSelected = { viewModel.setSelectedTab(it) }
                        )
                    }

                    if (prescriptions.isEmpty()) {
                        item {
                            EmptyStateView(
                                searchQuery = searchQuery,
                                onScanClick = { viewModel.openUploadSheet() }
                            )
                        }
                    } else {
                        items(
                            items = prescriptions,
                            key = { it.id }
                        ) { prescription ->
                            PrescriptionCard(
                                prescription = prescription,
                                onClick = { viewModel.selectPrescription(prescription) },
                                onToggleFavorite = { viewModel.toggleFavorite(prescription) }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }

                if (showUploadSheet) {
                    UploadPrescriptionSheet(
                        onDismiss = { viewModel.closeUploadSheet() },
                        onCameraCapture = { bitmap ->
                            viewModel.analyzeBitmap(bitmap, "")
                        },
                        onGalleryPick = { uri ->
                            viewModel.analyzeUri(uri)
                        },
                        onDemoSelect = { presetIndex ->
                            viewModel.analyzeDemoPreset(presetIndex)
                        }
                    )
                }

                if (isAnalyzing) {
                    AnalysisLoadingView(
                        stepText = stepText,
                        onDismiss = { }
                    )
                }

                if (showDetailModal && selectedPrescription != null) {
                    PrescriptionDetailModal(
                        prescription = selectedPrescription!!,
                        onDismiss = { viewModel.closeDetailModal() },
                        onToggleFavorite = { viewModel.toggleFavorite(selectedPrescription!!) },
                        onDelete = { viewModel.deletePrescription(selectedPrescription!!.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricsBar(
    totalPrescriptions: Int,
    totalMeds: Int,
    favorites: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricTile(
            icon = Icons.Default.Description,
            title = "کۆی ڕاچێتەکان",
            value = "$totalPrescriptions",
            containerColor = MaterialTheme.colorScheme.surface,
            iconTint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            icon = Icons.Default.Medication,
            title = "دەرمانەکان",
            value = "$totalMeds",
            containerColor = MaterialTheme.colorScheme.surface,
            iconTint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            icon = Icons.Default.Star,
            title = "دڵخوازەکان",
            value = "$favorites",
            containerColor = MaterialTheme.colorScheme.surface,
            iconTint = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    containerColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipsRow(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("هەمووی", "دڵخوازەکان ⭐", "نوێترین ⏱️")

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(options.size) { index ->
            FilterChip(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                label = {
                    Text(
                        text = options[index],
                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("filter_tab_$index")
            )
        }
    }
}

@Composable
private fun EmptyStateView(
    searchQuery: String,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Empty",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (searchQuery.isNotBlank()) "هیچ ڕاچێتەیەک نەدۆزرایەوە" else "ئەرشیفی ڕاچێتەکان چۆڵە",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (searchQuery.isNotBlank())
                    "دڵنیا ببەوە لە ناوی دەرمان یان ناوی پزیشکەکە"
                else
                    "یەکەم ڕاچێتەی پزیشکەکەت بە کامێرا بگرە بۆ ئەوەی ژیری دەستکردی Gemini شیکاری بکات",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
