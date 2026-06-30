package de.teamlapen.werewolves.entities.player.werewolf;

import de.teamlapen.lib.lib.storage.ISyncableSaveData;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Dedicated single slot that holds the equipped werewolf claw item.
 * <p>
 * The slot lives outside of the vanilla inventory and the {@link WerewolfInventory} armor storage.
 * It is serialized in the {@link WerewolfPlayer} NBT and therefore persists across death/respawn
 * (the werewolf player attachment uses {@code copyOnDeath()}). It is intentionally never dropped.
 */
public class WerewolfClawSlot implements ISyncableSaveData {

    private static final String KEY_CLAW_SLOT = "claw_slot";

    private @NotNull ItemStack stack = ItemStack.EMPTY;
    private boolean active = false;

    public @NotNull ItemStack getStack() {
        return this.stack;
    }

    public void setStack(@NotNull ItemStack stack) {
        this.stack = stack;
    }

    public boolean isActive() {
        return this.active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public @NotNull CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag nbt = new CompoundTag();
        nbt.putBoolean("active", this.active);
        if (!this.stack.isEmpty()) {
            nbt.put("claw", this.stack.save(provider, new CompoundTag()));
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, @NotNull CompoundTag compoundTag) {
        this.active = compoundTag.getBoolean("active");
        if (compoundTag.contains("claw")) {
            this.stack = ItemStack.parseOptional(provider, compoundTag.getCompound("claw"));
        } else {
            this.stack = ItemStack.EMPTY;
        }
    }

    @Override
    public @NotNull CompoundTag serializeUpdateNBT(HolderLookup.Provider provider) {
        return serializeNBT(provider);
    }

    @Override
    public void deserializeUpdateNBT(HolderLookup.Provider provider, @NotNull CompoundTag compoundTag) {
        // only apply when the packet actually carries claw slot data to avoid clobbering on unrelated partial syncs
        if (compoundTag.contains("active")) {
            deserializeNBT(provider, compoundTag);
        }
    }

    @Override
    public String nbtKey() {
        return KEY_CLAW_SLOT;
    }
}
