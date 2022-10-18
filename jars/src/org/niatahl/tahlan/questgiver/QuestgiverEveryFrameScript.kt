package org.niatahl.tahlan.questgiver

import com.fs.starfarer.api.EveryFrameScript
import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import org.niatahl.tahlan.questgiver.Questgiver.game

internal object QuestgiverEveryFrameScript {
    fun start() {
        if (game.sector.transientScripts.none { it is Script }) {
            game.sector.addTransientScript(Script())
        }
    }

    fun stop() {
        if (game.sector.transientScripts.any { it is Script }) {
            game.sector.removeTransientScriptsOfClass(Script::class.java)
        }
    }

    internal class Script : EveryFrameScript {
        var timestamp = Long.MIN_VALUE

        override fun isDone(): Boolean = false

        override fun runWhilePaused(): Boolean = false

        /**
         * Called every frame.
         * @param time seconds elapsed during the last frame.
         */
        override fun advance(time: Float) {
            // Once per day
            if (game.sector.clock.getElapsedDaysSince(timestamp) >= 1) {
                timestamp = game.sector.clock.timestamp

                Questgiver.hubMissionCreators.forEach { qgHubMissionCreator ->
                    BarEventManager.getInstance()
                        .configureBarEventCreator(
                            shouldGenerateBarEvent = true,
                            barEventCreator = qgHubMissionCreator.createBarEventCreator(),
                            isStarted = !qgHubMissionCreator.shouldBeAddedToBarEventPool()
                        )
                }
            }
        }
    }
}