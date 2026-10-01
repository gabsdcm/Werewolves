package de.teamlapen.werewolves.client.core;

import de.teamlapen.werewolves.client.particle.ClawHitParticle;
import de.teamlapen.werewolves.core.ModParticles;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public final class ModParticleFactories {

    private ModParticleFactories() {
    }

    public static void registerFactories(@NotNull RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.WEREWOLF_CLAW_HIT.get(), ClawHitParticle.Provider::new);
    }
}
