package de.teamlapen.werewolves.inventory;

import de.teamlapen.werewolves.items.WerewolfClawItem;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ClawMenuSlot extends Slot {

    private final @NotNull ClawSlotContainer container;

    public ClawMenuSlot(@NotNull ClawSlotContainer container, int x, int y) {
        super(container, 0, x, y);
        this.container = container;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return !this.container.isActive() && stack.getItem() instanceof WerewolfClawItem;
    }

    @Override
    public boolean mayPickup(@NotNull net.minecraft.world.entity.player.Player player) {
        return !this.container.isActive();
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
