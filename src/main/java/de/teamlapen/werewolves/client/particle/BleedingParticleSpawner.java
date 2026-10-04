package de.teamlapen.werewolves.client.particle;

import de.teamlapen.werewolves.core.ModEffects;
import de.teamlapen.werewolves.core.ModParticles;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class BleedingParticleSpawner {

    private static final int SPAWN_INTERVAL_TICKS = 2;
    private static final int MAX_DROPS_PER_BURST = 2;
    private static final double MIN_HEIGHT_FRACTION = 0.65D;
    private static final double HEIGHT_FRACTION_VARIATION = 0.10D;
    private static final double TORSO_WIDTH_FRACTION = 0.6D;

    @SubscribeEvent
    public void onEntityTickPost(@NotNull EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity entity) || !entity.level().isClientSide()) {
            return;
        }
        if (entity.tickCount % SPAWN_INTERVAL_TICKS != 0 || !entity.hasEffect(ModEffects.BLEEDING)) {
            return;
        }
        int drops = 1 + entity.getRandom().nextInt(MAX_DROPS_PER_BURST);
        for (int i = 0; i < drops; i++) {
            this.spawnDrop(entity);
        }
    }

    private void spawnDrop(@NotNull LivingEntity entity) {
        double heightFraction = MIN_HEIGHT_FRACTION + entity.getRandom().nextDouble() * HEIGHT_FRACTION_VARIATION;
        entity.level().addParticle(
                ModParticles.BLEEDING_DROP.get(),
                entity.getRandomX(TORSO_WIDTH_FRACTION),
                entity.getY() + entity.getBbHeight() * heightFraction,
                entity.getRandomZ(TORSO_WIDTH_FRACTION),
                0.0D, 0.0D, 0.0D
        );
    }
}
