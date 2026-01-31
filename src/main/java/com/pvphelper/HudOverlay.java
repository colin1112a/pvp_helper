package com.pvphelper;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Locale;

public final class HudOverlay {
    private HudOverlay() {
    }

    public static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        PvpHelperConfig config = ConfigManager.get();
        if (!config.hudEnabled) {
            return;
        }

        ArrowPrediction.HudSnapshot snapshot = ArrowPrediction.getHudSnapshot();
        if (snapshot == null) {
            return;
        }

        int x = Math.max(0, config.hudX);
        int y = Math.max(0, config.hudY);
        TextRenderer renderer = client.textRenderer;

        if (config.hudShowArrowCount) {
            Text line = Text.translatable("hud.pvp_helper.arrow_counts", snapshot.arrowCount(), snapshot.hitCount());
            context.drawTextWithShadow(renderer, line, x, y, 0xFFFFFF);
            y += 10;
        }

        if (config.hudShowAimInfo) {
            if (snapshot.aimPosition() != null) {
                double distance = snapshot.aimPosition().distanceTo(client.player.getEyePos());
                double seconds = snapshot.aimTicks() / 20.0;
                Text line = Text.translatable("hud.pvp_helper.aim_info", format(distance), format(seconds));
                context.drawTextWithShadow(renderer, line, x, y, 0x8FE3FF);
            } else {
                Text line = Text.translatable("hud.pvp_helper.aim_none");
                context.drawTextWithShadow(renderer, line, x, y, 0x8FE3FF);
            }
        }
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
