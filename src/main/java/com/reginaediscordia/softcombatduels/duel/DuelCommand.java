package com.reginaediscordia.softcombatduels.duel;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class DuelCommand {
    private DuelCommand() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /duel challenge <jugador> [3|5] [safe|free]
        var target = Commands.argument("player", EntityArgument.player())
                .executes(c -> challenge(c, 3, WeaponMode.SAFE));
        for (int pts : new int[] {3, 5}) {
            var points = Commands.literal(String.valueOf(pts)).executes(c -> challenge(c, pts, WeaponMode.SAFE));
            points.then(Commands.literal("safe").executes(c -> challenge(c, pts, WeaponMode.SAFE)));
            points.then(Commands.literal("free").executes(c -> challenge(c, pts, WeaponMode.FREE)));
            target.then(points);
        }

        dispatcher.register(Commands.literal("duel")
                .then(Commands.literal("challenge").then(target))
                .then(Commands.literal("accept").executes(c -> {
                    DuelManager.accept(c.getSource().getServer(), c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("deny").executes(c -> {
                    DuelManager.deny(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("leave").executes(c -> {
                    DuelManager.leave(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("status").executes(c -> {
                    DuelManager.status(c.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static int challenge(CommandContext<CommandSourceStack> c, int points, WeaponMode mode) throws CommandSyntaxException {
        ServerPlayer from = c.getSource().getPlayerOrException();
        ServerPlayer to = EntityArgument.getPlayer(c, "player");
        DuelManager.challenge(c.getSource().getServer(), from, to, points, mode);
        return 1;
    }
}
