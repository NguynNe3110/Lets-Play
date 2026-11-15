package com.uzuu.letplay.domain

import com.uzuu.letplay.domain.model.Balance
import com.uzuu.letplay.domain.model.MembershipId
import com.uzuu.letplay.domain.rules.SettlementSuggester
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SettlementSuggesterTest {

    @Test
    fun computesGreedySettlementSuggestionsFromBalances() {
        // From money.test.ts:
        val balances = listOf(695_000L, 125_000L, -205_000L, -205_000L, -205_000L, -205_000L)
        val debts = SettlementSuggester.computeFromBalances(balances)

        assertTrue(debts.isNotEmpty())
        val totalDebtAmount = debts.sumOf { it.amount.amount }
        assertEquals(820_000L, totalDebtAmount)

        // All settlement suggestions must have positive amount
        for (d in debts) {
            assertTrue(d.amount.amount > 0)
        }
    }

    @Test
    fun computesSuggestionsFromDomainBalanceObjects() {
        val balances = listOf(
            Balance(MembershipId("m0"), "An", 695_000L, memberIndex = 0),
            Balance(MembershipId("m1"), "Minh", 125_000L, memberIndex = 1),
            Balance(MembershipId("m2"), "Linh", -205_000L, memberIndex = 2),
            Balance(MembershipId("m3"), "Hà", -205_000L, memberIndex = 3),
            Balance(MembershipId("m4"), "Nam", -205_000L, memberIndex = 4),
            Balance(MembershipId("m5"), "Vy", -205_000L, memberIndex = 5)
        )

        val suggestions = SettlementSuggester.compute(balances)
        assertEquals(5, suggestions.size)
        assertEquals(820_000L, suggestions.sumOf { it.amount.amount })

        // Check first suggestion: Linh (index 2) pays An (index 0) 205k
        assertEquals(MembershipId("m2"), suggestions[0].fromMembershipId)
        assertEquals(MembershipId("m0"), suggestions[0].toMembershipId)
        assertEquals(205_000L, suggestions[0].amount.amount)
    }

    @Test
    fun returnsEmptyListWhenBalancesAreAllZero() {
        val balances = listOf(
            Balance(MembershipId("m0"), "An", 0L),
            Balance(MembershipId("m1"), "Bình", 0L)
        )
        val suggestions = SettlementSuggester.compute(balances)
        assertTrue(suggestions.isEmpty())
    }
}
