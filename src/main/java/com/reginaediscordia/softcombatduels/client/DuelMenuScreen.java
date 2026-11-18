package com.reginaediscordia.softcombatduels.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Menu para retar a un jugador. Cada boton ejecuta /duel challenge <jugador> <puntos> <modo>. */
public class DuelMenuScreen extends Screen {
    private final String target;

    public DuelMenuScreen(String target) {
        super(Component.literal("Retar a " + target));
        this.target = target;
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int y = this.height / 2 - 55;
        Component title = Component.literal("Retar a " + target + " a un duelo");
        int w = this.font.width(title);
        this.addRenderableWidget(new StringWidget(cx - w / 2, y - 22, w, 9, title, this.font));
        this.addRenderableWidget(option(cx, y, "A 3 puntos - modo seguro (solo Boffer)", 3, "safe"));
        this.addRenderableWidget(option(cx, y + 24, "A 3 puntos - modo libre (cualquier arma)", 3, "free"));
        this.addRenderableWidget(option(cx, y + 48, "A 5 puntos - modo seguro (solo Boffer)", 5, "safe"));
        this.addRenderableWidget(option(cx, y + 72, "A 5 puntos - modo libre (cualquier arma)", 5, "free"));
        this.addRenderableWidget(Button.builder(Component.literal("Cancelar"), b -> this.onClose())
                .bounds(cx - 120, y + 100, 240, 20).build());
    }

    private Button option(int cx, int y, String label, int points, String mode) {
        return Button.builder(Component.literal(label), b -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.connection.sendCommand("duel challenge " + target + " " + points + " " + mode);
            }
            this.onClose();
        }).bounds(cx - 120, y, 240, 20).build();
    }
}
