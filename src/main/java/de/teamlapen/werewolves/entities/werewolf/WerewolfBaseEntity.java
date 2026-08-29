package de.teamlapen.werewolves.entities.werewolf;

import de.teamlapen.vampirism.entity.VampirismEntity;
import de.teamlapen.werewolves.api.entities.werewolf.IWerewolfMob;
import de.teamlapen.werewolves.config.WerewolvesConfig;
import de.teamlapen.werewolves.core.ModItems;
import de.teamlapen.werewolves.util.FormHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

public abstract class WerewolfBaseEntity extends VampirismEntity implements IWerewolfMob {

    private final boolean countAsMonsterForSpawn;
    private int healthRegenTimer;

    public WerewolfBaseEntity(EntityType<? extends VampirismEntity> type, Level world, boolean countAsMonsterForSpawn) {
        super(type, world);
        this.countAsMonsterForSpawn = countAsMonsterForSpawn;
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

    @Override
    public void aiStep() {
        super.aiStep();
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
