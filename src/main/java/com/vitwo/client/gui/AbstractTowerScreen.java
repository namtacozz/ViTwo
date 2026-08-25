package com.vitwo.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public abstract class AbstractTowerScreen extends Screen {

    protected AbstractTowerScreen(Text title) {
        super(title);
    }

    protected void renderPanelBackground(DrawContext context, int x, int y, int width, int height) {
        // Solid high-contrast slate background
        context.fill(x, y, x + width, y + height, TowerTheme.PANEL_BACKGROUND);

        // Bold double border in bright vibrant cyan
        context.drawBorder(x, y, width, height, TowerTheme.PANEL_BORDER_OUTER);
        context.drawBorder(x + 1, y + 1, width - 2, height - 2, TowerTheme.PANEL_BORDER_INNER);
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
