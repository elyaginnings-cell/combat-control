package com.gatto.interactiondelay.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Modern visual shell for Combat Control.
 *
 * This screen is intentionally presentation/navigation only. It does not
 * change any gameplay/configuration behavior; the existing configuration
 * screen remains the source of truth for the actual settings.
 */
public final class ModernCombatControlScreen extends Screen {
    private static final int PURPLE = 0xFFB84DFF;
    private static final int PURPLE_DARK = 0xFF6F2AA6;
    private static final int PANEL = 0xD9141722;
    private static final int PANEL_HOVER = 0xE61E2130;
    private static final int CARD = 0xD90D1018;
    private static final int CARD_BORDER = 0x403A3E4C;
    private static final int TEXT = 0xFFF1F1F5;
    private static final int MUTED = 0xFF9A9BA7;
    private static final int GREEN = 0xFF32D583;
    private static final int RED = 0xFFE13B63;

    private final Screen parent;
    private final List<Category> categories = List.of(
            new Category("Delays", "Interaction timing"),
            new Category("Anchors", "Anchor controls"),
            new Category("Crystal", "Crystal controls"),
            new Category("Combat", "Combat helpers"),
            new Category("Weapons", "Weapon settings"),
            new Category("Misc", "General settings")
    );

    private int selectedCategory = 0;
    private TextFieldWidget search;
    private List<ButtonWidget> categoryButtons = new ArrayList<>();
    private List<ButtonWidget> cardButtons = new ArrayList<>();

    public ModernCombatControlScreen(Screen parent) {
        super(Text.literal("Combat Control"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        categoryButtons.clear();
        cardButtons.clear();

        int sidebarX = 28;
        int sidebarWidth = 154;
        int contentX = sidebarX + sidebarWidth + 18;

        search = new TextFieldWidget(
                this.textRenderer,
                contentX,
                28,
                Math.max(220, this.width - contentX - 28),
                28,
                Text.literal("Search")
        );
        search.setDrawsBackground(false);
        search.setPlaceholder(Text.literal("Search for any module or feature"));
        search.setMaxLength(64);
        search.setChangedListener(value -> rebuildCards());
        this.addDrawableChild(search);

        int y = 98;
        for (int i = 0; i < categories.size(); i++) {
            final int index = i;
            Category category = categories.get(i);
            ButtonWidget button = ButtonWidget.builder(
                    Text.literal(category.name),
                    b -> {
                        selectedCategory = index;
                        init();
                    }
            ).dimensions(sidebarX, y, sidebarWidth, 34).build();
            categoryButtons.add(button);
            this.addDrawableChild(button);
            y += 39;
        }

        this.addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                b -> close()
        ).dimensions(this.width - 128, this.height - 38, 100, 24).build());

        rebuildCards();
    }

    private void rebuildCards() {
        for (ButtonWidget button : cardButtons) {
            remove(button);
        }
        cardButtons.clear();

        if (search == null) {
            return;
        }

        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        List<ModuleCard> cards = cardsFor(categories.get(selectedCategory));
        if (!query.isEmpty()) {
            cards = cards.stream()
                    .filter(card -> (card.title + " " + card.description).toLowerCase(Locale.ROOT).contains(query))
                    .toList();
        }

        int contentX = 200;
        int top = 84;
        int gap = 12;
        int cardWidth = Math.max(180, (this.width - contentX - 28 - gap) / 2);
        int cardHeight = 122;

        for (int i = 0; i < cards.size(); i++) {
            ModuleCard card = cards.get(i);
            int column = i % 2;
            int row = i / 2;
            int x = contentX + column * (cardWidth + gap);
            int y = top + row * (cardHeight + gap);

            ButtonWidget options = ButtonWidget.builder(
                    Text.literal("OPTIONS"),
                    b -> openDetails()
            ).dimensions(x + 12, y + cardHeight - 31, cardWidth - 24, 22).build();

            cardButtons.add(options);
            this.addDrawableChild(options);
        }
    }

    private void openDetails() {
        if (this.client != null) {
            this.client.setScreen(new InteractionDelayConfigScreen(this));
        }
    }

    private List<ModuleCard> cardsFor(Category category) {
        return switch (category.name) {
            case "Delays" -> List.of(
                    new ModuleCard("Block Placement", "Configure interaction timing for blocks.", false),
                    new ModuleCard("Item Delays", "Pearls, firework, chorus, potions and more.", false),
                    new ModuleCard("Armor & Elytra", "Configure equipment interaction timing.", false)
            );
            case "Anchors" -> List.of(
                    new ModuleCard("Anchor Sequence", "Configure anchor sequence options.", true),
                    new ModuleCard("Anchor Place Lock", "Configure anchor placement protection.", false),
                    new ModuleCard("Anchor Timing", "Configure placement and charge timing.", false)
            );
            case "Crystal" -> List.of(
                    new ModuleCard("Crystal Place Lock", "Configure crystal placement protection.", false),
                    new ModuleCard("Crystal Timing", "Configure crystal interaction timing.", false)
            );
            case "Combat" -> List.of(
                    new ModuleCard("Axe Switch", "Configure the existing axe-switch settings.", true),
                    new ModuleCard("Double Hit", "Configure the existing double-hit settings.", true),
                    new ModuleCard("Stun Slam", "Configure the existing stun-slam settings.", true),
                    new ModuleCard("Auto-Hit", "Configure the existing auto-hit settings.", false)
            );
            case "Weapons" -> List.of(
                    new ModuleCard("Lunge Assist", "Configure existing lunge settings.", true),
                    new ModuleCard("Mace Assist", "Configure existing mace settings.", true),
                    new ModuleCard("Sword Assist", "Configure existing sword settings.", false)
            );
            default -> List.of(
                    new ModuleCard("Master Enable", "Global Combat Control configuration.", true),
                    new ModuleCard("Cooldown Reset", "Configure cooldown reset behavior.", true),
                    new ModuleCard("Interface", "Configure the configuration interface.", false)
            );
        };
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x99070A10);

        int sidebarX = 28;
        int sidebarWidth = 154;
        int contentX = 200;

        // Main glass panels.
        context.fill(sidebarX - 4, 20, sidebarX + sidebarWidth + 4, this.height - 20, PANEL);
        context.fill(contentX - 10, 20, this.width - 24, this.height - 20, PANEL);

        // Search field backing.
        context.fill(contentX, 28, this.width - 28, 56, 0xCC0B0E15);
        context.fill(contentX, 55, this.width - 28, 56, PURPLE_DARK);

        context.drawTextWithShadow(this.textRenderer, Text.literal("COMBAT CONTROL"),
                sidebarX + 12, 40, TEXT);

        context.drawTextWithShadow(this.textRenderer,
                Text.literal(categories.get(selectedCategory).name.toUpperCase(Locale.ROOT)),
                contentX, 68, TEXT);

        context.drawTextWithShadow(this.textRenderer,
                Text.literal(categories.get(selectedCategory).description),
                contentX + 76, 68, MUTED);

        drawCards(context, mouseX, mouseY);

        // Sidebar labels.
        context.drawTextWithShadow(this.textRenderer, Text.literal("MODULES"),
                sidebarX + 12, 72, MUTED);
        for (int i = 0; i < categories.size(); i++) {
            if (i == selectedCategory) {
                int y = 98 + i * 39;
                context.fill(sidebarX, y, sidebarX + sidebarWidth, y + 34, PURPLE_DARK);
                context.fill(sidebarX, y, sidebarX + 3, y + 34, PURPLE);
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawCards(DrawContext context, int mouseX, int mouseY) {
        String query = search == null ? "" : search.getText().trim().toLowerCase(Locale.ROOT);
        List<ModuleCard> cards = cardsFor(categories.get(selectedCategory));
        if (!query.isEmpty()) {
            cards = cards.stream()
                    .filter(card -> (card.title + " " + card.description).toLowerCase(Locale.ROOT).contains(query))
                    .toList();
        }

        int contentX = 200;
        int top = 84;
        int gap = 12;
        int cardWidth = Math.max(180, (this.width - contentX - 28 - gap) / 2);
        int cardHeight = 122;

        for (int i = 0; i < cards.size(); i++) {
            ModuleCard card = cards.get(i);
            int column = i % 2;
            int row = i / 2;
            int x = contentX + column * (cardWidth + gap);
            int y = top + row * (cardHeight + gap);

            boolean hovered = mouseX >= x && mouseX < x + cardWidth
                    && mouseY >= y && mouseY < y + cardHeight;

            context.fill(x + 2, y + 3, x + cardWidth + 2, y + cardHeight + 3, 0x66000000);
            context.fill(x, y, x + cardWidth, y + cardHeight, hovered ? PANEL_HOVER : CARD);
            context.fill(x, y, x + cardWidth, y + 1, hovered ? PURPLE : CARD_BORDER);

            context.drawTextWithShadow(this.textRenderer, Text.literal(card.title),
                    x + 12, y + 13, TEXT);
            context.drawText(this.textRenderer, Text.literal(card.description),
                    x + 12, y + 32, MUTED);

            int statusColor = card.enabled ? GREEN : RED;
            String status = card.enabled ? "ENABLED" : "DISABLED";
            context.fill(x + 12, y + 56, x + cardWidth - 12, y + 78, statusColor);
            context.drawCenteredTextWithShadow(this.textRenderer, Text.literal(status),
                    x + cardWidth / 2, y + 63, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        com.gatto.interactiondelay.InteractionDelay.getConfig().save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    private record Category(String name, String description) {}
    private record ModuleCard(String title, String description, boolean enabled) {}
}
