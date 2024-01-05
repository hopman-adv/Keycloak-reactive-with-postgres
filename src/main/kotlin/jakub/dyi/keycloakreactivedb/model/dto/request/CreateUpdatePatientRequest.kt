package jakub.dyi.keycloakreactivedb.model.dto.request

data class CreateUpdatePatientRequest(
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val description: String?,
    ) {}

data class PatientResponse(
    val firstName: String?,
    val lastName: String?,
    val email: String,
    val description: String?,
) {}
