package de.teamlapen.werewolves.core;

import de.teamlapen.werewolves.util.REFERENCE;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, REFERENCE.MODID);

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WEREWOLF_CLAW_HIT =
            PARTICLE_TYPES.register("werewolf_claw_hit", () -> new SimpleParticleType(true));

    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> WEREWOLF_CLAW_SHARPEN =
            PARTICLE_TYPES.register("werewolf_claw_sharpen", () -> new SimpleParticleType(true));

    private ModParticles() {
    }

    static void register(IEventBus bus) {
        PARTICLE_TYPES.register(bus);
    }
}
