package org.niatahl.tahlan.lostech

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.Global

/**
 * Show the AllMother offer dialog as soon as the player isn't in a dialog, then quit script.
 */
class AllmotherContactScript : EveryFrameScript {
    private var isDone = false
    override fun isDone() = isDone

    override fun runWhilePaused() = true

    override fun advance(amount: Float) {
        if (Global.getSector().campaignUI.isShowingDialog)
            return

        Global.getSector().campaignUI.showInteractionDialog(AllmotherOfferDialog().build(), Global.getSector().playerFleet)
        isDone = true
    }
}