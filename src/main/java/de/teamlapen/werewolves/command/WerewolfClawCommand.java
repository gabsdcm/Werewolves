package de.teamlapen.werewolves.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import de.teamlapen.lib.lib.util.BasicCommand;
import de.teamlapen.werewolves.entities.player.werewolf.WerewolfPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class WerewolfClawCommand extends BasicCommand {

    public static ArgumentBuilder<CommandSourceStack, ?> register() {
        return Commands.literal("claw")
                .requires(context -> context.hasPermission(PERMISSION_LEVEL_CHEAT))
                .then(Commands.literal("level")
                        .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                .executes(context -> setLevel(
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "level")))));
    }

    private static int setLevel(ServerPlayer player, int level) {
        WerewolfPlayer werewolf = WerewolfPlayer.get(player);
        int actualLevel = werewolf.setClawLevel(level);
        player.sendSystemMessage(Component.translatable(
                "command.werewolves.claw.level.success", actualLevel, werewolf.getClawLevelHandler().getMaxLevel()));
        return 1;
    }
}
