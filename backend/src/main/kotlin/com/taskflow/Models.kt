package com.taskflow

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity @Table(name = "users")
class User(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) var name: String = "",
    @Column(nullable = false, unique = true) var email: String = "",
    @Column(nullable = false) var password: String = "",
    @Column(nullable = false, updatable = false) var createdAt: Instant = Instant.now()
)

@Entity @Table(name = "projects")
class Project(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false) var name: String = "",
    @Column(columnDefinition = "text") var description: String? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "owner_id") var owner: User = User(),
    @Column(nullable = false, updatable = false) var createdAt: Instant = Instant.now()
)

enum class TaskStatus { TODO, IN_PROGRESS, DONE }
enum class Priority { LOW, MEDIUM, HIGH }

@Entity @Table(name = "tasks")
class Task(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @Column(nullable = false, length = 120) var title: String = "",
    @Column(columnDefinition = "text") var description: String? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var status: TaskStatus = TaskStatus.TODO,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var priority: Priority = Priority.MEDIUM,
    var dueDate: LocalDate? = null,
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assignee_id") var assignee: User? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "project_id") var project: Project = Project(),
    @Column(nullable = false, updatable = false) var createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now()
)

@Entity @Table(name = "task_activities")
class TaskActivity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "task_id") var task: Task = Task(),
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "actor_id") var actor: User = User(),
    @Enumerated(EnumType.STRING) @Column(length = 20) var previousStatus: TaskStatus? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) var newStatus: TaskStatus = TaskStatus.TODO,
    @Column(nullable = false, updatable = false) var createdAt: Instant = Instant.now()
)
