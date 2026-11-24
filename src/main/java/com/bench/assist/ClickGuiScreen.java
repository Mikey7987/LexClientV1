package com.bench.assist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;

public final class ClickGuiScreen extends Screen {
    private static final int CARD_W = 246;
    private static final int CARD_H = 26;
    private static final int CARD_GAP = 6;
    private static final int SLIDER_H = 18;
    private static final int SLIDER_GAP = 4;

    private final AssistConfig config;
    private final List<ModuleRow> rows = new ArrayList<>();
    private final List<ValueSlider> activeSliders = new ArrayList<>();

    private int expandedModuleIndex = 0; // 0 = Aim Assist, 1 = TriggerBot, 2 = W Sprint

    private static final class ModuleRow {
        final String name;
        final int x;
        final int y;
        final int w;
        final int h;
        final int moduleIndex;

        ModuleRow(String name, int x, int y, int w, int h, int moduleIndex) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.moduleIndex = moduleIndex;
        }

        boolean contains(double mx, double my) {
            return mx >= x && mx <= x + w && my >= y && my <= y + h;
        }
    }

    public ClickGuiScreen(AssistConfig config) {
        super(Text.literal("BenchAssist"));
        this.config = config;
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    private void rebuildLayout() {
        clearChildren();
        rows.clear();
        activeSliders.clear();

        int panelX = (this.width - CARD_W) / 2;
        int cursorY = Math.max(24, (this.height / 2) - 145);

        // Module 0: Aim Assist
        rows.add(new ModuleRow("Aim Assist", panelX, cursorY, CARD_W, CARD_H, 0));
        cursorY += CARD_H + CARD_GAP;
        if (expandedModuleIndex == 0) {
            cursorY = addAimAssistSliders(panelX, cursorY);
        }

        // Module 1: TriggerBot
        rows.add(new ModuleRow("TriggerBot", panelX, cursorY, CARD_W, CARD_H, 1));
        cursorY += CARD_H + CARD_GAP;
        if (expandedModuleIndex == 1) {
            cursorY = addTriggerBotSliders(panelX, cursorY);
        }

        // Module 2: W Sprint Assist
        rows.add(new ModuleRow("W Sprint Assist", panelX, cursorY, CARD_W, CARD_H, 2));
        cursorY += CARD_H + CARD_GAP;
        if (expandedModuleIndex == 2) {
            cursorY = addSprintInfo(panelX, cursorY);
        }
    }

    private int addAimAssistSliders(int x, int y) {
        y = addSlider(x, y, "Smoothness", normalize(config.smoothFactor, 0.05, 0.50),
                v -> denormalize(v, 0.05, 0.50),
                val -> String.format("%.2f", val),
                val -> config.smoothFactor = val.floatValue());

        y = addSlider(x, y, "FOV Cone", normalize(config.fovConeDeg, 10.0, 90.0),
                v -> denormalize(v, 10.0, 90.0),
                val -> String.format("%.0f°", val),
                val -> config.fovConeDeg = val.floatValue());

        y = addSlider(x, y, "Max Reach", normalize(config.maxReach, 2.5, 4.5),
                v -> denormalize(v, 2.5, 4.5),
                val -> String.format("%.2fm", val),
                val -> config.maxReach = val);

        y = addSlider(x, y, "Deadzone", normalize(config.deadzoneDeg, 0.05, 1.50),
                v -> denormalize(v, 0.05, 1.50),
                val -> String.format("%.2f°", val),
                val -> config.deadzoneDeg = val.floatValue());

        y = addSlider(x, y, "Micro Jitter", normalize(config.microJitterDeg, 0.0, 0.50),
                v -> denormalize(v, 0.0, 0.50),
                val -> String.format("%.2f°", val),
                val -> config.microJitterDeg = val.floatValue());

        y = addSlider(x, y, "Pitch Scale", normalize(config.pitchScale, 0.10, 1.20),
                v -> denormalize(v, 0.10, 1.20),
                val -> String.format("%.2fx", val),
                val -> config.pitchScale = val.floatValue());

        return y + 4;
    }

    private int addTriggerBotSliders(int x, int y) {
        y = addSlider(x, y, "Cooldown Min", normalize(config.minCooldownStrength, 0.50, 1.00),
                v -> denormalize(v, 0.50, 1.00),
                val -> String.format("%.0f%%", val * 100.0),
                val -> config.minCooldownStrength = val.floatValue());

        y = addSlider(x, y, "Delay Min", normalize(config.minAttackDelayMs, 15.0, 250.0),
                v -> denormalize(v, 15.0, 250.0),
                val -> String.format("%dms", (int) Math.round(val)),
                val -> {
                    int minV = (int) Math.round(val);
                    config.minAttackDelayMs = minV;
                    if (config.maxAttackDelayMs < minV + 15) {
                        config.maxAttackDelayMs = minV + 15;
                    }
                });

        y = addSlider(x, y, "Delay Max", normalize(config.maxAttackDelayMs, 30.0, 320.0),
                v -> denormalize(v, 30.0, 320.0),
                val -> String.format("%dms", (int) Math.round(val)),
                val -> {
                    int maxV = (int) Math.round(val);
                    config.maxAttackDelayMs = Math.max(config.minAttackDelayMs + 15, maxV);
                });

        y = addSlider(x, y, "Miss Chance", normalize(config.missChancePct, 0.0, 25.0),
                v -> denormalize(v, 0.0, 25.0),
                val -> String.format("%.1f%%", val),
                val -> config.missChancePct = val.floatValue());

        return y + 4;
    }

    private int addSprintInfo(int x, int y) {
        return addSlider(x, y, "Cooldown Min", normalize(config.minCooldownStrength, 0.50, 1.00),
                v -> denormalize(v, 0.50, 1.00),
                val -> String.format("%.0f%%", val * 100.0),
                val -> config.minCooldownStrength = val.floatValue()) + 4;
    }

    private int addSlider(
            int x, int y, String label, double initialNorm,
            DoubleFunction<Double> mapValue, DoubleFunction<String> formatter, Consumer<Double> applier
    ) {
        ValueSlider slider = new ValueSlider(x + 8, y, CARD_W - 16, SLIDER_H, label, initialNorm, mapValue, formatter, applier);
        activeSliders.add(slider);
        addDrawableChild(slider);
        return y + SLIDER_H + SLIDER_GAP;
    }

    private boolean isModuleEnabled(int index) {
        return switch (index) {
            case 0 -> config.aimAssistEnabled;
            case 1 -> config.triggerBotEnabled;
            case 2 -> config.keepSprintOnW;
            default -> false;
        };
    }

    private void toggleModule(int index) {
        switch (index) {
            case 0 -> config.aimAssistEnabled = !config.aimAssistEnabled;
            case 1 -> config.triggerBotEnabled = !config.triggerBotEnabled;
            case 2 -> config.keepSprintOnW = !config.keepSprintOnW;
            default -> {}
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (ModuleRow row : rows) {
            if (row.contains(mouseX, mouseY)) {
                if (button == 0) {
                    toggleModule(row.moduleIndex);
                    return true;
                } else if (button == 1) {
                    expandedModuleIndex = (expandedModuleIndex == row.moduleIndex) ? -1 : row.moduleIndex;
                    rebuildLayout();
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x7A090B10);

        int panelX = (this.width - CARD_W) / 2;
        int topY = Math.max(8, (this.height / 2) - 166);

        context.drawTextWithShadow(this.textRenderer, "BENCHASSIST // 1.21.11", panelX + 2, topY, 0xFFE2E8F0);
        context.drawTextWithShadow(this.textRenderer, "LMB: Toggle  |  RMB: Settings", panelX + 2, topY + 11, 0xFF94A3B8);

        for (ModuleRow row : rows) {
            boolean enabled = isModuleEnabled(row.moduleIndex);
            boolean hovered = row.contains(mouseX, mouseY);
            boolean open = expandedModuleIndex == row.moduleIndex;

            int bg = enabled ? (hovered ? 0xFF1E3A5F : 0xFF172B47) : (hovered ? 0xFF232730 : 0xFF181B22);
            int accent = enabled ? 0xFF38BDF8 : 0xFF475569;

            context.fill(row.x, row.y, row.x + row.w, row.y + row.h, bg);
            context.fill(row.x, row.y, row.x + 3, row.y + row.h, accent);

            int textColor = enabled ? 0xFFF8FAFC : 0xFFCBD5E1;
            context.drawTextWithShadow(this.textRenderer, row.name, row.x + 10, row.y + 9, textColor);

            String status = (enabled ? "ON" : "OFF") + (open ? "  ▾" : "  ▸");
            int statusColor = enabled ? 0xFF7DD3FC : 0xFF94A3B8;
            int statusW = this.textRenderer.getWidth(status);
            context.drawTextWithShadow(this.textRenderer, status, row.x + row.w - statusW - 8, row.y + 9, statusColor);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static double normalize(double value, double min, double max) {
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    private static double denormalize(double norm, double min, double max) {
        return min + norm * (max - min);
    }

    private static final class ValueSlider extends SliderWidget {
        private final String label;
        private final DoubleFunction<Double> mapValue;
        private final DoubleFunction<String> formatter;
        private final Consumer<Double> applier;

        ValueSlider(
                int x, int y, int width, int height,
                String label, double initialNorm,
                DoubleFunction<Double> mapValue,
                DoubleFunction<String> formatter,
                Consumer<Double> applier
        ) {
            super(x, y, width, height, Text.empty(), initialNorm);
            this.label = label;
            this.mapValue = mapValue;
            this.formatter = formatter;
            this.applier = applier;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double real = mapValue.apply(this.value);
            this.setMessage(Text.literal(label + ": " + formatter.apply(real)));
        }

        @Override
        protected void applyValue() {
            double real = mapValue.apply(this.value);
            applier.accept(real);
        }
    }
}
