package de.teamlapen.werewolves.entities.goals;

import de.teamlapen.werewolves.core.ModSounds;
import de.teamlapen.werewolves.mixin.LivingEntityAccessor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Leap variant that only triggers at a distance and enforces a per mob cooldown, so werewolves close
 * enough for melee keep attacking instead of jumping repeatedly. Mirrors the player LeapAction's physics
 * (jump power scaled by the werewolf form's leap modifier, added on top of the base jump) so the mob leap
 * covers the same distance as the player's.
 */
public class WerewolfLeapGoal extends Goal {

    private static final Logger LOGGER = LogManager.getLogger();

    private final Mob mob;
    private final float leapModifier;
    private final double minDistanceSqr;
    private final double maxDistanceSqr;
    private final int cooldownTicks;

    @Nullable
    private LivingEntity target;
    private int nextLeapTick;

    public WerewolfLeapGoal(Mob mob, float leapModifier, double minDistance, double maxDistance, int cooldownTicks) {
        this.mob = mob;
        this.leapModifier = leapModifier;
        this.minDistanceSqr = minDistance * minDistance;
        this.maxDistanceSqr = maxDistance * maxDistance;
        this.cooldownTicks = cooldownTicks;
        this.setFlags(EnumSet.of(Goal.Flag.JUMP));
    }

    @Override
    public boolean canUse() {
        this.target = this.mob.getTarget();
        boolean debugTick = this.target != null && this.mob.tickCount % 20 == 0;
        if (this.target == null) {
            return false;
        }
        if (!this.mob.onGround()) {
            if (debugTick) {
                LOGGER.info("[WerewolfLeapGoal DEBUG] {} skip: not onGround", this.mob);
            }
            return false;
        }
        if (this.mob.tickCount < this.nextLeapTick) {
            if (debugTick) {
                LOGGER.info("[WerewolfLeapGoal DEBUG] {} skip: cooldown, {} ticks left", this.mob, this.nextLeapTick - this.mob.tickCount);
            }
            return false;
        }
        double distanceSqr = this.mob.distanceToSqr(this.target);
        boolean inBand = distanceSqr >= this.minDistanceSqr && distanceSqr <= this.maxDistanceSqr;
        if (debugTick) {
            LOGGER.info("[WerewolfLeapGoal DEBUG] {} distance={} band=[{}, {}] inBand={}",
                    this.mob, Math.sqrt(distanceSqr), Math.sqrt(this.minDistanceSqr), Math.sqrt(this.maxDistanceSqr), inBand);
        }
        return inBand;
    }

    @Override
    public boolean canContinueToUse() {
        return !this.mob.onGround();
    }

    @Override
    public void start() {
        if (this.target == null) {
            return;
        }
        this.nextLeapTick = this.mob.tickCount + this.cooldownTicks;
        Vec3 movement = this.mob.getDeltaMovement();
        Vec3 direction = new Vec3(this.target.getX() - this.mob.getX(), 0.0, this.target.getZ() - this.mob.getZ());
        if (direction.lengthSqr() > 1.0E-7) {
            direction = direction.normalize();
        }
        float jumpPower = ((LivingEntityAccessor) this.mob).getJumpPower$werewolves();
        Vec3 leap = new Vec3(direction.x, jumpPower * 0.5, direction.z).scale(this.leapModifier);
        LOGGER.info("[WerewolfLeapGoal DEBUG] {} LEAP START jumpPower={} leapModifier={} leap={}", this.mob, jumpPower, this.leapModifier, leap);
        this.mob.playSound(ModSounds.ENTITY_WEREWOLF_GROWL.get(), 1.5F, 0.8F);
        this.mob.setDeltaMovement(movement.x + leap.x, jumpPower + leap.y, movement.z + leap.z);
    }
}
