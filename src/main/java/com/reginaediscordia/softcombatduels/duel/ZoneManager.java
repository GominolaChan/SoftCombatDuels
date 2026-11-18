package com.reginaediscordia.softcombatduels.duel;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Zonas SoftCombat: dentro de ellas los golpes entre jugadores dan "hit" pero no quitan vida. Se guardan por mundo. */
public final class ZoneManager {
    public static final class Zone {
        public String name, dimension;
        public int minX, minY, minZ, maxX, maxY, maxZ;

        boolean contains(String dim, double x, double y, double z) {
            return dimension.equals(dim) && x >= minX && x < maxX + 1 && y >= minY && y < maxY + 1 && z >= minZ && z < maxZ + 1;
        }
    }

    private static final Gson GSON = new Gson();
    private static final List<Zone> ZONES = new ArrayList<>();
    private static final Map<UUID, int[]> POS1 = new HashMap<>(), POS2 = new HashMap<>();
    private static final Map<UUID, String> CURRENT = new HashMap<>();
    private static Path file;
    private static int counter = 0;

    private ZoneManager() {}

    public static void load(MinecraftServer server) {
        file = server.getWorldPath(LevelResource.ROOT).resolve("softcombatduels_zones.json");
        ZONES.clear();
        try {
            if (Files.exists(file)) {
                List<Zone> loaded = GSON.fromJson(Files.readString(file), new TypeToken<List<Zone>>() {}.getType());
                if (loaded != null) ZONES.addAll(loaded);
            }
        } catch (Exception e) {
            System.err.println("[SoftCombat Duels] No se pudieron cargar las zonas: " + e);
        }
    }

    private static void save() {
        try {
            if (file != null) Files.writeString(file, GSON.toJson(ZONES));
        } catch (Exception e) {
            System.err.println("[SoftCombat Duels] No se pudieron guardar las zonas: " + e);
        }
    }

    private static void msg(ServerPlayer p, String s) { p.sendSystemMessage(Component.literal("[SoftCombat Duels] " + s)); }

    public static int setPos(ServerPlayer p, int which) {
        int[] pos = {p.blockPosition().getX(), p.blockPosition().getY(), p.blockPosition().getZ()};
        (which == 1 ? POS1 : POS2).put(p.getUUID(), pos);
        msg(p, "Punto " + which + " = " + pos[0] + ", " + pos[1] + ", " + pos[2]);
        return 1;
    }

    public static int create(ServerPlayer p, String name) {
        int[] a = POS1.get(p.getUUID()), b = POS2.get(p.getUUID());
        if (a == null || b == null) { msg(p, "Primero marca los dos puntos: /softcombat zone pos1 y pos2."); return 0; }
        if (find(name) != null) { msg(p, "Ya existe una zona llamada " + name + "."); return 0; }
        Zone z = new Zone();
        z.name = name; z.dimension = p.level().dimension().toString();
        z.minX = Math.min(a[0], b[0]); z.maxX = Math.max(a[0], b[0]);
        z.minY = Math.min(a[1], b[1]); z.maxY = Math.max(a[1], b[1]);
        z.minZ = Math.min(a[2], b[2]); z.maxZ = Math.max(a[2], b[2]);
        ZONES.add(z);
        save();
        msg(p, "Zona SoftCombat «" + name + "» creada.");
        return 1;
    }

    public static int remove(ServerPlayer p, String name) {
        Zone z = find(name);
        if (z == null) { msg(p, "No existe la zona " + name + "."); return 0; }
        ZONES.remove(z);
        save();
        msg(p, "Zona «" + name + "» eliminada.");
        return 1;
    }

    public static int list(ServerPlayer p) {
        if (ZONES.isEmpty()) { msg(p, "No hay zonas SoftCombat."); return 1; }
        for (Zone z : ZONES) msg(p, z.name + ": " + z.minX + "," + z.minY + "," + z.minZ + " a " + z.maxX + "," + z.maxY + "," + z.maxZ);
        return 1;
    }

    public static Zone find(String name) {
        for (Zone z : ZONES) if (z.name.equalsIgnoreCase(name)) return z;
        return null;
    }

    public static Zone zoneAt(ServerPlayer p) {
        String dim = p.level().dimension().toString();
        for (Zone z : ZONES) if (z.contains(dim, p.getX(), p.getY(), p.getZ())) return z;
        return null;
    }

    /** Avisa al entrar y salir de una zona (cada medio segundo). */
    public static void tick(MinecraftServer server) {
        if (++counter % 10 != 0 || ZONES.isEmpty()) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            Zone z = zoneAt(p);
            String now = z == null ? null : z.name, before = CURRENT.get(p.getUUID());
            if ((now == null ? "" : now).equals(before == null ? "" : before)) continue;
            if (now == null) CURRENT.remove(p.getUUID()); else CURRENT.put(p.getUUID(), now);
            p.displayClientMessage(Component.literal(now != null
                    ? "Zona SoftCombat «" + now + "»: los golpes entre jugadores no quitan vida"
                    : "Has salido de la zona SoftCombat"), true);
        }
    }
}
