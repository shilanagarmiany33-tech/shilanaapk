package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val doctorName: String,
    val doctorSpecialty: String,
    val doctorLocation: String,
    val dateText: String,
    val imageUri: String,
    val medicationsJson: String,
    val summary: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
