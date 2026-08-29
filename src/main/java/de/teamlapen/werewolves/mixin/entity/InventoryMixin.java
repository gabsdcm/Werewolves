package de.teamlapen.werewolves.mixin.entity;

import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow @Final public Player player;

    @Inject(method = "getSelected", at = @At("HEAD"), cancellable = true)
    private void werewolves$hideHandWhileClawActive(CallbackInfoReturnable<ItemStack> cir) {
        if (WerewolfPlayer.get(this.player).getClawSlot().isActive()) {
            cir.setReturnValue(ItemStack.EMPTY);
        }
    }
}
