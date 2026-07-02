package de.teamlapen.werewolves.inventory;

import de.teamlapen.werewolves.entities.player.werewolf.WerewolfClawSlot;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Single slot {@link Container} adapter that exposes the werewolf's dedicated {@link WerewolfClawSlot}
 * to the menu system without keeping a second copy of the stack.
 * <p>
 * Every mutation writes straight back into the real {@link WerewolfClawSlot} and, on the server,
 * triggers the existing claw slot sync via {@link WerewolfPlayer#syncClawSlot()} so the client and
 * the persisted NBT stay in agreement.
 */
public class ClawSlotContainer implements Container {

    private final @NotNull WerewolfPlayer player;

    public ClawSlotContainer(@NotNull WerewolfPlayer player) {
        this.player = player;
    }

    private @NotNull WerewolfClawSlot slot() {
        return this.player.getClawSlot();
    }

    /**
     * @return whether the claw action is currently active. While active the stack must not be mutated
     * (the {@link de.teamlapen.werewolves.entities.player.werewolf.actions.ClawAction} owns attribute
     * modifiers keyed to it); the menu slot blocks insertion/removal in that case.
     */
    public boolean isActive() {
        return this.slot().isActive();
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.slot().getStack().isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int index) {
        return index == 0 ? this.slot().getStack() : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int index, int count) {
        if (index != 0 || count <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = this.slot().getStack();
        if (current.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = current.split(count);
        if (current.isEmpty()) {
            this.slot().setStack(ItemStack.EMPTY);
        }
        setChanged();
        return removed;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int index) {
        if (index != 0) {
            return ItemStack.EMPTY;
        }
        ItemStack current = this.slot().getStack();
        this.slot().setStack(ItemStack.EMPTY);
        setChanged();
        return current;
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        if (index != 0) {
            return;
        }
        this.slot().setStack(stack);
        setChanged();
    }

    @Override
    public void setChanged() {
        // the sync mechanism is server side only; WerewolfPlayer.syncClawSlot() guards on isRemote()
        this.player.syncClawSlot();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        this.slot().setStack(ItemStack.EMPTY);
        setChanged();
    }
}
