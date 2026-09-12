package de.teamlapen.werewolves.entities.werewolf;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.difficulty.Difficulty;
import de.teamlapen.vampirism.config.BalanceMobProps;
import de.teamlapen.vampirism.entity.VampirismEntity;
import de.teamlapen.vampirism.entity.ai.goals.LookAtClosestVisibleGoal;
import de.teamlapen.vampirism.entity.factions.FactionPlayerHandler;
import de.teamlapen.vampirism.entity.vampire.VampireBaseEntity;
import de.teamlapen.werewolves.api.entities.werewolf.IWerewolfAlpha;
import de.teamlapen.werewolves.api.entities.werewolf.WerewolfForm;
import de.teamlapen.werewolves.config.BalanceConfig;
import de.teamlapen.werewolves.config.WerewolvesConfig;
import de.teamlapen.werewolves.core.ModBiomes;
import de.teamlapen.werewolves.core.ModSounds;
import de.teamlapen.werewolves.entities.goals.HowlOnHurtGoal;
import de.teamlapen.werewolves.entities.goals.EatToHealGoal;
import de.teamlapen.werewolves.entities.goals.WerewolfLeapGoal;
import net.minecraft.core.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class WerewolfAlphaEntity extends WerewolfBaseEntity implements IWerewolfAlpha {
    public static final int MAX_LEVEL = 4;
    private static final EntityDataAccessor<Integer> LEVEL = SynchedEntityData.defineId(WerewolfAlphaEntity.class, EntityDataSerializers.INT);


    public static boolean spawnPredicateAlpha(EntityType<? extends WerewolfAlphaEntity> entityType, ServerLevelAccessor world, MobSpawnType spawnReason, BlockPos blockPos, RandomSource random) {
        return world.getBiome(blockPos).is(ModBiomes.WEREWOLF_FOREST) && world.getDifficulty() != net.minecraft.world.Difficulty.PEACEFUL && spawnPredicateWerewolf(entityType, world, spawnReason, blockPos, random);
    }

    public static AttributeSupplier.Builder getAttributeBuilder() {
        // attribute suppliers are built during mod loading, before the server balance config is loaded, so only the defaults are readable here. updateEntityAttributes applies the configured values once the entity exists.
        BalanceConfig.MobProps props = WerewolvesConfig.BALANCE.MOBPROPS;
        return VampireBaseEntity.getAttributeBuilder()
                .add(Attributes.MAX_HEALTH, props.alpha_werewolf_max_health.getDefault())
                .add(Attributes.ATTACK_DAMAGE, props.alpha_werewolf_attack_damage.getDefault())
                .add(Attributes.MOVEMENT_SPEED, props.alpha_werewolf_speed.getDefault())
                .add(Attributes.FOLLOW_RANGE, props.alpha_werewolf_follow_range.getDefault());
    }

    private int followingEntities = 0;

    public WerewolfAlphaEntity(EntityType<? extends VampirismEntity> type, Level world) {
        super(type, world, true);
    }

    @Override
    public void addAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        nbt.putInt("level", getEntityLevel());
    }

    @Override
    public void decreaseFollowerCount() {
        this.followingEntities = Math.max(0, this.followingEntities - 1);
    }

    @Override
    public int getFollowingCount() {
        return this.followingEntities;
    }

    @Override
    public void setEntityLevel(int level) {
        level = Mth.clamp(level, 0, MAX_LEVEL);
        if (level >= 0) {
            getEntityData().set(LEVEL, level);
            this.updateEntityAttributes();
            this.setCustomName(getTypeName().plainCopy().append(Component.translatable("entity.werewolves.alpha_werewolf.level", level + 1)));
        } else {
            this.setCustomName(null);
        }
    }

    @Override
    public int getEntityLevel() {
        return getEntityData().get(LEVEL);
    }

    @Override
    public int getMaxEntityLevel() {
        return MAX_LEVEL;
    }

    @Override
    public int getMaxHeadXRot() {
        return 5;
    }

    @Override
    public int getMaxHeadYRot() {
        return 5;
    }

    @Override
    public int getMaxFollowerCount() {
        return (int) ((BalanceMobProps.mobProps.ADVANCED_VAMPIRE_MAX_FOLLOWER) * ((this.getEntityLevel() + 1) / (float) (this.getMaxEntityLevel() + 1)));
    }

    @Override
    public int getPortalCooldown() {
        return 500;
    }

    @Override
    public boolean increaseFollowerCount() {
        if (this.followingEntities < getMaxFollowerCount()) {
            this.followingEntities++;
            return true;
        }
        return false;
    }

    @Override
    protected boolean canRide(@NotNull Entity pVehicle) {
        return false;
    }

    @Override
    public boolean doHurtTarget(@NotNull Entity entity) {
        if (entity instanceof LivingEntity living) {
            return this.applyBiteEffects(living);
        }
        return super.doHurtTarget(entity);
    }

    @Override
    public boolean killedEntity(@NotNull ServerLevel level, @NotNull LivingEntity entity) {
        super.killedEntity(level, entity);
        if (entity instanceof WerewolfAlphaEntity) {
            this.setHealth(this.getMaxHealth());
        }
        return true;
    }

    @Override
    public void readAdditionalSaveData(@Nonnull CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        setEntityLevel(nbt.getInt("level"));
    }

    @Override
    public boolean shouldShowName() {
        return true;
    }

    public int getAmbientSoundInterval() {
        return 240;
    }

    protected SoundEvent getAmbientSound() {
        return ModSounds.ENTITY_WEREWOLF_GROWL.get();
    }

    @Override
    public int suggestEntityLevel(Difficulty d) {
        int avg = Math.round(((d.avgPercLevel()) / 100F - 5 / 14F) / (1F - 5 / 14F) * MAX_LEVEL);
        int max = Math.round(((d.maxPercLevel()) / 100F - 5 / 14F) / (1F - 5 / 14F) * MAX_LEVEL);
        int min = Math.round(((d.minPercLevel()) / 100F - 5 / 14F) / (1F - 5 / 14F) * (MAX_LEVEL));

        return switch (random.nextInt(7)) {
            case 0 -> min;
            case 1 -> max + 1;
            case 2 -> avg;
            case 3 -> avg + 1;
            case 4, 5 -> random.nextInt(MAX_LEVEL + 1);
            default -> random.nextInt(max + 2 - min) + min;
        };
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder pBuilder) {
        super.defineSynchedData(pBuilder);
        pBuilder.define(LEVEL, -1);
    }

    @Override
    protected int getBaseExperienceReward() {
        return 20 + 5 * getEntityLevel();
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(3, new WerewolfLeapGoal(this, this.getForm().getMobLeapModifier(), 8.0D, 16.0D, 300));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0F, false));
        this.goalSelector.addGoal(5, new EatToHealGoal<>(this));
        this.goalSelector.addGoal(6, new AvoidEntityGoal<>(this, Player.class, 6.0F, 0.6, 0.7F, input -> input != null && !isLowerLevel(input)));
        this.goalSelector.addGoal(7, new RandomStrollGoal(this, 0.2));
        this.goalSelector.addGoal(9, new LookAtClosestVisibleGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(0, new HowlOnHurtGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isLowerLevel));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, WerewolfAlphaEntity.class, true, false));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal<>(this, PathfinderMob.class, 5, true, false, VampirismAPI.factionRegistry().getPredicate(getFaction(), false, true, false, false, null)));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal<>(this, AbstractSkeleton.class, false));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(@Nonnull ServerLevelAccessor world, @Nonnull DifficultyInstance difficulty, @Nonnull MobSpawnType reason, @Nullable SpawnGroupData spawnData) {
        SpawnGroupData data = super.finalizeSpawn(world, difficulty, reason, spawnData);
        this.stockHealingFood();
        return data;
    }

    private boolean isLowerLevel(LivingEntity entity) {
        if (entity instanceof Player player) {
            int playerLevel = FactionPlayerHandler.get(player).getCurrentLevel();
            return (playerLevel - 8) / 2F - this.getEntityLevel() <= 0;
        }
        return false;
    }

    @Nonnull
    @Override
    public WerewolfForm getForm() {
        return WerewolfForm.BEAST4L;
    }

    @Override
    public int getSkinType(@Nullable WerewolfForm form) {
        return this.getId();
    }

    @Override
    public int getEyeType(@Nullable WerewolfForm form) {
        return 0;
    }

    @Override
    public boolean setSkinType(@org.jetbrains.annotations.Nullable WerewolfForm form, int skinType) {
        return false;
    }

    @Override
    public boolean setEyeType(@org.jetbrains.annotations.Nullable WerewolfForm form, int eyeType) {
        return false;
    }

    @Override
    public boolean hasGlowingEyes(WerewolfForm form) {
        return true;
    }

    protected void updateEntityAttributes() {
        int l = Math.max(getEntityLevel(), 0);
        BalanceConfig.MobProps props = WerewolvesConfig.BALANCE.MOBPROPS;
        this.getAttribute(Attributes.FOLLOW_RANGE).setBaseValue(props.alpha_werewolf_follow_range.get());
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(props.alpha_werewolf_speed.get() + props.alpha_werewolf_speed_pl.get() * l);
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(props.alpha_werewolf_max_health.get() + props.alpha_werewolf_max_health_pl.get() * l);
        this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(props.alpha_werewolf_attack_damage.get() + props.alpha_werewolf_attack_damage_pl.get() * l);
    }
}
