package de.teamlapen.werewolves.mixin.client;

import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import de.teamlapen.werewolves.util.Helper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public abstract class GuiMixin {

    private static final ResourceLocation HOTBAR_SELECTION_SPRITE = ResourceLocation.withDefaultNamespace("hud/hotbar_selection");

    @Redirect(method = "renderItemHotbar", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"))
    private void werewolves$suppressVanillaSelectionWhileClawActive(GuiGraphics graphics, ResourceLocation sprite, int x, int y, int width, int height) {
        if (HOTBAR_SELECTION_SPRITE.equals(sprite) && werewolves$isClawActive()) {
            return;
        }
        graphics.blitSprite(sprite, x, y, width, height);
    }

    private static boolean werewolves$isClawActive() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && Helper.isWerewolf(player) && WerewolfPlayer.get(player).getClawSlot().isActive();
    }
}
