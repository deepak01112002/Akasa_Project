package com.taskflow

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.put
import com.fasterxml.jackson.databind.ObjectMapper

@SpringBootTest(properties = ["spring.datasource.url=jdbc:h2:mem:taskflow;DB_CLOSE_DELAY=-1", "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=", "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false", "JWT_SECRET=01234567890123456789012345678901"])
@AutoConfigureMockMvc
class AuthIntegrationTest(@Autowired val mvc: MockMvc, @Autowired val mapper: ObjectMapper) {
    @Test fun `registration returns a JWT and protected endpoints require one`() {
        mvc.get("/api/projects").andExpect { status { isUnauthorized() } }
        mvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"Alice","email":"alice@example.com","password":"password123"}"""
        }.andExpect { status { isCreated() }; jsonPath("$.token") { isNotEmpty() } }
    }
    @Test fun `validation returns field errors`() {
        mvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"","email":"bad","password":"short"}"""
        }.andExpect { status { isBadRequest() }; jsonPath("$.fieldErrors.name") { exists() } }
    }
    @Test fun `docker frontend origin can sign in`() {
        mvc.post("/api/auth/login") {
            header("Origin", "http://localhost:3000")
            contentType = MediaType.APPLICATION_JSON
            content = """{"email":"missing@example.com","password":"password123"}"""
        }.andExpect { status { isUnauthorized() }; header { string("Access-Control-Allow-Origin", "http://localhost:3000") } }
    }
    @Test fun `project owner can manage tasks while other users are forbidden`() {
        fun register(email: String): String {
            val result = mvc.post("/api/auth/register") { contentType = MediaType.APPLICATION_JSON; content = """{"name":"User","email":"$email","password":"password123"}""" }.andExpect { status { isCreated() } }.andReturn()
            return mapper.readTree(result.response.contentAsString)["token"].asText()
        }
        val owner = register("owner@example.com"); val other = register("other@example.com")
        val created = mvc.post("/api/projects") { header("Authorization", "Bearer $owner"); contentType = MediaType.APPLICATION_JSON; content = """{"name":"Launch"}""" }.andExpect { status { isCreated() } }.andReturn()
        val projectId = mapper.readTree(created.response.contentAsString)["id"].asLong()
        mvc.get("/api/projects/$projectId/tasks") { header("Authorization", "Bearer $other") }.andExpect { status { isForbidden() } }
        mvc.post("/api/projects/$projectId/tasks") { header("Authorization", "Bearer $owner"); contentType = MediaType.APPLICATION_JSON; content = """{"title":"Past","dueDate":"2000-01-01"}""" }.andExpect { status { isBadRequest() }; jsonPath("$.fieldErrors.dueDate") { exists() } }
        mvc.get("/api/projects/$projectId/tasks?priority=INVALID") { header("Authorization", "Bearer $owner") }.andExpect { status { isBadRequest() }; jsonPath("$.code") { value("INVALID_PARAMETER") } }
        val task = mvc.post("/api/projects/$projectId/tasks") { header("Authorization", "Bearer $owner"); contentType = MediaType.APPLICATION_JSON; content = """{"title":"Ship","priority":"HIGH"}""" }.andExpect { status { isCreated() } }.andReturn()
        val taskId = mapper.readTree(task.response.contentAsString)["id"].asLong()
        mvc.post("/api/projects/$projectId/tasks") { header("Authorization", "Bearer $owner"); contentType = MediaType.APPLICATION_JSON; content = """{"title":"Low task","priority":"LOW"}""" }.andExpect { status { isCreated() } }
        mvc.get("/api/projects/$projectId/tasks?sort=priority&size=1") { header("Authorization", "Bearer $owner") }.andExpect { status { isOk() }; jsonPath("$.content[0].priority") { value("HIGH") }; jsonPath("$.totalElements") { value(2) } }
        mvc.get("/api/projects/$projectId/tasks?priority=LOW") { header("Authorization", "Bearer $owner") }.andExpect { status { isOk() }; jsonPath("$.totalElements") { value(1) } }
        mvc.put("/api/tasks/$taskId") { header("Authorization", "Bearer $owner"); contentType = MediaType.APPLICATION_JSON; content = """{"title":"Ship updated","priority":"HIGH"}""" }.andExpect { status { isOk() }; jsonPath("$.title") { value("Ship updated") } }
        mvc.patch("/api/tasks/$taskId/status") { header("Authorization", "Bearer $owner"); contentType = MediaType.APPLICATION_JSON; content = """{"status":"DONE"}""" }.andExpect { status { isOk() }; jsonPath("$.status") { value("DONE") } }
        mvc.get("/api/projects/$projectId/summary") { header("Authorization", "Bearer $owner") }.andExpect { status { isOk() }; jsonPath("$.DONE") { value(1) } }
        mvc.get("/api/tasks/$taskId/activities") { header("Authorization", "Bearer $owner") }.andExpect { status { isOk() }; jsonPath("$.length()") { value(2) } }
        mvc.delete("/api/tasks/$taskId") { header("Authorization", "Bearer $owner") }.andExpect { status { isNoContent() } }
    }
}
