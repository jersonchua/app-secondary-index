package com.jersonchua.secondaryindex

/**
 * T is the primary key type
 *
 * lookupPrimaryIndices:
 *      First parameter (String) is the field name and second parameter (Any?) is the field value.
 *      lookupPrimaryIndices should return
 *          Result.Success if the field is indexed. If the value is not in the index then primaryIndices should be an empty list.
 *          Result.Failed if the field is not indexed or if an error has occurred
 */
class PrimaryIndexCalculator<out T>(private val lookupPrimaryIndices: (String, Any?) -> Result<T>) {
    fun computePrimaryIndices(condition: Condition): Result<T> {
        return when (condition) {
            is EqualsCondition -> lookupPrimaryIndices(condition.fieldName, condition.value)
            is InCondition -> computePrimaryIndices(condition.toOrCondition())
            is OrCondition -> computePrimaryIndices(condition)
            is AndCondition -> computePrimaryIndices(condition)
            is UnsupportedCondition -> Result.Failed()
        }
    }

    private fun computePrimaryIndices(orCondition: OrCondition): Result<T> {
        val results = orCondition.conditions.map { computePrimaryIndices(it) }
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

    private fun computePrimaryIndices(andCondition: AndCondition): Result<T> {
        val successes = andCondition.conditions.map { computePrimaryIndices(it) }.filterIsInstance<Result.Success<T>>()
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

