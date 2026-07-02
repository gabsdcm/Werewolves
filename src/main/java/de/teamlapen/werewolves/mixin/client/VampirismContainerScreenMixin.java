package de.teamlapen.werewolves.mixin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import de.teamlapen.vampirism.client.gui.screens.VampirismContainerScreen;
import de.teamlapen.vampirism.inventory.VampirismMenu;
import de.teamlapen.werewolves.api.WResourceLocation;
import de.teamlapen.werewolves.inventory.ClawMenuSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Draws the werewolf-only claw slot frame and, while the slot is empty, a translucent claw
 * placeholder. The equipped claw item itself is rendered by the vanilla slot rendering because the
 * claw slot is a real {@link ClawMenuSlot} in the menu, so nothing extra is needed for that case.
 * <p>
 * The frame reuses the 18x18 slot region already present in the Vampirism refinement background
 * texture (top-left refinement slot at texture 57,7); no Vampirism asset is overwritten and the
 * werewolves claw texture is only sampled, never modified.
 */
@Mixin(value = VampirismContainerScreen.class, remap = false)
public abstract class VampirismContainerScreenMixin extends AbstractContainerScreen<VampirismMenu> {

    @org.spongepowered.asm.mixin.Unique
    private static final ResourceLocation WEREWOLVES$REFINEMENTS_BG = ResourceLocation.fromNamespaceAndPath("vampirism", "textures/gui/container/vampirism_menu_refinements.png");

    @org.spongepowered.asm.mixin.Unique
    private static final ResourceLocation WEREWOLVES$CLAW_ICON = WResourceLocation.mod("textures/actions/claw.png");

    private VampirismContainerScreenMixin(VampirismMenu menu, net.minecraft.world.entity.player.Inventory inventory, net.minecraft.network.chat.Component title) {
        super(menu, inventory, title);
    }

    @Inject(method = "renderBg", at = @At("TAIL"), remap = false)
    private void werewolves$renderClawSlot(GuiGraphics graphics, float partialTick, int mouseX, int mouseY, CallbackInfo ci) {
        Slot clawSlot = werewolves$findClawSlot();
        if (clawSlot == null) {
            return;
        }
        // frame: reuse the top-left refinement slot border (texture region 57,7 -> 18x18)
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(WEREWOLVES$REFINEMENTS_BG, this.leftPos + clawSlot.x - 1, this.topPos + clawSlot.y - 1, 57, 7, 18, 18);

        if (!clawSlot.hasItem()) {
            // translucent claw placeholder: keep RGB, reduce alpha only, then restore render state
            RenderSystem.enableBlend();
            graphics.setColor(1.0F, 1.0F, 1.0F, 0.4F);
            graphics.blit(WEREWOLVES$CLAW_ICON, this.leftPos + clawSlot.x, this.topPos + clawSlot.y, 0, 0, 16, 16, 16, 16);
            graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private Slot werewolves$findClawSlot() {
        for (Slot slot : this.menu.slots) {
            if (slot instanceof ClawMenuSlot) {
                return slot;
            }
        }
        return null;
    }
}
