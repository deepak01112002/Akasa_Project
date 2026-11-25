package com.taskflow

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.Instant
import java.time.LocalDate

data class TaskRequest(@field:NotBlank @field:Size(max = 120) val title: String, @field:Size(max = 5000) val description: String? = null, val priority: Priority = Priority.MEDIUM, val dueDate: LocalDate? = null, val assigneeId: Long? = null, val status: TaskStatus = TaskStatus.TODO)
data class StatusRequest(val status: TaskStatus)
data class TaskDto(val id: Long, val title: String, val description: String?, val status: TaskStatus, val priority: Priority, val dueDate: LocalDate?, val assignee: UserDto?, val projectId: Long, val createdAt: Instant, val updatedAt: Instant)
data class ActivityDto(val id: Long, val actor: UserDto, val previousStatus: TaskStatus?, val newStatus: TaskStatus, val createdAt: Instant)
data class TaskPage(val content: List<TaskDto>, val page: Int, val size: Int, val totalElements: Long, val totalPages: Int)
data class SummaryDto(@get:com.fasterxml.jackson.annotation.JsonProperty("TODO") val todo: Long, @get:com.fasterxml.jackson.annotation.JsonProperty("IN_PROGRESS") val inProgress: Long, @get:com.fasterxml.jackson.annotation.JsonProperty("DONE") val done: Long, val overdue: Long)

@Service
class TaskService(private val projects: ProjectRepository, private val tasks: TaskRepository, private val activities: TaskActivityRepository) {
    private fun project(id: Long, userId: Long): Project {
        val p = projects.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Project not found") }
        if (p.owner.id != userId) throw ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied")
        return p
    }
    private fun task(id: Long, userId: Long): Task {
        val t = tasks.findById(id).orElseThrow { ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Task not found") }
        if (t.project.owner.id != userId) throw ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access denied")
        return t
    }
    private fun dto(t: Task) = TaskDto(t.id!!, t.title, t.description, t.status, t.priority, t.dueDate, t.assignee?.let(DtoMapper::user), t.project.id!!, t.createdAt, t.updatedAt)
    private fun validate(r: TaskRequest, owner: User, oldDate: LocalDate? = null) {
        if (r.dueDate != null && r.dueDate != oldDate && r.dueDate.isBefore(LocalDate.now())) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", mapOf("dueDate" to "Due date cannot be in the past"))
        if (r.assigneeId != null && r.assigneeId != owner.id) throw ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Validation failed", mapOf("assigneeId" to "Assignee must be the project owner"))
    }
    private fun record(t: Task, old: TaskStatus?, status: TaskStatus) { activities.save(TaskActivity(task = t, actor = t.project.owner, previousStatus = old, newStatus = status)) }
    @Transactional(readOnly = true) fun list(id: Long, userId: Long, status: TaskStatus?, priority: Priority?, assignee: Long?, sort: String, page: Int, size: Int): TaskPage {
        project(id, userId)
        val strategy = TaskSortStrategies.forName(sort) ?: throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION", "Invalid page, size or sort")
        if (page < 0 || size !in 1..100) throw ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGINATION", "Invalid page, size or sort")
        val spec = Specification<Task> { root, query, cb ->
            val conditions = mutableListOf(cb.equal(root.get<Project>("project").get<Long>("id"), id))
            if (status != null) conditions.add(cb.equal(root.get<TaskStatus>("status"), status))
            if (priority != null) conditions.add(cb.equal(root.get<Priority>("priority"), priority))
            if (assignee != null) conditions.add(cb.equal(root.get<User>("assignee").get<Long>("id"), assignee))
            if (query.resultType != java.lang.Long::class.java && query.resultType != Long::class.javaPrimitiveType) {
                query.orderBy(strategy.order(root, cb), cb.asc(root.get<Long>("id")))
            }
            cb.and(*conditions.toTypedArray())
        }
        val result = tasks.findAll(spec, PageRequest.of(page, size))
        return TaskPage(result.content.map(::dto), result.number, result.size, result.totalElements, result.totalPages)
    }
    @Transactional fun create(id: Long, userId: Long, r: TaskRequest): TaskDto {
        val p = project(id, userId); validate(r, p.owner)
        val t = tasks.save(Task(title = r.title.trim(), description = r.description, status = r.status, priority = r.priority, dueDate = r.dueDate, assignee = if (r.assigneeId != null) p.owner else null, project = p))
        record(t, null, t.status); return dto(t)
    }
    @Transactional fun update(id: Long, userId: Long, r: TaskRequest): TaskDto {
        val t = task(id, userId); validate(r, t.project.owner, t.dueDate)
        val old = t.status
        t.title = r.title.trim(); t.description = r.description; t.priority = r.priority; t.dueDate = r.dueDate; t.assignee = if (r.assigneeId != null) t.project.owner else null; t.status = r.status; t.updatedAt = Instant.now()
        if (old != t.status) record(t, old, t.status)
        return dto(t)
    }
    @Transactional fun status(id: Long, userId: Long, value: TaskStatus): TaskDto {
        val t = task(id, userId); val old = t.status
        if (old != value) { t.status = value; t.updatedAt = Instant.now(); record(t, old, value) }
        return dto(t)
    }
    @Transactional fun delete(id: Long, userId: Long) { val existing = task(id, userId); activities.deleteAllByTaskId(id); tasks.delete(existing) }
    @Transactional(readOnly = true) fun summary(id: Long, userId: Long): SummaryDto {
        project(id, userId); val all = tasks.findAllByProjectId(id)
        return SummaryDto(all.count { it.status == TaskStatus.TODO }.toLong(), all.count { it.status == TaskStatus.IN_PROGRESS }.toLong(), all.count { it.status == TaskStatus.DONE }.toLong(), all.count { it.status != TaskStatus.DONE && it.dueDate?.isBefore(LocalDate.now()) == true }.toLong())
    }
    @Transactional(readOnly = true) fun activity(id: Long, userId: Long): List<ActivityDto> {
        task(id, userId); return activities.findAllByTaskIdOrderByCreatedAtDesc(id).map { ActivityDto(it.id!!, DtoMapper.user(it.actor), it.previousStatus, it.newStatus, it.createdAt) }
    }
}

@RestController
class TaskController(private val service: TaskService) {
    @GetMapping("/api/projects/{id}/tasks") fun list(@PathVariable id: Long, @AuthenticationPrincipal user: Long, @RequestParam(required = false) status: TaskStatus?, @RequestParam(required = false) priority: Priority?, @RequestParam(required = false) assignee: Long?, @RequestParam(defaultValue = "dueDate") sort: String, @RequestParam(defaultValue = "0") page: Int, @RequestParam(defaultValue = "20") size: Int) = service.list(id, user, status, priority, assignee, sort, page, size)
    @PostMapping("/api/projects/{id}/tasks") @ResponseStatus(HttpStatus.CREATED) fun create(@PathVariable id: Long, @AuthenticationPrincipal user: Long, @Valid @RequestBody body: TaskRequest) = service.create(id, user, body)
    @PutMapping("/api/tasks/{id}") fun update(@PathVariable id: Long, @AuthenticationPrincipal user: Long, @Valid @RequestBody body: TaskRequest) = service.update(id, user, body)
    @PatchMapping("/api/tasks/{id}/status") fun status(@PathVariable id: Long, @AuthenticationPrincipal user: Long, @Valid @RequestBody body: StatusRequest) = service.status(id, user, body.status)
    @DeleteMapping("/api/tasks/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) fun delete(@PathVariable id: Long, @AuthenticationPrincipal user: Long) = service.delete(id, user)
    @GetMapping("/api/projects/{id}/summary") fun summary(@PathVariable id: Long, @AuthenticationPrincipal user: Long) = service.summary(id, user)
    @GetMapping("/api/tasks/{id}/activities") fun activity(@PathVariable id: Long, @AuthenticationPrincipal user: Long) = service.activity(id, user)
}
