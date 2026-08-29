package de.teamlapen.werewolves.mixin;

import de.teamlapen.vampirism.inventory.VampirismMenu;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import de.teamlapen.werewolves.inventory.ClawMenuSlot;
import de.teamlapen.werewolves.inventory.ClawSlotContainer;
import de.teamlapen.werewolves.util.Helper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VampirismMenu.class, remap = false)
public abstract class VampirismMenuMixin extends AbstractContainerMenu {

    private VampirismMenuMixin(net.minecraft.world.inventory.MenuType<?> menuType, int id) {
        super(menuType, id);
    }

    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;)V", at = @At("TAIL"))
    private void werewolves$addClawSlot(int id, Inventory playerInventory, CallbackInfo ci) {
        Player player = playerInventory.player;
        if (!Helper.isWerewolf(player)) {
            return;
        }
        WerewolfPlayer werewolf = WerewolfPlayer.get(player);
        ClawSlotContainer container = new ClawSlotContainer(werewolf);
        this.addSlot(new ClawMenuSlot(container, 58, 62));
    }
}
