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

/**
 * While the werewolf claw action is active the player is treated as empty handed: the selected
 * hotbar item is hidden so the claw (which supplies its own attack modifiers) is what gets used.
 * <p>
 * This is the "selection redirect" from the hotbar to the {@link de.teamlapen.werewolves.entities.player.werewolf.WerewolfClawSlot}:
 * the real hotbar item is left untouched in its slot and simply not held while the claw is active.
 * {@link net.minecraft.world.entity.player.Player#getMainHandItem()} and
 * {@code getItemBySlot(MAINHAND)} both funnel through {@link Inventory#getSelected()}, so a single hook
 * covers attacking, item use and held-item rendering. The synced claw slot {@code active} flag keeps
 * client and server in agreement, and the flag is only ever true for a transformed werewolf.
 */
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
