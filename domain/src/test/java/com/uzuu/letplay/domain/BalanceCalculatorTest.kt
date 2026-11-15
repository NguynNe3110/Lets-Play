package com.uzuu.letplay.domain

import com.uzuu.letplay.domain.model.Allocation
import com.uzuu.letplay.domain.model.Expense
import com.uzuu.letplay.domain.model.LocalExpense
import com.uzuu.letplay.domain.model.LocalTransfer
import com.uzuu.letplay.domain.model.MembershipId
import com.uzuu.letplay.domain.model.MoneyVnd
import com.uzuu.letplay.domain.model.SettlementStatus
import com.uzuu.letplay.domain.model.SettlementSuggestion
import com.uzuu.letplay.domain.rules.BalanceCalculator
import com.uzuu.letplay.domain.rules.EqualShareCalculator
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceCalculatorTest {

    @Test
    fun ac21CalculatesBalancesForAnAndMinhWithSumZero() {
        val participants = (0..5).toList()
        val expenses = listOf(
            LocalExpense(
                id = 1L,
                title = "Lẩu",
                amount = MoneyVnd(900_000L),
                payerIndex = 0,
                participantIndices = participants,
                shares = EqualShareCalculator.calculate(MoneyVnd(900_000L), participants, 6)
            ),
            LocalExpense(
                id = 2L,
                title = "Cà phê",
                amount = MoneyVnd(330_000L),
                payerIndex = 1,
                participantIndices = participants,
                shares = EqualShareCalculator.calculate(MoneyVnd(330_000L), participants, 6)
            )
        )

        val balances = BalanceCalculator.calculateLocal(expenses, emptyList(), 6)

        // An (index 0): paid 900k, share is 150k + 55k = 205k -> net +695k
        assertEquals(695_000L, balances[0])
        // Minh (index 1): paid 330k, share is 150k + 55k = 205k -> net +125k
        assertEquals(125_000L, balances[1])
        // Others (indices 2..5): paid 0k, share is 205k -> net -205k
        assertEquals(-205_000L, balances[2])
        assertEquals(-205_000L, balances[3])
        assertEquals(-205_000L, balances[4])
        assertEquals(-205_000L, balances[5])

        // Sum of all balances MUST equal 0
        assertEquals(0L, balances.sum())
    }

    @Test
    fun ac22PendingTransferDoesNotChangeBalancesConfirmedTransferSettlesDebt() {
        val participants = (0..5).toList()
        val expenses = listOf(
            LocalExpense(
                id = 1L,
                title = "Lẩu",
                amount = MoneyVnd(900_000L),
                payerIndex = 0,
                participantIndices = participants,
                shares = EqualShareCalculator.calculate(MoneyVnd(900_000L), participants, 6)
            )
        )

        val pendingTransfer = LocalTransfer(
            id = 999L,
            fromIndex = 1,
            toIndex = 0,
            amount = MoneyVnd(150_000L),
            status = SettlementStatus.PENDING
        )

        // Pending transfer has no effect
        val balancesPending = BalanceCalculator.calculateLocal(expenses, listOf(pendingTransfer), 6)
        assertEquals(-150_000L, balancesPending[1])
        assertEquals(750_000L, balancesPending[0])
        assertEquals(0L, balancesPending.sum())

        // Rejected transfer has no effect
        val rejectedTransfer = pendingTransfer.copy(status = SettlementStatus.REJECTED)
        val balancesRejected = BalanceCalculator.calculateLocal(expenses, listOf(rejectedTransfer), 6)
        assertEquals(-150_000L, balancesRejected[1])
        assertEquals(750_000L, balancesRejected[0])
        assertEquals(0L, balancesRejected.sum())

        // Confirmed transfer settles debt
        val confirmedTransfer = pendingTransfer.copy(status = SettlementStatus.CONFIRMED)
        val balancesConfirmed = BalanceCalculator.calculateLocal(expenses, listOf(confirmedTransfer), 6)
        assertEquals(0L, balancesConfirmed[1])
        assertEquals(600_000L, balancesConfirmed[0])
        assertEquals(0L, balancesConfirmed.sum())
    }

    @Test
    fun onlineBalanceCalculationSupportsMultiPayerAndConfirmedSettlements() {
        val m1 = MembershipId("user_1")
        val m2 = MembershipId("user_2")
        val m3 = MembershipId("user_3")

        // Expense: 300,000 VND total.
        // Payers: user_1 pays 200k, user_2 pays 100k
        // Shares: user_1: 100k, user_2: 100k, user_3: 100k
        val expense = Expense(
            id = "exp_01",
            title = "Khách sạn",
            totalAmount = MoneyVnd(300_000L),
            payers = listOf(
                Allocation(m1, "User 1", MoneyVnd(200_000L)),
                Allocation(m2, "User 2", MoneyVnd(100_000L))
            ),
            shares = listOf(
                Allocation(m1, "User 1", MoneyVnd(100_000L)),
                Allocation(m2, "User 2", MoneyVnd(100_000L)),
                Allocation(m3, "User 3", MoneyVnd(100_000L))
            )
        )

        val members = listOf(m1, m2, m3)
        val initialBalances = BalanceCalculator.calculateOnline(members, listOf(expense), emptyList())

        val balMap = initialBalances.associate { it.membershipId to it.netAmount }
        // user_1: paid 200k, share 100k -> +100k
        assertEquals(100_000L, balMap[m1])
        // user_2: paid 100k, share 100k -> 0
        assertEquals(0L, balMap[m2])
        // user_3: paid 0, share 100k -> -100k
        assertEquals(-100_000L, balMap[m3])
        assertEquals(0L, initialBalances.sumOf { it.netAmount })

        // Settle: user_3 pays user_1 100k
        val settlement = SettlementSuggestion(
            fromMembershipId = m3,
            toMembershipId = m1,
            amount = MoneyVnd(100_000L)
        )

        val settledBalances = BalanceCalculator.calculateOnline(members, listOf(expense), listOf(settlement))
        val settledMap = settledBalances.associate { it.membershipId to it.netAmount }
        assertEquals(0L, settledMap[m1])
        assertEquals(0L, settledMap[m2])
        assertEquals(0L, settledMap[m3])
        assertEquals(0L, settledBalances.sumOf { it.netAmount })
    }
}
