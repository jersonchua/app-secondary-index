package com.jersonchua.secondaryindex

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class CollectionsTest {
    @Test
    fun testPartitionByTypes() {
        val numbers = listOf<Number>(1, 2, 3.0, 4.0, 5L)
        val (ints, doubles, rest) =  numbers.partitionByTypes<Int, Double, Number>()
        assertEquals(listOf(1, 2), ints)
        assertEquals(listOf(3.0, 4.0), doubles)
        assertEquals(listOf(5L), rest)
    }
}
