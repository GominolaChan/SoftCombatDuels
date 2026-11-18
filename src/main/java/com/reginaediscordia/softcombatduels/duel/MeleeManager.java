package com.reginaediscordia.softcombatduels.duel;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Batalla campal: todos contra todos dentro de una zona; cada golpe valido quita una vida; gana el ultimo en pie. */
public final class MeleeManager {
    private enum Phase { COUNTDOWN, ACTIVE }

    private static final class Melee {
        String zone; WeaponMode mode; Phase phase = Phase.COUNTDOWN; int ticks = 0; long activeTicks = 0;
        final Map<UUID, Integer> lives = new LinkedHashMap<>();
        final Set<UUID> alive = new LinkedHashSet<>();
        final Map<UUID, GameType> prev = new HashMap<>();
        final Map<UUID, Long> lastHit = new HashMap<>();
    }

    private static Melee current;
    private static MinecraftServer server;
    private static final Map<UUID, GameType> PENDING = new HashMap<>();
    private static final int COUNTDOWN_TICKS = 100;

    private MeleeManager() {}

    private static void msg(ServerPlayer p, String s) { p.sendSystemMessage(Component.literal("[Batalla campal] " + s)); }
    private static String name(ServerPlayer p) { return p.getName().getString(); }

    public static boolean isParticipant(ServerPlayer p) { return current != null && current.lives.containsKey(p.getUUID()); }

    private static void broadcast(String s) {
        if (current == null || server == null) return;
        for (UUID id : current.lives.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) msg(p, s);
        }
    }

    private static void titleAll(Component title, Component subtitle, int stay) {
        if (current == null || server == null) return;
        for (UUID id : current.lives.keySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) DuelManager.sendTitle(p, title, subtitle, stay);
        }
    }

    public static int start(MinecraftServer s, ServerPlayer op, String zoneName, int lives, WeaponMode mode) {
        server = s;
        if (current != null) { msg(op, "Ya hay una batalla campal en marcha (/softcombat melee stop)."); return 0; }
        ZoneManager.Zone zone = ZoneManager.find(zoneName);
        if (zone == null) { msg(op, "No existe la zona " + zoneName + "."); return 0; }
        Melee m = new Melee();
        m.zone = zone.name; m.mode = mode;
        for (ServerPlayer p : s.getPlayerList().getPlayers()) {
            ZoneManager.Zone z = ZoneManager.zoneAt(p);
            if (z == null || !z.name.equals(zone.name) || DuelManager.getDuel(p) != null) continue;
            m.lives.put(p.getUUID(), lives);
            m.alive.add(p.getUUID());
            m.prev.put(p.getUUID(), p.gameMode.getGameModeForPlayer());
        }
        if (m.alive.size() < 2) { msg(op, "Hacen falta al menos 2 jugadores dentro de la zona."); return 0; }
        current = m;
        broadcast("¡Batalla campal en «" + zone.name + "»! " + m.alive.size() + " jugadores, " + lives + " vidas cada uno, modo "
                + (mode == WeaponMode.SAFE ? "seguro" : "libre") + ". Empieza en 5 s.");
        return 1;
    }

    public static int stop(ServerPlayer op) {
        if (current == null) { msg(op, "No hay ninguna batalla campal."); return 0; }
        broadcast("Batalla campal cancelada.");
        restoreAll();
        current = null;
        return 1;
    }

    private static void restoreAll() {
        for (Map.Entry<UUID, GameType> e : current.prev.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(e.getKey());
            if (p != null) p.setGameMode(e.getValue());
            else PENDING.put(e.getKey(), e.getValue());
        }
    }

    public static void handleHit(ServerPlayer attacker, ServerPlayer victim, LivingIncomingDamageEvent event) {
        Melee m = current;
        if (m == null || m.phase != Phase.ACTIVE || !m.alive.contains(attacker.getUUID()) || !m.alive.contains(victim.getUUID())) {
            event.setCanceled(true);
            return;
        }
        event.setAmount(0.0F); // nunca se quita vida real: el golpe da hit y ya
        if (!HitRules.valid(attacker, victim, event, m.mode)) return;
        long last = m.lastHit.getOrDefault(victim.getUUID(), -100L);
        if (m.activeTicks - last < 10) return;
        m.lastHit.put(victim.getUUID(), m.activeTicks);
        int left = m.lives.merge(victim.getUUID(), -1, Integer::sum);
        broadcast(name(attacker) + " golpea a " + name(victim) + " (" + Math.max(left, 0) + " vidas)");
        if (left <= 0) eliminate(victim.getUUID(), name(victim) + " eliminado");
    }

    private static void eliminate(UUID id, String message) {
        Melee m = current;
        if (m == null || !m.alive.remove(id)) return;
        ServerPlayer p = server.getPlayerList().getPlayer(id);
        if (p != null) p.setGameMode(GameType.SPECTATOR);
        String who = p != null ? name(p) : "?";
        titleAll(Component.empty().append(DuelManager.head(who)).append(Component.literal(" eliminado")),
                Component.literal("Quedan " + m.alive.size() + " jugadores"), 40);
        broadcast(message + ". Quedan " + m.alive.size() + ".");
        if (m.alive.size() <= 1) finish();
    }

    private static void finish() {
        Melee m = current;
        ServerPlayer winner = null;
        for (UUID id : m.alive) winner = server.getPlayerList().getPlayer(id);
        if (winner != null) {
            titleAll(Component.empty().append(DuelManager.head(name(winner))).append(Component.literal(" ¡GANA!")),
                    Component.literal("Batalla campal en " + m.zone), 100);
            broadcast("¡" + name(winner) + " gana la batalla campal!");
        } else {
            broadcast("La batalla campal termina sin ganador.");
        }
        restoreAll();
        current = null;
    }

    public static void tick(MinecraftServer s) {
        server = s;
        Melee m = current;
        if (m == null) return;
        if (m.phase == Phase.COUNTDOWN) {
            m.ticks++;
            if (m.ticks % 20 == 0 && m.ticks < COUNTDOWN_TICKS) {
                int left = (COUNTDOWN_TICKS - m.ticks) / 20;
                for (UUID id : m.lives.keySet()) {
                    ServerPlayer p = s.getPlayerList().getPlayer(id);
                    if (p != null) p.displayClientMessage(Component.literal("Batalla campal: empieza en " + left + " s"), true);
                }
            }
            if (m.ticks >= COUNTDOWN_TICKS) {
                m.phase = Phase.ACTIVE;
                titleAll(Component.literal("¡YA!"), Component.literal("Último en pie gana"), 30);
            }
            return;
        }
        m.activeTicks++;
        if (m.activeTicks % 20 != 0) return;
        for (UUID id : new LinkedHashSet<>(m.alive)) {
            ServerPlayer p = s.getPlayerList().getPlayer(id);
            if (p == null) { eliminate(id, "Un jugador se desconecto"); continue; }
            ZoneManager.Zone z = ZoneManager.zoneAt(p);
            if (z == null || !z.name.equals(m.zone)) { eliminate(id, name(p) + " salio de la zona"); continue; }
            if (current == null) return;
            p.displayClientMessage(Component.literal("Vidas: " + m.lives.get(id) + " | Quedan " + m.alive.size() + " jugadores"), true);
        }
    }

    public static void onLogout(ServerPlayer p) {
        if (current == null || !isParticipant(p)) return;
        PENDING.put(p.getUUID(), current.prev.getOrDefault(p.getUUID(), GameType.SURVIVAL));
        eliminate(p.getUUID(), name(p) + " se desconecto");
    }

    public static void onLogin(ServerPlayer p) {
        GameType g = PENDING.get(p.getUUID());
        if (g == null) return;
        if (current != null && isParticipant(p)) { p.setGameMode(GameType.SPECTATOR); return; }
        p.setGameMode(g);
        PENDING.remove(p.getUUID());
    }
}
