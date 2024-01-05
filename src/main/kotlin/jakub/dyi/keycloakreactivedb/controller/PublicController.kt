package jakub.dyi.keycloakreactivedb.controller

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController()
class PublicController {

    @GetMapping("/public")
    fun getPublic(): ResponseEntity<String> = ResponseEntity.ok("Hello. This is unsecured endpoint.")
}