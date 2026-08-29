package de.teamlapen.werewolves.entities.werewolf;

import de.teamlapen.vampirism.api.entity.EntityClassType;
import de.teamlapen.vampirism.api.entity.actions.EntityActionTier;
import de.teamlapen.vampirism.entity.ai.goals.LookAtClosestVisibleGoal;
import de.teamlapen.vampirism.entity.hunter.HunterBaseEntity;
import de.teamlapen.vampirism.entity.vampire.VampireBaseEntity;
import de.teamlapen.werewolves.api.entities.werewolf.TransformType;
import de.teamlapen.werewolves.api.entities.werewolf.WerewolfForm;
import de.teamlapen.werewolves.api.entities.werewolf.WerewolfTransformable;
import de.teamlapen.werewolves.config.BalanceConfig;
import de.teamlapen.werewolves.config.WerewolvesConfig;
import de.teamlapen.werewolves.core.ModEntities;
import de.teamlapen.werewolves.core.ModItems;
import de.teamlapen.werewolves.util.FormHelper;
import de.teamlapen.werewolves.util.Helper;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class HumanWerewolfEntity extends PathfinderMob implements WerewolfTransformable {
    private static final EntityDataAccessor<Integer> FORM = SynchedEntityData.defineId(HumanWerewolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SKIN_TYPE = SynchedEntityData.defineId(HumanWerewolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EYE_TYPE = SynchedEntityData.defineId(HumanWerewolfEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GLOWING_EYES = SynchedEntityData.defineId(HumanWerewolfEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EquipmentSlot[] PELT_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final float[] PELT_SLOT_CHANCES = {0.4F, 0.9F, 0.75F, 0.5F};

    private final EntityClassType classType;
    private final EntityActionTier actionTier;

    protected int rage;

    public HumanWerewolfEntity(EntityType<? extends PathfinderMob> type, Level worldIn) {
        super(type, worldIn);
        this.classType = EntityClassType.getRandomClass(this.getRandom());
        this.actionTier = EntityActionTier.Medium;
    }

    public static boolean spawnPredicateHumanWerewolf(EntityType<? extends PathfinderMob> entityType, ServerLevelAccessor world, MobSpawnType spawnReason, BlockPos blockPos, RandomSource random) {
        if (world.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL) return false;
        if (spawnReason == MobSpawnType.EVENT) return true;
        if (!Monster.isDarkEnoughToSpawn(world, blockPos, random) && !FormHelper.isInWerewolfBiome(world, blockPos)) return false;
        return Mob.checkMobSpawnRules(entityType, world, spawnReason, blockPos, random);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(FORM, -1);
        builder.define(SKIN_TYPE, -1);
        builder.define(EYE_TYPE, -1);
        builder.define(GLOWING_EYES, false);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.2));

        this.goalSelector.addGoal(9, new RandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(10, new LookAtClosestVisibleGoal(this, Player.class, 20F, 0.6F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, HunterBaseEntity.class, 17F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, VampireBaseEntity.class, 17F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor world, @Nonnull DifficultyInstance difficulty, @Nonnull MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        this.randomEquipments();
        return super.finalizeSpawn(world, difficulty, reason, spawnData);
    }

    protected void randomEquipments() {
        int roll = this.getRandom().nextInt(20);
        if (roll < 10) {
            return;
        }
        List<ItemLike> pelt;
        if (roll < 17) {
            pelt = List.of(ModItems.PELT_HELMET, ModItems.PELT_CHESTPLATE, ModItems.PELT_LEGGINGS, ModItems.PELT_BOOTS);
        } else if (roll < 19) {
            pelt = List.of(ModItems.DARK_PELT_HELMET, ModItems.DARK_PELT_CHESTPLATE, ModItems.DARK_PELT_LEGGINGS, ModItems.DARK_PELT_BOOTS);
        } else {
            pelt = List.of(ModItems.WHITE_PELT_HELMET, ModItems.WHITE_PELT_CHESTPLATE, ModItems.WHITE_PELT_LEGGINGS, ModItems.WHITE_PELT_BOOTS);
        }
        for (int i = 0; i < PELT_SLOTS.length; i++) {
            if (this.getRandom().nextFloat() < PELT_SLOT_CHANCES[i]) {
                this.setItemSlot(PELT_SLOTS[i], new ItemStack(pelt.get(i)));
            }
        }
        this.setDontDropEquipment();
    }

    protected void setDontDropEquipment() {
        Arrays.fill(this.armorDropChances, 0);
        Arrays.fill(this.handDropChances, 0);
    }

    @Override
    public boolean hurt(@Nonnull DamageSource source, float amount) {
        if (super.hurt(source, amount)) {
            this.rage += (int) (amount * 10);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.rage > 150) {
            WerewolfTransformable werewolf = this.transformToWerewolf(TransformType.TIME_LIMITED);
            ((Mob) werewolf).setLastHurtByMob(this.getTarget());
        }
        if (this.level().getGameTime() % 400 == 10) {
            if (Helper.isFullMoon(this.level())) {
                this.transformToWerewolf(TransformType.FULL_MOON);
            }
            this.rage -= 2;
        }
    }

    @Override
    public void reset() {
        this.rage = 0;
    }

    public static AttributeSupplier.Builder getAttributeBuilder() {
        // attribute suppliers are built during mod loading, before the server balance config is loaded, so only the defaults are readable here. updateEntityAttributes applies the configured values once the entity exists.
        BalanceConfig.MobProps props = WerewolvesConfig.BALANCE.MOBPROPS;
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MOVEMENT_SPEED, props.human_werewolf_speed.getDefault())
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, props.human_werewolf_attack_damage.getDefault())
                .add(Attributes.MAX_HEALTH, props.human_werewolf_max_health.getDefault());
    }

    protected void updateEntityAttributes() {
        BalanceConfig.MobProps props = WerewolvesConfig.BALANCE.MOBPROPS;
        boolean wasFullHealth = this.getHealth() >= this.getMaxHealth();
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(props.human_werewolf_speed.get());
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(props.human_werewolf_attack_damage.get());
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(props.human_werewolf_max_health.get());
        if (wasFullHealth) {
            this.setHealth(this.getMaxHealth());
        }
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("form")) {
            int t = compound.getInt("form");
            this.getEntityData().set(FORM, t < 2 && t >= 0 ? t : -1);
        }
        if (compound.contains("type")) {
            int t = compound.getInt("type");
            this.getEntityData().set(SKIN_TYPE, t < 126 && t >= 0 ? t : -1);
        }
        if (compound.contains("eye_type")) {
            int t = compound.getInt("eye_type");
            this.getEntityData().set(EYE_TYPE, t < 126 && t >= 0 ? t : -1);
        }
        if (compound.contains("glowing_eye")) {
            this.getEntityData().set(GLOWING_EYES, compound.getBoolean("glowing_eye"));
        }
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("form", this.getEntityData().get(FORM));
        compound.putInt("type", this.getEntityData().get(SKIN_TYPE));
        compound.putInt("eye_type", this.getEntityData().get(EYE_TYPE));
        compound.putBoolean("glowing_eye", this.getEntityData().get(GLOWING_EYES));
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        if (!this.level().isClientSide()) {
            this.updateEntityAttributes();
        }
        if (this.getEntityData().get(FORM) == -1) {
            this.getEntityData().set(FORM, this.getRandom().nextInt(2));
        }
        if (this.getEntityData().get(SKIN_TYPE) == -1) {
            this.getEntityData().set(SKIN_TYPE, this.getRandom().nextInt(126));
        }
        if (this.getEntityData().get(EYE_TYPE) == -1) {
            this.getEntityData().set(EYE_TYPE, this.getRandom().nextInt(126));
        }
        this.getEntityData().set(GLOWING_EYES, this.getRandom().nextBoolean());
    }

    @Override
    public int getSkinType(@Nullable WerewolfForm form) {
        int i = this.getEntityData().get(SKIN_TYPE);
        return Math.max(i, 0);
    }

    @Override
    public int getEyeType(@Nullable WerewolfForm form) {
        int i = this.getEntityData().get(EYE_TYPE);
        return Math.max(i, 0);
    }

    @Override
    public boolean setSkinType(@org.jetbrains.annotations.Nullable WerewolfForm form, int skinType) {
        if (this.getForm() == form) {
            this.getEntityData().set(SKIN_TYPE, skinType);
            return true;
        }
        return false;
    }

    @Override
    public boolean setEyeType(@org.jetbrains.annotations.Nullable WerewolfForm form, int eyeType) {
        if (this.getForm() == form) {
            this.getEntityData().set(EYE_TYPE, eyeType);
            return true;
        }
        return false;
    }

    @Override
    public boolean hasGlowingEyes(WerewolfForm form) {
        return this.getEntityData().get(GLOWING_EYES);
    }

    @Override
    public BasicWerewolfEntity _transformToWerewolf() {
        EntityType<? extends BasicWerewolfEntity> type;
        if (this.getEntityData().get(FORM) == 0) {
            type = ModEntities.WEREWOLF_BEAST.get();
        } else {
            type = ModEntities.WEREWOLF_SURVIVALIST.get();
        }
        BasicWerewolfEntity werewolf = WerewolfTransformable.copyData(type, this);
        werewolf.setSkinType(this.getForm(), getSkinType(this.getForm()));
        werewolf.setEyeType(this.getForm(), getEyeType(this.getForm()));
        werewolf.setSourceEntity(this);
        return werewolf;
    }

    @Override
    public EntityActionTier getEntityTier() {
        return this.actionTier;
    }

    @Override
    public EntityClassType getEntityClass() {
        return this.classType;
    }

    @Override
    public WerewolfTransformable _transformBack() {
        return this;
    }

    @Override
    public boolean canTransform() {
        return !this.level().isClientSide && Helper.isNight(this.level()) && this.rage > 0;
    }

    @Nonnull
    @Override
    public WerewolfForm getForm() {
        return switch (this.getEntityData().get(FORM)) {
            case 0 -> WerewolfForm.BEAST;
            case 1 -> WerewolfForm.SURVIVALIST;
            default -> WerewolfForm.HUMAN;
        };
    }
}
