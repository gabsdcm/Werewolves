package de.teamlapen.werewolves.inventory;

import de.teamlapen.werewolves.items.WerewolfClawItem;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * The fourth accessory slot shown in the Vampirism menu for werewolves only.
 * <p>
 * It is backed directly by a {@link ClawSlotContainer} (which wraps the real
 * {@link de.teamlapen.werewolves.entities.player.werewolf.WerewolfClawSlot}) so there is no second
 * copy of the equipped claw. It accepts exactly one {@link WerewolfClawItem} and refuses any change
 * while the claw action is active to keep attribute modifiers / active state consistent.
 */
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
        // only rendered/interactable for werewolves; the menu only adds this slot for werewolves,
        // so being present already implies the owner is a werewolf
        return true;
    }
}
