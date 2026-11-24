package com.bench.assist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.DoubleFunction;

public final class ClickGuiScreen extends Screen {
    private final AssistConfig config;

    public ClickGuiScreen(AssistConfig config) {
        super(Text.literal("BenchAssist GUI"));
        this.config = config;
    }

    @Override
    protected void init() {
        int panelW = 220;
        int x = (this.width - panelW) / 2;
        int startY = this.height / 2 - 86;
        int stepY = 24;
        int row = 0;

        addDrawableChild(ButtonWidget.builder(aimLabel(), btn -> {
            config.aimAssistEnabled = !config.aimAssistEnabled;
            btn.setMessage(aimLabel());
        }).dimensions(x, startY + row++ * stepY, panelW, 20).build());

        addDrawableChild(ButtonWidget.builder(tbotLabel(), btn -> {
            config.triggerBotEnabled = !config.triggerBotEnabled;
            btn.setMessage(tbotLabel());
        }).dimensions(x, startY + row++ * stepY, panelW, 20).build());

        addDrawableChild(ButtonWidget.builder(wSprintLabel(), btn -> {
            config.keepSprintOnW = !config.keepSprintOnW;
            btn.setMessage(wSprintLabel());
        }).dimensions(x, startY + row++ * stepY, panelW, 20).build());

        // Aim Smoothness: 0.05 to 0.50
        addDrawableChild(new ValueSlider(
                x, startY + row++ * stepY, panelW, 20,
                "Aim Smooth",
                normalize(config.smoothFactor, 0.05, 0.50),
                v -> denormalize(v, 0.05, 0.50),
                val -> String.format("%.2f", val),
                val -> config.smoothFactor = val.floatValue()
        ));

        // FOV Cone: 10 to 90 deg
        addDrawableChild(new ValueSlider(
                x, startY + row++ * stepY, panelW, 20,
                "FOV Cone",
                normalize(config.fovConeDeg, 10.0, 90.0),
                v -> denormalize(v, 10.0, 90.0),
                val -> String.format("%.0f deg", val),
                val -> config.fovConeDeg = val.floatValue()
        ));

        // Cooldown Threshold: 0.50 to 1.00
        addDrawableChild(new ValueSlider(
                x, startY + row++ * stepY, panelW, 20,
                "Hit Cooldown",
                normalize(config.minCooldownStrength, 0.50, 1.00),
                v -> denormalize(v, 0.50, 1.00),
                val -> String.format("%.0f%%", val * 100.0),
                val -> config.minCooldownStrength = val.floatValue()
        ));

        // TriggerBot Base Delay: 20 to 200 ms
        addDrawableChild(new ValueSlider(
                x, startY + row++ * stepY, panelW, 20,
                "TBot Delay",
                normalize(config.minAttackDelayMs, 20.0, 200.0),
                v -> denormalize(v, 20.0, 200.0),
                val -> {
                    int ms = (int) Math.round(val);
                    return ms + "-" + (ms + 45) + " ms";
                },
                val -> {
                    int ms = (int) Math.round(val);
                    config.minAttackDelayMs = ms;
                    config.maxAttackDelayMs = ms + 45;
                }
        ));

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(x, startY + row * stepY + 4, panelW, 20).build());
    }

    private static double normalize(double value, double min, double max) {
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    private static double denormalize(double norm, double min, double max) {
        return min + norm * (max - min);
    }

    private Text aimLabel() {
        return Text.literal("Aim Assist: " + (config.aimAssistEnabled ? "ON" : "OFF"));
    }

    private Text tbotLabel() {
        return Text.literal("TriggerBot: " + (config.triggerBotEnabled ? "ON" : "OFF"));
    }

    private Text wSprintLabel() {
        return Text.literal("W Sprint Assist: " + (config.keepSprintOnW ? "ON" : "OFF"));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x88000000);
        int panelW = 234;
        int x = (this.width - panelW) / 2;
        int y = this.height / 2 - 104;
        context.fill(x, y, x + panelW, y + 216, 0xCC14171D);
        context.drawTextWithShadow(this.textRenderer, "BenchAssist // 1.21.11", x + 10, y + 8, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private static final class ValueSlider extends SliderWidget {
        private final String prefix;
        private final DoubleFunction<Double> mapValue;
        private final DoubleFunction<String> formatter;
        private final Consumer<Double> applier;

        ValueSlider(
                int x, int y, int width, int height,
                String prefix,
                double initialNorm,
                DoubleFunction<Double> mapValue,
                DoubleFunction<String> formatter,
                Consumer<Double> applier
        ) {
            super(x, y, width, height, Text.empty(), initialNorm);
            this.prefix = prefix;
            this.mapValue = mapValue;
            this.formatter = formatter;
            this.applier = applier;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            double real = mapValue.apply(this.value);
            this.setMessage(Text.literal(prefix + ": " + formatter.apply(real)));
        }

        @Override
        protected void applyValue() {
            double real = mapValue.apply(this.value);
            applier.accept(real);
        }
    }
}
