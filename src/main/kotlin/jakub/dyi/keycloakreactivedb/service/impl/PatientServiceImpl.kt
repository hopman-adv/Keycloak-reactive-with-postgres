package jakub.dyi.keycloakreactivedb.service.impl

import jakub.dyi.keycloakreactivedb.model.entity.Patient
import jakub.dyi.keycloakreactivedb.model.entity.assertOwner
import jakub.dyi.keycloakreactivedb.repostiory.PatientRepository
import jakub.dyi.keycloakreactivedb.service.PatientService
import org.springframework.stereotype.Service
import java.util.*

@Service
class PatientServiceImpl(val repository: PatientRepository) : PatientService {
    override suspend fun getPatientByIdAndOwnerId(id: UUID, ownerId: UUID): Patient {
        return assertOwnershipAndGetPatient(id, ownerId)
    }

    override suspend fun updateOrCreatePatient(
        ownerId: UUID,
        firstName: String?,
        lastName: String?,
        email: String,
        description: String?
    ): Patient {
        val existingPatient = repository.findByEmail(email)
        return if (existingPatient == null) {
            repository.save(Patient(null, ownerId, firstName, lastName, email, description))
        } else {
            existingPatient.assertOwner(ownerId)
            repository.save(Patient(existingPatient.id, ownerId, firstName, lastName, email, description))
        }
    }

    override suspend fun deletePatient(id: UUID, ownerId: UUID) {
        assertOwnershipAndGetPatient(id, ownerId)
        repository.deleteById(id)
    }

    private suspend fun assertOwnershipAndGetPatient(id: UUID, ownerId: UUID): Patient {
        val patient = repository.findById(id)
        patient?.assertOwner(ownerId) ?: throw NoSuchElementException("Patient not found.")
        return patient
    }
}