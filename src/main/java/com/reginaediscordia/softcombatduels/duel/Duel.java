package com.reginaediscordia.softcombatduels.duel;

import java.util.UUID;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

public final class Duel {
    public enum Phase { COUNTDOWN, ACTIVE }

    public final UUID a, b;
    public final int target;
    public final WeaponMode mode;
    public final ResourceKey<Level> dimension;
    /** x, y, z, yaw de cada jugador al empezar; a ellas se vuelve tras cada punto. */
    public final double[] posA, posB;

    public int scoreA, scoreB, ticks;
    public Phase phase = Phase.COUNTDOWN;
    public boolean resetPending;

    public Duel(UUID a, UUID b, int target, WeaponMode mode, ResourceKey<Level> dimension, double[] posA, double[] posB) {
        this.a = a; this.b = b; this.target = target; this.mode = mode;
        this.dimension = dimension; this.posA = posA; this.posB = posB;
    }

    public boolean isOpponent(ServerPlayer x, ServerPlayer y) {
        return (x.getUUID().equals(a) && y.getUUID().equals(b)) || (x.getUUID().equals(b) && y.getUUID().equals(a));
    }
}
