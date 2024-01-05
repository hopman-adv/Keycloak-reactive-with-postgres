package jakub.dyi.keycloakreactivedb.controller

import jakub.dyi.keycloakreactivedb.model.dto.request.CreateUpdatePatientRequest
import jakub.dyi.keycloakreactivedb.model.dto.request.PatientResponse
import jakub.dyi.keycloakreactivedb.model.entity.toPatientResponse
import jakub.dyi.keycloakreactivedb.service.PatientService
import mu.KotlinLogging
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("/api/patients")
class PatientController(val patientService: PatientService) {

    @PreAuthorize("hasAuthority('USER')")
    @GetMapping("/{patientId}")
    suspend fun getPatient(@AuthenticationPrincipal jwt: Jwt, @PathVariable patientId: UUID)
            : ResponseEntity<PatientResponse> =
        patientService.getPatientByIdAndOwnerId(patientId, getOwnerUUID(jwt))
            .let { ResponseEntity.ok().body(it.toPatientResponse()) }

    @PreAuthorize("hasAuthority('USER')")
    @PutMapping
    suspend fun createUpdatePatient(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestBody request: CreateUpdatePatientRequest
    ): ResponseEntity<PatientResponse> {
        return patientService.updateOrCreatePatient(
            getOwnerUUID(jwt),
            request.firstName,
            request.lastName,
            request.email,
            request.description
        ).let {
            ResponseEntity
                .ok().body(it.toPatientResponse())
        }
    }

    @PreAuthorize("hasAuthority('USER')")
    @DeleteMapping("/{patientId}")
    suspend fun deletePatient(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable patientId: UUID,
    ): ResponseEntity<String> {
        patientService.deletePatient(patientId, getOwnerUUID(jwt))
        return ResponseEntity.noContent().build()
    }

    private fun getOwnerUUID(jwt: Jwt): UUID {
        return try {
            val uuid = UUID.fromString(jwt.getClaim("sub"))
            val name = jwt.getClaim<String>("preferred_username")
            log.debug { "Owner named: $name has UUID: $uuid" }
            uuid
        } catch (ex: Exception) {
            throw NoSuchElementException("Token does not contain owner UUID.")
        }
    }
}