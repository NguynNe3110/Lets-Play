package com.uzuu.letplay.domain

import com.uzuu.letplay.domain.model.MembershipId
import com.uzuu.letplay.domain.model.MoneyVnd
import com.uzuu.letplay.domain.rules.EqualShareCalculator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EqualShareCalculatorTest {

    @Test
    fun ac17Divides100VndAmong3PeopleAs343333WithSum100() {
        val shares = EqualShareCalculator.calculate(
            amount = MoneyVnd(100L),
            participants = listOf(0, 1, 2),
            totalMembers = 6
        )

        assertEquals(6, shares.size)
        assertEquals(34L, shares[0].amount)
        assertEquals(33L, shares[1].amount)
        assertEquals(33L, shares[2].amount)
        assertEquals(0L, shares[3].amount)
        assertEquals(0L, shares[4].amount)
        assertEquals(0L, shares[5].amount)

        val totalSum = shares.fold(0L) { acc, s -> acc + s.amount }
        assertEquals(100L, totalSum)
    }

    @Test
    fun distributesRemainderToParticipantsInAscendingIndexOrder() {
        // 10 VND among 3 participants with indices [4, 1, 2] out of 5 members
        // sorted indices: [1, 2, 4]. base = 3, remainder = 1.
        // index 1 gets 3 + 1 = 4.
        // index 2 gets 3.
        // index 4 gets 3.
        val shares = EqualShareCalculator.calculate(
            amount = MoneyVnd(10L),
            participants = listOf(4, 1, 2),
            totalMembers = 5
        )

        assertEquals(0L, shares[0].amount)
        assertEquals(4L, shares[1].amount)
        assertEquals(3L, shares[2].amount)
        assertEquals(0L, shares[3].amount)
        assertEquals(3L, shares[4].amount)
        assertEquals(10L, shares.sumOf { it.amount })
    }

    @Test
    fun verifiesSumConservationAcrossVariousAmountsAndParticipantCounts() {
        val testAmounts = listOf(1L, 2L, 3L, 7L, 100L, 999_999L, 10_000_000L, 100_000_000L)
        val participantCounts = listOf(1, 2, 3, 4, 5, 6, 7, 10)

        for (amt in testAmounts) {
            for (pCount in participantCounts) {
                val participants = (0 until pCount).toList()
                val totalMembers = pCount + 2
                val shares = EqualShareCalculator.calculate(
                    amount = MoneyVnd(amt),
                    participants = participants,
                    totalMembers = totalMembers
                )

                val sum = shares.sumOf { it.amount }
                assertEquals(amt, sum, "Sum conservation failed for amount=$amt with $pCount participants")
            }
        }
    }

    @Test
    fun zeroAmountGivesAllZeroShares() {
        val shares = EqualShareCalculator.calculate(
            amount = MoneyVnd.ZERO,
            participants = listOf(0, 1),
            totalMembers = 3
        )
        assertEquals(listOf(MoneyVnd.ZERO, MoneyVnd.ZERO, MoneyVnd.ZERO), shares)
    }

    @Test
    fun zeroParticipantsWithZeroAmountReturnsAllZeros() {
        val shares = EqualShareCalculator.calculate(
            amount = MoneyVnd.ZERO,
            participants = emptyList(),
            totalMembers = 3
        )
        assertEquals(listOf(MoneyVnd.ZERO, MoneyVnd.ZERO, MoneyVnd.ZERO), shares)
    }

    @Test
    fun zeroParticipantsWithNonZeroAmountThrowsException() {
        assertFailsWith<IllegalArgumentException> {
            EqualShareCalculator.calculate(
                amount = MoneyVnd(100L),
                participants = emptyList(),
                totalMembers = 3
            )
        }
    }

    @Test
    fun duplicateParticipantsRejected() {
        assertFailsWith<IllegalArgumentException> {
            EqualShareCalculator.calculate(
                amount = MoneyVnd(100L),
                participants = listOf(0, 1, 1),
                totalMembers = 3
            )
        }
    }

    @Test
    fun outOfBoundsParticipantRejected() {
        assertFailsWith<IllegalArgumentException> {
            EqualShareCalculator.calculate(
                amount = MoneyVnd(100L),
                participants = listOf(0, 3),
                totalMembers = 3
            )
        }
        assertFailsWith<IllegalArgumentException> {
            EqualShareCalculator.calculate(
                amount = MoneyVnd(100L),
                participants = listOf(-1, 0),
                totalMembers = 3
            )
        }
    }

    @Test
    fun negativeTotalMembersRejected() {
        assertFailsWith<IllegalArgumentException> {
            EqualShareCalculator.calculate(
                amount = MoneyVnd(100L),
                participants = listOf(0),
                totalMembers = -1
            )
        }
    }

    @Test
    fun calculateForMembersDistributesRemainderByMembershipIdOrder() {
        val m1 = MembershipId("m_01")
        val m2 = MembershipId("m_02")
        val m3 = MembershipId("m_03")

        val result = EqualShareCalculator.calculateForMembers(
            amount = MoneyVnd(100L),
            memberIds = listOf(m3, m1, m2)
        )

        assertEquals(34L, result[m1]?.amount)
        assertEquals(33L, result[m2]?.amount)
        assertEquals(33L, result[m3]?.amount)
        assertEquals(100L, result.values.sumOf { it.amount })
    }

    @Test
    fun calculateForMembersRejectsDuplicates() {
        val m1 = MembershipId("m_01")
        assertFailsWith<IllegalArgumentException> {
            EqualShareCalculator.calculateForMembers(
                amount = MoneyVnd(100L),
                memberIds = listOf(m1, m1)
            )
        }
    }

    @Test
    fun overflowProtectionForLargeAmounts() {
        val largeAmount = MoneyVnd(Long.MAX_VALUE - 1)
        val shares = EqualShareCalculator.calculate(
            amount = largeAmount,
            participants = listOf(0, 1),
            totalMembers = 2
        )
        val sum = shares.sumOf { it.amount }
        assertEquals(largeAmount.amount, sum)
    }
}
