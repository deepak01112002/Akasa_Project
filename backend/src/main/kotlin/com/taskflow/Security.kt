package com.taskflow

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant
import java.util.Date

@Component
class JwtService(@Value("\${JWT_SECRET}") secret: String, @Value("\${JWT_EXPIRY_SECONDS:3600}") private val expiry: Long) {
    private val key = Keys.hmacShaKeyFor(secret.toByteArray(Charsets.UTF_8))
    fun create(user: User): String = Jwts.builder().subject(user.id.toString()).issuedAt(Date()).expiration(Date.from(Instant.now().plusSeconds(expiry))).signWith(key).compact()
    fun userId(token: String): Long = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject.toLong()
}

@Component
class JwtFilter(private val jwt: JwtService) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            try {
                val id = jwt.userId(header.removePrefix("Bearer "))
                SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(id, null, emptyList())
            } catch (_: Exception) { SecurityContextHolder.clearContext() }
        }
        chain.doFilter(request, response)
    }
}

@Configuration
class SecurityConfig(private val jwtFilter: JwtFilter, private val mapper: ObjectMapper) {
    @Bean fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
    @Bean fun security(http: HttpSecurity): SecurityFilterChain {
        http.csrf { it.disable() }.cors { }.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { it.requestMatchers("/api/auth/register", "/api/auth/login", "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll().anyRequest().authenticated() }
            .exceptionHandling { it.authenticationEntryPoint { req, res, _ ->
                res.status = 401; res.contentType = "application/json"
                mapper.writeValue(res.outputStream, ErrorResponse(Instant.now(), 401, "UNAUTHORIZED", "Authentication required", req.requestURI))
            } }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
        return http.build()
    }
    @Bean fun corsConfigurationSource(): org.springframework.web.cors.CorsConfigurationSource {
        val config = org.springframework.web.cors.CorsConfiguration()
        config.allowedOrigins = listOf("http://localhost:5173", "http://localhost:3000")
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("Authorization", "Content-Type")
        return org.springframework.web.cors.UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/**", config) }
    }
}
