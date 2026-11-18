package com.reginaediscordia.softcombatduels.duel;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** /softcombat zone ... y /softcombat melee ...  (solo operadores). */
public final class SoftCombatCommand {
    private SoftCombatCommand() {}

    public static void register(RegisterCommandsEvent event) {
        var zone = Commands.literal("zone")
                .then(Commands.literal("pos1").executes(c -> ZoneManager.setPos(c.getSource().getPlayerOrException(), 1)))
                .then(Commands.literal("pos2").executes(c -> ZoneManager.setPos(c.getSource().getPlayerOrException(), 2)))
                .then(Commands.literal("create").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> ZoneManager.create(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")))))
                .then(Commands.literal("remove").then(Commands.argument("name", StringArgumentType.word())
                        .executes(c -> ZoneManager.remove(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "name")))))
                .then(Commands.literal("list").executes(c -> ZoneManager.list(c.getSource().getPlayerOrException())));

        var lives = Commands.argument("lives", IntegerArgumentType.integer(1, 20))
                .executes(c -> start(c, IntegerArgumentType.getInteger(c, "lives"), WeaponMode.SAFE))
                .then(Commands.literal("safe").executes(c -> start(c, IntegerArgumentType.getInteger(c, "lives"), WeaponMode.SAFE)))
                .then(Commands.literal("free").executes(c -> start(c, IntegerArgumentType.getInteger(c, "lives"), WeaponMode.FREE)));
        var melee = Commands.literal("melee")
                .then(Commands.literal("start").then(Commands.argument("zone", StringArgumentType.word())
                        .executes(c -> start(c, 3, WeaponMode.SAFE))
                        .then(lives)))
                .then(Commands.literal("stop").executes(c -> MeleeManager.stop(c.getSource().getPlayerOrException())));

        event.getDispatcher().register(Commands.literal("softcombat")
                .requires(src -> src.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(zone)
                .then(melee));
    }

    private static int start(CommandContext<CommandSourceStack> c, int lives, WeaponMode mode) throws CommandSyntaxException {
        return MeleeManager.start(c.getSource().getServer(), c.getSource().getPlayerOrException(),
                StringArgumentType.getString(c, "zone"), lives, mode);
    }
}
