package com.uzuu.letplay.domain

import com.uzuu.letplay.domain.model.MoneyVnd
import com.uzuu.letplay.domain.model.PlanDraft
import com.uzuu.letplay.domain.model.Proposal
import com.uzuu.letplay.domain.model.ProposalCategory
import com.uzuu.letplay.domain.model.ProposalId
import com.uzuu.letplay.domain.model.PublishedPlan
import com.uzuu.letplay.domain.model.RoomLifecycle
import com.uzuu.letplay.domain.model.VoteValue
import com.uzuu.letplay.domain.rules.PlanningRules
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PlanningRulesTest {

    private val p1 = Proposal(
        id = ProposalId("1"),
        title = "Cà phê",
        estimatedCostPerPerson = MoneyVnd(50_000L),
        durationMinutes = 60,
        category = ProposalCategory.COFFEE
    )
    private val p2 = Proposal(
        id = ProposalId("2"),
        title = "Đi dạo",
        estimatedCostPerPerson = MoneyVnd.ZERO,
        durationMinutes = 30,
        category = ProposalCategory.PIN
    )

    private fun createInitialPlanState(): PlanningRules.PlanningState {
        val initialProposals = listOf(p1, p2)
        val published = PublishedPlan(
            version = 1,
            orderedProposals = listOf(p1.id, p2.id),
            startTime = "18:00"
        )
        return PlanningRules.PlanningState(
            lifecycle = RoomLifecycle.ACTIVE,
            proposals = initialProposals,
            publishedPlan = published,
            completedProposals = setOf(p1.id),
            votesByMember = mapOf("host" to mapOf(p1.id to VoteValue.VERY_WANT))
        )
    }

    @Test
    fun migAc05AddsAndVotesWhileTravellingPreservingPublishedItineraryAndCompletion() {
        var state = createInitialPlanState()
        val p3 = Proposal(id = ProposalId("3"), title = "Ăn kem", durationMinutes = 30)

        // 1. Add idea 3 while travelling
        val addResult = PlanningRules.reduce(state, PlanningRules.PlanningAction.AddProposal(p3))
        assertIs<PlanningRules.Result.Success>(addResult)
        state = addResult.newState

        // 2. Vote for idea 3
        val vote1 = PlanningRules.reduce(state, PlanningRules.PlanningAction.CastVote(p3.id, "host", VoteValue.VERY_WANT))
        assertIs<PlanningRules.Result.Success>(vote1)
        state = vote1.newState

        val vote2 = PlanningRules.reduce(state, PlanningRules.PlanningAction.CastVote(p3.id, "host", VoteValue.PASS))
        assertIs<PlanningRules.Result.Success>(vote2)
        state = vote2.newState

        // Verified: published order, completion, plan version are unchanged
        assertEquals(listOf(p1.id, p2.id), state.publishedPlan?.orderedProposals)
        assertEquals(setOf(p1.id), state.completedProposals)
        assertEquals(1, state.currentPlanVersion)
        assertEquals(VoteValue.PASS, state.votesByMember["host"]?.get(p3.id))
    }

    @Test
    fun keepsDraftSeparateAndSnapshotsNewPlan() {
        var state = createInitialPlanState()
        val p3 = Proposal(id = ProposalId("3"), title = "Ăn kem")
        state = (PlanningRules.reduce(state, PlanningRules.PlanningAction.AddProposal(p3)) as PlanningRules.Result.Success).newState

        // Reopen plan for editing draft
        state = (PlanningRules.reduce(state, PlanningRules.PlanningAction.ReopenPlan(isHost = true)) as PlanningRules.Result.Success).newState

        // Select p3 into draft
        state = (PlanningRules.reduce(state, PlanningRules.PlanningAction.SelectProposal(p3.id, isSelected = true, isHost = true)) as PlanningRules.Result.Success).newState

        // Change draft start time
        state = (PlanningRules.reduce(state, PlanningRules.PlanningAction.SetDraftStartTime("19:30", isHost = true)) as PlanningRules.Result.Success).newState

        // Published plan is untouched while draft is modified!
        assertEquals(listOf(p1.id, p2.id), state.publishedPlan?.orderedProposals)
        assertEquals("18:00", state.publishedPlan?.startTime)
        assertEquals(setOf(p1.id), state.completedProposals)

        // Now host seals the new plan
        val sealResult = PlanningRules.reduce(state, PlanningRules.PlanningAction.SealPlan(isHost = true, reason = "", timestampMillis = 1000L))
        assertIs<PlanningRules.Result.Success>(sealResult)
        state = sealResult.newState

        assertEquals(2, state.currentPlanVersion)
        assertEquals(listOf(p1.id, p2.id, p3.id), state.publishedPlan?.orderedProposals)
        assertEquals("19:30", state.publishedPlan?.startTime)
        assertEquals(setOf(p1.id), state.completedProposals)

        // Previous plan is archived in history snapshot
        assertEquals(1, state.planHistory.size)
        assertEquals(1, state.planHistory[0].version)
        assertEquals(listOf(p1.id, p2.id), state.planHistory[0].orderedProposals)
        assertEquals("18:00", state.planHistory[0].startTime)
    }

    @Test
    fun rejectsEditsToCurrentStopsAndToOtherMembersProposals() {
        val state = createInitialPlanState()

        // Cannot edit stop currently in published plan
        val editStop = PlanningRules.reduce(
            state,
            PlanningRules.PlanningAction.EditProposal(p1.id, editorIsOwner = true, title = "Đổi lịch lén", description = "")
        )
        assertIs<PlanningRules.Result.Failure>(editStop)

        // Cannot remove stop currently in published plan
        val removeStop = PlanningRules.reduce(
            state,
            PlanningRules.PlanningAction.RemoveProposal(p1.id, editorIsOwner = true)
        )
        assertIs<PlanningRules.Result.Failure>(removeStop)

        // Cannot edit if not owner
        val p3 = Proposal(id = ProposalId("3"), title = "Hát karaoke")
        val withP3 = (PlanningRules.reduce(state, PlanningRules.PlanningAction.AddProposal(p3)) as PlanningRules.Result.Success).newState
        val editNotOwner = PlanningRules.reduce(
            withP3,
            PlanningRules.PlanningAction.EditProposal(p3.id, editorIsOwner = false, title = "Sửa trộm", description = "")
        )
        assertIs<PlanningRules.Result.Failure>(editNotOwner)
    }

    @Test
    fun migAc06InvalidatesOnlyVotesForAnEditedUnscheduledIdea() {
        var state = createInitialPlanState()
        val p3 = Proposal(id = ProposalId("3"), title = "Ăn kem")
        state = (PlanningRules.reduce(state, PlanningRules.PlanningAction.AddProposal(p3)) as PlanningRules.Result.Success).newState

        // Vote on p3
        state = (PlanningRules.reduce(state, PlanningRules.PlanningAction.CastVote(p3.id, "host", VoteValue.VERY_WANT)) as PlanningRules.Result.Success).newState
        assertEquals(VoteValue.VERY_WANT, state.votesByMember["host"]?.get(p3.id))

        // Edit p3
        val editResult = PlanningRules.reduce(
            state,
            PlanningRules.PlanningAction.EditProposal(p3.id, editorIsOwner = true, title = "Ăn bánh", description = "")
        )
        assertIs<PlanningRules.Result.Success>(editResult)
        state = editResult.newState

        // p1 vote is preserved, p3 vote was reset/invalidated
        assertEquals(VoteValue.VERY_WANT, state.votesByMember["host"]?.get(p1.id))
        assertTrue(state.votesByMember["host"]?.containsKey(p3.id) == false)
    }

    @Test
    fun sealRequiresReasonIfDeviatingFromBordaOrdering() {
        var state = createInitialPlanState()
        state = (PlanningRules.reduce(state, PlanningRules.PlanningAction.ReopenPlan(isHost = true)) as PlanningRules.Result.Success).newState

        // Ballots suggest [p2, p1]
        state = (PlanningRules.reduce(
            state,
            PlanningRules.PlanningAction.SubmitOrderBallot("member1", listOf(p2.id, p1.id))
        ) as PlanningRules.Result.Success).newState

        // Draft order is [p1, p2], which differs from Borda suggestion [p2, p1]
        val sealWithoutReason = PlanningRules.reduce(
            state,
            PlanningRules.PlanningAction.SealPlan(isHost = true, reason = "", timestampMillis = 1000L)
        )
        assertIs<PlanningRules.Result.Failure>(sealWithoutReason)

        val sealWithReason = PlanningRules.reduce(
            state,
            PlanningRules.PlanningAction.SealPlan(isHost = true, reason = "Cà phê đóng cửa sớm", timestampMillis = 1000L)
        )
        assertIs<PlanningRules.Result.Success>(sealWithReason)
    }

    @Test
    fun locksContributionsAfterRoomEnded() {
        var state = createInitialPlanState()
        state = (PlanningRules.reduce(state, PlanningRules.PlanningAction.EndRoom(isHost = true, timestampMillis = 5000L)) as PlanningRules.Result.Success).newState
        assertEquals(RoomLifecycle.ENDED, state.lifecycle)

        // Actions fail once ended
        val voteResult = PlanningRules.reduce(state, PlanningRules.PlanningAction.CastVote(p1.id, "host", VoteValue.VERY_WANT))
        assertIs<PlanningRules.Result.Failure>(voteResult)

        val addResult = PlanningRules.reduce(state, PlanningRules.PlanningAction.AddProposal(Proposal(ProposalId("99"), "Mới")))
        assertIs<PlanningRules.Result.Failure>(addResult)
    }
}
