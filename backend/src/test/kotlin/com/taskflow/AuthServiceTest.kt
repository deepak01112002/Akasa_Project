package com.taskflow

import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.password.PasswordEncoder

class AuthServiceTest {
    private val users = mockk<UserRepository>()
    private val encoder = mockk<PasswordEncoder>()
    private val jwt = mockk<JwtService>()
    private val service = AuthService(users, encoder, jwt)

    @Test fun `registration hashes password and normalizes email`() {
        every { users.existsByEmail("a@example.com") } returns false
        every { encoder.encode("password123") } returns "hashed"
        every { users.saveAndFlush(any()) } answers { firstArg<User>().apply { id = 1 } }
        every { jwt.create(any()) } returns "token"
        val result = service.register(RegisterRequest("Alice", "A@example.com", "password123"))
        assertEquals("a@example.com", result.user.email)
        verify { users.saveAndFlush(match { it.password == "hashed" }) }
    }
    @Test fun `invalid credentials are rejected`() {
        every { users.findByEmail("a@example.com") } returns null
        assertThrows(ApiException::class.java) { service.login(LoginRequest("a@example.com", "wrong")) }
    }
}
