package com.taskflow

import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.Order
import jakarta.persistence.criteria.Root
import java.time.LocalDate

/** Sorting strategies keep database ordering separate from filtering and pagination. */
interface TaskSortStrategy {
    fun order(root: Root<Task>, builder: CriteriaBuilder): Order
}

object DueDateSort : TaskSortStrategy {
    override fun order(root: Root<Task>, builder: CriteriaBuilder) = builder.asc(root.get<LocalDate>("dueDate"))
}

object PrioritySort : TaskSortStrategy {
    override fun order(root: Root<Task>, builder: CriteriaBuilder): Order = builder.asc(
        builder.selectCase<Int>()
            .`when`(builder.equal(root.get<Priority>("priority"), Priority.HIGH), 0)
            .`when`(builder.equal(root.get<Priority>("priority"), Priority.MEDIUM), 1)
            .otherwise(2)
    )
}

object TaskSortStrategies {
    fun forName(name: String): TaskSortStrategy? = when (name) {
        "dueDate" -> DueDateSort
        "priority" -> PrioritySort
        else -> null
    }
}
