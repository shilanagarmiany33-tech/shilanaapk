package com.example.data

import kotlinx.coroutines.flow.Flow

class PrescriptionRepository(private val dao: PrescriptionDao) {
    val allPrescriptions: Flow<List<PrescriptionEntity>> = dao.getAllPrescriptions()
    val favoritePrescriptions: Flow<List<PrescriptionEntity>> = dao.getFavoritePrescriptions()

    fun search(query: String): Flow<List<PrescriptionEntity>> {
        return if (query.isBlank()) {
            allPrescriptions
        } else {
            dao.searchPrescriptions(query)
        }
    }

    suspend fun insert(prescription: PrescriptionEntity): Long {
        return dao.insertPrescription(prescription)
    }

    suspend fun toggleFavorite(prescription: PrescriptionEntity) {
        dao.updatePrescription(prescription.copy(isFavorite = !prescription.isFavorite))
    }

    suspend fun delete(id: Long) {
        dao.deletePrescriptionById(id)
    }
}
