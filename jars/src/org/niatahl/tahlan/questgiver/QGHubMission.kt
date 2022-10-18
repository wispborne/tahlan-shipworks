package org.niatahl.tahlan.questgiver

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.BaseCampaignPlugin
import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.InteractionDialogPlugin
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.comm.IntelInfoPlugin
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.impl.campaign.CommRelayEntityPlugin.CommSnifferReadableIntel
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission
import com.fs.starfarer.api.impl.campaign.missions.hub.BaseHubMission.Abortable
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMission
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.rulecmd.CallEvent.CallableEvent
import com.fs.starfarer.api.ui.TooltipMakerAPI
import org.niatahl.tahlan.questgiver.Questgiver.game
import org.niatahl.tahlan.questgiver.wispLib.Text
import java.awt.Color
import kotlin.random.Random

/**
 * A [HubMission] with a few extra features, such as [onGameLoad] and [pickInteractionDialogPlugin].
 * Usage is the same as a regular [HubMission].
 */
interface IQGHubMission : HubMission, IntelInfoPlugin, CallableEvent, EveryFrameScript,
    CommSnifferReadableIntel {
    fun updateTextReplacements(text: Text)

    /**
     * Creates the hub mission and returns false if it cannot be created.
     * Add things here that are needed to exist to offer the mission to the player.
     *
     * This method doesn't exist in [HubMission], but is added by [BaseHubMission].
     */
    fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        updateTextReplacements(game.text)
        return true
    }

    /**
     * Called when a save game is loaded.
     */
    fun onGameLoad()

    /**
     * Handle interactions or return null to ignore.
     * HubMission already has tons of methods and overrides, what's one more?
     */
    fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken): PluginPick<InteractionDialogPlugin>? {
        return null
    }

    /**
     * Called when the player chooses to accept the mission.
     */
    override fun accept(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?)

    /**
     * Called when the mission should be cleaned up, potentially to be re-offered. Automatically aborts all [Abortable]s.
     */
    override fun abort()

}

/**
 * Equivalent of [HubMissionWithTriggers], with the extra features of [IQGHubMission].
 */
abstract class QGHubMission : HubMissionWithTriggers(), IQGHubMission {
    @Transient
    private var hasRunSinceGameLoad = false

    override fun advanceImpl(amount: Float) {
        super.advanceImpl(amount)

        if (!hasRunSinceGameLoad) {
            onGameLoad()
            hasRunSinceGameLoad = true
        }
    }

    override fun onGameLoad() {
        updateTextReplacements(game.text)
        registerPlugin()
    }

    private fun registerPlugin() {
        game.sector.registerPlugin(object : BaseCampaignPlugin() {
            // Choose random id each run since it's not kept in save, and we don't want diff HubMissions to use the same id.
            private val id = "Questgiver_Wisp_CampaignPlugin_${Random.nextInt()}"

            override fun getId(): String {
                return id
            }

            // No need to add to saves
            override fun isTransient(): Boolean = true

            /**
             * When the player interacts with a dialog, override the default interaction with a
             * mod-specific one if necessary.
             */
            override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken): PluginPick<InteractionDialogPlugin>? {
                return this@QGHubMission.pickInteractionDialogPlugin(interactionTarget)
            }
        })
    }

    /**
     * Bullet points on left side of intel.
     */
    abstract override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean

    /**
     * Description on right side of intel.
     */
    abstract override fun addDescriptionForCurrentStage(info: TooltipMakerAPI, width: Float, height: Float)
}

/**
 * Equivalent of [HubMissionWithBarEvent], with the extra features of [IQGHubMission].
 * Not actually a subclass of [HubMissionWithBarEvent] due to vanilla's use of inheritance over composition.
 * However, this should be a drop-in replacement for [HubMissionWithBarEvent] for use with Questgiver.
 */
abstract class QGHubMissionWithBarEvent(missionId: String) : QGHubMission(), IQGHubMission {
    init {
        super.missionId = missionId
    }

    abstract fun shouldShowAtMarket(market: MarketAPI?): Boolean
}