package com.gatto.interactiondelay.config;

import com.gatto.interactiondelay.InteractionDelay;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;

/**
 * Modern Combat Control configuration shell.
 *
 * The cards are backed by the existing InteractionDelayConfig instance.
 * This screen changes presentation/navigation only; gameplay behavior remains
 * implemented by the existing handlers.
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

    private final Screen parent;\n    private long animationStartNanos;\n    private float categoryIndicatorY;\n    private float categoryIndicatorTargetY;\n    private float contentProgress;
    private final List<Category> categories = List.of(
            new Category("Delays", "Interaction timing"),
            new Category("Anchors", "Anchor controls"),
            new Category("Crystal", "Crystal controls"),
            new Category("Combat", "Combat helpers"),
            new Category("Weapons", "Weapon settings"),
            new Category("Misc", "General settings")
    );

    private int selectedCategory;
    private TextFieldWidget search;
    private final List<ButtonWidget> cardButtons = new ArrayList<>();
    private final List<ButtonWidget> toggleButtons = new ArrayList<>();

    public ModernCombatControlScreen(Screen parent) {
        super(Text.literal("Combat Control"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        clearChildren();
        cardButtons.clear();
        toggleButtons.clear();

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
        addDrawableChild(search);

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
            addDrawableChild(button);
            y += 39;
        }

        addDrawableChild(ButtonWidget.builder(
                Text.literal("Done"),
                b -> close()
        ).dimensions(this.width - 128, this.height - 38, 100, 24).build());

        rebuildCards();
    }

    private void rebuildCards() {
        for (ButtonWidget button : cardButtons) {
            remove(button);
        }
        for (ButtonWidget button : toggleButtons) {
            remove(button);
        }
        cardButtons.clear();
        toggleButtons.clear();

        if (search == null) {
            return;
        }

        String query = search.getText().trim().toLowerCase(Locale.ROOT);
        List<ModuleCard> cards = cardsFor(categories.get(selectedCategory));

        if (!query.isEmpty()) {
            cards = cards.stream()
                    .filter(card -> (card.title + " " + card.description)
                            .toLowerCase(Locale.ROOT)
                            .contains(query))
                    .toList();
        }

        int contentX = 200;
        int top = 84;
        int gap = 12;
        int cardWidth = Math.max(180, (this.width - contentX - 28 - gap) / 2);
        int cardHeight = 138;

        for (int i = 0; i < cards.size(); i++) {
            ModuleCard card = cards.get(i);
            int column = i % 2;
            int row = i / 2;
            int x = contentX + column * (cardWidth + gap);
            int y = top + row * (cardHeight + gap);\n            int animatedY = y + Math.round((1.0f - contentProgress) * 10.0f);

            ButtonWidget options = ButtonWidget.builder(
                    Text.literal("OPTIONS"),
                    b -> openDetails()
            ).dimensions(x + 12, y + cardHeight - 29, cardWidth - 24, 20).build();
            cardButtons.add(options);
            addDrawableChild(options);

            if (card.toggleable) {
                ButtonWidget toggle = ButtonWidget.builder(
                        Text.literal(statusLabel(card)),
                        b -> {
                            card.toggle.run();
                            InteractionDelay.getConfig().validate();
                            InteractionDelay.getConfig().save();
                            b.setMessage(Text.literal(statusLabel(card)));
                        }
                ).dimensions(x + 12, y + 82, cardWidth - 24, 20).build();
                toggleButtons.add(toggle);
                addDrawableChild(toggle);
            }
        }
    }

    private void openDetails() {
        if (client != null) {
            client.setScreen(new InteractionDelayConfigScreen(this));
        }
    }

    private List<ModuleCard> cardsFor(Category category) {
        InteractionDelayConfig c = InteractionDelay.getConfig();

        return switch (category.name) {
            case "Delays" -> List.of(
                    new ModuleCard("Block Placement", "Configure interaction timing for blocks.",
                            () -> c.blockPlacementDelay != InteractionDelayConfig.VANILLA, null, false),
                    new ModuleCard("Item Delays", "Pearls, firework, chorus, potions and more.",
                            () -> c.enderPearlDelay != InteractionDelayConfig.VANILLA
                                    || c.windChargeDelay != InteractionDelayConfig.VANILLA
                                    || c.fireworkDelay != InteractionDelayConfig.VANILLA
                                    || c.chorusFruitDelay != InteractionDelayConfig.VANILLA
                                    || c.xpBottleDelay != InteractionDelayConfig.VANILLA
                                    || c.splashPotionDelay != InteractionDelayConfig.VANILLA
                                    || c.otherItemDelay != InteractionDelayConfig.VANILLA,
                            null, false),
                    new ModuleCard("Armor & Elytra", "Configure equipment interaction timing.",
                            () -> c.armorDelay != InteractionDelayConfig.VANILLA
                                    || c.elytraDelay != InteractionDelayConfig.VANILLA,
                            null, false)
            );
            case "Anchors" -> List.of(
                    new ModuleCard("Anchor Sequence", "Existing anchor sequence setting.",
                            () -> c.anchorComboEnabled, () -> c.anchorComboEnabled = !c.anchorComboEnabled, true),
                    new ModuleCard("Anchor Place Lock", "Existing anchor placement protection.",
                            () -> c.anchorPlaceLockEnabled, () -> c.anchorPlaceLockEnabled = !c.anchorPlaceLockEnabled, true),
                    new ModuleCard("Auto Glowstone", "Existing automatic glowstone setting.",
                            () -> c.anchorAutoGlowstone, () -> c.anchorAutoGlowstone = !c.anchorAutoGlowstone, true)
            );
            case "Crystal" -> List.of(
                    new ModuleCard("Crystal Place Lock", "Existing crystal placement protection.",
                            () -> c.crystalPlaceLockEnabled, () -> c.crystalPlaceLockEnabled = !c.crystalPlaceLockEnabled, true)
            );
            case "Combat" -> List.of(
                    new ModuleCard("Axe Switch", "Existing axe-switch setting.",
                            () -> c.axeSwitchEnabled, () -> c.axeSwitchEnabled = !c.axeSwitchEnabled, true),
                    new ModuleCard("Double Hit", "Existing double-hit setting.",
                            () -> c.doubleHitEnabled, () -> c.doubleHitEnabled = !c.doubleHitEnabled, true),
                    new ModuleCard("Stun Slam", "Existing stun-slam setting.",
                            () -> c.stunSlamEnabled, () -> c.stunSlamEnabled = !c.stunSlamEnabled, true),
                    new ModuleCard("Auto-Hit", "Existing auto-hit setting.",
                            () -> c.autoHitEnabled, () -> c.autoHitEnabled = !c.autoHitEnabled, true)
            );
            case "Weapons" -> List.of(
                    new ModuleCard("Lunge Assist", "Existing lunge setting.",
                            () -> c.lungeSwapEnabled, () -> c.lungeSwapEnabled = !c.lungeSwapEnabled, true),
                    new ModuleCard("Mace Assist", "Existing mace setting.",
                            () -> c.maceSwapEnabled, () -> c.maceSwapEnabled = !c.maceSwapEnabled, true),
                    new ModuleCard("Sword Cooldown", "Existing sword cooldown setting.",
                            () -> c.swordCooldownAssist, () -> c.swordCooldownAssist = !c.swordCooldownAssist, true),
                    new ModuleCard("Sword Crit Assist", "Existing sword critical-hit setting.",
                            () -> c.swordCritAssist, () -> c.swordCritAssist = !c.swordCritAssist, true)
            );
            default -> List.of(
                    new ModuleCard("Master Enable", "Global Combat Control setting.",
                            () -> c.modEnabled, () -> c.modEnabled = !c.modEnabled, true),
                    new ModuleCard("Cooldown Reset", "Existing hotbar-swap cooldown setting.",
                            () -> c.resetCooldownOnSwap, () -> c.resetCooldownOnSwap = !c.resetCooldownOnSwap, true)
            );
        };
    }

    private static String statusLabel(ModuleCard card) {
        return card.enabled.getAsBoolean() ? "ENABLED" : "DISABLED";
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, width, height, 0x99070A10);

        int sidebarX = 28;
        int sidebarWidth = 154;
        int contentX = 200;

        context.fill(sidebarX - 4, 20, sidebarX + sidebarWidth + 4, height - 20, PANEL);
        context.fill(contentX - 10, 20, width - 24, height - 20, PANEL);

        context.fill(contentX, 28, width - 28, 56, 0xCC0B0E15);
        context.fill(contentX, 55, width - 28, 56, PURPLE_DARK);

        context.drawTextWithShadow(textRenderer, Text.literal("COMBAT CONTROL"),
                sidebarX + 12, 40, TEXT);

        context.drawTextWithShadow(textRenderer,
                Text.literal(categories.get(selectedCategory).name.toUpperCase(Locale.ROOT)),
                contentX, 68, TEXT);

        context.drawTextWithShadow(textRenderer,
                Text.literal(categories.get(selectedCategory).description),
                contentX + 76, 68, MUTED);

        drawCards(context, mouseX, mouseY);

        context.drawTextWithShadow(textRenderer, Text.literal("MODULES"),
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
                    .filter(card -> (card.title + " " + card.description)
                            .toLowerCase(Locale.ROOT)
                            .contains(query))
                    .toList();
        }

        int contentX = 200;
        int top = 84;
        int gap = 12;
        int cardWidth = Math.max(180, (width - contentX - 28 - gap) / 2);
        int cardHeight = 138;

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

            context.drawTextWithShadow(textRenderer, Text.literal(card.title),
                    x + 12, y + 13, TEXT);
            context.drawText(textRenderer, Text.literal(card.description),
                    x + 12, y + 32, MUTED);

            int statusColor = card.enabled.getAsBoolean() ? GREEN : RED;
            String status = card.toggleable ? statusLabel(card)
                    : (card.enabled.getAsBoolean() ? "CUSTOM" : "VANILLA");

            context.fill(x + 12, y + 56, x + cardWidth - 12, y + 78,
                    card.toggleable ? statusColor : 0xFF3A3E4C);
            context.drawCenteredTextWithShadow(textRenderer, Text.literal(status),
                    x + cardWidth / 2, y + 63, 0xFFFFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        InteractionDelay.getConfig().validate();
        InteractionDelay.getConfig().save();
        if (client != null) {
            client.setScreen(parent);
        }
    }

    private record Category(String name, String description) {}

    private record ModuleCard(
            String title,
            String description,
            BooleanSupplier enabled,
            Runnable toggle,
            boolean toggleable
    ) {}
}
