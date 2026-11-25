package com.taskflow

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface UserRepository : JpaRepository<User, Long> {
    fun findByEmail(email: String): User?
    fun existsByEmail(email: String): Boolean
}

interface ProjectRepository : JpaRepository<Project, Long> {
    fun findAllByOwnerIdOrderByCreatedAtDesc(ownerId: Long): List<Project>
}

interface TaskRepository : JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    fun findAllByProjectId(projectId: Long): List<Task>
}
interface TaskActivityRepository : JpaRepository<TaskActivity, Long> {
    fun findAllByTaskIdOrderByCreatedAtDesc(taskId: Long): List<TaskActivity>
    fun deleteAllByTaskId(taskId: Long)
}
