package com.uzuu.letplay.domain.rules

import com.uzuu.letplay.domain.model.MembershipId
import com.uzuu.letplay.domain.model.MoneyVnd

object EqualShareCalculator {

    /**
     * Divides [amount] equally among [participants] out of [totalMembers].
     * Remainder VND is distributed to participants in ascending index order (BR/AC17).
     *
     * Invariants & Validations:
     * - [participants] cannot contain duplicates
     * - each participant index must be in [0, [totalMembers])
     * - sum of resulting shares exactly equals [amount]
     */
    fun calculate(
        amount: MoneyVnd,
        participants: List<Int>,
        totalMembers: Int
    ): List<MoneyVnd> {
        require(totalMembers >= 0) { "totalMembers must be non-negative: $totalMembers" }
        require(participants.distinct().size == participants.size) {
            "Participant indices must be distinct"
        }
        for (p in participants) {
            require(p in 0 until totalMembers) {
                "Participant index $p out of range [0, $totalMembers)"
            }
        }

        if (totalMembers == 0) return emptyList()
        if (participants.isEmpty()) {
            require(amount == MoneyVnd.ZERO) {
                "Cannot divide non-zero amount among zero participants"
            }
            return List(totalMembers) { MoneyVnd.ZERO }
        }

        val totalAmount = amount.amount
        val count = participants.size.toLong()
        val base = totalAmount / count
        val remainder = totalAmount % count

        val shares = LongArray(totalMembers)
        val sortedParticipants = participants.sorted()
        for (i in sortedParticipants.indices) {
            val memberIdx = sortedParticipants[i]
            val extra = if (i.toLong() < remainder) 1L else 0L
            shares[memberIdx] = base + extra
        }

        val result = shares.map { MoneyVnd(it) }

        // Invariant: sum of all shares equals original amount
        val sum = result.fold(0L) { acc, s -> Math.addExact(acc, s.amount) }
        check(sum == totalAmount) {
            "Internal error: Sum of shares ($sum) != original amount ($totalAmount)"
        }

        return result
    }

    /**
     * Online version: Divides [amount] equally among [memberIds].
     * Remainder VND is distributed in ascending order of MembershipId string value.
     */
    fun calculateForMembers(
        amount: MoneyVnd,
        memberIds: List<MembershipId>
    ): Map<MembershipId, MoneyVnd> {
        require(memberIds.distinct().size == memberIds.size) {
            "MembershipIds must be distinct"
        }
        if (memberIds.isEmpty()) {
            require(amount == MoneyVnd.ZERO) {
                "Cannot divide non-zero amount among zero members"
            }
            return emptyMap()
        }

        val totalAmount = amount.amount
        val count = memberIds.size.toLong()
        val base = totalAmount / count
        val remainder = totalAmount % count

        val sortedMembers = memberIds.sortedBy { it.value }
        val map = mutableMapOf<MembershipId, MoneyVnd>()

        for (i in sortedMembers.indices) {
            val memberId = sortedMembers[i]
            val extra = if (i.toLong() < remainder) 1L else 0L
            map[memberId] = MoneyVnd(base + extra)
        }

        // Invariant check
        val sum = map.values.fold(0L) { acc, s -> Math.addExact(acc, s.amount) }
        check(sum == totalAmount) {
            "Internal error: Sum of shares ($sum) != original amount ($totalAmount)"
        }

        return map
    }
}
