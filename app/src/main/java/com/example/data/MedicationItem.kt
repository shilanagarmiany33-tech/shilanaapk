package com.example.data

data class MedicationItem(
    val name: String,
    val dosage: String,
    val schedule: String,
    val warning: String = ""
)
