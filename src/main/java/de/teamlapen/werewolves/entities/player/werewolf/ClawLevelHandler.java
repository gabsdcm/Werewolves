package de.teamlapen.werewolves.entities.player.werewolf;

import de.teamlapen.lib.lib.storage.ISyncableSaveData;
import de.teamlapen.vampirism.api.items.IItemWithTier;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

public class ClawLevelHandler implements ISyncableSaveData {

    private static final String KEY_CLAW_LEVEL = "claw_level";
    private static final String KEY_TIER = "tier";
    private static final String KEY_LEVEL = "level";
    private static final String KEY_PROGRESS = "progress";

    private Tier tier = Tier.NATURAL;
    private int level;
    private double progress;

    public Tier getTier() {
        return this.tier;
    }

    public int getLevel() {
        return this.level;
    }

    public int getMaxLevel() {
        return this.tier.maxLevel;
    }

    public double getProgress() {
        return this.progress;
    }

    public int getNeededProgress() {
        return this.isMaxLevel() ? 0 : this.tier.requirements[this.level];
    }

    public double getRemainingProgress() {
        return Math.max(0, this.getNeededProgress() - this.progress);
    }

    public float getLevelPerc() {
        if (this.isMaxLevel()) {
            return 1.0F;
        }
        return Mth.clamp((float) (this.progress / this.getNeededProgress()), 0.0F, 1.0F);
    }

    public boolean isMaxLevel() {
        return this.level >= this.tier.maxLevel;
    }

    public boolean canLevelUp() {
        return !this.isMaxLevel() && this.progress >= this.getNeededProgress();
    }

    public void addProgress(double amount) {
        if (amount <= 0 || this.isMaxLevel()) {
            return;
        }

        this.progress += amount;
        while (!this.isMaxLevel() && this.canLevelUp()) {
            this.progress -= this.getNeededProgress();
            this.level++;
        }
        if (this.isMaxLevel()) {
            this.progress = 0;
        }
    }

    public void setTier(Tier tier) {
        if (tier != this.tier) {
            this.tier = tier;
            this.level = 0;
            this.progress = 0;
        }
    }

    public void adoptTier(IItemWithTier.TIER itemTier) {
        Tier newTier = Tier.from(itemTier);
        if (newTier.ordinal() > this.tier.ordinal()) {
            this.setTier(newTier);
        }
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.@NotNull Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putString(KEY_TIER, this.tier.name());
        nbt.putInt(KEY_LEVEL, this.level);
        nbt.putDouble(KEY_PROGRESS, this.progress);
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        if (nbt.contains(KEY_TIER)) {
            try {
                this.tier = Tier.valueOf(nbt.getString(KEY_TIER));
            } catch (IllegalArgumentException ignored) {
                this.tier = Tier.NATURAL;
            }
        }
        this.level = Mth.clamp(nbt.getInt(KEY_LEVEL), 0, this.tier.maxLevel);
        this.progress = Math.max(0, nbt.getDouble(KEY_PROGRESS));
        if (this.isMaxLevel()) {
            this.progress = 0;
        }
    }

    @Override
    public void deserializeUpdateNBT(HolderLookup.@NotNull Provider provider, @NotNull CompoundTag nbt) {
        this.deserializeNBT(provider, nbt);
    }

    @Override
    public @NotNull CompoundTag serializeUpdateNBT(HolderLookup.@NotNull Provider provider) {
        return this.serializeNBT(provider);
    }

    @Override
    public String nbtKey() {
        return KEY_CLAW_LEVEL;
    }

    public enum Tier {
        NATURAL("Natural", 5, 50, 80, 110, 140, 170),
        NORMAL("Normal", 3, 170, 245, 320),
        ENHANCED("Avançada", 4, 300, 380, 460, 540),
        ULTIMATE("Ultimate", 5, 400, 500, 600, 700, 800);

        private final String displayName;
        private final int maxLevel;
        private final int[] requirements;

        Tier(String displayName, int maxLevel, int... requirements) {
            this.displayName = displayName;
            this.maxLevel = maxLevel;
            this.requirements = requirements;
        }

        public String getDisplayName() {
            return this.displayName;
        }

        private static Tier from(IItemWithTier.TIER tier) {
            return switch (tier) {
                case NORMAL -> NORMAL;
                case ENHANCED -> ENHANCED;
                case ULTIMATE -> ULTIMATE;
            };
        }
    }
}
