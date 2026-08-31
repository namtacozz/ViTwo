package com.vitwo.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class AbstractTowerScreen extends Screen {

    protected AbstractTowerScreen(Text title) {
        super(title);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Direct solid dark overlay without Minecraft 1.21's blurry post-processing shader
        context.fill(0, 0, this.width, this.height, 0xD0000000);
    }

    protected void renderPanelBackground(DrawContext context, int x, int y, int width, int height) {
        // Solid high-contrast slate background (100% opaque, zero background bleed)
        context.fill(x, y, x + width, y + height, TowerTheme.PANEL_BACKGROUND);

        // Bold solid outer border in bright vibrant neon cyan (2px thick)
        context.fill(x, y, x + width, y + 2, TowerTheme.PANEL_BORDER_OUTER); // Top
        context.fill(x, y + height - 2, x + width, y + height, TowerTheme.PANEL_BORDER_OUTER); // Bottom
        context.fill(x, y, x + 2, y + height, TowerTheme.PANEL_BORDER_OUTER); // Left
        context.fill(x + width - 2, y, x + width, y + height, TowerTheme.PANEL_BORDER_OUTER); // Right

        // Inner solid border in deep teal (1px thick)
        context.fill(x + 2, y + 2, x + width - 2, y + 3, TowerTheme.PANEL_BORDER_INNER); // Top
        context.fill(x + 2, y + height - 3, x + width - 2, y + height - 2, TowerTheme.PANEL_BORDER_INNER); // Bottom
        context.fill(x + 2, y + 2, x + 3, y + height - 2, TowerTheme.PANEL_BORDER_INNER); // Left
        context.fill(x + width - 3, y + 2, x + width - 2, y + height - 2, TowerTheme.PANEL_BORDER_INNER); // Right
    }

    protected void renderBevelBorder(DrawContext context, int x, int y, int width, int height, boolean raised) {
        int colorTopLeft = raised ? TowerTheme.BEVEL_LIGHT : TowerTheme.BEVEL_DARK;
        int colorBottomRight = raised ? TowerTheme.BEVEL_DARK : TowerTheme.BEVEL_LIGHT;

        context.fill(x, y, x + width, y + 1, colorTopLeft); // Top
        context.fill(x, y, x + 1, y + height, colorTopLeft); // Left
        context.fill(x, y + height - 1, x + width, y + height, colorBottomRight); // Bottom
        context.fill(x + width - 1, y, x + width, y + height, colorBottomRight); // Right
    }
}
