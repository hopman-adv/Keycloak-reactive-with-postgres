package jakub.dyi.keycloakreactivedb.model.entity

import jakub.dyi.keycloakreactivedb.model.dto.request.PatientResponse
import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import org.springframework.security.access.AccessDeniedException
import java.util.NoSuchElementException
import java.util.UUID

@Table
class Patient(
    @Id
    val id: UUID?,
    val ownerId: UUID,
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val description: String?,
) {

}

fun Patient.toPatientResponse() =
    PatientResponse(firstName, lastName, email, description)

fun Patient.assertOwner(ownerId: UUID) {
    if (this.ownerId != ownerId) throw AccessDeniedException("Patient owned by different user.")
}
