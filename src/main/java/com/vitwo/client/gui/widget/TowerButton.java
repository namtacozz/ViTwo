package com.vitwo.client.gui.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class TowerButton extends ButtonWidget {

    public enum ButtonStyle {
        DEFAULT(0xFF151C28, 0xFF243246, 0xFF354B68, 0xFF00E5FF, 0xFFFFFFFF, 0xFF55FFFF),
        GOLD(0xFF281E10, 0xFF3D2C14, 0xFF6B4F20, 0xFFFFD700, 0xFFFFF2A3, 0xFFFFD700),
        GREEN(0xFF102818, 0xFF173D23, 0xFF246338, 0xFF55FF55, 0xFFA3FFA3, 0xFF55FF55),
        RED(0xFF281014, 0xFF3D141B, 0xFF63202C, 0xFFFF5555, 0xFFFFA3A3, 0xFFFF5555),
        SECONDARY(0xFF14141E, 0xFF1E1E2E, 0xFF2E2E42, 0xFF8888AA, 0xFFCCCCCC, 0xFFFFFFFF);

        final int bgNormal;
        final int bgHover;
        final int borderNormal;
        final int borderHover;
        final int textNormal;
        final int textHover;

        ButtonStyle(int bgNormal, int bgHover, int borderNormal, int borderHover, int textNormal, int textHover) {
            this.bgNormal = bgNormal;
            this.bgHover = bgHover;
            this.borderNormal = borderNormal;
            this.borderHover = borderHover;
            this.textNormal = textNormal;
            this.textHover = textHover;
        }
    }

    private ButtonStyle style = ButtonStyle.DEFAULT;

    public TowerButton(int x, int y, int width, int height, Text message, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
    }

    public TowerButton setStyle(ButtonStyle style) {
        this.style = style != null ? style : ButtonStyle.DEFAULT;
        return this;
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
        private ButtonStyle style = ButtonStyle.DEFAULT;

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

        public Builder style(ButtonStyle style) {
            this.style = style;
            return this;
        }

        public TowerButton build() {
            TowerButton btn = new TowerButton(this.x, this.y, this.width, this.height, this.message, this.onPress);
            btn.setStyle(this.style);
            return btn;
        }
    }

    @Override
    protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        boolean hovered = this.isHovered();
        boolean active = this.active;

        int bgColor = !active ? 0xFF0E121A : (hovered ? style.bgHover : style.bgNormal);
        int borderColor = !active ? 0xFF202633 : (hovered ? style.borderHover : style.borderNormal);
        int textColor = !active ? 0xFF555566 : (hovered ? style.textHover : style.textNormal);

        // Solid Outer Border
        context.fill(x - 1, y - 1, x + w + 1, y + h + 1, borderColor);

        // Button Inner Background Fill
        context.fill(x, y, x + w, y + h, bgColor);

        // Highlight top bar on hover
        if (hovered && active) {
            context.fill(x, y, x + w, y + 1, borderColor);
        }

        // Crisp Centered Text with guaranteed 100% Alpha
        TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
        int textY = y + (h - 8) / 2;
        context.drawCenteredTextWithShadow(textRenderer, this.getMessage(), x + (w / 2), textY, textColor);
    }
}
