package com.uzuu.letplay.domain.rules

import com.uzuu.letplay.domain.model.PlanDraft
import com.uzuu.letplay.domain.model.PlanSnapshot
import com.uzuu.letplay.domain.model.Proposal
import com.uzuu.letplay.domain.model.ProposalId
import com.uzuu.letplay.domain.model.PublishedPlan
import com.uzuu.letplay.domain.model.RoomLifecycle
import com.uzuu.letplay.domain.model.VoteValue

/**
 * Domain rules and reducer for planning (Ticket P2-03, MIG-AC05, MIG-AC06).
 */
object PlanningRules {

    sealed class PlanningAction {
        data class AddProposal(val proposal: Proposal) : PlanningAction()
        data class EditProposal(val proposalId: ProposalId, val editorIsOwner: Boolean, val title: String, val description: String) : PlanningAction()
        data class RemoveProposal(val proposalId: ProposalId, val editorIsOwner: Boolean) : PlanningAction()
        data class CastVote(val proposalId: ProposalId, val voterId: String, val value: VoteValue) : PlanningAction()
        data class SelectProposal(val proposalId: ProposalId, val isSelected: Boolean, val isHost: Boolean) : PlanningAction()
        data class ReorderDraft(val newOrder: List<ProposalId>) : PlanningAction()
        data class SetDraftStartTime(val startTime: String, val isHost: Boolean) : PlanningAction()
        data class SubmitOrderBallot(val voterId: String, val orderedProposals: List<ProposalId>) : PlanningAction()
        data class SealPlan(val isHost: Boolean, val reason: String, val timestampMillis: Long) : PlanningAction()
        data class ReopenPlan(val isHost: Boolean) : PlanningAction()
        data class ToggleCompletion(val proposalId: ProposalId) : PlanningAction()
        data class EndRoom(val isHost: Boolean, val timestampMillis: Long) : PlanningAction()
    }

    data class PlanningState(
        val lifecycle: RoomLifecycle = RoomLifecycle.ACTIVE,
        val proposals: List<Proposal> = emptyList(),
        val votesByMember: Map<String, Map<ProposalId, VoteValue>> = emptyMap(),
        val draft: PlanDraft = PlanDraft(),
        val publishedPlan: PublishedPlan? = null,
        val planHistory: List<PlanSnapshot> = emptyList(),
        val completedProposals: Set<ProposalId> = emptySet(),
        val endedAtMillis: Long? = null
    ) {
        val hasPublishedPlan: Boolean get() = publishedPlan != null
        val currentPlanVersion: Int get() = publishedPlan?.version ?: 0
    }

    sealed class Result {
        data class Success(val newState: PlanningState) : Result()
        data class Failure(val reason: String) : Result()
    }

    fun reduce(state: PlanningState, action: PlanningAction): Result {
        if (state.lifecycle == RoomLifecycle.ENDED) {
            return Result.Failure("Kèo đã kết thúc, kế hoạch hiện chỉ xem lại.")
        }

        return when (action) {
            is PlanningAction.AddProposal -> {
                if (state.proposals.any { it.id == action.proposal.id }) {
                    return Result.Failure("Ý tưởng này đã được tạo.")
                }
                Result.Success(state.copy(proposals = state.proposals + action.proposal))
            }

            is PlanningAction.EditProposal -> {
                val existing = state.proposals.find { it.id == action.proposalId }
                    ?: return Result.Failure("Không tìm thấy ý tưởng.")
                if (!action.editorIsOwner) {
                    return Result.Failure("Chỉ sửa ý tưởng của chính bạn.")
                }
                // MIG-AC06: Cannot directly edit an idea that is already in a published plan
                if (state.publishedPlan?.orderedProposals?.contains(action.proposalId) == true) {
                    return Result.Failure("Ý này đang trong lịch chung. Hãy thêm ý thay thế để nhóm cập nhật lịch.")
                }

                // Invalidate ONLY votes for this edited idea across all members
                val updatedVotes = state.votesByMember.mapValues { (_, memberVotes) ->
                    memberVotes - action.proposalId
                }

                val updatedProposal = existing.copy(
                    title = action.title.trim(),
                    description = action.description.trim()
                )
                val updatedProposals = state.proposals.map {
                    if (it.id == action.proposalId) updatedProposal else it
                }

                Result.Success(
                    state.copy(
                        proposals = updatedProposals,
                        votesByMember = updatedVotes
                    )
                )
            }

            is PlanningAction.RemoveProposal -> {
                if (state.proposals.none { it.id == action.proposalId }) {
                    return Result.Failure("Không tìm thấy ý tưởng.")
                }
                if (!action.editorIsOwner) {
                    return Result.Failure("Chỉ xóa ý tưởng của chính bạn.")
                }
                if (state.publishedPlan?.orderedProposals?.contains(action.proposalId) == true) {
                    return Result.Failure("Không xóa trực tiếp hoạt động đang trong lịch chung.")
                }

                val updatedProposals = state.proposals.filterNot { it.id == action.proposalId }
                val updatedVotes = state.votesByMember.mapValues { (_, memberVotes) ->
                    memberVotes - action.proposalId
                }
                val updatedDraftSelected = state.draft.selectedProposals.filterNot { it == action.proposalId }
                val updatedDraftOrdered = state.draft.orderedProposals.filterNot { it == action.proposalId }

                Result.Success(
                    state.copy(
                        proposals = updatedProposals,
                        votesByMember = updatedVotes,
                        draft = state.draft.copy(
                            selectedProposals = updatedDraftSelected,
                            orderedProposals = updatedDraftOrdered
                        )
                    )
                )
            }

            is PlanningAction.CastVote -> {
                if (state.proposals.none { it.id == action.proposalId }) {
                    return Result.Failure("Ý tưởng không tồn tại.")
                }
                val memberVotes = (state.votesByMember[action.voterId] ?: emptyMap()) + (action.proposalId to action.value)
                val updatedVotes = state.votesByMember + (action.voterId to memberVotes)
                Result.Success(state.copy(votesByMember = updatedVotes))
            }

            is PlanningAction.SelectProposal -> {
                if (!action.isHost) return Result.Failure("Chỉ chủ phòng được chọn hoạt động vào lịch.")
                if (state.proposals.none { it.id == action.proposalId }) {
                    return Result.Failure("Ý tưởng không tồn tại.")
                }
                val currentSelected = state.draft.selectedProposals
                val newSelected = if (action.isSelected) {
                    if (action.proposalId in currentSelected) currentSelected else currentSelected + action.proposalId
                } else {
                    currentSelected - action.proposalId
                }
                val newOrdered = state.draft.orderedProposals.filter { it in newSelected }.let { list ->
                    if (action.isSelected && action.proposalId !in list) list + action.proposalId else list
                }
                Result.Success(state.copy(draft = state.draft.copy(selectedProposals = newSelected, orderedProposals = newOrdered)))
            }

            is PlanningAction.ReorderDraft -> {
                val selectedSet = state.draft.selectedProposals.toSet()
                if (action.newOrder.size != selectedSet.size || action.newOrder.toSet() != selectedSet) {
                    return Result.Failure("Thứ tự chặng mới phải chứa đúng các hoạt động đã chọn.")
                }
                Result.Success(state.copy(draft = state.draft.copy(orderedProposals = action.newOrder)))
            }

            is PlanningAction.SetDraftStartTime -> {
                if (!action.isHost) return Result.Failure("Chỉ chủ phòng được chỉnh giờ bắt đầu.")
                val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
                if (!timeRegex.matches(action.startTime)) {
                    return Result.Failure("Giờ bắt đầu cần đúng định dạng HH:mm.")
                }
                Result.Success(state.copy(draft = state.draft.copy(draftStartTime = action.startTime)))
            }

            is PlanningAction.SubmitOrderBallot -> {
                val selectedSet = state.draft.selectedProposals.toSet()
                if (action.orderedProposals.size != selectedSet.size || action.orderedProposals.toSet() != selectedSet) {
                    return Result.Failure("Hãy xếp đủ các hoạt động đã chọn.")
                }
                val memberId = com.uzuu.letplay.domain.model.MembershipId(action.voterId)
                val updatedBallots = state.draft.orderBallots + (memberId to action.orderedProposals)
                Result.Success(state.copy(draft = state.draft.copy(orderBallots = updatedBallots)))
            }

            is PlanningAction.SealPlan -> {
                if (!action.isHost) return Result.Failure("Chỉ chủ phòng được chốt lịch chung.")
                val draft = state.draft
                val selectedSet = draft.selectedProposals.toSet()
                if (selectedSet.isEmpty() || draft.orderedProposals.size != selectedSet.size || draft.orderedProposals.toSet() != selectedSet) {
                    return Result.Failure("Chọn và sắp đủ ít nhất một hoạt động trước khi chốt.")
                }
                val reason = action.reason.trim()
                if (reason.length > 500) {
                    return Result.Failure("Lý do tối đa 500 ký tự.")
                }

                // If ballots exist and draft order differs from Borda order, reason is required
                if (draft.orderBallots.isNotEmpty()) {
                    val borda = BordaCalculator.calculateBordaOrder(draft.selectedProposals, draft.orderBallots.values)
                    if (draft.orderedProposals != borda && reason.isEmpty()) {
                        return Result.Failure("Ghi lý do nếu lịch khác gợi ý thứ tự của nhóm.")
                    }
                }

                // Create snapshot of previous published plan if exists
                val history = state.planHistory.toMutableList()
                state.publishedPlan?.let { prev ->
                    history.add(
                        PlanSnapshot(
                            version = prev.version,
                            orderedProposals = prev.orderedProposals,
                            startTime = prev.startTime,
                            completedProposals = state.completedProposals.filter { it in prev.orderedProposals }.toSet(),
                            reason = prev.reason,
                            savedAtMillis = action.timestampMillis,
                            proposalsSnapshot = state.proposals.filter { it.id in prev.orderedProposals }
                        )
                    )
                }

                val nextVersion = state.currentPlanVersion + 1
                val newPublishedPlan = PublishedPlan(
                    version = nextVersion,
                    orderedProposals = draft.orderedProposals,
                    startTime = draft.draftStartTime,
                    completedProposals = state.completedProposals.filter { it in draft.orderedProposals }.toSet(),
                    reason = reason,
                    publishedAtMillis = action.timestampMillis
                )

                // Preserve completion for stops that remain in the new plan
                val preservedCompleted = state.completedProposals.filter { it in draft.orderedProposals }.toSet()

                Result.Success(
                    state.copy(
                        publishedPlan = newPublishedPlan,
                        planHistory = history,
                        completedProposals = preservedCompleted
                    )
                )
            }

            is PlanningAction.ReopenPlan -> {
                if (!action.isHost) return Result.Failure("Chỉ chủ phòng được mở lại kế hoạch.")
                val prevPlan = state.publishedPlan ?: return Result.Failure("Chưa có kế hoạch nào được chốt.")
                Result.Success(
                    state.copy(
                        draft = state.draft.copy(
                            selectedProposals = prevPlan.orderedProposals,
                            orderedProposals = prevPlan.orderedProposals,
                            draftStartTime = prevPlan.startTime
                        )
                    )
                )
            }

            is PlanningAction.ToggleCompletion -> {
                val plan = state.publishedPlan ?: return Result.Failure("Chưa có kế hoạch được chốt.")
                if (action.proposalId !in plan.orderedProposals) {
                    return Result.Failure("Hoạt động không thuộc lịch đang đi.")
                }
                val newCompleted = if (action.proposalId in state.completedProposals) {
                    state.completedProposals - action.proposalId
                } else {
                    state.completedProposals + action.proposalId
                }
                Result.Success(state.copy(completedProposals = newCompleted))
            }

            is PlanningAction.EndRoom -> {
                if (!action.isHost) return Result.Failure("Chỉ chủ phòng được kết thúc kèo.")
                if (state.publishedPlan == null) return Result.Failure("Chốt lịch chung trước khi kết thúc kèo.")
                Result.Success(
                    state.copy(
                        lifecycle = RoomLifecycle.ENDED,
                        endedAtMillis = action.timestampMillis
                    )
                )
            }
        }
    }
}
