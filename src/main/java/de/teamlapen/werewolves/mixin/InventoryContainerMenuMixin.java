package de.teamlapen.werewolves.mixin;

import de.teamlapen.lib.lib.inventory.InventoryContainerMenu;
import de.teamlapen.werewolves.inventory.ClawMenuSlot;
import de.teamlapen.werewolves.items.WerewolfClawItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Routes shift-clicks to and from the werewolf claw slot that {@code VampirismMenuMixin} appends to
 * the {@link de.teamlapen.vampirism.inventory.VampirismMenu}.
 * <p>
 * The base {@link InventoryContainerMenu#quickMoveStack} only knows about the selector slots and the
 * 36 player slots (indices 0..size+35); the appended claw slot sits after those, so its shift-click
 * handling has to be added here. The guard only fires for menus that actually contain a
 * {@link ClawMenuSlot}, leaving every other {@code InventoryContainerMenu} untouched. Claws are the
 * only item that ever enters the claw slot, and while the claw action is active the slot refuses
 * changes (see {@link ClawMenuSlot#mayPlace}/{@code mayPickup}).
 */
@Mixin(value = InventoryContainerMenu.class, remap = false)
public abstract class InventoryContainerMenuMixin extends AbstractContainerMenu {

    private InventoryContainerMenuMixin(net.minecraft.world.inventory.MenuType<?> menuType, int id) {
        super(menuType, id);
    }

    @Inject(method = "quickMoveStack", at = @At("HEAD"), cancellable = true, remap = false)
    private void werewolves$quickMoveClaw(Player player, int index, CallbackInfoReturnable<ItemStack> cir) {
        int clawIndex = -1;
        ClawMenuSlot clawSlot = null;
        for (int i = 0; i < this.slots.size(); i++) {
            if (this.slots.get(i) instanceof ClawMenuSlot cms) {
                clawIndex = i;
                clawSlot = cms;
                break;
            }
        }
        if (clawSlot == null) {
            return; // not a werewolf claw menu -> let vanilla base logic run
        }

        Slot clicked = this.slots.get(index);
        if (!clicked.hasItem()) {
            return;
        }

        if (index == clawIndex) {
            // claw slot -> player inventory (claw slot always precedes nothing else we own)
            if (!clawSlot.mayPickup(player)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            ItemStack inSlot = clicked.getItem();
            ItemStack original = inSlot.copy();
            // player slots are the 36 slots directly before the claw slot
            int playerStart = clawIndex - 36;
            if (!this.moveItemStackTo(inSlot, playerStart, clawIndex, true)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            if (inSlot.isEmpty()) {
                clicked.setByPlayer(ItemStack.EMPTY);
            } else {
                clicked.setChanged();
            }
            clicked.onTake(player, inSlot);
            cir.setReturnValue(original);
            return;
        }

        // any other slot: only intercept when a claw is shift-clicked and the claw slot can take it
        if (clicked.getItem().getItem() instanceof WerewolfClawItem && clawSlot.mayPlace(clicked.getItem())) {
            ItemStack inSlot = clicked.getItem();
            ItemStack original = inSlot.copy();
            if (!this.moveItemStackTo(inSlot, clawIndex, clawIndex + 1, false)) {
                return; // could not place (e.g. already occupied) -> fall through to vanilla handling
            }
            if (inSlot.isEmpty()) {
                clicked.setByPlayer(ItemStack.EMPTY);
            } else {
                clicked.setChanged();
            }
            clicked.onTake(player, inSlot);
            cir.setReturnValue(original);
        }
    }
}
