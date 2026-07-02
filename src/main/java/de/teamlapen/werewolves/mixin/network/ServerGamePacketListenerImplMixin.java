package de.teamlapen.werewolves.mixin.network;

import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
    private void werewolves$fixOffhandSwapWhileClawActive(ServerboundPlayerActionPacket packet, CallbackInfo ci) {
        if (packet.getAction() != ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND
                || this.player.isSpectator()) {
            return;
        }
        if (!WerewolfPlayer.get(this.player).getClawSlot().isActive()) {
            return;
        }
        int selected = this.player.getInventory().selected;
        ItemStack realMainHand = this.player.getInventory().items.get(selected);
        ItemStack offHand = this.player.getItemBySlot(EquipmentSlot.OFFHAND);
        this.player.setItemSlot(EquipmentSlot.OFFHAND, realMainHand);
        this.player.setItemSlot(EquipmentSlot.MAINHAND, offHand);
        this.player.stopUsingItem();
        ci.cancel();
    }
}
