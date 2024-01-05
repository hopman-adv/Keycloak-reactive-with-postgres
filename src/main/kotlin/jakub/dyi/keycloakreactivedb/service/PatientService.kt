package jakub.dyi.keycloakreactivedb.service

import jakub.dyi.keycloakreactivedb.model.entity.Patient
import java.util.UUID

interface PatientService {

    suspend fun getPatientByIdAndOwnerId(id: UUID, ownerId: UUID): Patient

    suspend fun updateOrCreatePatient(
        ownerId: UUID,
        firstName: String?,
        lastName: String?,
        email: String,
        description: String?,
    ): Patient

    suspend fun deletePatient(id: UUID, ownerId: UUID)
}