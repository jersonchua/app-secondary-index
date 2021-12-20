package com.jersonchua.secondaryindex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * The test creates an in-memory secondary index (see the companion object for details) and uses it for testing
 * different use cases.
 */
internal class PrimaryIndexCalculatorTest {
    @Test
    fun testEquals() {
        val condition = Equals("color", "blue")
        assertEquals(Result.Success(setOf(5, 6)), primaryKeyCalculator.computePrimaryIndices(condition))
    }

    @Test
    fun testEqualsWhereFieldNotIndexed() {
        val condition = Equals("location", "Rockaway")
        assertEquals(Result.Failed<Int>("location is not indexed"), primaryKeyCalculator.computePrimaryIndices(condition))
    }

    @Test
    fun testIn() {
        val condition = In("color", listOf("blue", "red"))
        assertEquals(Result.Success(setOf(1, 3, 5, 6)), primaryKeyCalculator.computePrimaryIndices(condition))
    }

    @Test
    fun testAnd() {
        val condition = And(
            Equals("color", "blue"),
            Equals("make", "Toyota")
        )
        assertEquals(Result.Success(setOf(5)), primaryKeyCalculator.computePrimaryIndices(condition))
        assertEquals(Result.Success(setOf(5)), strictPrimaryKeyCalculator.computePrimaryIndices(condition))
    }

    /**
     * Test "And" condition where at least one of the nested conditions derived to Result.Failed
     */
    @Test
    fun testAndWhereAtLeast1Failed() {
        val condition = And(
            Equals("color", "blue"),
            Equals("location", "Edgewater")
        )
        // in none-strict mode, the calculator can yield false positive i.e. Car(6, "blue", "Honda", "Jersey City")
        assertEquals(Result.Success(setOf(5, 6)), primaryKeyCalculator.computePrimaryIndices(condition))
        assertEquals(Result.Failed<Int>("location is not indexed"), strictPrimaryKeyCalculator.computePrimaryIndices(condition))
    }

    /**
     * Test "And" condition where all nested conditions derived to Result.Failed
     */
    @Test
    fun testAndWhereAllFailed() {
        val condition = And(
            Equals("location", "Rockaway")
        )
        assertEquals(Result.Failed<Int>("location is not indexed"), primaryKeyCalculator.computePrimaryIndices(condition))
        assertEquals(Result.Failed<Int>("location is not indexed"), strictPrimaryKeyCalculator.computePrimaryIndices(condition))
    }

    /**
     * Test "And" condition where the primary indices derived from the nested conditions do not overlap
     */
    @Test
    fun testAndPrimaryKeyNotFound() {
        val condition = And(
            Equals("color", "red"),
            Equals("make", "Toyota"),
        )
        assertEquals(Result.Success(setOf<Int>()), primaryKeyCalculator.computePrimaryIndices(condition))
        assertEquals(Result.Success(setOf<Int>()), strictPrimaryKeyCalculator.computePrimaryIndices(condition))
    }

    @Test
    fun testOr() {
        val condition = Or(
            Equals("color", "silver"),
            Equals("color", "black")
        )
        assertEquals(Result.Success(setOf(2, 4)), primaryKeyCalculator.computePrimaryIndices(condition))
    }

    /**
     * Test "Or" condition where at least one of the nested conditions derived to Result.Failed
     */
    @Test
    fun testOrWhereAtLeast1Failed() {
        val condition = Or(
            Equals("color", "blue"),
            UnsupportedCondition
        )
        assertEquals(Result.Failed<Int>(), primaryKeyCalculator.computePrimaryIndices(condition))
    }

    /**
     * Test "Or" condition where the primary indices derived from the nested conditions are all empty
     */
    @Test
    fun testOrPrimaryKeyNotFound() {
        val condition = Or(
            Equals("color", "green"),
            Equals("make", "Acura"),
        )
        assertEquals(Result.Success(setOf<Int>()), primaryKeyCalculator.computePrimaryIndices(condition))
    }

    companion object {
        private val cars = listOf(
            Car(1, "red", "Honda", "Jersey City"),
            Car(2, "silver", "Ford", "Rockaway"),
            Car(3, "red", "Audi", "Rockaway"),
            Car(4, "black", "Ford", "Fort Lee"),
            Car(5, "blue", "Toyota", "Edgewater"),
            Car(6, "blue", "Honda", "Jersey City"),
        )

        private val indexedFields = listOf("color", "make")
        private val invertedMap =
            indexedFields.flatMap { createMapping(it, cars) }.groupBy({ it.first }, { it.second })
                .mapValues { it.value.toSet() }
        private val primaryKeyCalculator = PrimaryIndexCalculator(Companion::lookupCarId)
        private val strictPrimaryKeyCalculator = PrimaryIndexCalculator(Companion::lookupCarId, strict = true)

        private fun lookupCarId(fieldName: String, fieldValue: Any?): Result<Int> {
            return if (indexedFields.contains(fieldName)) {
                val key = createdInvertedMapKey(fieldName, fieldValue)
                Result.Success(invertedMap.getOrDefault(key, setOf()))
            } else {
                Result.Failed("$fieldName is not indexed")
            }
        }

        private fun createMapping(fieldName: String, cars: List<Car>) = cars.map {
            val field = Car::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            val fieldValue = field.get(it)
            createdInvertedMapKey(fieldName, fieldValue) to it.id
        }

        private fun createdInvertedMapKey(fieldName: String, fieldValue: Any?) = "$fieldName:$fieldValue"
    }
}

data class Car(val id: Int, val color: String, val make: String, val location: String)
