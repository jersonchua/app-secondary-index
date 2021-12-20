package com.jersonchua.secondaryindex

// TODO can we get rid of "Any?" to improve type safety?

sealed interface Condition

object UnsupportedCondition : Condition

data class EqualsCondition(val fieldName: String, val value: Any?) : Condition

data class InCondition(val fieldName: String, val values: List<Any?>) : Condition {
    init {
        require(values.isNotEmpty())
    }

    fun toOrCondition() = OrCondition(values.map { EqualsCondition(fieldName, it) })
}

data class AndCondition(val conditions: List<Condition>) : Condition {
    init {
        require(conditions.isNotEmpty())
    }
}

data class OrCondition(val conditions: List<Condition>) : Condition {
    init {
        require(conditions.isNotEmpty())
    }
}

