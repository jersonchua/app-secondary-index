package com.jersonchua.secondaryindex

/**
 * @param T the type of the primary key
 * @property lookupPrimaryIndices function type that accepts field name and value, and it should return
 *      Result.Success with primary indices if field is indexed and value is in the index
 *      Result.Success with empty primary indices if field is indexed and values is not in the index
 *      Result.Failed if the field is not indexed, or an error occurs
 */
class PrimaryIndexCalculator<out T>(private val lookupPrimaryIndices: (String, Any?) -> Result<T>) {
    fun computePrimaryIndices(condition: Condition): Result<T> {
        return when (condition) {
            is Equals -> lookupPrimaryIndices(condition.fieldName, condition.value)
            is In -> computePrimaryIndices(condition.toOr())
            is Or -> computePrimaryIndices(condition)
            is And -> computePrimaryIndices(condition)
            is UnsupportedCondition -> Result.Failed()
        }
    }

    private fun computePrimaryIndices(or: Or): Result<T> {
        val results = or.conditions.map { computePrimaryIndices(it) }
        return if (results.filterIsInstance<Result.Failed<T>>().isNotEmpty()) {
            Result.Failed()
        } else {
            val successes = results.filterIsInstance<Result.Success<T>>()
            val primaryIndices =
                successes.map { it.primaryIndices }.reduce { cumulativePrimaryIndices, primaryIndices ->
                    cumulativePrimaryIndices.union(primaryIndices)
                }
            Result.Success(primaryIndices)
        }
    }

    private fun computePrimaryIndices(and: And): Result<T> {
        val successes = and.conditions.map { computePrimaryIndices(it) }.filterIsInstance<Result.Success<T>>()
        return if (successes.isEmpty()) {
            Result.Failed()
        } else {
            val primaryIndices =
                successes.map { it.primaryIndices }.reduce { cumulativePrimaryIndices, primaryIndices ->
                    cumulativePrimaryIndices.intersect(primaryIndices)
                }
            Result.Success(primaryIndices)
        }
    }
}

sealed interface Result<out T> {
    data class Success<out T>(val primaryIndices: Set<T>) : Result<T>

    /**
     * TODO is there a better way of handling this?
     * If we make it a data class, it will require attributes.
     * If we make it a singleton object, we can't pass the generic type
     */
    class Failed<out T> : Result<T> {
        override fun equals(other: Any?): Boolean {
            return other is Failed<*>
        }
    }
}

