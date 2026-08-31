package com.vitwo.client.gui;

import com.vitwo.client.gui.widget.TowerButton;
import com.vitwo.network.c2s.ChooseDraftPokemonC2SPacket;
import com.vitwo.network.s2c.OpenPokemonDraftS2CPacket.DraftOption;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TowerPokemonDraftScreen extends AbstractTowerScreen {
    private final int floor;
    private final String bossName;
    private final List<DraftOption> options;

    public TowerPokemonDraftScreen(int floor, String bossName, List<DraftOption> options) {
        super(Text.literal("CobbleTower Draft"));
        this.floor = floor;
        this.bossName = bossName;
        this.options = options;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int cardW = 110;
        int cardH = 70;
        int gapX = 8;
        int gapY = 8;
        int startX = centerX - ((3 * cardW + 2 * gapX) / 2);
        int startY = centerY - 55;

        for (int i = 0; i < Math.min(6, options.size()); i++) {
            DraftOption opt = options.get(i);
            int col = i % 3;
            int row = i / 3;
            int cardX = startX + col * (cardW + gapX);
            int cardY = startY + row * (cardH + gapY);

            String btnLabel = opt.isLegendary() ? "§6★ Master Ball" : "§a✔ Chọn Trứng";
            this.addDrawableChild(TowerButton.towerBuilder(
                    Text.literal(btnLabel),
                    btn -> {
                        ClientPlayNetworking.send(new ChooseDraftPokemonC2SPacket(floor, opt.slotIndex()));
                        this.close();
                    }
            ).dimensions(cardX + 4, cardY + cardH - 18, cardW - 8, 14).build());
        }

        // Skip Button
        this.addDrawableChild(TowerButton.towerBuilder(
                Text.literal("§7Bỏ Qua Lựa Chọn"),
                btn -> {
                    ClientPlayNetworking.send(new ChooseDraftPokemonC2SPacket(floor, -1));
                    this.close();
                }
        ).dimensions(centerX - 60, centerY + 96, 120, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int panelW = 370;
        int panelH = 250;
        this.renderPanelBackground(context, centerX - panelW / 2, centerY - panelH / 2, panelW, panelH);

        // Header Title (Solid Alpha Colors)
        context.drawCenteredTextWithShadow(this.textRenderer, "§6§l🏆 TUYỂN MỘ TRỨNG POKÉMON ĐỐI THỦ 🏆", centerX, centerY - 110, 0xFFFFD700);
        context.drawCenteredTextWithShadow(this.textRenderer, "§7Đã hạ gục §e" + bossName + " §7(Tầng " + floor + ") • Nhận 1 Trứng Max 6x31 IVs", centerX, centerY - 95, 0xFFCCCCCC);

        // Render Card Contents
        int cardW = 110;
        int cardH = 70;
        int gapX = 8;
        int gapY = 8;
        int startX = centerX - ((3 * cardW + 2 * gapX) / 2);
        int startY = centerY - 55;

        DraftOption hoveredOption = null;

        for (int i = 0; i < Math.min(6, options.size()); i++) {
            DraftOption opt = options.get(i);
            int col = i % 3;
            int row = i / 3;
            int cardX = startX + col * (cardW + gapX);
            int cardY = startY + row * (cardH + gapY);

            boolean isHovered = mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH;
            if (isHovered) {
                hoveredOption = opt;
            }

            // Card background frame with hover effect
            int borderColor = isHovered ? (opt.isLegendary() ? 0xFFFFD700 : 0xFF0FD9C2) : 0xFF353545;
            int bgColor = isHovered ? 0xFF1E2436 : 0xFF14141E;

            // Outer Border & Background
            context.fill(cardX - 1, cardY - 1, cardX + cardW + 1, cardY + cardH + 1, borderColor);
            context.fill(cardX, cardY, cardX + cardW, cardY + cardH, bgColor);

            // Icon: Compact item icon
            ItemStack iconStack;
            if (opt.isLegendary()) {
                Identifier masterBallId = Identifier.of("cobblemon", "master_ball");
                iconStack = Registries.ITEM.containsId(masterBallId) ? new ItemStack(Registries.ITEM.get(masterBallId)) : new ItemStack(Items.NETHER_STAR);
            } else {
                Identifier eggId = Identifier.of("cobblemon", "pokemon_egg");
                iconStack = Registries.ITEM.containsId(eggId) ? new ItemStack(Registries.ITEM.get(eggId)) : new ItemStack(Items.EGG);
            }
            context.drawItem(iconStack, cardX + 4, cardY + 4);

            // Text Labels with FULL Alpha (0xFF)
            if (opt.isLegendary()) {
                context.drawTextWithShadow(this.textRenderer, "§6★ Master Ball", cardX + 24, cardY + 4, 0xFFFFFF55);
                context.drawTextWithShadow(this.textRenderer, "§7Huyền Thoại", cardX + 24, cardY + 14, 0xFFAAAAAA);
                context.drawTextWithShadow(this.textRenderer, "§d" + truncate(opt.displayName(), 12), cardX + 4, cardY + 25, 0xFFFFAAFF);
                context.drawTextWithShadow(this.textRenderer, "§8(Không thể phối)", cardX + 4, cardY + 36, 0xFF888888);
            } else {
                String shinyStar = opt.isShiny() ? " §6✨" : "";
                String formTag = (opt.formAspect() != null && !opt.formAspect().isBlank()) ? (" [" + capitalize(opt.formAspect()) + "]") : "";
                String baseTitle = "§e" + capitalize(opt.baseSpecies()) + formTag + shinyStar;
                context.drawTextWithShadow(this.textRenderer, truncate(baseTitle, 14), cardX + 24, cardY + 4, 0xFFFFFF55);
                context.drawTextWithShadow(this.textRenderer, "§a6x31 Max IVs", cardX + 24, cardY + 14, 0xFF55FF55);
                context.drawTextWithShadow(this.textRenderer, "§7Từ: §f" + truncate(opt.displayName(), 11), cardX + 4, cardY + 25, 0xFFCCCCCC);
                String types = "§b" + opt.primaryType() + (opt.secondaryType().isEmpty() ? "" : ("/" + opt.secondaryType()));
                context.drawTextWithShadow(this.textRenderer, types, cardX + 4, cardY + 36, 0xFF88CCFF);
            }
        }

        // Render widgets (buttons) on top of cards
        super.render(context, mouseX, mouseY, delta);

        // Tooltip rendering when hovering over a card
        if (hoveredOption != null) {
            List<Text> tooltip = new ArrayList<>();
            if (hoveredOption.isLegendary()) {
                tooltip.add(Text.literal("§6§l★ Phần Thưởng Master Ball"));
                tooltip.add(Text.literal("§7Đối thủ: §d" + hoveredOption.displayName() + " §8(Huyền Thoại/Thần Thoại)"));
                tooltip.add(Text.literal("§eDo không thể phối giống/sinh trứng, bạn sẽ nhận"));
                tooltip.add(Text.literal("§d1x Master Ball §ehoàn hảo thay thế!"));
                tooltip.add(Text.literal("§a► Bấm để nhận Master Ball"));
            } else {
                String shinyText = hoveredOption.isShiny() ? " §6✨ [SHINY]" : "";
                String formText = (hoveredOption.formAspect() != null && !hoveredOption.formAspect().isBlank()) ? (" [" + capitalize(hoveredOption.formAspect()) + "]") : "";
                tooltip.add(Text.literal("§e§lQuả Trứng Pokémon: §a" + capitalize(hoveredOption.baseSpecies()) + formText + shinyText));
                tooltip.add(Text.literal("§7Dạng sơ cấp từ: §f" + hoveredOption.displayName()));
                tooltip.add(Text.literal("§bHệ: §f" + hoveredOption.primaryType() + (hoveredOption.secondaryType().isEmpty() ? "" : (" / " + hoveredOption.secondaryType()))));
                tooltip.add(Text.literal("§aChỉ số: §eHoàn hảo Max 6x31 IVs (Best x6)"));
                tooltip.add(Text.literal("§7Cấp độ khi nở: §eLv.1"));
                tooltip.add(Text.literal("§a► Bấm để nhận Trứng vào Party/PC Box!"));
            }
            context.drawTooltip(this.textRenderer, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        int cardW = 110;
        int cardH = 70;
        int gapX = 8;
        int gapY = 8;
        int startX = centerX - ((3 * cardW + 2 * gapX) / 2);
        int startY = centerY - 55;

        for (int i = 0; i < Math.min(6, options.size()); i++) {
            DraftOption opt = options.get(i);
            int col = i % 3;
            int row = i / 3;
            int cardX = startX + col * (cardW + gapX);
            int cardY = startY + row * (cardH + gapY);

            if (mouseX >= cardX && mouseX <= cardX + cardW && mouseY >= cardY && mouseY <= cardY + cardH) {
                ClientPlayNetworking.send(new ChooseDraftPokemonC2SPacket(floor, opt.slotIndex()));
                this.close();
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "";
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static String truncate(String str, int maxLen) {
        if (str == null) return "";
        if (str.length() <= maxLen) return str;
        return str.substring(0, maxLen - 1) + "…";
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
