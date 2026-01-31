package com.pvphelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class PvpHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "pvp_helper";

    private static final KeyBinding HIGHLIGHT_KEY = new KeyBinding(
            "key.pvp_helper.highlight",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_TAB,
            "key.categories.pvp_helper"
    );

    private static boolean persistentHighlight = false;

    @Override
    public void onInitializeClient() {
        KeyBindingHelper.registerKeyBinding(HIGHLIGHT_KEY);

        ClientTickEvents.END_CLIENT_TICK.register(PvpHelperClient::onEndClientTick);
        WorldRenderEvents.LAST.register(ArrowPrediction::render);
    }

    private static void onEndClientTick(MinecraftClient client) {
        if (client.world == null || client.player == null) {
            return;
        }

        boolean shiftDown = isShiftDown(client);
        while (HIGHLIGHT_KEY.wasPressed()) {
            if (shiftDown) {
                persistentHighlight = !persistentHighlight;
            }
        }

        boolean highlightActive = HIGHLIGHT_KEY.isPressed() || persistentHighlight;
        HighlightManager.update(client, highlightActive);
        ArrowPrediction.update(client);
    }

    private static boolean isShiftDown(MinecraftClient client) {
        long handle = client.getWindow().getHandle();
        return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
