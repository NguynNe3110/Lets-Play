package com.uzuu.letplay.domain.rules

import com.uzuu.letplay.domain.model.Balance
import com.uzuu.letplay.domain.model.Expense
import com.uzuu.letplay.domain.model.LocalExpense
import com.uzuu.letplay.domain.model.LocalTransfer
import com.uzuu.letplay.domain.model.MembershipId
import com.uzuu.letplay.domain.model.SettlementStatus
import com.uzuu.letplay.domain.model.SettlementSuggestion

object BalanceCalculator {

    /**
     * Calculates net balances for local room members:
     * Positive balance (> 0): is owed money (creditor)
     * Negative balance (< 0): owes money (debtor)
     * Sum of all balances across all members is guaranteed to be 0.
     * Pending or rejected transfers do NOT affect balances.
     */
    fun calculateLocal(
        expenses: List<LocalExpense>,
        transfers: List<LocalTransfer>,
        totalMembers: Int
    ): List<Long> {
        require(totalMembers >= 0) { "totalMembers must be non-negative: $totalMembers" }
        if (totalMembers == 0) return emptyList()

        val balances = LongArray(totalMembers)

        // 1. Process expenses
        for (expense in expenses) {
            if (expense.payerIndex in 0 until totalMembers) {
                balances[expense.payerIndex] = Math.addExact(
                    balances[expense.payerIndex],
                    expense.amount.amount
                )
            }
            for (i in expense.shares.indices) {
                if (i in 0 until totalMembers) {
                    balances[i] = Math.subtractExact(
                        balances[i],
                        expense.shares[i].amount
                    )
                }
            }
        }

        // 2. Process transfers (ONLY CONFIRMED ones affect balance)
        for (transfer in transfers) {
            if (transfer.status == SettlementStatus.CONFIRMED) {
                if (transfer.fromIndex in 0 until totalMembers) {
                    // Payer reduces their debt -> balance increases towards zero
                    balances[transfer.fromIndex] = Math.addExact(
                        balances[transfer.fromIndex],
                        transfer.amount.amount
                    )
                }
                if (transfer.toIndex in 0 until totalMembers) {
                    // Receiver receives money -> their receivable decreases
                    balances[transfer.toIndex] = Math.subtractExact(
                        balances[transfer.toIndex],
                        transfer.amount.amount
                    )
                }
            }
        }

        // Check invariant: total sum == 0
        val sum = balances.fold(0L) { acc, b -> Math.addExact(acc, b) }
        check(sum == 0L) { "Internal invariant violated: sum of balances ($sum) must be 0" }

        return balances.toList()
    }

    /**
     * Calculates net balances as domain [Balance] objects for local rooms.
     */
    fun calculateLocalBalances(
        expenses: List<LocalExpense>,
        transfers: List<LocalTransfer>,
        memberNames: List<String>
    ): List<Balance> {
        val raw = calculateLocal(expenses, transfers, memberNames.size)
        return raw.mapIndexed { index, netAmount ->
            Balance(
                membershipId = MembershipId(index.toString()),
                displayName = memberNames.getOrElse(index) { "Member $index" },
                netAmount = netAmount,
                memberIndex = index
            )
        }
    }

    /**
     * Calculates net balances for online room members from expenses and confirmed settlements.
     */
    fun calculateOnline(
        allMembers: List<MembershipId>,
        expenses: List<Expense>,
        confirmedSettlements: List<SettlementSuggestion>
    ): List<Balance> {
        val balanceMap = mutableMapOf<MembershipId, Long>()
        for (m in allMembers) {
            balanceMap[m] = 0L
        }

        // 1. Process expenses
        for (expense in expenses) {
            for (payer in expense.payers) {
                val current = balanceMap[payer.membershipId] ?: 0L
                balanceMap[payer.membershipId] = Math.addExact(current, payer.amount.amount)
            }
            for (share in expense.shares) {
                val current = balanceMap[share.membershipId] ?: 0L
                balanceMap[share.membershipId] = Math.subtractExact(current, share.amount.amount)
            }
        }

        // 2. Process settlements
        for (settlement in confirmedSettlements) {
            val fromBal = balanceMap[settlement.fromMembershipId] ?: 0L
            balanceMap[settlement.fromMembershipId] = Math.addExact(fromBal, settlement.amount.amount)

            val toBal = balanceMap[settlement.toMembershipId] ?: 0L
            balanceMap[settlement.toMembershipId] = Math.subtractExact(toBal, settlement.amount.amount)
        }

        // Check sum == 0
        val sum = balanceMap.values.fold(0L) { acc, b -> Math.addExact(acc, b) }
        check(sum == 0L) { "Internal invariant violated: sum of balances ($sum) must be 0" }

        return allMembers.map { id ->
            Balance(
                membershipId = id,
                netAmount = balanceMap[id] ?: 0L
            )
        }
    }
}
