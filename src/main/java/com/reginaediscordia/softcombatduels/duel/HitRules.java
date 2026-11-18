package com.reginaediscordia.softcombatduels.duel;

import com.reginaediscordia.softcombatduels.Config;
import com.reginaediscordia.softcombatduels.ModItems;
import com.reginaediscordia.softcombatduels.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Reglas comunes para decidir si un golpe es "valido" (duelos y batalla campal). */
public final class HitRules {
    private HitRules() {}

    public static boolean combatModLoaded() {
        ModList m = ModList.get();
        return m.isLoaded("bettercombat") || m.isLoaded("epicfight");
    }

    /** True si el atacante esta dentro del arco frontal de la victima (config blockHalfAngle). */
    public static boolean inFront(ServerPlayer victim, Entity attacker) {
        double dx = attacker.getX() - victim.getX(), dz = attacker.getZ() - victim.getZ();
        double yaw = Math.toRadians(victim.getYRot());
        double lx = -Math.sin(yaw), lz = Math.cos(yaw);
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1.0E-4) return true;
        return (dx * lx + dz * lz) / len >= Math.cos(Math.toRadians(Config.BLOCK_HALF_ANGLE.get()));
    }

    public static boolean blocked(ServerPlayer victim, Entity attacker) {
        return victim.isBlocking() && inFront(victim, attacker);
    }

    public static boolean valid(ServerPlayer attacker, ServerPlayer victim, LivingIncomingDamageEvent event, WeaponMode mode) {
        if (mode == WeaponMode.SAFE && !attacker.getMainHandItem().is(ModTags.SOFTCOMBAT_WEAPONS)) return false;
        if (!ModItems.handsAllowed(attacker.getMainHandItem(), attacker.getOffhandItem())) {
            attacker.displayClientMessage(Component.literal("Manos no validas para esta arma."), true);
            return false;
        }
        if (!combatModLoaded() && attacker.getAttackStrengthScale(0.5F) < Config.MIN_ATTACK_CHARGE.get()) return false;
        if (blocked(victim, attacker)) return false;
        // esquiva (Combat Roll): invulnerable durante la voltereta; creativo no cuenta
        if (!victim.getAbilities().invulnerable
                && victim.isInvulnerableTo((ServerLevel) victim.level(), event.getSource())) return false;
        return true;
    }
}
