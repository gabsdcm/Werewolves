package de.teamlapen.werewolves.client.gui;

import de.teamlapen.werewolves.api.WResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * Preview screen for the scroll textures and their opening animation.
 */
public class ScrollScreen extends Screen {

    private static final ResourceLocation[] OPENING_FRAMES = {
            WResourceLocation.mod("textures/gui/scroll/scroll_frame_00.png"),
            WResourceLocation.mod("textures/gui/scroll/scroll_frame_01.png"),
            WResourceLocation.mod("textures/gui/scroll/scroll_frame_02.png"),
            WResourceLocation.mod("textures/gui/scroll/scroll_frame_03.png"),
            WResourceLocation.mod("textures/gui/scroll/scroll_frame_04_open.png")
    };
    private static final int FRAME_TICKS = 4;
    private static final int SCROLL_WIDTH = 82;
    private static final int CLOSED_FRAME_HEIGHT = 31;
    private static final int SMALL_OPEN_FRAME_HEIGHT = 40;
    private static final int MEDIUM_OPEN_FRAME_HEIGHT = 44;
    private static final int LARGE_OPEN_FRAME_HEIGHT = 52;
    private static final int OPEN_SCROLL_HEIGHT = 181;
    private static final int[] FRAME_HEIGHTS = {
            CLOSED_FRAME_HEIGHT,
            SMALL_OPEN_FRAME_HEIGHT,
            MEDIUM_OPEN_FRAME_HEIGHT,
            LARGE_OPEN_FRAME_HEIGHT,
            OPEN_SCROLL_HEIGHT
    };

    private int animationTicks;
    private int frameIndex;
    private int scrollLeft;
    private int scrollTop;

    public ScrollScreen() {
        super(Component.translatable("screen.werewolves.scroll"));
    }

    @Override
    protected void init() {
        this.scrollLeft = (this.width - SCROLL_WIDTH) / 2;
        this.scrollTop = (this.height - OPEN_SCROLL_HEIGHT) / 2;
    }

    @Override
    public void tick() {
        if (this.frameIndex >= OPENING_FRAMES.length - 1) {
            return;
        }

        ++this.animationTicks;
        if (this.animationTicks >= FRAME_TICKS) {
            this.animationTicks = 0;
            ++this.frameIndex;
        }
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        ResourceLocation frame = OPENING_FRAMES[this.frameIndex];
        int frameHeight = this.getFrameHeight();
        graphics.blit(frame, this.scrollLeft, this.scrollTop, 0, 0, SCROLL_WIDTH, frameHeight,
                SCROLL_WIDTH, frameHeight);
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getFrameHeight() {
        return FRAME_HEIGHTS[this.frameIndex];
    }
}
