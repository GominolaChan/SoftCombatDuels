package com.reginaediscordia.softcombatduels;

import com.reginaediscordia.softcombatduels.duel.CombatEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/** SoftCombat Duels - por ReginaeDiscordia. */
@Mod(SoftCombatDuels.MODID)
public class SoftCombatDuels {
    public static final String MODID = "softcombatduels";

    public SoftCombatDuels(IEventBus modEventBus, ModContainer container) {
        ModItems.ITEMS.register(modEventBus);
        ModTabs.TABS.register(modEventBus);
        container.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        CombatEvents.register();
    }
}
