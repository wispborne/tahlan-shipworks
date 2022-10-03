package data.scripts.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;

import static data.scripts.TahlanUtils.Utils.txt;

public class tahlan_VundAutoforge extends BaseHullMod {

    @Override
    public String getDescriptionParam(int index, ShipAPI.HullSize hullSize) {
        if ( index == 0 ) return txt("hmd_VundForge1");
        if ( index == 1 ) return txt("hmd_VundForge2");
        if ( index == 2 ) return txt("hmd_VundForge3");
        if ( index == 3 ) return txt("hmd_VundForge4");
        return null;
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
