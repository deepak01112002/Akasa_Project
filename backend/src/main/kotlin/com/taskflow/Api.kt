package com.taskflow

import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.time.Instant

data class RegisterRequest(@field:NotBlank @field:Size(max = 120) val name: String, @field:NotBlank @field:Email @field:Size(max = 255) val email: String, @field:Size(min = 8, max = 72) val password: String)
data class LoginRequest(@field:NotBlank @field:Email val email: String, @field:NotBlank val password: String)
data class AuthResponse(val token: String, val user: UserDto)
data class UserDto(val id: Long, val name: String, val email: String, val createdAt: Instant)
data class ProjectRequest(@field:NotBlank @field:Size(max = 120) val name: String, @field:Size(max = 5000) val description: String? = null)
data class ProjectDto(val id: Long, val name: String, val description: String?, val owner: UserDto, val createdAt: Instant)
object DtoMapper {
    fun user(user: User) = UserDto(user.id!!, user.name, user.email, user.createdAt)
    fun project(project: Project) = ProjectDto(project.id!!, project.name, project.description, user(project.owner), project.createdAt)
}

@RestController @RequestMapping("/api/auth")
class AuthController(private val service: AuthService) {
    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: RegisterRequest) = service.register(request)
    @PostMapping("/login") fun login(@Valid @RequestBody request: LoginRequest) = service.login(request)
}

@RestController @RequestMapping("/api/projects")
class ProjectController(private val service: ProjectService) {
    @GetMapping fun list(@AuthenticationPrincipal userId: Long) = service.list(userId)
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal userId: Long, @Valid @RequestBody request: ProjectRequest) = service.create(userId, request)
}
