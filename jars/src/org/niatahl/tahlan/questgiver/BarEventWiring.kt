package org.niatahl.tahlan.questgiver

import com.fs.starfarer.api.impl.campaign.intel.bar.events.BaseBarEventCreator
import com.fs.starfarer.api.impl.campaign.missions.hub.HubMissionWithBarEvent


/**
 * Pass to `QuestGiver.loadQuests`.
 * Creates and adds a [BaseBarEventCreator] to launch a bar event for the specified HubMission.
 */
abstract class BarEventWiring<H : QGHubMissionWithBarEvent>(val missionId: String, val isPriority: Boolean) {
    /**
     * Creates a [BaseBarEventCreator].
     */
    abstract fun createBarEventCreator(): QGBarEventCreator<H>

    /**
     * Creates the logic that drives the bar event (not saved).
     */
    abstract fun createBarEventLogic(): BarEventLogic<H>

    /**
     * Creates a [HubMissionWithBarEvent] that's passed into the [BarEventLogic].
     */
    abstract fun createMission(): H

    /**
     * Whether this bar event should be added to the bar event pool or not.
     * True if quest is enabled and never started, false otherwise.
     */
    abstract fun shouldBeAddedToBarEventPool(): Boolean
}

/**
 * A dedicated [BaseBarEventCreator] class is needed because vanilla uses class type as unique identifiers,
 * such as its remove method, which removes all creators of the specified type.
 */
abstract class QGBarEventCreator<H : QGHubMissionWithBarEvent>(private val wiring: BarEventWiring<H>) :
    BaseBarEventCreator() {
    /**
     * Creates the bar event (which gets saved).
     */
    override fun createBarEvent(): BarEvent<H> = object : BarEvent<H>(wiring.missionId) {
        override fun createBarEventLogic(): BarEventLogic<H> = wiring.createBarEventLogic()
        override fun createMission(): H = wiring.createMission()
    }

    override fun isPriority(): Boolean {
        return wiring.isPriority
    }
}