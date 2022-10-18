package org.niatahl.tahlan.lostech

import com.fs.starfarer.api.campaign.InteractionDialogAPI
import com.fs.starfarer.api.campaign.SectorEntityToken
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.questgiver.*
import org.niatahl.tahlan.questgiver.Questgiver.game
import org.niatahl.tahlan.questgiver.wispLib.*
import java.awt.Color
import java.util.*

class NXAHubMission : QGHubMissionWithBarEvent(MISSION_ID) {
    companion object {
        val MISSION_ID = "nxa"

        val state = State(PersistentMapData<String, Any?>(key = "nxaState").withDefault { null })
        // TODO in case you wanna have the player make choices that get saved.
        val choices: Choices =
            Choices(PersistentMapData<String, Any?>(key = "nxaChoices").withDefault { null })
        val tags = listOf(Tags.INTEL_STORY, Tags.INTEL_ACCEPTED)
        val cieve: PersonAPI
            get() = game.sector.playerFaction.createRandomPerson() // TODO
    }

    class State(val map: MutableMap<String, Any?>) {
        var seed: Random? by map
        var startDateMillis: Long? by map
        var startLocation: SectorEntityToken? by map
        var cievePlanet: SectorEntityToken? by map
        var completeDateInMillis: Long? by map
    }

    /**
     * All choices that can be made.
     * Leave `map` public and accessible so it can be cleared if the quest is restarted.
     */
    class Choices(val map: MutableMap<String, Any?>) {
        var askedWhyNotBuyOwnShip by map
    }

    init {
        missionId = MISSION_ID
    }

    override fun shouldShowAtMarket(market: MarketAPI?): Boolean {
        return state.startDateMillis == null // todo
    }

    override fun onGameLoad() {
        super.onGameLoad()

        if (isDevMode()) {
            updateTextReplacements(game.text)
        }
    }

    override fun updateTextReplacements(text: Text) {
        eatBugs { game.text.resourceBundles.remove(ResourceBundle.getBundle("nxa")) }
        game.text.resourceBundles.add(ResourceBundle.getBundle("nxa"))
        text.globalReplacementGetters["nxaCredits"] = { Misc.getDGSCredits(creditsReward.toFloat()) }
        text.globalReplacementGetters["nxaStg1DestPlanet"] = { state.cievePlanet?.name }
        text.globalReplacementGetters["nxaStg1DestSystem"] = { state.cievePlanet?.starSystem?.baseName }
        text.globalReplacementGetters["nxaStarName"] = { state.cievePlanet?.starSystem?.star?.name }
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        // Ignore warning, there are two overrides and it's complaining about just one of them.
        @Suppress("ABSTRACT_SUPER_CALL_WARNING")
        super.create(createdAt, barEvent)
        state.seed = genRandom

        startingStage = Stage.GoToScanMissionDest
        setSuccessStage(Stage.Completed)
        setAbandonStage(Stage.Abandoned)

        name = game.text["nxa_missionName"]
        // TODO Nia
        setCreditReward(CreditReward.VERY_HIGH) // 95k ish, we want the player to take this.
        setGiverFaction(cieve.faction.id) // Rep reward.
        personOverride = cieve // Shows on intel, needed for rep reward or else crash.

        // TODO Nia
        setIconName(IInteractionLogic.Portrait(category = "intel", id = "red_planet").spriteName(game))

        state.startLocation = createdAt?.primaryEntity

        // TODO Nia
        state.cievePlanet = SystemFinder(includeHiddenSystems = false)
            .requireSystemOnFringeOfSector()
            .requireSystemHasAtLeastNumJumpPoints(min = 1)
            .requirePlanetNotGasGiant()
            .requirePlanetNotStar()
            .preferMarketConditions(ReqMode.ALL, Conditions.HABITABLE)
            .preferEntityUndiscovered()
            .preferSystemNotPulsar()
            .preferSystemTags(ReqMode.NOT_ANY, Tags.THEME_REMNANT, Tags.THEME_UNSAFE)
            .pickPlanet()
            ?: kotlin.run { game.logger.w { "Unable to find a planet for ${this.name}." }; return false }


        return true
    }

    override fun acceptImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.acceptImpl(dialog, memoryMap)

        val startLocation = dialog?.interactionTarget
            ?: kotlin.run {
                game.logger.e { "Aborting acceptance of ${this.name} because dialog was null." }
                abort()
                return
            }

        state.startLocation = startLocation
        game.logger.i { "${this.name} start location set to ${startLocation.fullName} in ${startLocation.starSystem.baseName}" }
        state.startDateMillis = game.sector.clock.timestamp

        // Sets the system as the map objective.
        makeImportant(state.cievePlanet?.starSystem?.hyperspaceAnchor, null, Stage.GoToScanMissionDest)
        makePrimaryObjective(state.cievePlanet?.starSystem?.hyperspaceAnchor)

//        trigger {
//            beginWithinHyperspaceRangeTrigger(state.cievePlanet?.starSystem, 1f, true, Stage.GoToScanMissionDest)
//
//            triggerCustomAction {
//                Telo1CompleteDialog().build().show(game.sector.campaignUI, game.sector.playerFleet)
//                game.sector.playerFleet.clearAssignments()
//            }
//        }
    }

    override fun endSuccessImpl(dialog: InteractionDialogAPI?, memoryMap: MutableMap<String, MemoryAPI>?) {
        super.endSuccessImpl(dialog, memoryMap)

        state.completeDateInMillis = game.sector.clock.timestamp

        // Credit reward is automatically given and shown.
    }

    override fun endAbandonImpl() {
        super.endAbandonImpl()
        game.logger.i { "Abandoning ${this.name} quest." }

        state.map.clear()
        setCurrentStage(null, null, null)
    }

    /**
     * Bullet points on left side of intel.
     */
    override fun addNextStepText(info: TooltipMakerAPI, tc: Color, pad: Float): Boolean {
        return when (currentStage) {
            Stage.GoToScanMissionDest -> {
                info.addPara(pad, tc) {
                    game.text["nxa_missionSubtitle"].replacePlaceholders()
                }
                true
            }

            else -> false
        }
    }

    /**
     * Description on right side of intel.
     */
    override fun addDescriptionForCurrentStage(info: TooltipMakerAPI, width: Float, height: Float) {
        // TODO
        when (currentStage) {
            Stage.GoToScanMissionDest -> {
                info.addPara { game.text["nxa_missionSubtitle"].replacePlaceholders() }
            }
        }
    }

    enum class Stage {
        GoToScanMissionDest,
        GoFindOriginalShipLocation,
        TrackDownNXA,
        LostechDefeated,
        ReturnToOrigin,
        Completed,
        Abandoned,
    }
}