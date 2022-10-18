package org.niatahl.tahlan.questgiver

import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import com.thoughtworks.xstream.XStream
import org.niatahl.tahlan.questgiver.wispLib.QuestgiverServiceLocator
import org.niatahl.tahlan.questgiver.wispLib.ServiceLocator

object Questgiver {
    internal lateinit var hubMissionCreators: List<BarEventWiring<*>>

    /**
     * An idempotent method to initialize Questgiver with enough information to start up.
     *
     * @param modPrefix The mod prefix to use, without a trailing underscore.
     */
    fun init(modPrefix: String) {
        MOD_PREFIX = modPrefix
    }

    /**
     * Call this when a save game is loaded.
     * This refreshes the sector data so that the loaded game doesn't have any data from the previous save.
     */
    fun onGameLoad(
    ) {
        this.game = QuestgiverServiceLocator()
    }

    fun loadQuests(
        creators: List<BarEventWiring<*>>,
        configuration: Configuration
    ) {
        this.hubMissionCreators = creators

        game.configuration = configuration

        creators.forEach { creator ->
            BarEventManager.getInstance()
                .configureBarEventCreator(
                    shouldGenerateBarEvent = creator.shouldBeAddedToBarEventPool(),
                    barEventCreator = creator.createBarEventCreator(),
                    isStarted = !creator.shouldBeAddedToBarEventPool()
                )
        }

        QuestgiverEveryFrameScript.start()
    }

    /**
     * The mod prefix, without a trailing underscore.
     */
    internal lateinit var MOD_PREFIX: String


    /**
     * Singleton instance of the service locator. Set a new one of these for unit tests.
     */
    var game: ServiceLocator = QuestgiverServiceLocator()
        internal set

    fun configureXStream(x: XStream) {
        // DO NOT CHANGE THESE STRINGS, DOING SO WILL BREAK SAVE GAMES
        // No periods allowed in the serialized name, causes crash.
        val aliases = listOf(
            BarEvent::class to "BarEvent",
            BarEventLogic::class to "BarEventLogic",
            HubMissionBarEventWrapperWithoutRules::class to "HubMissionBarEventWrapperWithoutRules",
            IInteractionLogic::class to "IInteractionLogic",
            InteractionDialog::class to "InteractionDialog",
            InteractionDialogLogic::class to "InteractionDialogLogic",
            QGHubMission::class to "QGHubMission",
            QGHubMissionWithBarEvent::class to "QGHubMissionWithBarEvent",
        )

        // Prepend with mod prefix so the classes don't conflict with anything else getting serialized
        aliases.forEach { x.alias("${MOD_PREFIX}_${it.second}", it.first.java) }
    }
}