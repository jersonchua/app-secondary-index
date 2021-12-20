package com.jersonchua.secondaryindex

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class PrimaryIndexCalculatorTest {
    @Test
    fun testEquals() {
        val condition = Equals("color", "blue")
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Success(setOf(5, 6)), result)
    }

    @Test
    fun testIn() {
        val condition = In("color", listOf("blue", "red"))
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Success(setOf(1, 3, 5, 6)), result)
    }

    @Test
    fun testUnIndexedField() {
        val condition = Equals("location", "Rockaway")
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Failed<Int>(), result)
    }

    @Test
    fun testAnd() {
        val condition = And(listOf(
            Equals("color", "blue"),
            Equals("make", "Toyota"),
            UnsupportedCondition
        ))
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Success(setOf(5)), result)
    }

    @Test
    fun testAndPrimaryKeyNotFound() {
        val condition = And(listOf(
            Equals("color", "green"),
            Equals("make", "Toyota"),
        ))
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Success(setOf<Int>()), result)
    }

    @Test
    fun testAndFailed() {
        val condition = And(listOf(
            Equals("location", "Rockaway")
        ))
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Failed<Int>(), result)
    }

    @Test
    fun testOr() {
        val condition = Or(listOf(
            Equals("color", "silver")
        ))
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Success(setOf(2)), result)
    }

    @Test
    fun testOrPrimaryKeyNotFound() {
        val condition = Or(listOf(
            Equals("color", "green"),
            Equals("make", "Acura"),
        ))
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Success(setOf<Int>()), result)
    }

    @Test
    fun testOrFailed() {
        val condition = Or(listOf(
            Equals("color", "blue"),
            UnsupportedCondition
        ))
        val result = calculator.computePrimaryIndices(condition)
        assertEquals(Result.Failed<Int>(), result)
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
        private val invertedMap = indexedFields.flatMap { createInvertedMap(it, cars).toList() }.toMap()
        private val calculator = PrimaryIndexCalculator(Companion::lookupCarId)

        private fun lookupCarId(fieldName:String, value: Any?): Result<Int> {
            return if (indexedFields.contains(fieldName)) {
                val key = "$fieldName:$value"
                Result.Success(invertedMap.getOrDefault(key, setOf()))
            } else {
                Result.Failed()
            }
        }

        private fun createInvertedMap(fieldName: String, cars: List<Car>): Map<String, Set<Int>> {
            return cars.map {
                val field = Car::class.java.getDeclaredField(fieldName)
                field.isAccessible = true
                val fieldValue = field.get(it)
                "$fieldName:$fieldValue" to it.id
            }.groupBy({ it.first }, { it.second }).mapValues { it.value.toSet() }
        }
    }
}

data class Car(val id: Int, val color: String, val make: String, val location: String)
