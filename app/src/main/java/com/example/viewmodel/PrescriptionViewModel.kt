package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.MedicationItem
import com.example.data.PrescriptionEntity
import com.example.data.PrescriptionJsonParser
import com.example.data.PrescriptionRepository
import com.example.network.AnalysisResult
import com.example.network.GeminiPrescriptionAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream

class PrescriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PrescriptionRepository

    val searchQuery = MutableStateFlow("")
    val selectedTab = MutableStateFlow(0) // 0: All, 1: Favorites, 2: Recent

    val isAnalyzing = MutableStateFlow(false)
    val analyzingStep = MutableStateFlow("")
    val lastAnalysisResult = MutableStateFlow<AnalysisResult?>(null)
    val lastScannedImageUri = MutableStateFlow<String>("")

    val selectedPrescription = MutableStateFlow<PrescriptionEntity?>(null)
    val showUploadSheet = MutableStateFlow(false)
    val showDetailModal = MutableStateFlow(false)

    init {
        val dao = AppDatabase.getDatabase(application).prescriptionDao()
        repository = PrescriptionRepository(dao)

        viewModelScope.launch {
            prepopulateDemoDataIfEmpty()
        }
    }

    val prescriptions: StateFlow<List<PrescriptionEntity>> = combine(
        repository.allPrescriptions,
        searchQuery,
        selectedTab
    ) { list, query, tab ->
        var filtered = if (query.isBlank()) {
            list
        } else {
            list.filter { item ->
                item.doctorName.contains(query, ignoreCase = true) ||
                item.doctorSpecialty.contains(query, ignoreCase = true) ||
                item.medicationsJson.contains(query, ignoreCase = true)
            }
        }

        when (tab) {
            1 -> filtered = filtered.filter { it.isFavorite }
            2 -> filtered = filtered.sortedByDescending { it.createdAt }
        }

        filtered
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun setSelectedTab(tab: Int) {
        selectedTab.value = tab
    }

    fun toggleFavorite(prescription: PrescriptionEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(prescription)
            if (selectedPrescription.value?.id == prescription.id) {
                selectedPrescription.value = selectedPrescription.value?.copy(isFavorite = !prescription.isFavorite)
            }
        }
    }

    fun deletePrescription(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            if (selectedPrescription.value?.id == id) {
                showDetailModal.value = false
                selectedPrescription.value = null
            }
        }
    }

    fun selectPrescription(prescription: PrescriptionEntity) {
        selectedPrescription.value = prescription
        showDetailModal.value = true
    }

    fun closeDetailModal() {
        showDetailModal.value = false
    }

    fun openUploadSheet() {
        showUploadSheet.value = true
    }

    fun closeUploadSheet() {
        showUploadSheet.value = false
    }

    fun analyzeBitmap(bitmap: Bitmap, imageUriStr: String) {
        viewModelScope.launch {
            isAnalyzing.value = true
            lastScannedImageUri.value = imageUriStr
            analyzingStep.value = "خوێندنەوەی دەستوخەتی پزیشک لە وێنەکە..."

            delay(600)
            analyzingStep.value = "شیکردنەوەی ناو و ژەمی دەرمانەکان..."

            val result = GeminiPrescriptionAnalyzer.analyzePrescriptionImage(bitmap)
            result.onSuccess { analysis ->
                analyzingStep.value = "پاشەکەوتکردن لە ئەرشیف..."
                delay(300)
                lastAnalysisResult.value = analysis

                val entity = PrescriptionEntity(
                    doctorName = analysis.doctorName,
                    doctorSpecialty = analysis.doctorSpecialty,
                    doctorLocation = analysis.doctorLocation,
                    dateText = analysis.dateText,
                    imageUri = imageUriStr.ifBlank { "android.resource://com.aistudio.rxanalyzer.kurdish/drawable/sample_rx_1" },
                    medicationsJson = PrescriptionJsonParser.toJson(analysis.medications),
                    summary = analysis.summary,
                    isFavorite = false
                )
                val newId = repository.insert(entity)
                val savedEntity = entity.copy(id = newId)
                selectedPrescription.value = savedEntity

                isAnalyzing.value = false
                showUploadSheet.value = false
                showDetailModal.value = true
            }.onFailure {
                isAnalyzing.value = false
            }
        }
    }

    fun analyzeUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                if (bitmap != null) {
                    analyzeBitmap(bitmap, uri.toString())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun analyzeDemoPreset(presetIndex: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val resId = com.example.R.drawable.sample_rx_1
            val bitmap = BitmapFactory.decodeResource(context.resources, resId)
            val uriStr = "android.resource://${context.packageName}/$resId"
            analyzeBitmap(bitmap, uriStr)
        }
    }

    private suspend fun prepopulateDemoDataIfEmpty() {
        val currentList = repository.search("").stateIn(viewModelScope).value
        if (currentList.isEmpty()) {
            val demo1 = PrescriptionEntity(
                doctorName = "د. دیار کاوان",
                doctorSpecialty = "پسپۆڕی نەخۆشییەکانی ناوخۆ و دڵ",
                doctorLocation = "سلێمانی - شەقامی زانکۆ",
                dateText = "2026-07-30",
                imageUri = "android.resource://com.aistudio.rxanalyzer.kurdish/drawable/sample_rx_1",
                medicationsJson = PrescriptionJsonParser.toJson(
                    listOf(
                        MedicationItem(
                            name = "Amoxicillin 500mg",
                            dosage = "١ حەپ 💊",
                            schedule = "ڕۆژانە ٣ جار (٨ کاتژمێر جارێک) - دوای نان",
                            warning = "دەبێت دەورەی دەرمانەکە بەتەواوی تەواو بکەیت"
                        ),
                        MedicationItem(
                            name = "Paracetamol 500mg",
                            dosage = "١-٢ حەپ",
                            schedule = "لە کاتی بوونی ئازار یان تای بەرز",
                            warning = "زیاتر لە ٤ گرام لە ڕۆژێکدا بەکار مەهێنە"
                        ),
                        MedicationItem(
                            name = "Omeprazole 20mg",
                            dosage = "١ کەپسول",
                            schedule = "بەیانیان نیو کاتژمێر پێش نانی بەیانی",
                            warning = "بەکارهێنانی لەگەڵ ئاوی زۆر"
                        )
                    )
                ),
                summary = "ڕاچێتەی دەرمانی هه‌وکردن و هێورکەرەوەی گەدە بۆ نەخۆشی گەدە نووسراوە.",
                isFavorite = true,
                createdAt = System.currentTimeMillis() - 86400000
            )

            val demo2 = PrescriptionEntity(
                doctorName = "د. ژینۆ ئارام",
                doctorSpecialty = "پسپۆڕی نەخۆشییەکانی منداڵان",
                doctorLocation = "هەولێر - شەقامی پزیشکان",
                dateText = "2026-07-28",
                imageUri = "android.resource://com.aistudio.rxanalyzer.kurdish/drawable/sample_rx_1",
                medicationsJson = PrescriptionJsonParser.toJson(
                    listOf(
                        MedicationItem(
                            name = "Cefixime Syrup 100mg/5ml",
                            dosage = "٥ مل",
                            schedule = "ڕۆژانە ١ جار (شوێن کەوتنی کاتژمێر)",
                            warning = "لە سەلاجە هەڵبگیرێت دوای بەکارهێنان"
                        ),
                        MedicationItem(
                            name = "Ibuprofen Syrup",
                            dosage = "٢.٥ مل",
                            schedule = "هەر ٨ کاتژمێر جارێک بۆ دا بەزاندن",
                            warning = "لەگەڵ گەدەی بەتاڵ نەدرێت بە منداڵ"
                        )
                    )
                ),
                summary = "ڕاچێتەی چارەسەری هەوکردنی قوڕگ و تای منداڵان.",
                isFavorite = false,
                createdAt = System.currentTimeMillis() - 259200000
            )

            repository.insert(demo1)
            repository.insert(demo2)
        }
    }
}
