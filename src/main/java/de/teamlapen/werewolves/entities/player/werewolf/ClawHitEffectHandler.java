package de.teamlapen.werewolves.entities.player.werewolf;

import de.teamlapen.werewolves.core.ModActions;
import de.teamlapen.werewolves.core.ModParticles;
import de.teamlapen.werewolves.items.WerewolfClawItem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

public class ClawHitEffectHandler {

    private static final int PARTICLE_COUNT = 1;
    private static final double PARTICLE_Y_OFFSET = 0.5D;
    private static final double SURFACE_MARGIN = 0.08D;
    private static final double LATERAL_OFFSET_FACTOR = 0.22D;
    private static final Map<UUID, Boolean> NEXT_LEFT_BY_PLAYER = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F || event.getEntity().level().isClientSide()) {
            return;
        }

        if (!(event.getSource().is(DamageTypes.PLAYER_ATTACK)
                && event.getSource().getEntity() instanceof Player attacker
                && isClawAttack(attacker))) {
            return;
        }

        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean leftSide = NEXT_LEFT_BY_PLAYER.merge(attacker.getUUID(), true, (previous, ignored) -> !previous);
        Vec3 particlePosition = findSurfacePosition(target, attacker, leftSide);
        serverLevel.sendParticles(
                ModParticles.WEREWOLF_CLAW_HIT.get(),
                particlePosition.x,
                particlePosition.y,
                particlePosition.z,
                PARTICLE_COUNT,
                0.0D,
                0.0D,
                0.0D,
                0.0D);
    }

    private static Vec3 findSurfacePosition(LivingEntity target, Player attacker, boolean leftSide) {
        AABB bounds = target.getBoundingBox();
        Vec3 targetCenter = bounds.getCenter();
        Vec3 outward = attacker.getEyePosition().subtract(targetCenter);
        if (outward.lengthSqr() < 1.0E-6D) {
            outward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            outward = outward.normalize();
        }

        Vec3 horizontalOutward = new Vec3(outward.x, 0.0D, outward.z);
        if (horizontalOutward.lengthSqr() < 1.0E-6D) {
            horizontalOutward = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalOutward = horizontalOutward.normalize();
        }

        Vec3 right = new Vec3(-horizontalOutward.z, 0.0D, horizontalOutward.x);
        double lateralOffset = Math.min(bounds.getXsize(), bounds.getZsize()) * LATERAL_OFFSET_FACTOR;
        Vec3 sideOffset = right.scale(leftSide ? -lateralOffset : lateralOffset);
        Vec3 rayStart = new Vec3(
                clamp(targetCenter.x + sideOffset.x, bounds.minX + 1.0E-4D, bounds.maxX - 1.0E-4D),
                clamp(targetCenter.y + target.getBbHeight() * (PARTICLE_Y_OFFSET - 0.5D), bounds.minY + 1.0E-4D, bounds.maxY - 1.0E-4D),
                clamp(targetCenter.z + sideOffset.z, bounds.minZ + 1.0E-4D, bounds.maxZ - 1.0E-4D));

        double distanceToSurface = distanceToAabbBoundary(bounds, rayStart, outward);
        return rayStart.add(outward.scale(distanceToSurface + SURFACE_MARGIN));
    }

    private static double distanceToAabbBoundary(AABB bounds, Vec3 origin, Vec3 direction) {
        double distance = Double.POSITIVE_INFINITY;
        if (direction.x > 1.0E-6D) {
            distance = Math.min(distance, (bounds.maxX - origin.x) / direction.x);
        } else if (direction.x < -1.0E-6D) {
            distance = Math.min(distance, (bounds.minX - origin.x) / direction.x);
        }
        if (direction.y > 1.0E-6D) {
            distance = Math.min(distance, (bounds.maxY - origin.y) / direction.y);
        } else if (direction.y < -1.0E-6D) {
            distance = Math.min(distance, (bounds.minY - origin.y) / direction.y);
        }
        if (direction.z > 1.0E-6D) {
            distance = Math.min(distance, (bounds.maxZ - origin.z) / direction.z);
        } else if (direction.z < -1.0E-6D) {
            distance = Math.min(distance, (bounds.minZ - origin.z) / direction.z);
        }
        return Double.isFinite(distance) && distance >= 0.0D ? distance : 0.0D;
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static boolean isClawAttack(Player attacker) {
        WerewolfPlayer werewolf = WerewolfPlayer.get(attacker);
        return werewolf.getForm().isTransformed()
                && werewolf.getActionHandler().isActionActive(ModActions.CLAW.get())
                && werewolf.getClawSlot().isActive()
                && werewolf.getClawSlot().getStack().getItem() instanceof WerewolfClawItem;
    }
}
