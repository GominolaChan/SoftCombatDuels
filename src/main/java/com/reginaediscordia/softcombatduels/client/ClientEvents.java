package com.reginaediscordia.softcombatduels.client;

import com.reginaediscordia.softcombatduels.ModItems;
import com.reginaediscordia.softcombatduels.SoftCombatDuels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = SoftCombatDuels.MODID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {}

    /**
     * Clic derecho sobre otro jugador = menu de reto.
     * - Agachado (Shift) no se abre nunca: Shift + clic derecho con las manos vacias es de Carry On.
     * - Con un Boffer o un escudo en la mano (para no estorbar al bloqueo) hay que mantener la tecla de correr (Ctrl).
     */
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof LocalPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getTarget() instanceof Player other) || other == player) return;
        if (player.isShiftKeyDown()) return;
        ItemStack held = player.getMainHandItem();
        boolean blocker = ModItems.typeOf(held) != null || held.has(DataComponents.BLOCKS_ATTACKS);
        if (blocker && !Minecraft.getInstance().options.keySprint.isDown()) return;
        event.setCanceled(true);
        Minecraft.getInstance().setScreen(new DuelMenuScreen(other.getName().getString()));
    }
}
