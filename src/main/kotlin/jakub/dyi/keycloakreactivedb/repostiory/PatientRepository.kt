package jakub.dyi.keycloakreactivedb.repostiory

import jakub.dyi.keycloakreactivedb.model.entity.Patient
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface PatientRepository : CoroutineCrudRepository<Patient, UUID> {
    suspend fun findByEmail(email: String): Patient?

}