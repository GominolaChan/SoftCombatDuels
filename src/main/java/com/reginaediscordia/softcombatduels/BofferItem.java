package com.reginaediscordia.softcombatduels;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class BofferItem extends Item {
    private final BofferType type;

    public BofferItem(Item.Properties properties, BofferType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Con un escudo en la mano secundaria, el bloqueo lo hace el escudo (el Boffer cede el clic derecho).
        if (hand == InteractionHand.MAIN_HAND && type.shield && Config.ALLOW_SHIELD.get()
                && ModItems.isShield(player.getOffhandItem())) {
            return InteractionResult.PASS;
        }
        return super.use(level, player, hand);
    }
}
