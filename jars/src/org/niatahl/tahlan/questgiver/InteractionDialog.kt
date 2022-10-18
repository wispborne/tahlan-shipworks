package org.niatahl.tahlan.questgiver

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.combat.EngagementResultAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.questgiver.Questgiver.game

abstract class InteractionDialog<S : InteractionDialogLogic<S>> : InteractionDialogPlugin {
    abstract fun createInteractionDialogLogic(): S

    @Transient
    private var logic: S = setupInteractionDialogLogic()

    private fun setupInteractionDialogLogic(): S = createInteractionDialogLogic()

    /**
     * Called when the dialog is shown.
     */
    override fun init(dialog: InteractionDialogAPI) {
        logic.dialog = dialog
        val peopleInner = logic.people?.invoke(logic)

        if (peopleInner?.getOrNull(0) != null) {
            dialog.visualPanel.showPersonInfo(peopleInner[0], true)
        }

        if (peopleInner?.getOrNull(1) != null) {
            dialog.visualPanel.showSecondPerson(peopleInner[1])
        }

        if (peopleInner?.getOrNull(2) != null) {
            dialog.visualPanel.showThirdPerson(peopleInner[2])
        }

        logic.onInteractionStarted?.invoke(logic)

        if (logic.pages.any()) {
            logic.navigator.showPage(logic.pages.first())
        }
    }

    override fun optionSelected(optionText: String?, optionData: Any?) {
        val option = logic.pages.flatMap { it.options }
            .firstOrNull { it.id == optionData }

        if (optionText != null) {
            // Print out the text of the option the user just selected
            // If the color is default text color, use vanilla option selected color instead.
            val textColor = option
                ?.textColor
                .let {
                    if (it == null || it == Misc.getTextColor())
                        game.settings.getColor("buttonText")
                    else
                        it
                }
            logic.para(textColor = textColor) { optionText }
        }

        logic.navigator.onOptionSelected(optionText, optionData)

        if (option?.disableAutomaticHandling == false) {
            logic.navigator.refreshOptions()
        }
    }

    // Other overrides that are necessary but do nothing
    override fun optionMousedOver(optionText: String?, optionData: Any?) {
    }

    override fun getMemoryMap(): MutableMap<String, MemoryAPI> = mutableMapOf()
    override fun backFromEngagement(battleResult: EngagementResultAPI?) {
    }

    override fun advance(amount: Float) {
    }

    override fun getContext(): Any? = null
}