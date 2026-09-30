package de.teamlapen.werewolves.client.gui;

import de.teamlapen.werewolves.api.WResourceLocation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    private static final float MAX_VERTICAL_SCALE = 2.6F;
    private static final float MAX_HORIZONTAL_SCALE = 4.0F;
    private static final float HORIZONTAL_STRETCH = 1.5F;
    private static final int SCREEN_MARGIN = 12;
    private static final int CONTENT_LEFT = 18;
    private static final int CONTENT_RIGHT = 18;
    private static final int CONTENT_TOP = 36;
    private static final int CONTENT_BOTTOM_MARGIN = 18;
    private static final int TITLE_TOP = 17;
    private static final double MOUSE_WHEEL_SCROLL = 18.0D;
    private static final int TEXT_COLOR = 0x3B2A1D;
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
    private float horizontalScale;
    private float verticalScale;
    private int contentLeft;
    private int contentTop;
    private int contentRight;
    private int contentBottom;
    private double contentScroll;
    private double maxContentScroll;
    private boolean draggingContent;
    private List<FormattedCharSequence> bodyLines = List.of();

    public ScrollScreen() {
        super(Component.translatable("screen.werewolves.scroll"));
    }

    public static void open() {
        net.minecraft.client.Minecraft.getInstance().setScreen(new ScrollScreen());
    }

    @Override
    protected void init() {
        float availableVerticalScale = (this.height - SCREEN_MARGIN * 2.0F) / OPEN_SCROLL_HEIGHT;
        this.verticalScale = Math.min(MAX_VERTICAL_SCALE, availableVerticalScale);
        float availableHorizontalScale = (this.width - SCREEN_MARGIN * 2.0F) / SCROLL_WIDTH;
        this.horizontalScale = Math.min(
                MAX_HORIZONTAL_SCALE,
                Math.min(availableHorizontalScale, this.verticalScale * HORIZONTAL_STRETCH)
        );

        int scaledWidth = Math.round(SCROLL_WIDTH * this.horizontalScale);
        int scaledHeight = Math.round(OPEN_SCROLL_HEIGHT * this.verticalScale);
        this.scrollLeft = (this.width - scaledWidth) / 2;
        this.scrollTop = (this.height - scaledHeight) / 2;
        this.contentLeft = this.scrollLeft + Math.round(CONTENT_LEFT * this.horizontalScale);
        this.contentTop = this.scrollTop + Math.round(CONTENT_TOP * this.verticalScale);
        this.contentRight = this.scrollLeft + Math.round((SCROLL_WIDTH - CONTENT_RIGHT) * this.horizontalScale);
        this.contentBottom = this.scrollTop + Math.round((OPEN_SCROLL_HEIGHT - CONTENT_BOTTOM_MARGIN) * this.verticalScale);
        this.reflowContent();
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
        int frameTop = this.scrollTop + Math.round((OPEN_SCROLL_HEIGHT - frameHeight) * this.verticalScale / 2.0F);
        graphics.pose().pushPose();
        graphics.pose().translate(this.scrollLeft, frameTop, 0);
        graphics.pose().scale(this.horizontalScale, this.verticalScale, 1.0F);
        graphics.blit(frame, 0, 0, 0, 0, SCROLL_WIDTH, frameHeight, SCROLL_WIDTH, frameHeight);
        graphics.pose().popPose();
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.frameIndex < OPENING_FRAMES.length - 1) {
            return;
        }

        Component title = Component.translatable("screen.werewolves.scroll.example.title");
        int titleLeft = this.contentLeft + (this.contentRight - this.contentLeft - this.font.width(title)) / 2;
        int titleTop = this.scrollTop + Math.round(TITLE_TOP * this.verticalScale);
        graphics.drawString(this.font, title, titleLeft, titleTop, TEXT_COLOR, false);

        graphics.enableScissor(this.contentLeft, this.contentTop, this.contentRight, this.contentBottom);
        int lineTop = this.contentTop - Mth.floor(this.contentScroll);
        for (FormattedCharSequence line : this.bodyLines) {
            graphics.drawString(this.font, line, this.contentLeft, lineTop, TEXT_COLOR, false);
            lineTop += this.font.lineHeight + 1;
        }
        graphics.disableScissor();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isMouseOverContent(mouseX, mouseY)) {
            this.draggingContent = true;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && this.draggingContent) {
            this.draggingContent = false;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button == 0 && this.draggingContent) {
            this.scrollContent(dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isMouseOverContent(mouseX, mouseY) && scrollY != 0.0D) {
            this.scrollContent(-scrollY * MOUSE_WHEEL_SCROLL);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int getFrameHeight() {
        return FRAME_HEIGHTS[this.frameIndex];
    }

    private void reflowContent() {
        Component body = Component.translatable("screen.werewolves.scroll.example.body");
        this.bodyLines = this.font.split(body, this.contentRight - this.contentLeft);
        int lineSpacing = this.font.lineHeight + 1;
        int contentHeight = Math.max(0, this.bodyLines.size() * lineSpacing - 1);
        int visibleHeight = this.contentBottom - this.contentTop;
        this.maxContentScroll = Math.max(0, contentHeight - visibleHeight);
        this.contentScroll = Mth.clamp(this.contentScroll, 0.0D, this.maxContentScroll);
    }

    private void scrollContent(double amount) {
        this.contentScroll = Mth.clamp(this.contentScroll + amount, 0.0D, this.maxContentScroll);
    }

    private boolean isMouseOverContent(double mouseX, double mouseY) {
        return this.frameIndex == OPENING_FRAMES.length - 1
                && mouseX >= this.contentLeft && mouseX < this.contentRight
                && mouseY >= this.contentTop && mouseY < this.contentBottom;
    }
}
