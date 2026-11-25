package com.taskflow

import org.springframework.http.HttpStatus
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

class ApiException(val status: HttpStatus, val code: String, override val message: String, val fieldErrors: Map<String, String>? = null) : RuntimeException(message)

@Service
class AuthService(private val users: UserRepository, private val encoder: PasswordEncoder, private val jwt: JwtService) {
    @Transactional fun register(request: RegisterRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        if (users.existsByEmail(email)) throw ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email is already registered")
        val user = try { users.saveAndFlush(User(name = request.name.trim(), email = email, password = encoder.encode(request.password))) } catch (_: org.springframework.dao.DataIntegrityViolationException) { throw ApiException(HttpStatus.CONFLICT, "EMAIL_EXISTS", "Email is already registered") }
        return AuthResponse(jwt.create(user), DtoMapper.user(user))
    }
    @Transactional(readOnly = true) fun login(request: LoginRequest): AuthResponse {
        val user = users.findByEmail(request.email.trim().lowercase())
        if (user == null || !encoder.matches(request.password, user.password)) throw ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password")
        return AuthResponse(jwt.create(user), DtoMapper.user(user))
    }
}

@Service
class ProjectService(private val projects: ProjectRepository, private val users: UserRepository) {
    @Transactional(readOnly = true) fun list(userId: Long): List<ProjectDto> = projects.findAllByOwnerIdOrderByCreatedAtDesc(userId).map(DtoMapper::project)
    @Transactional fun create(userId: Long, request: ProjectRequest): ProjectDto {
        val owner = users.findById(userId).orElseThrow { ApiException(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "User no longer exists") }
        return DtoMapper.project(projects.save(Project(name = request.name.trim(), description = request.description, owner = owner)))
    }
}
