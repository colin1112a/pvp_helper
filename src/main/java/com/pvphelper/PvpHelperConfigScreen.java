package com.pvphelper;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class PvpHelperConfigScreen extends Screen {
    private static final int ROW_HEIGHT = 24;

    private final Screen parent;
    private PvpHelperConfig config;

    private TextFieldWidget hudXField;
    private TextFieldWidget hudYField;
    private int leftColumnX;
    private int rightColumnX;
    private int hudFieldY;

    public PvpHelperConfigScreen(Screen parent) {
        super(Text.translatable("text.pvp_helper.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.config = ConfigManager.get().copy();

        this.leftColumnX = this.width / 2 - 155;
        this.rightColumnX = this.width / 2 + 5;
        int y = 36;

        addToggle(leftColumnX, y, "config.pvp_helper.highlight_enabled", config.highlightEnabled,
                value -> config.highlightEnabled = value);
        addToggle(rightColumnX, y, "config.pvp_helper.persistent_enabled", config.persistentHighlightEnabled,
                value -> config.persistentHighlightEnabled = value);
        y += ROW_HEIGHT;

        addToggle(leftColumnX, y, "config.pvp_helper.arrow_prediction_enabled", config.arrowPredictionEnabled,
                value -> config.arrowPredictionEnabled = value);
        addToggle(rightColumnX, y, "config.pvp_helper.arrow_warning_enabled", config.arrowWarningEnabled,
                value -> config.arrowWarningEnabled = value);
        y += ROW_HEIGHT;

        addToggle(leftColumnX, y, "config.pvp_helper.aim_prediction_enabled", config.aimPredictionEnabled,
                value -> config.aimPredictionEnabled = value);
        addToggle(rightColumnX, y, "config.pvp_helper.hud_enabled", config.hudEnabled,
                value -> config.hudEnabled = value);
        y += ROW_HEIGHT;

        addToggle(leftColumnX, y, "config.pvp_helper.hud_show_counts", config.hudShowArrowCount,
                value -> config.hudShowArrowCount = value);
        addToggle(rightColumnX, y, "config.pvp_helper.hud_show_aim", config.hudShowAimInfo,
                value -> config.hudShowAimInfo = value);
        y += ROW_HEIGHT + 6;

        this.hudFieldY = y;
        hudXField = new TextFieldWidget(this.textRenderer, leftColumnX, hudFieldY, 120, 20,
                Text.translatable("config.pvp_helper.hud_x"));
        hudXField.setText(Integer.toString(config.hudX));
        hudXField.setTextPredicate(PvpHelperConfigScreen::isNumericInput);
        hudXField.setChangedListener(value -> config.hudX = parseInt(value, config.hudX));
        addDrawableChild(hudXField);

        hudYField = new TextFieldWidget(this.textRenderer, rightColumnX, hudFieldY, 120, 20,
                Text.translatable("config.pvp_helper.hud_y"));
        hudYField.setText(Integer.toString(config.hudY));
        hudYField.setTextPredicate(PvpHelperConfigScreen::isNumericInput);
        hudYField.setChangedListener(value -> config.hudY = parseInt(value, config.hudY));
        addDrawableChild(hudYField);

        int bottomY = this.height - 28;
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.cancel"), button -> close())
                .dimensions(this.width / 2 - 155, bottomY, 150, 20)
                .build());
        addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), button -> saveAndClose())
                .dimensions(this.width / 2 + 5, bottomY, 150, 20)
                .build());
    }

    @Override
    public void tick() {
        super.tick();
        if (hudXField != null) {
            hudXField.tick();
        }
        if (hudYField != null) {
            hudYField.tick();
        }
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("config.pvp_helper.hud_x"),
                leftColumnX, hudFieldY - 10, 0xA0A0A0);
        context.drawTextWithShadow(this.textRenderer, Text.translatable("config.pvp_helper.hud_y"),
                rightColumnX, hudFieldY - 10, 0xA0A0A0);
        super.render(context, mouseX, mouseY, delta);
    }

    private void saveAndClose() {
        ConfigManager.save(config);
        close();
    }

    private void addToggle(int x, int y, String key, boolean value, ToggleConsumer consumer) {
        addDrawableChild(CyclingButtonWidget.onOffBuilder(value)
                .build(x, y, 150, 20, Text.translatable(key), (button, state) -> consumer.accept(state)));
    }

    private static boolean isNumericInput(String value) {
        return value.isEmpty() || value.chars().allMatch(Character::isDigit);
    }

    private static int parseInt(String value, int fallback) {
        if (value == null || value.isEmpty()) {
            return fallback;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private interface ToggleConsumer {
        void accept(boolean value);
    }
}
