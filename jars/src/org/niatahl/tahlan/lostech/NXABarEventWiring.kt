package org.niatahl.tahlan.lostech

import org.niatahl.tahlan.questgiver.BarEventWiring
import org.niatahl.tahlan.questgiver.QGBarEventCreator

class NXABarEventWiring :
    BarEventWiring<NXAHubMission>(missionId = NXAHubMission.MISSION_ID, isPriority = false) {
    override fun createBarEventLogic() = NXABarEvent()
    override fun createMission() = NXAHubMission()
    override fun shouldBeAddedToBarEventPool() = NXAHubMission.state.startDateMillis == null // ugh...
    override fun createBarEventCreator() = NXABarEventCreator(this)
    class NXABarEventCreator(wiring: NXABarEventWiring) : QGBarEventCreator<NXAHubMission>(wiring)
}