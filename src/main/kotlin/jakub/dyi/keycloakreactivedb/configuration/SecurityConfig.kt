package jakub.dyi.keycloakreactivedb.configuration

import com.jayway.jsonpath.JsonPath
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.invoke
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.security.web.server.savedrequest.NoOpServerRequestCache
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.security.Principal


@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(
        http: ServerHttpSecurity,
        customConverter: Converter<Jwt, Mono<out AbstractAuthenticationToken>>
    )
            : SecurityWebFilterChain {
        return http {
            oauth2ResourceServer {
                jwt { jwtAuthenticationConverter = customConverter }
            }

            authorizeExchange {
                authorize("/actuator/health/readiness", permitAll)
                authorize("/actuator/health/liveness", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/public", permitAll)
                authorize("/api/**", authenticated)
            }
            cors { configurationSource = corsConfigurationSource("localhost:8081") } // TODO: property
            csrf { disable() }

            /** State je uložen v Access tokenu. Nahrazuje STATELSESS session management.
             * Webflux sice session nepoužívá, ale prohledává je. Tohle vymaže request cache.
             * A nastaví prázdné repository pro contextHandler, takže se sessions nebudou hledat. */
            requestCache { NoOpServerRequestCache.getInstance() }
            http.securityContextRepository(NoOpServerSecurityContextRepository.getInstance())

            exceptionHandling {
                this.accessDeniedHandler = customAccessDeniedHandler()
            }
        }
    }

    /**
     * Sets response to UNAUTHORIZED or FORBIDDEN and takes message from Exception and
     * writes it to response. It is lower than REST, so it will not be caught by advice/RestExceptionHandler.kt.
     * Therefore, we are writing directly using DataBuffer because WebExceptionHandler is low level component.
     *
     * Author: Jakub
     */
    private fun customAccessDeniedHandler(): ServerAccessDeniedHandler {
        return ServerAccessDeniedHandler { exchange: ServerWebExchange, ex: AccessDeniedException ->
            exchange.getPrincipal<Principal>().flatMap {
                val response = exchange.response.apply {
                    statusCode =
                        if (it is AnonymousAuthenticationToken) HttpStatus.UNAUTHORIZED else HttpStatus.FORBIDDEN
                    headers.contentType = MediaType.APPLICATION_JSON
                }
                val errorMessage = ex.message?.toByteArray() ?: "Exception occurred".toByteArray()
                val buffer = response.bufferFactory().wrap(errorMessage)
                response.writeWith(Mono.just(buffer)).doOnError { DataBufferUtils.release(buffer) }
            }
        }
    }

    private fun corsConfigurationSource(vararg origins: String): CorsConfigurationSource {
        return with(CorsConfiguration()) conf@{
            allowedOrigins = listOf(*origins)
            allowedMethods = listOf("*")
            allowedHeaders = listOf("*")
            exposedHeaders = listOf("*")
            UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", this@conf) }
        }
    }

    @Component
    class CustomJwtAuthenticationConverter : Converter<Jwt, Mono<out AbstractAuthenticationToken>> {
        override fun convert(jwt: Jwt): Mono<out AbstractAuthenticationToken> {
            val authorities = JwtGrantedAuthoritiesConverter().convert(jwt)
            val username = JsonPath.read<String>(jwt.claims, "preferred_username")
            return Mono.just(JwtAuthenticationToken(jwt, authorities, username))
        }
    }

    internal class JwtGrantedAuthoritiesConverter : Converter<Jwt, Collection<GrantedAuthority>> {
        override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
            val claimRealm: Any? =
                JsonPath.read(jwt.claims, "$.realm_access.roles") // PathNotFoundException should be caught

            val roles: List<String> = when (claimRealm) {
                is ArrayList<*> -> {
                    if (claimRealm.firstOrNull() is String) {
                        claimRealm.toList() as List<String>
                    } else {
                        emptyList()
                    }
                }

                is String -> claimRealm.split(",")
                is Array<*> -> {
                    if (claimRealm.isArrayOf<String>()) {
                        claimRealm.toList() as List<String>
                    } else {
                        emptyList()
                    }
                }

                else -> emptyList() // změnit na příslušnou Exception
            }
            println(roles)
            return roles
                .map { SimpleGrantedAuthority(it) }
        }
    }
/** TODO: Pro podporu multi-tenancy je třeba udělat Authentication Manager co bude resolvovat tokeny různých "tenantů".
*    Je nutné odkomentovat dolní 2 metody a přidat:
*    - authenticationManagerResolver: ReactiveAuthenticationManagerResolver<ServerWebExchange> do parametrů
*    securityFilter
*    - this.authenticationManagerResolver = authenticationManagerResolver do oauth2ResourceServer
*    @Bean
*    fun authenticationManagerResolver(
*        authenticationConverter: CustomJwtAuthenticationConverter
*    ): ReactiveAuthenticationManagerResolver<ServerWebExchange> {
*        val url = "http://localhost:8080/realms/MyRealm"
*        val jwtManagers: Map<String, Mono<ReactiveAuthenticationManager>> =
*            mapOf(Pair(url, Mono.just(authenticationManager(url, authenticationConverter))))
*        return JwtIssuerReactiveAuthenticationManagerResolver { issuerLocation: String ->
*            jwtManagers[issuerLocation] ?: Mono.empty()
*        }
*    }
*    fun authenticationManager(
*        issuer: String,
*        authenticationConverter: CustomJwtAuthenticationConverter
*    ): JwtReactiveAuthenticationManager {
*        val decoder = ReactiveJwtDecoders.fromIssuerLocation(issuer)
*        val provider = JwtReactiveAuthenticationManager(decoder)
*        provider.setJwtAuthenticationConverter(authenticationConverter)
*        return provider
 *        }
**/
}