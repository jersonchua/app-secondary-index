package com.jersonchua.secondaryindex

// TODO can we get rid of "Any?" to improve type safety?

sealed interface Condition

object UnsupportedCondition : Condition

data class Equals(val fieldName: String, val value: Any?) : Condition

data class In(val fieldName: String, val values: List<Any?>) : Condition {
    init {
        require(values.isNotEmpty())
    }

    fun toOr() = Or(values.map { Equals(fieldName, it) })
}

data class And(val conditions: List<Condition>) : Condition {
    init {
        require(conditions.isNotEmpty())
    }
}

data class Or(val conditions: List<Condition>) : Condition {
    init {
        require(conditions.isNotEmpty())
    }
}

