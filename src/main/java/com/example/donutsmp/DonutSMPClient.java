package com.example.donutsmp;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

public class DonutSMPClient implements ClientModInitializer {
    private static int clientMoney = 0;

    @Override
    public void onInitializeClient() {
        // Listen for sync packets
        // ClientPlayNetworking.registerGlobalReceiver(DonutSMP.MONEY_SYNC_PACKET, (client, handler, buf, responseSender) -> {
        //     int money = buf.readInt();
        //     client.execute(() -> clientMoney = money);
        // });

        // Render HUD Overlay Left
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                TextRenderer renderer = client.textRenderer;
                String text = "§eBalance: $" + clientMoney;
                drawContext.drawText(renderer, text, 10, 10, 0xFFFFFF, true);
            }
        });
    }
}
