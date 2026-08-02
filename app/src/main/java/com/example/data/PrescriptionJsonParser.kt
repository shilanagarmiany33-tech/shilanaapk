package com.example.data

import org.json.JSONArray
import org.json.JSONObject

object PrescriptionJsonParser {
    fun toJson(medications: List<MedicationItem>): String {
        val array = JSONArray()
        medications.forEach { med ->
            val obj = JSONObject()
            obj.put("name", med.name)
            obj.put("dosage", med.dosage)
            obj.put("schedule", med.schedule)
            obj.put("warning", med.warning)
            array.put(obj)
        }
        return array.toString()
    }

    fun fromJson(jsonStr: String): List<MedicationItem> {
        if (jsonStr.isBlank()) return emptyList()
        val list = mutableListOf<MedicationItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    MedicationItem(
                        name = obj.optString("name", ""),
                        dosage = obj.optString("dosage", ""),
                        schedule = obj.optString("schedule", ""),
                        warning = obj.optString("warning", "")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }
}
