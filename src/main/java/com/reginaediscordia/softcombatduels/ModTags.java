package com.reginaediscordia.softcombatduels;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModTags {
    /** Armas validas de SoftCombat. Editable por datapack: data/softcombatduels/tags/item/softcombat_weapons.json */
    public static final TagKey<Item> SOFTCOMBAT_WEAPONS = TagKey.create(
            Registries.ITEM, Identifier.fromNamespaceAndPath(SoftCombatDuels.MODID, "softcombat_weapons"));
}
