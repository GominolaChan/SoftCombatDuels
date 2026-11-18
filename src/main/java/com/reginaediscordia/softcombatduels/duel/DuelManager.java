package com.reginaediscordia.softcombatduels.duel;

import com.reginaediscordia.softcombatduels.Config;
import com.reginaediscordia.softcombatduels.ModItems;
import com.reginaediscordia.softcombatduels.ModTags;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

public final class DuelManager {
    private record Challenge(UUID from, int points, WeaponMode mode, long expiresAt) {}

    private static final Map<UUID, Challenge> CHALLENGES = new HashMap<>(); // clave: retado
    private static final Map<UUID, Duel> DUELS = new HashMap<>();           // clave: cada participante
    private static MinecraftServer server;
    private static long now = 0;
    private static final int MIN_READY_TICKS = 10;

    private DuelManager() {}

    public static Duel getDuel(ServerPlayer p) { return DUELS.get(p.getUUID()); }

    private static void msg(ServerPlayer p, String s) { p.sendSystemMessage(Component.literal("[SoftCombat Duels] " + s)); }
    private static void bar(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }
    private static String name(ServerPlayer p) { return p.getName().getString(); }
    /** Cara del jugador como "sprite" de texto (Minecraft 1.21.9+). Si falla, no se muestra nada. */
    static Component head(String playerName) {
        try {
            var json = JsonParser.parseString("{\"object\":\"player\",\"player\":{\"name\":\"" + playerName + "\"},\"hat\":true}");
            return ComponentSerialization.CODEC.parse(JsonOps.INSTANCE, json).result().orElse(Component.empty());
        } catch (Exception e) {
            return Component.empty();
        }
    }

    static void sendTitle(ServerPlayer p, Component title, Component subtitle, int stayTicks) {
        p.connection.send(new ClientboundSetTitlesAnimationPacket(5, stayTicks, 10));
        p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        p.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    /** Titulo grande en pantalla: [cara A] 2 - 1 [cara B], con un subtitulo. */
    private static void announce(Duel d, ServerPlayer pa, ServerPlayer pb, String subtitle, int stayTicks) {
        Component title = Component.empty()
                .append(head(name(pa)))
                .append(Component.literal("  " + d.scoreA + " - " + d.scoreB + "  ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .append(head(name(pb)));
        Component sub = Component.literal(subtitle).withStyle(ChatFormatting.YELLOW);
        for (ServerPlayer p : new ServerPlayer[] {pa, pb}) {
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, stayTicks, 10));
            p.connection.send(new ClientboundSetSubtitleTextPacket(sub));
            p.connection.send(new ClientboundSetTitleTextPacket(title));
        }
    }

    private static boolean hasSoftWeapon(ServerPlayer p) { return p.getMainHandItem().is(ModTags.SOFTCOMBAT_WEAPONS); }

    private static boolean combatModLoaded() {
        ModList m = ModList.get();
        return m.isLoaded("bettercombat") || m.isLoaded("epicfight");
    }

    // ---------- Retos ----------

    public static void challenge(MinecraftServer s, ServerPlayer from, ServerPlayer to, int points, WeaponMode mode) {
        server = s;
        if (from == to) { msg(from, "No puedes retarte a ti mismo."); return; }
        if (DUELS.containsKey(from.getUUID()) || DUELS.containsKey(to.getUUID())) { msg(from, "Alguno de los dos ya esta en un duelo."); return; }
        if (!from.level().dimension().equals(to.level().dimension()) || from.distanceTo(to) > Config.MAX_DISTANCE.get()) {
            msg(from, "El jugador esta demasiado lejos (max. " + Config.MAX_DISTANCE.get() + " bloques)."); return;
        }
        CHALLENGES.put(to.getUUID(), new Challenge(from.getUUID(), points,  mode, now + Config.CHALLENGE_TIMEOUT_SECONDS.get() * 20L));
        String m = mode == WeaponMode.SAFE ? "seguro (armas SoftCombat)" : "libre (cualquier arma)";
        msg(from, "Reto enviado a " + name(to) + " (a " + points + " puntos, modo " + m + ").");
        msg(to, name(from) + " te reta a un duelo a " + points + " puntos, modo " + m + ". Escribe /duel accept o /duel deny.");
    }

    public static void accept(MinecraftServer s, ServerPlayer to) {
        server = s;
        Challenge c = CHALLENGES.remove(to.getUUID());
        if (c == null) { msg(to, "No tienes ningun reto pendiente."); return; }
        ServerPlayer from = s.getPlayerList().getPlayer(c.from());
        if (from == null) { msg(to, "El retador ya no esta conectado."); return; }
        if (DUELS.containsKey(from.getUUID()) || DUELS.containsKey(to.getUUID())) { msg(to, "Alguno de los dos ya esta en un duelo."); return; }
        if (!from.level().dimension().equals(to.level().dimension()) || from.distanceTo(to) > Config.MAX_DISTANCE.get()) {
            msg(to, "Estais demasiado lejos para duelear."); return;
        }
        if (c.mode() == WeaponMode.SAFE && (!hasSoftWeapon(from) || !hasSoftWeapon(to))) {
            String t = "Modo seguro: ambos necesitais un arma SoftCombat en la mano principal.";
            msg(from, t); msg(to, t); return;
        }
        double dx = to.getX() - from.getX(), dz = to.getZ() - from.getZ();
        double[] posFrom = {from.getX(), from.getY(), from.getZ(), Math.toDegrees(Math.atan2(-dx, dz))};
        double[] posTo = {to.getX(), to.getY(), to.getZ(), Math.toDegrees(Math.atan2(dx, -dz))};
        Duel d = new Duel(from.getUUID(), to.getUUID(), c.points(), c.mode(), from.level().dimension(), posFrom, posTo);
        DUELS.put(from.getUUID(), d);
        DUELS.put(to.getUUID(), d);
        d.resetPending = true; // coloca a ambos de cara en sus posiciones
        announce(d, from, to, "Duelo a " + c.points() + " puntos", 50);
        String t = "¡Duelo " + name(from) + " vs " + name(to) + " a " + c.points() + " puntos! Bloquead con el arma o esperad "
                + Config.COUNTDOWN_SECONDS.get() + " s para empezar.";
        msg(from, t); msg(to, t);
    }

    public static void deny(ServerPlayer to) {
        Challenge c = CHALLENGES.remove(to.getUUID());
        if (c == null) { msg(to, "No tienes ningun reto pendiente."); return; }
        msg(to, "Reto rechazado.");
        if (server != null) {
            ServerPlayer from = server.getPlayerList().getPlayer(c.from());
            if (from != null) msg(from, name(to) + " ha rechazado tu reto.");
        }
    }

    /** El que abandona pierde; gana el rival. */
    public static void leave(ServerPlayer p) {
        Duel d = getDuel(p);
        if (d == null) { msg(p, "No estas en ningun duelo."); return; }
        ServerPlayer other = lookup(d.a.equals(p.getUUID()) ? d.b : d.a);
        stop(d, "Duelo terminado: " + name(p) + " abandona." + (other != null ? " ¡Gana " + name(other) + "!" : ""));
    }

    public static void status(ServerPlayer p) {
        Duel d = getDuel(p);
        if (d == null) { msg(p, "No estas en ningun duelo."); return; }
        ServerPlayer a = lookup(d.a), b = lookup(d.b);
        msg(p, (a != null ? name(a) : "?") + " " + d.scoreA + " - " + d.scoreB + " " + (b != null ? name(b) : "?") + " (a " + d.target + ")");
    }

    public static void onLogout(ServerPlayer p) {
        CHALLENGES.remove(p.getUUID());
        Duel d = getDuel(p);
        if (d != null) stop(d, "Duelo cancelado: " + name(p) + " se desconecto.");
    }

    // ---------- Golpes ----------

    /** Se llama desde el evento de daño cuando el rival golpea directamente en fase ACTIVE. */
    public static void handleHit(Duel d, ServerPlayer attacker, ServerPlayer victim, LivingIncomingDamageEvent event) {
        event.setAmount(0.0F); // en duelo nunca se quita vida: el golpe da hit y ya
        if (d.mode == WeaponMode.SAFE && !hasSoftWeapon(attacker)) return;
        if (!ModItems.handsAllowed(attacker.getMainHandItem(), attacker.getOffhandItem())) {
            bar(attacker, "Manos no validas: dobles solo daga/espada corta; el mandoble pide la mano secundaria libre.");
            return;
        }
        if (!combatModLoaded() && attacker.getAttackStrengthScale(0.5F) < Config.MIN_ATTACK_CHARGE.get()) return;
        if (isBlocked(victim, attacker)) return;
        // Esquiva de Combat Roll (invulnerable durante la voltereta): no puntua. Creativo no cuenta como esquiva.
        if (!victim.getAbilities().invulnerable
                && victim.isInvulnerableTo((ServerLevel) victim.level(), event.getSource())) return;

        boolean aScores = attacker.getUUID().equals(d.a);
        if (aScores) d.scoreA++; else d.scoreB++;
        int score = aScores ? d.scoreA : d.scoreB;
        String board = d.scoreA + " - " + d.scoreB;
        ServerPlayer pa = aScores ? attacker : victim, pb = aScores ? victim : attacker;
        if (score >= d.target) {
            announce(d, pa, pb, "¡" + name(attacker) + " gana el duelo!", 80);
            stop(d, "¡" + name(attacker) + " gana el duelo " + board + "!");
        } else {
            String t = "¡Punto para " + name(attacker) + "! Marcador " + board;
            msg(attacker, t); msg(victim, t);
            announce(d, pa, pb, "¡Punto para " + name(attacker) + "!", 50);
            d.phase = Duel.Phase.COUNTDOWN; // ronda nueva: se recolocan en el siguiente tick
            d.ticks = 0;
            d.resetPending = true;
        }
    }

    private static boolean isBlocked(ServerPlayer victim, ServerPlayer attacker) {
        if (!victim.isBlocking()) return false;
        double dx = attacker.getX() - victim.getX(), dz = attacker.getZ() - victim.getZ();
        double yaw = Math.toRadians(victim.getYRot());
        double lx = -Math.sin(yaw), lz = Math.cos(yaw);
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) return true;
        double cos = (dx * lx + dz * lz) / len;
        return cos >= Math.cos(Math.toRadians(Config.BLOCK_HALF_ANGLE.get()));
    }

    // ---------- Tick ----------

    public static void tick(MinecraftServer s) {
        server = s;
        now++;
        CHALLENGES.values().removeIf(c -> c.expiresAt() < now);
        for (Duel d : new HashSet<>(DUELS.values())) {
            ServerPlayer a = s.getPlayerList().getPlayer(d.a);
            ServerPlayer b = s.getPlayerList().getPlayer(d.b);
            if (a == null || b == null || !a.level().dimension().equals(d.dimension) || !b.level().dimension().equals(d.dimension)) {
                stop(d, "Duelo cancelado: un jugador se desconecto o cambio de dimension.");
                continue;
            }
            if (d.phase != Duel.Phase.COUNTDOWN) continue;

            if (d.resetPending) {
                teleport(a, d.posA);
                teleport(b, d.posB);
                d.resetPending = false;
            }
            d.ticks++;
            int total = Config.COUNTDOWN_SECONDS.get() * 20;
            boolean ready = d.ticks >= MIN_READY_TICKS && a.isBlocking() && b.isBlocking();
            if (ready || d.ticks >= total) {
                d.phase = Duel.Phase.ACTIVE;
                bar(a, "¡LUCHAD!"); bar(b, "¡LUCHAD!");
            } else if (d.ticks % 10 == 0) {
                int left = (total - d.ticks + 19) / 20;
                String t = "Ronda " + (d.scoreA + d.scoreB + 1) + " - bloquead o esperad " + left + " s";
                bar(a, t); bar(b, t);
            }
        }
    }

    private static void teleport(ServerPlayer p, double[] pos) {
        p.connection.teleport(pos[0], pos[1], pos[2], (float) pos[3], 0.0F);
        p.setDeltaMovement(0.0D, 0.0D, 0.0D);
        p.resetFallDistance();
    }

    private static ServerPlayer lookup(UUID id) { return server == null ? null : server.getPlayerList().getPlayer(id); }

    private static void stop(Duel d, String message) {
        DUELS.remove(d.a);
        DUELS.remove(d.b);
        ServerPlayer a = lookup(d.a), b = lookup(d.b);
        if (a != null) msg(a, message);
        if (b != null) msg(b, message);
    }
}
