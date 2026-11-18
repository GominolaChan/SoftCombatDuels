package com.reginaediscordia.softcombatduels.duel;

import com.reginaediscordia.softcombatduels.Config;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Parry: si el golpe llega justo al empezar a bloquear (clic derecho), se anula y se desequilibra al atacante. */
public final class ParryManager {
    private static final Map<UUID, Long> USING_SINCE = new HashMap<>();
    private static long tick = 0;

    private ParryManager() {}

    public static void tick(MinecraftServer server) {
        tick++;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            boolean using = p.isUsingItem() && p.getUseItem().has(DataComponents.BLOCKS_ATTACKS);
            if (using) USING_SINCE.putIfAbsent(p.getUUID(), tick); else USING_SINCE.remove(p.getUUID());
        }
    }

    /** True si el golpe ha sido parado (el llamador debe cancelar el daño). */
    public static boolean tryParry(ServerPlayer victim, LivingEntity attacker) {
        if (!Config.PARRY_ENABLED.get()) return false;
        Long since = USING_SINCE.get(victim.getUUID());
        if (since == null || tick - since > Config.PARRY_WINDOW_TICKS.get()) return false;
        if (!HitRules.inFront(victim, attacker)) return false;
        // el atacante sale rebotado y pierde la carga de su ataque
        attacker.knockback(0.6D, victim.getX() - attacker.getX(), victim.getZ() - attacker.getZ());
        attacker.hurtMarked = true;
        if (attacker instanceof Player p) {
            p.resetAttackStrengthTicker();
            p.displayClientMessage(Component.literal("¡Te han parado el golpe!"), true);
        }
        victim.displayClientMessage(Component.literal("¡PARRY!"), true);
        return true;
    }
}
