package org.niatahl.tahlan.questgiver

import com.fs.starfarer.api.impl.campaign.intel.bar.events.BarEventManager
import org.niatahl.tahlan.questgiver.wispLib.removeBarEventCreator


/**
 * If quest has not been started, ensures that the [BarEventManager] has the [barEventCreator].
 *
 * If quest has been started, ensures that the [BarEventManager] does not have an instance of [barEventCreator].
 */
fun BarEventManager.configureBarEventCreator(
    shouldGenerateBarEvent: Boolean,
    barEventCreator: BarEventManager.GenericBarEventCreator,
    isStarted: Boolean
) {
    val hasEventCreator =
        this.hasEventCreator(barEventCreator::class.java)

    if (!shouldGenerateBarEvent || isStarted) {
        if (hasEventCreator) {
            this.removeBarEventCreator(barEventCreator::class.java)
        }
    } else {
        if (this.creators.count { it::class.java == barEventCreator::class.java } > 1) {
            this.removeBarEventCreator(barEventCreator::class.java)
        }

        if (!hasEventCreator) {
            this.addEventCreator(barEventCreator)
        }
    }
}
