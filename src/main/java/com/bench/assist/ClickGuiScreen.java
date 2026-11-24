package com.bench.assist;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class ClickGuiScreen extends Screen {
    private final AssistConfig config;

    public ClickGuiScreen(AssistConfig config) {
        super(Text.literal("BenchAssist GUI"));
        this.config = config;
    }

    @Override
    protected void init() {
        int panelW = 210;
        int x = (this.width - panelW) / 2;
        int y = this.height / 2 - 55;

        addDrawableChild(ButtonWidget.builder(aimLabel(), btn -> {
            config.aimAssistEnabled = !config.aimAssistEnabled;
            btn.setMessage(aimLabel());
        }).dimensions(x, y, panelW, 20).build());

        addDrawableChild(ButtonWidget.builder(tbotLabel(), btn -> {
            config.triggerBotEnabled = !config.triggerBotEnabled;
            btn.setMessage(tbotLabel());
        }).dimensions(x, y + 26, panelW, 20).build());

        addDrawableChild(ButtonWidget.builder(wSprintLabel(), btn -> {
            config.keepSprintOnW = !config.keepSprintOnW;
            btn.setMessage(wSprintLabel());
        }).dimensions(x, y + 52, panelW, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Close"), btn -> close())
                .dimensions(x, y + 84, panelW, 20).build());
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
        int panelW = 224;
        int x = (this.width - panelW) / 2;
        int y = this.height / 2 - 72;
        context.fill(x, y, x + panelW, y + 124, 0xCC14171D);
        context.drawTextWithShadow(this.textRenderer, "BenchAssist // 1.21.11", x + 10, y + 8, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
