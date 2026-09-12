package de.teamlapen.werewolves.entities.werewolf;

import de.teamlapen.vampirism.api.difficulty.IAdjustableLevel;
import de.teamlapen.vampirism.api.entity.player.refinement.IRefinementSet;
import de.teamlapen.vampirism.api.items.IRefinementItem;
import de.teamlapen.vampirism.entity.VampirismEntity;
import de.teamlapen.werewolves.api.entities.werewolf.IWerewolfMob;
import de.teamlapen.werewolves.config.BalanceConfig;
import de.teamlapen.werewolves.config.WerewolvesConfig;
import de.teamlapen.werewolves.core.ModEffects;
import de.teamlapen.werewolves.core.ModItems;
import de.teamlapen.werewolves.core.ModRefinementSets;
import de.teamlapen.werewolves.core.ModSounds;
import de.teamlapen.werewolves.items.WerewolfRefinementItem;
import de.teamlapen.werewolves.util.DamageHandler;
import de.teamlapen.werewolves.util.FormHelper;
import de.teamlapen.werewolves.world.ModDamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class WerewolfBaseEntity extends VampirismEntity implements IWerewolfMob {

    private final boolean countAsMonsterForSpawn;
    private int healthRegenTimer;

    public WerewolfBaseEntity(EntityType<? extends VampirismEntity> type, Level world, boolean countAsMonsterForSpawn) {
        super(type, world);
        this.countAsMonsterForSpawn = countAsMonsterForSpawn;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putBoolean("appliedUpgradedBite", this.appliedUpgradedBite);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        this.appliedUpgradedBite = nbt.getBoolean("appliedUpgradedBite");
    }

    public static boolean spawnPredicateWerewolf(EntityType<? extends WerewolfBaseEntity> entityType, ServerLevelAccessor world, MobSpawnType spawnReason, BlockPos blockPos, RandomSource random) {
        if (world.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        if (spawnReason == MobSpawnType.EVENT) return true;
        if (!Monster.isDarkEnoughToSpawn(world, blockPos, random) && !FormHelper.isInWerewolfBiome(world, blockPos)) return false;
        return Mob.checkMobSpawnRules(entityType, world, spawnReason, blockPos, random);
    }

    public void bite(LivingEntity entity) {
        //TODO take a look at ExtendedCreature#onBite
    }

    private static final List<DeferredHolder<IRefinementSet, IRefinementSet>> BITE_REFINEMENT_SETS = List.of(ModRefinementSets.STUN_BITE_SET, ModRefinementSets.BLEEDING_BITE_SET, ModRefinementSets.VARIABLE_BITE_SET);

    private static final int BITE_COOLDOWN = 160;
    private static final int STUN_BASE = 30;
    private static final int STUN_PER_LEVEL = 10;
    private static final int STUN_MAX = 60;
    private static final int BLEED_BASE = 60;
    private static final int BLEED_PER_LEVEL = 20;
    private static final int BLEED_MAX = 90;

    private int biteCooldown;
    private boolean appliedUpgradedBite;

    /**
     * The whole bite (damage, sound and the stun/bleeding roll) is gated by a single cooldown, so a werewolf can only land one bite every {@value #BITE_COOLDOWN} ticks.
     * The cooldown is only consumed when the damage actually goes through.
     */
    protected boolean applyBiteEffects(LivingEntity target) {
        BalanceConfig.MobProps config = WerewolvesConfig.BALANCE.MOBPROPS;
        if (this.biteCooldown > 0) {
            return false;
        }
        if (!DamageHandler.hurtModded(target, (ModDamageSources sources) -> sources.bite(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            return false;
        }
        this.setLastHurtMob(target);
        this.playSound(ModSounds.ENTITY_WEREWOLF_BITE.get(), 1.0F, 1.0F);
        this.biteCooldown = BITE_COOLDOWN;
        int level = this instanceof IAdjustableLevel adjustable ? Math.max(0, adjustable.getEntityLevel()) : 0;
        if (this.random.nextFloat() >= config.werewolf_bite_effect_chance.get() + config.werewolf_bite_effect_chance_pl.get() * level) {
            return true;
        }
        int stunDuration = Math.min(STUN_BASE + STUN_PER_LEVEL * level, STUN_MAX);
        int bleedingDuration = Math.min(BLEED_BASE + BLEED_PER_LEVEL * level, BLEED_MAX);
        if (this.random.nextFloat() < config.werewolf_upgraded_bite_chance.get() + config.werewolf_upgraded_bite_chance_pl.get() * level) {
            this.appliedUpgradedBite = true;
            target.addEffect(new MobEffectInstance(ModEffects.STUN, stunDuration, 1));
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, bleedingDuration, 1));
        } else if (this.random.nextBoolean()) {
            target.addEffect(new MobEffectInstance(ModEffects.STUN, stunDuration));
        } else {
            target.addEffect(new MobEffectInstance(ModEffects.BLEEDING, bleedingDuration));
        }
        return true;
    }

    @Override
    protected void dropCustomDeathLoot(@NotNull ServerLevel level, @NotNull DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (!this.appliedUpgradedBite) return;
        if (this.random.nextFloat() >= WerewolvesConfig.BALANCE.MOBPROPS.werewolf_upgraded_bite_trinket_chance.get()) return;
        IRefinementItem.AccessorySlotType[] slots = IRefinementItem.AccessorySlotType.values();
        WerewolfRefinementItem item = WerewolfRefinementItem.getRefinementItem(slots[this.random.nextInt(slots.length)]);
        ItemStack stack = new ItemStack(item);
        if (item.applyRefinementSet(stack, BITE_REFINEMENT_SETS.get(this.random.nextInt(BITE_REFINEMENT_SETS.size())).get())) {
            this.spawnAtLocation(stack);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }
        if (!this.level().isClientSide()) {
            this.tickHealthRegeneration();
        }
    }

    protected void tickHealthRegeneration() {
        if (this.getHealth() <= 0 || this.getHealth() >= this.getMaxHealth()) {
            this.healthRegenTimer = 0;
            return;
        }
        if (++this.healthRegenTimer < WerewolvesConfig.BALANCE.MOBPROPS.werewolf_regen_interval.get()) return;
        this.healthRegenTimer = 0;
        this.heal(WerewolvesConfig.BALANCE.MOBPROPS.werewolf_regen_amount.get().floatValue());
    }

    /**
     * Stocks the offhand with food the entity can eat mid fight. Golden apples are the rare case, raw meat the common one.
     */
    protected void stockHealingFood() {
        if (!this.getItemBySlot(EquipmentSlot.OFFHAND).isEmpty()) return;
        if (this.getRandom().nextDouble() >= WerewolvesConfig.BALANCE.MOBPROPS.werewolf_food_spawn_chance.get()) return;
        ItemStack food;
        if (this.getRandom().nextDouble() < WerewolvesConfig.BALANCE.MOBPROPS.werewolf_golden_apple_spawn_chance.get()) {
            food = new ItemStack(Items.GOLDEN_APPLE);
        } else {
            food = switch (this.getRandom().nextInt(3)) {
                case 0 -> new ItemStack(ModItems.V.HUMAN_HEART.get());
                case 1 -> new ItemStack(ModItems.V.WEAK_HUMAN_HEART.get());
                default -> new ItemStack(ModItems.LIVER.get());
            };
        }
        this.setItemSlot(EquipmentSlot.OFFHAND, food);
        this.setDropChance(EquipmentSlot.OFFHAND, 0);
    }

    @Override
    public @NotNull MobCategory getClassification(boolean forSpawnCount) {
        return forSpawnCount && this.countAsMonsterForSpawn ? MobCategory.MONSTER : super.getClassification(forSpawnCount);
    }

    @Override
    public LivingEntity getRepresentingEntity() {
        return this;
    }

    @Override
    public @NotNull LivingEntity asEntity() {
        return this;
    }

    public static AttributeSupplier.Builder getAttributeBuilder() {
        return VampirismEntity.getAttributeBuilder()
                .add(Attributes.MAX_HEALTH, 30.0)
                .add(Attributes.ATTACK_DAMAGE, 5)
                .add(Attributes.MOVEMENT_SPEED, 0.3);
    }
}
