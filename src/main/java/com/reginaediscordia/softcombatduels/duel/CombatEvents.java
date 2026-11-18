package com.reginaediscordia.softcombatduels.duel;

import com.reginaediscordia.softcombatduels.Config;
import com.reginaediscordia.softcombatduels.ModItems;
import com.reginaediscordia.softcombatduels.ModTags;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

public final class CombatEvents {
    private CombatEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(CombatEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onServerTick);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onLogout);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onLogin);
        NeoForge.EVENT_BUS.addListener(CombatEvents::onServerStarted);
        NeoForge.EVENT_BUS.addListener(DuelCommand::register);
        NeoForge.EVENT_BUS.addListener(SoftCombatCommand::register);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        ZoneManager.load(event.getServer());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        ParryManager.tick(event.getServer());
        ZoneManager.tick(event.getServer());
        MeleeManager.tick(event.getServer());
        DuelManager.tick(event.getServer());
    }

    /** Si la combinacion de manos no esta permitida, la mano secundaria vuelve al inventario. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Config.ENFORCE_HAND_RULES.get()) return;
        ItemStack off = p.getOffhandItem();
        if (off.isEmpty() || ModItems.handsAllowed(p.getMainHandItem(), off)) return;
        ItemStack moved = off.copy();
        p.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (!p.getInventory().add(moved)) p.drop(moved, false);
        p.displayClientMessage(Component.literal("Combinacion de manos no permitida para este Boffer."), true);
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            DuelManager.onLogout(p);
            MeleeManager.onLogout(p);
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) MeleeManager.onLogin(p);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        Entity causer = event.getSource().getEntity();
        boolean direct = causer != null && causer == event.getSource().getDirectEntity();

        if (target instanceof ServerPlayer victim) {
            // 0) Parry: el golpe llega justo al empezar a bloquear
            if (direct && causer instanceof LivingEntity atk && ParryManager.tryParry(victim, atk)) {
                event.setCanceled(true);
                return;
            }
            // 1) Duelo
            Duel d = DuelManager.getDuel(victim);
            if (d != null) {
                if (d.phase == Duel.Phase.ACTIVE && direct && causer instanceof ServerPlayer attacker && d.isOpponent(victim, attacker)) {
                    DuelManager.handleHit(d, attacker, victim, event);
                } else {
                    event.setCanceled(true); // fuera de ronda o daño ajeno: no pasa nada
                }
                return;
            }
            // 2) Batalla campal
            if (MeleeManager.isParticipant(victim)) {
                if (direct && causer instanceof ServerPlayer attacker) MeleeManager.handleHit(attacker, victim, event);
                else event.setCanceled(true);
                return;
            }
            // 3) Zona SoftCombat: golpes entre jugadores dentro de la zona dan hit pero no quitan vida
            if (direct && causer instanceof ServerPlayer attacker
                    && ZoneManager.zoneAt(victim) != null && ZoneManager.zoneAt(attacker) != null) {
                event.setAmount(0.0F);
                return;
            }
        }

        // 4) Boffers / armas SoftCombat: dan hit (knockback, animacion) pero 0 de daño, como las bolas de nieve
        if (Config.SOFT_WEAPONS_NEVER_DAMAGE.get() && direct && causer instanceof LivingEntity attacker
                && attacker.getMainHandItem().is(ModTags.SOFTCOMBAT_WEAPONS)) {
            event.setAmount(0.0F);
        }
    }
}
