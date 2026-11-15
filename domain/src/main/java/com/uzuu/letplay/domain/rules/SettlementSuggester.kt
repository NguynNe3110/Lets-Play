package com.uzuu.letplay.domain.rules

import com.uzuu.letplay.domain.model.Balance
import com.uzuu.letplay.domain.model.MembershipId
import com.uzuu.letplay.domain.model.MoneyVnd
import com.uzuu.letplay.domain.model.SettlementSuggestion

object SettlementSuggester {

    private data class MutableParty(val index: Int, var amount: Long)
    private data class PartyBalance(val balance: Balance, var amount: Long)

    /**
     * Computes greedy minimum settlement suggestions from raw net balance values.
     * Debtors (balance < 0) pay Creditors (balance > 0).
     */
    fun computeFromBalances(balances: List<Long>): List<SettlementSuggestion> {
        val debtors = mutableListOf<MutableParty>()
        val creditors = mutableListOf<MutableParty>()

        balances.forEachIndexed { index, balance ->
            if (balance < 0) {
                debtors.add(MutableParty(index, -balance))
            } else if (balance > 0) {
                creditors.add(MutableParty(index, balance))
            }
        }

        val suggestions = mutableListOf<SettlementSuggestion>()

        for (debtor in debtors) {
            for (creditor in creditors) {
                val settlement = Math.min(debtor.amount, creditor.amount)
                if (settlement > 0) {
                    suggestions.add(
                        SettlementSuggestion(
                            fromMembershipId = MembershipId(debtor.index.toString()),
                            toMembershipId = MembershipId(creditor.index.toString()),
                            amount = MoneyVnd(settlement),
                            fromIndex = debtor.index,
                            toIndex = creditor.index
                        )
                    )
                    debtor.amount -= settlement
                    creditor.amount -= settlement
                }
            }
        }

        return suggestions
    }

    /**
     * Computes greedy minimum settlement suggestions from domain [Balance] objects.
     */
    fun compute(balances: List<Balance>): List<SettlementSuggestion> {
        val debtors = mutableListOf<PartyBalance>()
        val creditors = mutableListOf<PartyBalance>()

        for (b in balances) {
            if (b.netAmount < 0) {
                debtors.add(PartyBalance(b, -b.netAmount))
            } else if (b.netAmount > 0) {
                creditors.add(PartyBalance(b, b.netAmount))
            }
        }

        val suggestions = mutableListOf<SettlementSuggestion>()

        for (debtor in debtors) {
            for (creditor in creditors) {
                val settlement = Math.min(debtor.amount, creditor.amount)
                if (settlement > 0) {
                    suggestions.add(
                        SettlementSuggestion(
                            fromMembershipId = debtor.balance.membershipId,
                            toMembershipId = creditor.balance.membershipId,
                            amount = MoneyVnd(settlement),
                            fromDisplayName = debtor.balance.displayName,
                            toDisplayName = creditor.balance.displayName,
                            fromIndex = debtor.balance.memberIndex,
                            toIndex = creditor.balance.memberIndex
                        )
                    )
                    debtor.amount -= settlement
                    creditor.amount -= settlement
                }
            }
        }

        return suggestions
    }
}
