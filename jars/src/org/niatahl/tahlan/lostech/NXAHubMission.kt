package org.niatahl.tahlan.lostech

import com.fs.starfarer.api.PluginPick
import com.fs.starfarer.api.campaign.*
import com.fs.starfarer.api.campaign.econ.MarketAPI
import com.fs.starfarer.api.campaign.rules.MemoryAPI
import com.fs.starfarer.api.characters.FullName
import com.fs.starfarer.api.characters.PersonAPI
import com.fs.starfarer.api.impl.campaign.ids.Conditions
import com.fs.starfarer.api.impl.campaign.ids.Factions
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes
import com.fs.starfarer.api.impl.campaign.ids.Tags
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithTriggers
import com.fs.starfarer.api.impl.campaign.missions.hub.ReqMode
import com.fs.starfarer.api.ui.TooltipMakerAPI
import com.fs.starfarer.api.util.Misc
import org.niatahl.tahlan.questgiver.*
import org.niatahl.tahlan.questgiver.Questgiver.game
import org.niatahl.tahlan.questgiver.wispLib.*
import org.niatahl.tahlan.utils.TahlanPeople
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
            get() = TahlanPeople.getPerson(TahlanPeople.CIEVE)
                    ?: game.factory.createPerson().apply { name = FullName("Cieve", "", FullName.Gender.FEMALE) }
        val allmother: PersonAPI
            get() = TahlanPeople.getPerson(TahlanPeople.FEARLESS) // TODO should be ALLMOTHER
                    ?: game.factory.createPerson().apply { name = FullName("ALLMOTHER", "", FullName.Gender.FEMALE) }
    }

    class State(val map: MutableMap<String, Any?>) {
        var seed: Random? by map
        var startDateMillis: Long? by map
        var startLocation: SectorEntityToken? by map
        var meetingPlanet: SectorEntityToken? by map
        var originalNXAStation: SectorEntityToken? by map
        var nxaSystem: StarSystemAPI? by map
        var completeDateInMillis: Long? by map
    }

    /**
     * All choices that can be made.
     * Leave `map` public and accessible so it can be cleared if the quest is restarted.
     */
    class Choices(val map: MutableMap<String, Any?>) {
        var someChoice: Boolean by map
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
        text.globalReplacementGetters["nxaStg1DestPlanet"] = { state.meetingPlanet?.name }
        text.globalReplacementGetters["nxaStg1DestSystem"] = { state.meetingPlanet?.starSystem?.baseName }
        text.globalReplacementGetters["nxaStarName"] = { state.meetingPlanet?.starSystem?.star?.name }
    }

    override fun create(createdAt: MarketAPI?, barEvent: Boolean): Boolean {
        super.create(createdAt, barEvent)
        state.seed = genRandom

        startingStage = Stage.GoMeetCieve
        setSuccessStage(Stage.Completed)
        setAbandonStage(Stage.Abandoned)

        name = game.text["nxa_missionName"]
        // TODO Nia
        setCreditReward(CreditReward.VERY_HIGH) // 95k ish.
        setGiverFaction(cieve.faction.id) // Rep reward.
        personOverride = cieve // Shows on intel, needed for rep reward or else crash.

        // TODO Nia
        setIconName(IInteractionLogic.Portrait(category = "intel", id = "red_planet").spriteName(game))

        state.startLocation = createdAt?.primaryEntity

        // TODO Nia
        // Pick where we meet Cieve.
        state.meetingPlanet = SystemFinder(includeHiddenSystems = false)
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

        // Pick station where the ship originally was. Our first, easy Lostech fight.
        state.originalNXAStation = SystemFinder()
                .requireEntityTags(ReqMode.ALL, Tags.STATION)
                .requireSystemHasAtLeastNumJumpPoints(min = 1)
                .requireSystemInterestingAndNotUnsafeOrCore()
                .preferEntityInDirectionOfOtherMissions()
                .preferEntityUndiscovered()
                .pickEntity()

        // Pick system where we find the NX-A. Our second, harder Lostech fight.
        state.nxaSystem = SystemFinder()
                .preferSystemWithinRangeOf(state.originalNXAStation?.locationInHyperspace, 10f)
                .requireSystemHasAtLeastNumJumpPoints(min = 1)
                .requireSystemInterestingAndNotUnsafeOrCore()
                .preferEntityInDirectionOfOtherMissions()
                .preferEntityUndiscovered()
                .pickSystem()

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
        makeImportant(state.meetingPlanet?.starSystem?.hyperspaceAnchor, null, Stage.GoMeetCieve)
        makePrimaryObjective(state.meetingPlanet?.starSystem?.hyperspaceAnchor)

        makeImportant(state.originalNXAStation, null, Stage.GoFindOriginalShipLocation)


        // TODO Nia look at fleet params, customize to liking.
        val lostechNXAFirstFleetFlag = "$${MISSION_ID}_lostechNXAFirstFleetFlag"

        // Create a Lostech fleet near the original NXA station for player to fight
        trigger {
            beginWithinHyperspaceRangeTrigger(
                    state.originalNXAStation?.starSystem,
                    1f,
                    true,
                    Stage.GoMeetCieve
            )

            triggerCreateFleet(
                    FleetSize.MEDIUM,
                    FleetQuality.LOWER,
                    Factions.NEUTRAL, // TODO change to Lostech
                    FleetTypes.PATROL_MEDIUM,
                    state.originalNXAStation
            )
            triggerMakeHostile()
            triggerMakeFleetIgnoreOtherFleetsExceptPlayer()
            triggerFleetNoAutoDespawn()
            triggerFleetNoJump()
            triggerMakeFleetIgnoredByOtherFleets()
            triggerAutoAdjustFleetStrengthModerate()
            triggerPickLocationAroundEntity(state.originalNXAStation, 1f)
            triggerSpawnFleetAtPickedLocation(lostechNXAFirstFleetFlag, null)
            triggerOrderFleetPatrol(false, state.originalNXAStation)
        }

        // Shortly after starting AwaitAllmotherOffer stage, open the dialog with her.
        // TODO check and make sure player isn't already in a dialog
        trigger {
            beginCustomTrigger(DaysElapsedChecker(0.25f, this), Stage.AwaitAllmotherOffer)
            triggerCustomAction { AllmotherOfferDialog().build().show(game.sector.campaignUI, game.sector.playerFleet) }
        }
    }

    override fun pickInteractionDialogPlugin(interactionTarget: SectorEntityToken): PluginPick<InteractionDialogPlugin>? =
            when {
                // Meeting Cieve
                currentStage == Stage.GoMeetCieve && interactionTarget.id == state.meetingPlanet?.id -> {
                    PluginPick(
                            MeetingCieveDialog().build(),
                            CampaignPlugin.PickPriority.MOD_SPECIFIC
                    )
                }
                // Found original NX-A location
                currentStage == Stage.GoFindOriginalShipLocation && interactionTarget.id == state.originalNXAStation?.id -> {
                    PluginPick(
                            LostechStationDialog().build(),
                            CampaignPlugin.PickPriority.MOD_SPECIFIC
                    )
                }
                // Found the NX-A
                currentStage == Stage.TrackDownAndRecoverNXA
                        && interactionTarget is CampaignFleetAPI
                        && interactionTarget.flagship.hullId == "tahlan_nxa" -> {
                    PluginPick(
                            RecoverNXADialog().build(),
                            CampaignPlugin.PickPriority.MOD_SPECIFIC
                    )
                }
                // Back at origin
                currentStage == Stage.ReturnToOrigin
                        && interactionTarget.id == state.startLocation?.id -> {
                    PluginPick(
                            DebriefWithCieveDialog().build(),
                            CampaignPlugin.PickPriority.MOD_SPECIFIC
                    )
                }

                else -> null
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
            Stage.GoMeetCieve -> {
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
            Stage.GoMeetCieve -> {
                info.addPara { game.text["nxa_missionSubtitle"].replacePlaceholders() }
            }
        }
    }

    enum class Stage {
        GoMeetCieve,
        GoFindOriginalShipLocation,
        TrackDownAndRecoverNXA,
        AwaitAllmotherOffer,
        ReturnToOrigin,
        Completed,
        Abandoned,
    }
}