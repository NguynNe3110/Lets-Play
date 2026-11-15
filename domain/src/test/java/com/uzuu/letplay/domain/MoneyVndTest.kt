package com.uzuu.letplay.domain

import com.uzuu.letplay.domain.model.MoneyVnd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MoneyVndTest {

    @Test
    fun rejectsNegativeAmountOnInit() {
        assertFailsWith<IllegalArgumentException> {
            MoneyVnd(-1L)
        }
    }

    @Test
    fun performsArithmeticOperationsCorrectly() {
        val a = MoneyVnd(50_000L)
        val b = MoneyVnd(25_000L)

        assertEquals(MoneyVnd(75_000L), a + b)
        assertEquals(MoneyVnd(25_000L), a - b)
        assertEquals(MoneyVnd(100_000L), a * 2L)
        assertEquals(MoneyVnd(25_000L), a / 2L)
        assertEquals(MoneyVnd(1L), MoneyVnd(7L) % 3L)
    }

    @Test
    fun subtractionBelowZeroThrowsIllegalArgumentException() {
        val a = MoneyVnd(10_000L)
        val b = MoneyVnd(20_000L)
        assertFailsWith<IllegalArgumentException> {
            a - b
        }
    }

    @Test
    fun detectsOverflowOnAddition() {
        val large = MoneyVnd(Long.MAX_VALUE)
        val small = MoneyVnd(1L)
        assertFailsWith<ArithmeticException> {
            large + small
        }
    }

    @Test
    fun detectsOverflowOnMultiplication() {
        val large = MoneyVnd(Long.MAX_VALUE / 2 + 10)
        assertFailsWith<ArithmeticException> {
            large * 2L
        }
    }

    @Test
    fun formatsVndCorrectly() {
        assertEquals("0đ", MoneyVnd(0L).formatVnd())
        assertEquals("100đ", MoneyVnd(100L).formatVnd())
        assertEquals("1.000đ", MoneyVnd(1000L).formatVnd())
        assertEquals("900.000đ", MoneyVnd(900_000L).formatVnd())
        assertEquals("100.000.000đ", MoneyVnd(100_000_000L).formatVnd())
    }

    @Test
    fun formatsNegativeAmountThroughCompanion() {
        assertEquals("-205.000đ", MoneyVnd.formatVndAmount(-205_000L))
        assertEquals("-1.000đ", MoneyVnd.formatVndAmount(-1_000L))
    }

    @Test
    fun parsesFormattedStringCorrectly() {
        assertEquals(MoneyVnd(900_000L), MoneyVnd.parse("900.000đ"))
        assertEquals(MoneyVnd(100L), MoneyVnd.parse("100đ"))
        assertEquals(MoneyVnd(0L), MoneyVnd.parse("0đ"))
    }

    @Test
    fun comparesMoneyVndCorrectly() {
        val a = MoneyVnd(10_000L)
        val b = MoneyVnd(20_000L)
        assertTrue(a < b)
        assertTrue(b > a)
        assertEquals(0, a.compareTo(MoneyVnd(10_000L)))
    }
}
