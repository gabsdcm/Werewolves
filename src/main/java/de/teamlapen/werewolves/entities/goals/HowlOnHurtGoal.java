package de.teamlapen.werewolves.entities.goals;

import de.teamlapen.werewolves.config.WerewolvesConfig;
import de.teamlapen.werewolves.core.ModSounds;
import de.teamlapen.werewolves.util.WUtils;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

/**
 * Howls when the mob is attacked by a new attacker and makes nearby werewolves target that attacker.
 */
public class HowlOnHurtGoal extends Goal {

    private final Mob mob;
    @Nullable
    private LivingEntity lastAttacker;
    private int nextHowlTick;

    public HowlOnHurtGoal(Mob mob) {
        this.mob = mob;
        this.setFlags(EnumSet.noneOf(Goal.Flag.class));
    }

    @Override
    public boolean canUse() {
        LivingEntity attacker = this.mob.getLastHurtByMob();
        if (attacker == null || !attacker.isAlive()) {
            this.lastAttacker = null;
            return false;
        }
        if (attacker == this.lastAttacker) {
            return false;
        }
        return this.mob.tickCount >= this.nextHowlTick;
    }

    @Override
    public boolean canContinueToUse() {
        return false;
    }

    @Override
    public void start() {
        LivingEntity attacker = this.mob.getLastHurtByMob();
        if (attacker == null) {
            return;
        }
        this.lastAttacker = attacker;
        this.nextHowlTick = this.mob.tickCount + WerewolvesConfig.BALANCE.MOBPROPS.werewolf_howl_aggro_cooldown.get();

        this.mob.playSound(ModSounds.ENTITY_WEREWOLF_HOWL.get(), 1.0F, 1.0F);

        AABB area = this.mob.getBoundingBox().inflate(WerewolvesConfig.BALANCE.MOBPROPS.werewolf_howl_aggro_radius.get());
        this.mob.level().getEntities(EntityTypeTest.forClass(Mob.class), area, WUtils.IS_WEREWOLF).forEach(werewolf -> {
            if (werewolf != this.mob && werewolf != attacker && werewolf.getTarget() != attacker && werewolf.canAttack(attacker)) {
                werewolf.setTarget(attacker);
            }
        });
    }
}
