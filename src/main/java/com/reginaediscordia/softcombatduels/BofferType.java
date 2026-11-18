package com.reginaediscordia.softcombatduels;

/** Tipos de Boffer. El daño es igual en todos (siempre 0); cambia el alcance y las reglas de manos. */
public enum BofferType {
    //            id                      alcance  doble   2 manos  escudo
    DAGGER("boffer_dagger",               2.25D,   true,   false,   true),
    SHORT_SWORD("boffer_short_sword",     3.0D,    true,   false,   true),
    BASTARD_SWORD("boffer_bastard_sword", 3.25D,   false,  false,   false),
    GREATSWORD("boffer_greatsword",       3.5D,    false,  true,    false),
    QTIP("boffer_qtip",                   3.4D,    false,  true,    false),
    SPEAR("boffer_spear",                 3.6D,    false,  false,   true);

    public static final double DEFAULT_REACH = 3.0D; // alcance base de Minecraft

    public final String id;
    public final double reach;
    public final boolean dualWield;
    public final boolean twoHanded;
    public final boolean shield;

    BofferType(String id, double reach, boolean dualWield, boolean twoHanded, boolean shield) {
        this.id = id; this.reach = reach; this.dualWield = dualWield; this.twoHanded = twoHanded; this.shield = shield;
    }
}
