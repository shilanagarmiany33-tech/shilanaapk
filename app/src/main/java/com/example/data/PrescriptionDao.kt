package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PrescriptionDao {
    @Query("SELECT * FROM prescriptions ORDER BY createdAt DESC")
    fun getAllPrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoritePrescriptions(): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE doctorName LIKE '%' || :query || '%' OR doctorSpecialty LIKE '%' || :query || '%' OR medicationsJson LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchPrescriptions(query: String): Flow<List<PrescriptionEntity>>

    @Query("SELECT * FROM prescriptions WHERE id = :id")
    suspend fun getPrescriptionById(id: Long): PrescriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrescription(prescription: PrescriptionEntity): Long

    @Update
    suspend fun updatePrescription(prescription: PrescriptionEntity)

    @Query("DELETE FROM prescriptions WHERE id = :id")
    suspend fun deletePrescriptionById(id: Long)
}
