package com.jersonchua.secondaryindex

/**
 * Represents the conditions in the where-clause in SQL
 *
 * If the SQL has (name = 'John' and age >= 18) in the where-clause, then it will be represented as follows:
 *  And(
 *      Equals("name", "John"),
 *      UnsupportedCondition    // assumption is that the secondary index only supports '=', 'in' is just a variation of '='
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

