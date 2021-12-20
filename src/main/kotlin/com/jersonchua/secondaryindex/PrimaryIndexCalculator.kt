package com.jersonchua.secondaryindex

/**
 * @param T the type of the primary key
 * @property lookupPrimaryIndices function type that accepts field name and value, and it should return
 *      [Success][com.jersonchua.secondaryindex.Result.Success] with primary indices if field and value are in the index
 *      [Success][com.jersonchua.secondaryindex.Result.Success] with empty primary indices if field is in the index but the value is not
 *      [Failed][com.jersonchua.secondaryindex.Result.Failed] if the field is not in the index, or if an error occurs
 * @property strict if true, all fields in the conditions must be in the index, and errors are not tolerated. TODO add more details
 */
class PrimaryIndexCalculator<out T>(
    private val lookupPrimaryIndices: (String, Any?) -> Result<T>,
    private val strict: Boolean = false
) {
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
        val (successes, failed, _) = results.partitionByTypes<Result.Success<T>, Result.Failed<T>, Result<T>>()

        return if (failed.isNotEmpty()) {
            Result.Failed(*failed.flatMap { it.reasons.toList() }.toTypedArray())
        } else {
            val primaryIndices =
                successes.map { it.primaryIndices }.reduce { cumulativePrimaryIndices, primaryIndices ->
                    cumulativePrimaryIndices.union(primaryIndices)
                }
            Result.Success(primaryIndices)
        }
    }

    private fun computePrimaryIndices(and: And): Result<T> {
        val results = and.conditions.map { computePrimaryIndices(it) }
        val (successes, failed, _) = results.partitionByTypes<Result.Success<T>, Result.Failed<T>, Result<T>>()

        return if ((failed.size == results.size) || (strict && failed.isNotEmpty())) {
            Result.Failed(*failed.flatMap { it.reasons.toList() }.toTypedArray())
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
    class Failed<out T>(vararg val reasons: String) : Result<T> {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Failed<*>

            if (!reasons.contentEquals(other.reasons)) return false

            return true
        }

        override fun hashCode(): Int {
            return reasons.contentHashCode()
        }

        override fun toString(): String {
            return "Failed(reasons=${reasons.contentToString()})"
        }
    }
}

