package com.reginaediscordia.softcombatduels;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue COUNTDOWN_SECONDS = B
            .comment("Segundos de cuenta atras al inicio de cada ronda (si ambos bloquean antes, empieza antes).")
            .defineInRange("countdownSeconds", 3, 1, 30);

    public static final ModConfigSpec.DoubleValue MIN_ATTACK_CHARGE = B
            .comment("Carga minima del cooldown de ataque (0-1) para que un golpe puntue. 0 = cualquier golpe puntua.",
                    "Se ignora automaticamente si Better Combat o Epic Fight estan instalados.")
            .defineInRange("minAttackCharge", 0.9D, 0.0D, 1.0D);

    public static final ModConfigSpec.IntValue BLOCK_HALF_ANGLE = B
            .comment("Angulo (grados, a cada lado de la mirada) en el que el bloqueo cubre al jugador. 90 = como el escudo.")
            .defineInRange("blockHalfAngle", 90, 10, 180);

    public static final ModConfigSpec.IntValue CHALLENGE_TIMEOUT_SECONDS = B
            .comment("Segundos que dura un reto antes de caducar.")
            .defineInRange("challengeTimeoutSeconds", 30, 5, 300);

    public static final ModConfigSpec.IntValue MAX_DISTANCE = B
            .comment("Distancia maxima (bloques) entre retador y retado para poder iniciar un duelo.")
            .defineInRange("maxDistance", 32, 2, 256);

    public static final ModConfigSpec.BooleanValue SOFT_WEAPONS_NEVER_DAMAGE = B
            .comment("Las armas del tag softcombatduels:softcombat_weapons (boffers) dan 'hit' pero nunca quitan vida.")
            .define("softWeaponsNeverDamage", true);

    public static final ModConfigSpec.BooleanValue GREATSWORD_NEEDS_FREE_OFFHAND = B
            .comment("El mandoble y el Q-Tip son a dos manos: si es true, solo valen con la mano secundaria vacia.")
            .define("greatswordNeedsFreeOffhand", true);

    public static final ModConfigSpec.BooleanValue ALLOW_SHIELD = B
            .comment("Permite llevar un escudo en la mano secundaria con la daga o la espada corta.")
            .define("allowShieldWithDaggerAndShortSword", true);

    public static final ModConfigSpec.BooleanValue ENFORCE_HAND_RULES = B
            .comment("Si es true, al equipar una combinacion de manos no permitida (p.ej. bastarda + otra arma) el objeto de la mano secundaria vuelve al inventario.")
            .define("enforceHandRules", true);

    public static final ModConfigSpec.BooleanValue PARRY_ENABLED = B
            .comment("Parry: si el golpe llega justo al empezar a bloquear (clic derecho), se anula y se desequilibra al atacante.")
            .define("parryEnabled", true);

    public static final ModConfigSpec.IntValue PARRY_WINDOW_TICKS = B
            .comment("Ventana del parry en ticks desde que empiezas a bloquear (20 ticks = 1 s).")
            .defineInRange("parryWindowTicks", 8, 1, 40);

    public static final ModConfigSpec SPEC = B.build();
}
