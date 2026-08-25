package com.vitwo.client.gui.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TowerButton extends ButtonWidget {
    public TowerButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public static Builder towerBuilder(Text message, PressAction onPress) {
        return new Builder(message, onPress);
    }

    public static class Builder {
        private final Text message;
        private final PressAction onPress;
        private int x;
        private int y;
        private int width = 150;
        private int height = 20;

        public Builder(Text message, PressAction onPress) {
            this.message = message;
            this.onPress = onPress;
        }

        public Builder dimensions(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            return this;
        }

        public TowerButton build() {
            return new TowerButton(this.x, this.y, this.width, this.height, this.message, this.onPress);
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderWidget(context, mouseX, mouseY, delta);
    }
}
