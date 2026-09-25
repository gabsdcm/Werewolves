package de.teamlapen.werewolves.client.gui;

import de.teamlapen.vampirism.api.VampirismAPI;
import de.teamlapen.vampirism.api.entity.factions.IFaction;
import de.teamlapen.werewolves.api.WResourceLocation;
import de.teamlapen.werewolves.entities.player.werewolf.ClawLevelHandler;
import de.teamlapen.werewolves.entities.player.werewolf.LevelHandler;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import de.teamlapen.werewolves.util.MultilineTooltipEx;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class ExpBar extends AbstractWidget {
    private static final ResourceLocation ICON = WResourceLocation.mod("textures/gui/exp_bar.png");
    private static final ResourceLocation CLAW_XP_BACKGROUND = WResourceLocation.mod("textures/gui/claw/xp_background.png");
    private static final ResourceLocation CLAW_XP_HEAD = WResourceLocation.mod("textures/gui/claw/werewolf_head_24.png");
    private static final ResourceLocation RANK_UP = WResourceLocation.mod("textures/gui/claw/rank_up.png");
    private static final ResourceLocation XP_BAR_BACKGROUND_SPRITE = WResourceLocation.mc("hud/experience_bar_background");
    private static final ResourceLocation XP_BAR_PROGRESS_SPRITE = WResourceLocation.mc("hud/experience_bar_progress");

    private static final int RANK_UP_SIZE = 9;
    private static final long RANK_UP_FRAME_MILLIS = 240L;
    private static final int[] RANK_UP_Y_OFFSETS = {0, -1, -2, -3, -2, -1};
    private static final int HORIZONTAL_WIDTH = 234;
    private static final int HORIZONTAL_HEIGHT = 15;
    private static final int BAR_X_OFFSET = 26;
    private static final int BAR_Y_OFFSET = 5;
    private static final int BAR_WIDTH = 201;
    private static final int BAR_HEIGHT = 5;

    private final boolean horizontal;
    private int observedClawLevel = -1;
    private long rankUpAnimationStartedAt;

    public ExpBar(int xIn, int yIn) {
        this(xIn, yIn, false);
    }

    public ExpBar(int xIn, int yIn, boolean horizontal) {
        super(xIn, yIn, horizontal ? HORIZONTAL_WIDTH : 15, horizontal ? HORIZONTAL_HEIGHT : 123,
                Component.translatable(horizontal ? "text.werewolves.claw_xp.progress" : "text.werewolves.skill_screen.level_progression",
                        (int) Math.ceil((horizontal
                                ? WerewolfPlayer.get(Minecraft.getInstance().player).getClawLevelHandler().getLevelPerc()
                                : WerewolfPlayer.get(Minecraft.getInstance().player).getLevelHandler().getLevelPerc()) * 100)));
        this.horizontal = horizontal;
        this.setTooltip(new MultilineTooltipEx(createToolTip()));
    }

    @Override
    public void renderWidget(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.setTooltip(new MultilineTooltipEx(createToolTip()));
        if (this.horizontal) {
            this.renderHorizontal(graphics);
            return;
        }

        graphics.blit(ICON, this.getX(), this.getY(), 10, 0, this.getWidth(), this.getHeight());

        float perc = WerewolfPlayer.get(Minecraft.getInstance().player).getLevelHandler().getLevelPerc();

        int ySize = ((int) (111 * perc));
        int color = Optional.ofNullable(VampirismAPI.factionPlayerHandler(Minecraft.getInstance().player).getCurrentFaction()).map(IFaction::getColor).orElse(Color.WHITE.getRGB());
        graphics.setColor(((color >> 16) & 0xFF) / 255F, ((color >> 8) & 0xFF) / 255F, (color & 0xFF) / 255F, 1.0F);

        graphics.blit(ICON, this.getX() + 5, this.getY() + 6, 0, 0, 5, 111);
        graphics.blit(ICON, this.getX() + 5, this.getY() + 6 + (111 - ySize), 5, (111 - ySize), 5, ySize);

        graphics.setColor(1, 1, 1, 1);
    }

    private void renderHorizontal(GuiGraphics graphics) {
        ClawLevelHandler handler = WerewolfPlayer.get(Minecraft.getInstance().player).getClawLevelHandler();
        int x = this.getX();
        int y = this.getY();
        int barX = x + BAR_X_OFFSET;
        int barY = y + BAR_Y_OFFSET;
        int clawLevel = handler.getLevel();
        long now = System.currentTimeMillis();
        if (this.observedClawLevel == -1) {
            this.observedClawLevel = clawLevel;
        } else if (clawLevel > this.observedClawLevel) {
            this.rankUpAnimationStartedAt = now;
            this.observedClawLevel = clawLevel;
        } else if (clawLevel < this.observedClawLevel) {
            this.observedClawLevel = clawLevel;
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(CLAW_XP_BACKGROUND, x, y, HORIZONTAL_WIDTH, HORIZONTAL_HEIGHT,
                0, 0, HORIZONTAL_WIDTH, HORIZONTAL_HEIGHT, 256, 256);
        graphics.blit(CLAW_XP_HEAD, x - 1, y - 4, 24, 24,
                0, 0, 24, 24, 24, 24);

        graphics.setColor(1.0F, 0.35F, 0.0F, 1.0F);
        graphics.blitSprite(XP_BAR_BACKGROUND_SPRITE, barX, barY, BAR_WIDTH, BAR_HEIGHT);

        int filledWidth = Mth.clamp(Math.round(BAR_WIDTH * handler.getLevelPerc()), 0, BAR_WIDTH);
        if (filledWidth > 0) {
            graphics.enableScissor(barX, barY, barX + filledWidth, barY + BAR_HEIGHT);
            graphics.blitSprite(XP_BAR_PROGRESS_SPRITE, barX, barY, BAR_WIDTH, BAR_HEIGHT);
            graphics.disableScissor();
        }

        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        long animationElapsed = now - this.rankUpAnimationStartedAt;
        if (this.rankUpAnimationStartedAt > 0
                && animationElapsed < RANK_UP_FRAME_MILLIS * RANK_UP_Y_OFFSETS.length) {
            int frame = (int) (animationElapsed / RANK_UP_FRAME_MILLIS);
            int rankUpY = y + (HORIZONTAL_HEIGHT - RANK_UP_SIZE) / 2 + RANK_UP_Y_OFFSETS[frame];
            graphics.blit(RANK_UP, x + HORIZONTAL_WIDTH - RANK_UP_SIZE, rankUpY,
                    RANK_UP_SIZE, RANK_UP_SIZE, 0, 0, RANK_UP_SIZE, RANK_UP_SIZE, RANK_UP_SIZE, RANK_UP_SIZE);
        }
    }

    public List<Component> createToolTip() {
        if (this.horizontal) {
            return createClawToolTip();
        }

        List<Component> tooltips = new ArrayList<>();
        tooltips.add(Component.translatable("text.werewolves.skill_screen.level_progression_label"));
        LevelHandler handler = WerewolfPlayer.get(Minecraft.getInstance().player).getLevelHandler();
        tooltips.add(Component.translatable("text.werewolves.skill_screen.prey_snatched", handler.getLevelProgress(), handler.getNeededProgress()));
        return tooltips;
    }

    private List<Component> createClawToolTip() {
        ClawLevelHandler handler = WerewolfPlayer.get(Minecraft.getInstance().player).getClawLevelHandler();
        List<Component> tooltips = new ArrayList<>();
        tooltips.add(Component.translatable("text.werewolves.claw_xp.tooltip.title"));
        tooltips.add(Component.translatable("text.werewolves.claw_xp.tooltip.tier", handler.getTier().getDisplayName()));
        tooltips.add(Component.translatable("text.werewolves.claw_xp.tooltip.level", handler.getLevel(), handler.getMaxLevel()));
        if (handler.isMaxLevel()) {
            tooltips.add(Component.translatable("text.werewolves.claw_xp.tooltip.max"));
        } else {
            tooltips.add(Component.translatable("text.werewolves.claw_xp.tooltip.progress",
                    String.format(Locale.ROOT, "%.1f", handler.getProgress()), handler.getNeededProgress(),
                    Math.round(handler.getLevelPerc() * 100)));
            tooltips.add(Component.translatable("text.werewolves.claw_xp.tooltip.remaining",
                    String.format(Locale.ROOT, "%.1f", handler.getRemainingProgress())));
        }
        return tooltips;
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, this.getMessage());
    }
}
