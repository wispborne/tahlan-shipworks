package org.niatahl.tahlan.shipsystems

import com.fs.starfarer.api.Global
import com.fs.starfarer.api.combat.MutableShipStatsAPI
import com.fs.starfarer.api.combat.ShipAPI
import com.fs.starfarer.api.impl.campaign.ids.Stats
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript
import com.fs.starfarer.api.plugins.ShipSystemStatsScript.StatusData
import java.awt.Color

class PhaseBreakerV2Stats : BaseShipSystemScript() {
    private var activeTime = 0f
    private var runOnce = false
    private var levelForAlpha = 1f
    protected var STATUSKEY2 = Any()

    protected fun maintainStatus(playerShip: ShipAPI, state: ShipSystemStatsScript.State?, effectLevel: Float) {
        val f = VULNERABLE_FRACTION
        var cloak = playerShip.phaseCloak
        if (cloak == null) cloak = playerShip.system
        if (cloak == null) return
        if (effectLevel > f) {
            Global.getCombatEngine().maintainStatusForPlayerShip(
                STATUSKEY2,
                cloak.specAPI.iconSpriteName, cloak.displayName, "time flow altered", false
            )
        }
    }

    override fun apply(stats: MutableShipStatsAPI, id: String, state: ShipSystemStatsScript.State, effectLevel: Float) {
        var id = id
        val ship: ShipAPI
        val player: Boolean
        if (stats.entity is ShipAPI) {
            ship = stats.entity as ShipAPI
            player = ship === Global.getCombatEngine().playerShip
            id = id + "_" + ship.id
        } else {
            return
        }
        if (player) {
            maintainStatus(ship, state, effectLevel)
        }
        if (Global.getCombatEngine().isPaused) {
            return
        }
        var cloak = ship.phaseCloak
        if (cloak == null) cloak = ship.system
        if (cloak == null) return
        if (state == ShipSystemStatsScript.State.COOLDOWN || state == ShipSystemStatsScript.State.IDLE) {
            unapply(stats, id)
            return
        }
        val engine = Global.getCombatEngine()
        activeTime += engine.elapsedInLastFrame

        var level = effectLevel

        when (state) {
            ShipSystemStatsScript.State.IN -> {
                levelForAlpha = effectLevel
            }
            ShipSystemStatsScript.State.ACTIVE -> {
                ship.isPhased = true
                levelForAlpha = effectLevel
            }
            ShipSystemStatsScript.State.OUT -> {
                Global.getSoundPlayer().playLoop("system_temporalshell_loop", ship, 1f, 1f, ship.location, ship.velocity)
                ship.isPhased = false
                levelForAlpha = (levelForAlpha - 2f * engine.elapsedInLastFrame).coerceAtLeast(0f)
                ship.setJitterUnder(id, Color(239, 40, 110, 80), 1f - levelForAlpha, 10, 8f)
                level = if (ship.fluxTracker.isVenting || ship.fluxTracker.isOverloaded) 0f else 1f
            }
            else -> {}
        }

        Global.getCombatEngine().maintainStatusForPlayerShip("tahlan_debug",cloak.getSpecAPI().getIconSpriteName(),"cloak","active: "+levelForAlpha,false);
        ship.extraAlphaMult = 1f - (1f - SHIP_ALPHA_MULT) * levelForAlpha
        ship.setApplyExtraAlphaToEngines(true)
        val shipTimeMult = 1f + (getMaxTimeMult(stats) - 1f) * level
        stats.timeMult.modifyMult(id, shipTimeMult)
        if (player) {
            Global.getCombatEngine().timeMult.modifyMult(id, 1f / shipTimeMult)
        } else {
            Global.getCombatEngine().timeMult.unmodify(id)
        }
        val speedPercentMod = stats.dynamic.getMod(Stats.PHASE_CLOAK_SPEED_MOD).computeEffective(0f)
        stats.maxSpeed.modifyPercent(id + "_skillmod", speedPercentMod * level)
    }

    override fun unapply(stats: MutableShipStatsAPI, id: String) {
        val ship: ShipAPI
        ship = if (stats.entity is ShipAPI) {
            stats.entity as ShipAPI
        } else {
            return
        }
        var cloak = ship.phaseCloak
        if (cloak == null) cloak = ship.system
        if (cloak == null) return
        runOnce = false
        ship.setJitterUnder(id, cloak.specAPI.effectColor2, 0f, 10, 2f)
        Global.getCombatEngine().timeMult.unmodify(id)
        stats.timeMult.unmodify(id)
        ship.isPhased = false
        ship.extraAlphaMult = 1f
        activeTime = 0f
    }

    override fun getStatusData(index: Int, state: ShipSystemStatsScript.State, effectLevel: Float): StatusData? {
        return null
    }

    companion object {
        const val SHIP_ALPHA_MULT = 0.25f
        const val VULNERABLE_FRACTION = 0f
        const val MAX_TIME_MULT = 3f
        fun getMaxTimeMult(stats: MutableShipStatsAPI): Float {
            return 1f + (MAX_TIME_MULT - 1f) * stats.dynamic.getValue(Stats.PHASE_TIME_BONUS_MULT)
        }
    }
}