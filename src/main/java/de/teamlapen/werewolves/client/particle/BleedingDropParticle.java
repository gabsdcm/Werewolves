package de.teamlapen.werewolves.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BleedingDropParticle extends TextureSheetParticle {

    private static final int MIN_LIFETIME = 15;
    private static final int LIFETIME_VARIATION = 11;
    private static final float MIN_SIZE = 0.06F;
    private static final float SIZE_VARIATION = 0.04F;
    private static final float GRAVITY = 0.2F;
    private static final double LATERAL_SPEED = 0.005D;
    private static final float FADE_START_PROGRESS = 0.7F;

    private final SpriteSet sprites;

    private BleedingDropParticle(
            @NotNull ClientLevel level,
            double x,
            double y,
            double z,
            @NotNull SpriteSet sprites
    ) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.lifetime = MIN_LIFETIME + this.random.nextInt(LIFETIME_VARIATION);
        this.gravity = GRAVITY;
        this.hasPhysics = false;
        this.quadSize = MIN_SIZE + this.random.nextFloat() * SIZE_VARIATION;
        this.xd = (this.random.nextDouble() - 0.5D) * LATERAL_SPEED;
        this.yd = 0.0D;
        this.zd = (this.random.nextDouble() - 0.5D) * LATERAL_SPEED;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        this.setSpriteFromAge(this.sprites);

        float progress = (float) this.age / this.lifetime;
        if (progress > FADE_START_PROGRESS) {
            this.setAlpha(1.0F - (progress - FADE_START_PROGRESS) / (1.0F - FADE_START_PROGRESS));
        }
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<SimpleParticleType> {

        private final SpriteSet sprites;

        public Provider(@NotNull SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public @Nullable Particle createParticle(
                @NotNull SimpleParticleType type,
                @NotNull ClientLevel level,
                double x,
                double y,
                double z,
                double xSpeed,
                double ySpeed,
                double zSpeed
        ) {
            return new BleedingDropParticle(level, x, y, z, this.sprites);
        }
    }
}
