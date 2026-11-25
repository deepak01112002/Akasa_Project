package com.taskflow

import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.util.Optional

class TaskServiceTest {
    private val projects = mockk<ProjectRepository>()
    private val tasks = mockk<TaskRepository>()
    private val activities = mockk<TaskActivityRepository>()
    private val service = TaskService(projects, tasks, activities)
    private val owner = User(id = 1, name = "Owner", email = "owner@example.com")
    private val project = Project(id = 2, name = "Project", owner = owner)

    @Test fun `rejects access by another user`() {
        every { projects.findById(2) } returns Optional.of(project)
        val error = assertThrows(ApiException::class.java) { service.summary(2, 99) }
        assertEquals(HttpStatus.FORBIDDEN, error.status)
        verify(exactly = 0) { tasks.findAllByProjectId(any()) }
    }
    @Test fun `status change records actor and old and new values`() {
        val task = Task(id = 3, title = "Work", project = project)
        every { tasks.findById(3) } returns Optional.of(task)
        every { activities.save(any()) } answers { firstArg() }
        val result = service.status(3, 1, TaskStatus.DONE)
        assertEquals(TaskStatus.DONE, result.status)
        verify { activities.save(match { it.actor.id == 1L && it.previousStatus == TaskStatus.TODO && it.newStatus == TaskStatus.DONE }) }
    }
    @Test fun `rejects past due date at creation`() {
        every { projects.findById(2) } returns Optional.of(project)
        val error = assertThrows(ApiException::class.java) { service.create(2, 1, TaskRequest(title = "Work", dueDate = LocalDate.now().minusDays(1))) }
        assertEquals(HttpStatus.BAD_REQUEST, error.status)
        verify(exactly = 0) { tasks.save(any()) }
    }
}
