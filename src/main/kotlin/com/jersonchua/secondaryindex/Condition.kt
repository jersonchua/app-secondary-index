package com.jersonchua.secondaryindex

// TODO can we get rid of "Any?" to improve type safety?

/**
 * Represents the filter conditions used in retrieving the data
 *
 * If the application querying for users with name = 'John' and age >= 18, then the filter condition will be represented
 * as follows:
 *
 *  And(
 *      Equals("name", "John"),
 *      UnsupportedCondition
 *  )
 */
sealed interface Condition

object UnsupportedCondition : Condition

data class Equals(val fieldName: String, val value: Any?) : Condition

data class In(private val fieldName: String, private val values: List<Any?>) : Condition {
    init {
        require(values.isNotEmpty())
    }

    fun toOr() = Or(*values.map { Equals(fieldName, it) }.toTypedArray())
}

class And(vararg val conditions: Condition) : Condition {
    init {
        require(conditions.isNotEmpty())
    }
}

class Or(vararg val conditions: Condition) : Condition {
    init {
        require(conditions.isNotEmpty())
    }
}

