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
            return; 
        }

        Slot clicked = this.slots.get(index);
        if (!clicked.hasItem()) {
            return;
        }

        if (index == clawIndex) {
            if (!clawSlot.mayPickup(player)) {
                cir.setReturnValue(ItemStack.EMPTY);
                return;
            }
            ItemStack inSlot = clicked.getItem();
            ItemStack original = inSlot.copy();
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

        if (clicked.getItem().getItem() instanceof WerewolfClawItem && clawSlot.mayPlace(clicked.getItem())) {
            ItemStack inSlot = clicked.getItem();
            ItemStack original = inSlot.copy();
            if (!this.moveItemStackTo(inSlot, clawIndex, clawIndex + 1, false)) {
                return; 
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
