package org.niatahl.tahlan.questgiver

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEvent

/**
 * Custom Questgiver bar event, subclass of [BaseBarEvent]. Implement this.
 */
abstract class BarEvent<H : QGHubMissionWithBarEvent>(barEventSpecId: String) :
    HubMissionBarEventWrapperWithoutRules<H>(barEventSpecId) {
    abstract fun createBarEventLogic(): BarEventLogic<H>

    @Transient
    private var barEventLogic: BarEventLogic<H> = setupBarEventLogic()

    override fun readResolve(): Any {
        barEventLogic = setupBarEventLogic()

        return super.readResolve()
    }

    private fun setupBarEventLogic(): BarEventLogic<H> {
        return createBarEventLogic().also { logic ->
            logic.missionGetter = { this.mission!! }
        }
    }

    override fun shouldShowAtMarket(market: MarketAPI?): Boolean =
        super.shouldShowAtMarket(market)
                && (market?.isValidQuestTarget ?: true)
                && mission?.result == null

    /**
     * Set up the text that appears when the player goes to the bar
     * and the option for them to init the conversation.
     */
    override fun addPromptAndOption(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI?>) {
        super.addPromptAndOption(dialog, memoryMap)
//            definition.manOrWoman = manOrWoman
//            definition.hisOrHer = hisOrHer
//            definition.heOrShe = heOrShe
        barEventLogic.dialog = dialog
        barEventLogic.event = this
        barEventLogic.createInteractionPrompt.invoke(barEventLogic)

        val option = barEventLogic.textToStartInteraction.invoke(barEventLogic)

        if (option.textColor != null) {
            dialog.optionPanel.addOption(
                option.text,
                this as BaseBarEvent,
                option.textColor,
                option.tooltip
            )
        } else {
            dialog.optionPanel.addOption(
                option.text,
                this as BaseBarEvent,
                option.tooltip
            )
        }
    }

    /**
     * Called when the player chooses to start the conversation.
     */
    override fun init(dialog: InteractionDialogAPI, memoryMap: MutableMap<String, MemoryAPI>) {
        super.init(dialog, memoryMap)
        barEventLogic.dialog = dialog
        barEventLogic.event = this

//            if (firstPerson?.name != null) {
//                this.person.apply { name = firstPerson.name }
//            }
        val people = barEventLogic.people?.invoke(barEventLogic)

        people?.forEachIndexed { index, person ->
            when (index) {
                0 -> dialog.visualPanel.showPersonInfo(person)
                1 -> dialog.visualPanel.showSecondPerson(person)
                2 -> dialog.visualPanel.showThirdPerson(person)
            }
        }

        // Set bar event close logic.
        barEventLogic.closeBarEvent = { doNotOfferAgain ->
            if (doNotOfferAgain) {
                BarEventManager.getInstance().notifyWasInteractedWith(this)
            }

            done = true
            noContinue = true
        }

        this.done = false
        this.noContinue = false

        barEventLogic.onInteractionStarted?.invoke(barEventLogic)

        if (barEventLogic.pages.any()) {
            showPage(barEventLogic.pages.first())
        }
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        barEventLogic.navigator.onOptionSelected(optionText, optionData)
    }

    fun showPage(page: IInteractionLogic.Page<BarEventLogic<H>>) {
        if (noContinue || done) return

        barEventLogic.navigator.showPage(page)
    }
}