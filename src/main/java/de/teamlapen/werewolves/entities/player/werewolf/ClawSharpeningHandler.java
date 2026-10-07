package de.teamlapen.werewolves.entities.player.werewolf;

import de.teamlapen.werewolves.config.WerewolvesConfig;
import de.teamlapen.werewolves.core.ModActions;
import de.teamlapen.werewolves.core.ModParticles;
import de.teamlapen.werewolves.items.WerewolfClawItem;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Sharpens the worn claw while a transformed werewolf holds sneak and right click on stone or a stonecutter.
 * The client repeats the right click every few ticks while the button is held, which keeps the session alive.
 */
public class ClawSharpeningHandler {

    private static final SoundEvent SHARPEN_SOUND = SoundEvents.GRINDSTONE_USE;
    private static final int SESSION_TIMEOUT_TICKS = 12;
    private static final double MAX_DISTANCE_SQR = 4.5D * 4.5D;
    private static final int PARTICLE_COUNT = 3;
    private static final Map<UUID, Session> SESSIONS = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isShiftKeyDown()
                || !canSharpenOn(event.getLevel().getBlockState(event.getPos()))
                || !canSharpen(player)) {
            return;
        }
        // Cancel for both hands so the stonecutter menu does not open.
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null || !session.pos.equals(event.getPos())) {
            session = new Session(event.getPos());
            SESSIONS.put(player.getUUID(), session);
        }
        session.hitLocation = event.getHitVec().getLocation();
        session.lastClickTick = player.tickCount;
    }

    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        Session session = SESSIONS.get(player.getUUID());
        if (session == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(session.pos);
        if (player.tickCount - session.lastClickTick > SESSION_TIMEOUT_TICKS
                || !player.isShiftKeyDown()
                || player.distanceToSqr(Vec3.atCenterOf(session.pos)) > MAX_DISTANCE_SQR
                || !canSharpenOn(state)
                || !canSharpen(player)) {
            SESSIONS.remove(player.getUUID());
            return;
        }

        double speed = state.is(Blocks.STONECUTTER)
                ? WerewolvesConfig.BALANCE.SKILLS.claw_sharpen_stonecutter_speed.get() : 1.0D;
        session.progress += speed;
        if (session.progress < WerewolvesConfig.BALANCE.SKILLS.claw_sharpen_cycle_ticks.get()) {
            return;
        }
        session.progress = 0.0D;
        finishCycle(player, level, session);
    }

    private static void finishCycle(ServerPlayer player, ServerLevel level, Session session) {
        WerewolfPlayer werewolf = WerewolfPlayer.get(player);
        player.swing(InteractionHand.MAIN_HAND, true);
        level.playSound(null, session.pos, SHARPEN_SOUND, SoundSource.PLAYERS, 0.8F, 1.0F);
        level.sendParticles(ModParticles.WEREWOLF_CLAW_SHARPEN.get(),
                session.hitLocation.x, session.hitLocation.y, session.hitLocation.z,
                PARTICLE_COUNT, 0.1D, 0.1D, 0.1D, 0.0D);
        if (WerewolfClawItem.sharpen(werewolf.getClawSlot().getStack(),
                WerewolvesConfig.BALANCE.SKILLS.claw_sharpen_wear_removed.get())) {
            werewolf.syncClawSlot();
            ModActions.CLAW.get().refreshModifiers(werewolf);
        }
    }

    private static boolean canSharpenOn(BlockState state) {
        return state.is(Blocks.STONECUTTER) || state.is(Blocks.COBBLESTONE) || state.is(BlockTags.BASE_STONE_OVERWORLD);
    }

    private static boolean canSharpen(ServerPlayer player) {
        return WerewolfPlayer.getOpt(player)
                .map(werewolf -> werewolf.getForm().isTransformed()
                        && WerewolfClawItem.isWorn(werewolf.getClawSlot().getStack()))
                .orElse(false);
    }

    private static final class Session {
        private final BlockPos pos;
        private Vec3 hitLocation = Vec3.ZERO;
        private int lastClickTick;
        private double progress;

        private Session(BlockPos pos) {
            this.pos = pos;
        }
    }
}
