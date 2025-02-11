package org.niatahl.tahlan.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.combat.listeners.ApplyDamageResultAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import data.scripts.util.MagicLensFlare;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

import java.awt.*;

import static com.fs.starfarer.api.util.Misc.ZERO;

public class FountainOnHitEffect implements OnHitEffectPlugin {

    private static final Color PARTICLE_COLOR = new Color(255, 104, 68);
    private static final Color FLASH_COLOR = new Color(255, 234, 212);
    private static final int NUM_PARTICLES = 10;

    @Override
    public void onHit(DamagingProjectileAPI projectile, CombatEntityAPI target, Vector2f point, boolean shieldHit, ApplyDamageResultAPI damageResult, CombatEngineAPI engine) {

        //MagicLensFlare.createSharpFlare(engine, projectile.getSource(), projectile.getLocation(), 8, 400, 0, new Color(186, 240, 255), new Color(255, 255, 255));

        engine.addSmoothParticle(point, ZERO, 200f, 0.5f, 0.1f, PARTICLE_COLOR);
        engine.addHitParticle(point, ZERO, 100f, 0.5f, 0.25f, FLASH_COLOR);
        for (int x = 0; x < NUM_PARTICLES; x++) {
            engine.addHitParticle(point,
                    MathUtils.getPointOnCircumference(null, MathUtils.getRandomNumberInRange(50f, 150f), (float) Math.random() * 360f),
                    5f, 1f, MathUtils.getRandomNumberInRange(0.3f, 0.6f), PARTICLE_COLOR);
        }

    }
}
