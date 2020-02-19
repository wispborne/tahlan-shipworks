package data.scripts.shipsystems;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import org.lazywizard.lazylib.combat.CombatUtils;

import java.util.ArrayList;
import java.util.List;

public class tahlan_PlasmaRounds extends BaseShipSystemScript {

	public static final float DAMAGE_BONUS = 0.5f;
    private List<DamagingProjectileAPI> registeredProjectiles = new ArrayList<DamagingProjectileAPI>();
	
	public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {
		ShipAPI ship = null;
		boolean player = false;
		CombatEngineAPI engine = Global.getCombatEngine();

		if (stats.getEntity() instanceof ShipAPI) {
			ship = (ShipAPI) stats.getEntity();
			player = ship == Global.getCombatEngine().getPlayerShip();
		} else {
			return;
		}

		List<WeaponAPI> weapons = new ArrayList<WeaponAPI>();

		for (WeaponAPI w: ship.getAllWeapons()) {
		    if (w.getId().contains("tahlan_glanzwurf_pulse")) {
		        weapons.add(w);
            }
        }


        for (DamagingProjectileAPI proj : CombatUtils.getProjectilesWithinRange(ship.getLocation(), 100f)) {
            if (weapons.contains(proj.getWeapon()) && !registeredProjectiles.contains(proj)) {
                registeredProjectiles.add(proj);
                //Replace projectile with explosive version
                DamagingProjectileAPI newProj = (DamagingProjectileAPI) engine.spawnProjectile(ship,proj.getWeapon(),"tahlan_sturmkanone_m2", proj.getLocation(),proj.getFacing(),ship.getVelocity());
                newProj.getVelocity().set(proj.getVelocity());
                Global.getSoundPlayer().playSound("tahlan_sturm_fire", 1.1f, 0.9f, newProj.getLocation(), newProj.getVelocity());
                Global.getCombatEngine().removeEntity(proj);
                registeredProjectiles.add(newProj);
            }
        }

	}

	public void unapply(MutableShipStatsAPI stats, String id) {

	}
	
	public StatusData getStatusData(int index, State state, float effectLevel) {
		if (index == 0) {
			return new StatusData("Plasma rounds loaded", false);
		}
		return null;
	}
}