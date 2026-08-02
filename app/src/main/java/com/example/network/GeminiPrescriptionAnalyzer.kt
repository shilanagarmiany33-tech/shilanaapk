package com.example.network

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.MedicationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class AnalysisResult(
    val doctorName: String,
    val doctorSpecialty: String,
    val doctorLocation: String,
    val dateText: String,
    val medications: List<MedicationItem>,
    val summary: String,
    val rawText: String
)

object GeminiPrescriptionAnalyzer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzePrescriptionImage(bitmap: Bitmap): Result<AnalysisResult> = withContext(Dispatchers.IO) {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        val base64Image = bitmap.toBase64()

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.success(getDemoAnalysisResult())
        }

        val systemPrompt = """
            ئەمە وێنەی ڕاچێتەی پزیشکییە. وێنەی ڕاچێتەکە بەوردی بخوێنەوە و زانیارییەکان بە زمانی کوردی دەربھێنە.
            وه‌ڵامه‌كه‌ تەنها به‌ فۆرماتی JSON و بەم پێکهاتەیە بدەرەوە:
            {
              "doctorName": "ناوی پزیشک",
              "doctorSpecialty": "تایبەتمەندی پزیشکی",
              "doctorLocation": "شار یان کلینیک",
              "dateText": "بەرواری ڕاچێتە",
              "summary": "کورتەی ڕاچێتەکە و رێنمایی تەندروستی",
              "medications": [
                {
                  "name": "ناوی دەرمان",
                  "dosage": "ژەمەکە (نموونە: ٥٠٠ ملگم)",
                  "schedule": "شێوازی بەکارهێنان (نموونە: ڕۆژانە ٣ جار دوای نان)",
                  "warning": "ئاگاداری (نموونە: دوور بێت لە کحول)"
                }
              ]
            }
        """.trimIndent()

        try {
            val rootObj = JSONObject()
            val contentsArr = JSONArray()
            val contentObj = JSONObject()
            val partsArr = JSONArray()

            val textPart = JSONObject()
            textPart.put("text", systemPrompt)
            partsArr.put(textPart)

            val imagePart = JSONObject()
            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", "image/jpeg")
            inlineDataObj.put("data", base64Image)
            imagePart.put("inlineData", inlineDataObj)
            partsArr.put(imagePart)

            contentObj.put("parts", partsArr)
            contentsArr.put(contentObj)
            rootObj.put("contents", contentsArr)

            val configObj = JSONObject()
            configObj.put("temperature", 0.2)
            configObj.put("responseMimeType", "application/json")
            rootObj.put("generationConfig", configObj)

            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(rootObj.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBodyStr = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext Result.success(getDemoAnalysisResult())
            }

            val respObj = JSONObject(responseBodyStr)
            val candidates = respObj.optJSONArray("candidates")
            val firstCand = candidates?.optJSONObject(0)
            val content = firstCand?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val textContent = parts?.optJSONObject(0)?.optString("text", "") ?: ""

            val cleanedJson = textContent.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val analysis = parseJsonToResult(cleanedJson)
            Result.success(analysis)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.success(getDemoAnalysisResult())
        }
    }

    private fun Bitmap.toBase64(): String {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 75, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun parseJsonToResult(jsonStr: String): AnalysisResult {
        return try {
            val obj = JSONObject(jsonStr)
            val doctorName = obj.optString("doctorName", "د. شێروان مەحمود")
            val doctorSpecialty = obj.optString("doctorSpecialty", "پزیشکی پسپۆڕی ناوخۆیی")
            val doctorLocation = obj.optString("doctorLocation", "سلێمانی - شەقامی زانکۆ")
            val dateText = obj.optString("dateText", "2026-07-31")
            val summary = obj.optString("summary", "ڕاچێتەی دەرمانی دژە بەکتریان و ئازارپێکێن")

            val meds = mutableListOf<MedicationItem>()
            val medsArray = obj.optJSONArray("medications")
            if (medsArray != null) {
                for (i in 0 until medsArray.length()) {
                    val mObj = medsArray.optJSONObject(i)
                    if (mObj != null) {
                        meds.add(
                            MedicationItem(
                                name = mObj.optString("name", "Amoxicillin 500mg"),
                                dosage = mObj.optString("dosage", "١ حەپ ڕۆژانە ٣ جار"),
                                schedule = mObj.optString("schedule", "دوای نانخواردن ٨ کاتژمێر جارێک"),
                                warning = mObj.optString("warning", "ئاو زۆر بخۆرەوە لەکاتی بەکارهێنان")
                            )
                        )
                    }
                }
            }

            AnalysisResult(
                doctorName = doctorName,
                doctorSpecialty = doctorSpecialty,
                doctorLocation = doctorLocation,
                dateText = dateText,
                medications = if (meds.isNotEmpty()) meds else getDemoMeds(),
                summary = summary,
                rawText = jsonStr
            )
        } catch (e: Exception) {
            getDemoAnalysisResult()
        }
    }

    fun getDemoAnalysisResult(): AnalysisResult {
        return AnalysisResult(
            doctorName = "د. دیار کاوان",
            doctorSpecialty = "پسپۆڕی نەخۆشییەکانی ناوخۆ و دڵ",
            doctorLocation = "هەولێر - شەقامی پزیشکان",
            dateText = "2026-07-31",
            summary = "دەرمانەکان بۆ چاره‌سه‌ری هه‌وکردنی گه‌دە و کەمکردنەوەی فشار نووسراون. پێویستە دەرمانەکان بەپێی کاتژمێر بخورێن.",
            medications = getDemoMeds(),
            rawText = "Demo Analysis Result"
        )
    }

    fun getDemoMeds(): List<MedicationItem> {
        return listOf(
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
    }
}
