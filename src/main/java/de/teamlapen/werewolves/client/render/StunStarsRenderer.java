package de.teamlapen.werewolves.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.teamlapen.werewolves.api.WResourceLocation;
import de.teamlapen.werewolves.core.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

/**
 * Draws an animated ring above the head of every stunned living entity.
 */
public class StunStarsRenderer {

    private static final ResourceLocation RING_TEXTURE = WResourceLocation.mod("textures/effect/stun_ring_sheet.png");
    // The sheet is vertical: FRAME_COUNT square frames stacked top to bottom.
    private static final int FRAME_COUNT = 8;
    private static final float TICKS_PER_FRAME = 2f;
    private static final float SIZE_IN_BLOCKS = 0.9f;
    private static final float HEAD_CLEARANCE = 0.3f;

    @SubscribeEvent
    public void onRenderLivingPost(@NotNull RenderLivingEvent.Post<?, ?> event) {
        LivingEntity entity = event.getEntity();
        if (!entity.hasEffect(ModEffects.STUN)) {
            return;
        }
        float time = entity.tickCount + event.getPartialTick();
        int frame = (int) (time / TICKS_PER_FRAME) % FRAME_COUNT;
        VertexConsumer consumer = event.getMultiBufferSource().getBuffer(RenderType.entityCutoutNoCull(RING_TEXTURE));
        this.renderRing(event.getPoseStack(), consumer, entity.getBbHeight() + HEAD_CLEARANCE, frame);
    }

    private void renderRing(@NotNull PoseStack poseStack, @NotNull VertexConsumer consumer, float y, int frame) {
        poseStack.pushPose();
        poseStack.translate(0, y, 0);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        float half = SIZE_IN_BLOCKS / 2;
        float v0 = (float) frame / FRAME_COUNT;
        float v1 = (float) (frame + 1) / FRAME_COUNT;
        this.addVertex(consumer, pose, matrix, -half, -half, 0, v1);
        this.addVertex(consumer, pose, matrix, half, -half, 1, v1);
        this.addVertex(consumer, pose, matrix, half, half, 1, v0);
        this.addVertex(consumer, pose, matrix, -half, half, 0, v0);
        poseStack.popPose();
    }

    private void addVertex(VertexConsumer consumer, PoseStack.Pose pose, Matrix4f matrix, float x, float y, float u, float v) {
        consumer.addVertex(matrix, x, y, 0)
                .setColor(-1)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0, 1, 0);
    }
}
