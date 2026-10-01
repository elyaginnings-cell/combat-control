package com.gatto.interactiondelay.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Tabbed Combat Control menu — numeric options use sliders.
 */
public class InteractionDelayConfigScreen extends Screen {
	private enum Tab {
		DELAYS("Delays"),
		ANCHORS("Anchors"),
		CRYSTAL("Crystal"),
		COMBAT("Combat"),
		WEAPONS("Weapons"),
		MISC("Misc");

		final String label;
		Tab(String label) { this.label = label; }
	}

	private final Screen parent;
	private final InteractionDelayConfig config;
	private Tab tab = Tab.DELAYS;

	public InteractionDelayConfigScreen(Screen parent) {
		super(Text.literal("Combat Control"));
		this.parent = parent;
		this.config = com.gatto.interactiondelay.InteractionDelay.getConfig();
	}

	@Override
	protected void init() {
		clearChildren();
		int cx = this.width / 2;
		int top = 28;
		int tabW = 72;
		int startX = cx - (Tab.values().length * tabW) / 2;

		for (int i = 0; i < Tab.values().length; i++) {
			Tab t = Tab.values()[i];
			int x = startX + i * tabW;
			String prefix = (t == this.tab) ? "[" : "[";
			String suffix = "]";
			this.addDrawableChild(ButtonWidget.builder(
					Text.literal(prefix + t.label + suffix),
					btn -> {
						this.tab = t;
						this.init();
					})
					.dimensions(x, top, tabW - 2, 18)
					.build());
		}

		int y = top + 26;
		int row = 22;

		switch (this.tab) {
			case DELAYS -> buildDelays(cx, y, row);
			case ANCHORS -> buildAnchors(cx, y, row);
			case CRYSTAL -> buildCrystal(cx, y, row);
			case COMBAT -> buildCombat(cx, y, row);
			case WEAPONS -> buildWeapons(cx, y, row);
			case MISC -> buildMisc(cx, y, row);
		}

		this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), b -> close())
				.dimensions(cx - 100, this.height - 28, 200, 20)
				.build());
	}

	private void buildDelays(int cx, int y, int row) {
		y = addDelaySlider(cx, y, row, "Blocks", () -> config.blockPlacementDelay, v -> config.blockPlacementDelay = v);
		y = addDelaySlider(cx, y, row, "Obsidian", () -> config.obsidianPlaceDelay, v -> config.obsidianPlaceDelay = v);
		y = addDelaySlider(cx, y, row, "Ender Pearl", () -> config.enderPearlDelay, v -> config.enderPearlDelay = v);
		y = addDelaySlider(cx, y, row, "Wind Charge", () -> config.windChargeDelay, v -> config.windChargeDelay = v);
		y = addDelaySlider(cx, y, row, "Firework", () -> config.fireworkDelay, v -> config.fireworkDelay = v);
		y = addDelaySlider(cx, y, row, "Chorus", () -> config.chorusFruitDelay, v -> config.chorusFruitDelay = v);
		y = addDelaySlider(cx, y, row, "XP Bottle", () -> config.xpBottleDelay, v -> config.xpBottleDelay = v);
		y = addDelaySlider(cx, y, row, "Splash Potion", () -> config.splashPotionDelay, v -> config.splashPotionDelay = v);
		y = addDelaySlider(cx, y, row, "Armor", () -> config.armorDelay, v -> config.armorDelay = v);
		y = addDelaySlider(cx, y, row, "Elytra", () -> config.elytraDelay, v -> config.elytraDelay = v);
		y = addDelaySlider(cx, y, row, "Other Items", () -> config.otherItemDelay, v -> config.otherItemDelay = v);
		y = addDelaySlider(cx, y, row, "Block Interact", () -> config.blockInteractionDelay, v -> config.blockInteractionDelay = v);
	}

	private void buildAnchors(int cx, int y, int row) {
		y = addDelaySlider(cx, y, row, "Anchor Place", () -> config.respawnAnchorPlaceDelay, v -> config.respawnAnchorPlaceDelay = v);
		y = addDelaySlider(cx, y, row, "Glowstone Charge", () -> config.glowstoneChargeDelay, v -> config.glowstoneChargeDelay = v);
		y = addDelaySlider(cx, y, row, "Anchor Explode", () -> config.anchorExplodeDelay, v -> config.anchorExplodeDelay = v);
		y += 4;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.anchorComboEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Anchor Sequence"),
						(b, v) -> config.anchorComboEnabled = v));
		y += row;
		y = addIntSlider(cx, y, row, "Min After Place", 0, 10,
				() -> config.anchorComboPlaceTicks, v -> config.anchorComboPlaceTicks = v);
		y = addIntSlider(cx, y, row, "Min On Glowstone", 0, 10,
				() -> config.anchorComboChargeTicks, v -> config.anchorComboChargeTicks = v);
		y = addIntSlider(cx, y, row, "Min On Totem", 0, 10,
				() -> config.anchorComboStepTicks, v -> config.anchorComboStepTicks = v);
		y += 4;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.anchorComboSafe)
				.build(cx - 100, y, 200, 20, Text.literal("Safe Anchoring (no auto)"),
						(b, v) -> config.anchorComboSafe = v));
		y += row;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.anchorPlaceLockEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Anchor Place Lock (disabled)"),
						(b, v) -> config.anchorPlaceLockEnabled = v));
	}

	private void buildCrystal(int cx, int y, int row) {
		y = addDelaySlider(cx, y, row, "Crystal Place", () -> config.endCrystalDelay, v -> config.endCrystalDelay = v);
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.crystalPlaceLockEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Crystal Place Lock (disabled)"),
						(b, v) -> config.crystalPlaceLockEnabled = v));
		y += row;
		y = addIntSlider(cx, y, row, "Crystal Lock Ticks", 1, 10,
				() -> config.crystalPlaceLockTicks, v -> config.crystalPlaceLockTicks = v);
	}

	private void buildCombat(int cx, int y, int row) {
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.axeSwitchEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Auto Axe-Switch"),
						(b, v) -> config.axeSwitchEnabled = v));
		y += row;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.axeSwitchPreemptive)
				.build(cx - 100, y, 200, 20, Text.literal("Axe Pre-Switch"),
						(b, v) -> config.axeSwitchPreemptive = v));
		y += row;
		y = addIntSlider(cx, y, row, "Axe Hold Ticks", 0, 10,
				() -> config.axeSwitchHoldTicks, v -> config.axeSwitchHoldTicks = v);
		y = addIntSlider(cx, y, row, "Shield-Break %", 0, 100,
				() -> config.axeSwitchChance, v -> config.axeSwitchChance = v);
		y += 4;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.doubleHitEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Double Hit"),
						(b, v) -> config.doubleHitEnabled = v));
		y += row;
		y = addIntSlider(cx, y, row, "Double-Hit CPS", 1, 20,
				() -> config.doubleHitCps, v -> config.doubleHitCps = v);
		y += 6;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.stunSlamEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Stun Slam"),
						(b, v) -> config.stunSlamEnabled = v));
		y += row;
		y = addIntSlider(cx, y, row, "Slot to Axe Ticks", 0, 20,
				() -> config.stunSlamSlotToAxeTicks, v -> config.stunSlamSlotToAxeTicks = v);
		y = addIntSlider(cx, y, row, "Axe to Mace Ticks", 0, 20,
				() -> config.stunSlamAxeToMaceTicks, v -> config.stunSlamAxeToMaceTicks = v);
		y = addIntSlider(cx, y, row, "Mace to Restore Ticks", 0, 20,
				() -> config.stunSlamMaceToRestoreTicks, v -> config.stunSlamMaceToRestoreTicks = v);
		y += 4;
		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Mace Filter: " + maceFilterLabel(config.stunSlamMaceFilter)),
				btn -> {
					int n = (config.stunSlamMaceFilter + 1) % 5;
					config.stunSlamMaceFilter = n;
					btn.setMessage(Text.literal("Mace Filter: " + maceFilterLabel(n)));
				}).dimensions(cx - 100, y, 200, 20).build());
		y += row + 6;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoHitEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Auto-Hit (crosshair)"),
						(b, v) -> config.autoHitEnabled = v));
		y += row;
		y = addIntSlider(cx, y, row, "Auto-Hit Range", 1, 6,
				() -> config.autoHitRange, v -> config.autoHitRange = v);
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoHitRequireCooldown)
				.build(cx - 100, y, 200, 20, Text.literal("Auto-Hit Use Cooldown Gate"),
						(b, v) -> config.autoHitRequireCooldown = v));
		y += row;
		y = addIntSlider(cx, y, row, "Auto-Hit CD %", 50, 100,
				() -> config.autoHitCooldownPercent, v -> config.autoHitCooldownPercent = v);
		y = addIntSlider(cx, y, row, "Auto-Hit Min Ticks", 0, 10,
				() -> config.autoHitMinTicks, v -> config.autoHitMinTicks = v);
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoHitWeaponsOnly)
				.build(cx - 100, y, 200, 20, Text.literal("Auto-Hit Weapons Only"),
						(b, v) -> config.autoHitWeaponsOnly = v));
		y += row;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoHitWaitForCrit)
				.build(cx - 100, y, 200, 20, Text.literal("Auto-Hit Wait For Crit"),
						(b, v) -> config.autoHitWaitForCrit = v));
		y += row;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoHitShieldDisable)
				.build(cx - 100, y, 200, 20, Text.literal("Auto-Hit Shield Disable"),
						(b, v) -> config.autoHitShieldDisable = v));
		y += row;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.autoHitMaceSwap)
				.build(cx - 100, y, 200, 20, Text.literal("Auto-Hit Mace Swap"),
						(b, v) -> config.autoHitMaceSwap = v));
		y += row;
		this.addDrawableChild(ButtonWidget.builder(
				Text.literal("Auto-Hit Mace Filter: " + maceFilterLabel(config.autoHitMaceFilter)),
				btn -> {
					int n = (config.autoHitMaceFilter + 1) % 5;
					config.autoHitMaceFilter = n;
					btn.setMessage(Text.literal("Auto-Hit Mace Filter: " + maceFilterLabel(n)));
				}).dimensions(cx - 100, y, 200, 20).build());
		y += row;
		y = addIntSlider(cx, y, row, "Mace Hold Ticks", 0, 10,
				() -> config.autoHitMaceHoldTicks, v -> config.autoHitMaceHoldTicks = v);
		y = addIntSlider(cx, y, row, "Crit Wait Timeout", 1, 40,
				() -> config.autoHitCritTimeout, v -> config.autoHitCritTimeout = v);
	}

	private void buildWeapons(int cx, int y, int row) {
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.lungeSwapEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Spear Lunge Assist"),
						(b, v) -> config.lungeSwapEnabled = v));
		y += row;
		y = addIntSlider(cx, y, row, "Lunge Delay", 0, 20,
				() -> config.lungeSwapDelay, v -> config.lungeSwapDelay = v);
		y += 4;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.maceSwapEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Mace Cooldown Assist"),
						(b, v) -> config.maceSwapEnabled = v));
		y += row;
		y = addIntSlider(cx, y, row, "Mace Delay", 0, 20,
				() -> config.maceSwapDelay, v -> config.maceSwapDelay = v);
		y += 4;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.swordCooldownAssist)
				.build(cx - 100, y, 200, 20, Text.literal("Sword CD Assist"),
						(b, v) -> config.swordCooldownAssist = v));
		y += row;
		y = addIntSlider(cx, y, row, "Sword CD Delay", 0, 20,
				() -> config.swordCooldownDelay, v -> config.swordCooldownDelay = v);
		y += 4;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.swordRequireFullCooldown)
				.build(cx - 100, y, 200, 20, Text.literal("Sword Full CD Only"),
						(b, v) -> config.swordRequireFullCooldown = v));
		y += row;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.swordSprintReset)
				.build(cx - 100, y, 200, 20, Text.literal("Sword Sprint Reset"),
						(b, v) -> config.swordSprintReset = v));
		y += row;
		y = addIntSlider(cx, y, row, "Sprint Reset Ticks", 1, 5,
				() -> config.swordSprintResetTicks, v -> config.swordSprintResetTicks = v);
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.swordCritAssist)
				.build(cx - 100, y, 200, 20, Text.literal("Sword Crit Assist"),
						(b, v) -> config.swordCritAssist = v));
	}

	private void buildMisc(int cx, int y, int row) {
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.modEnabled)
				.build(cx - 100, y, 200, 20, Text.literal("Master Enable"),
						(b, v) -> config.modEnabled = v));
		y += row;
		this.addDrawableChild(CyclingButtonWidget.onOffBuilder(config.resetCooldownOnSwap)
				.build(cx - 100, y, 200, 20, Text.literal("Reset CD On Swap"),
						(b, v) -> config.resetCooldownOnSwap = v));
	}

	private static String maceFilterLabel(int f) {
		return switch (f) {
			case 1 -> "Breach Only";
			case 2 -> "Density Only";
			case 3 -> "Breach OR Density";
			case 4 -> "Breach AND Density";
			default -> "Any Mace";
		};
	}

	private int addDelaySlider(int cx, int y, int row, String label, IntSupplier get, IntConsumer set) {
		int cur = get.getAsInt();
		int idx = (cur == InteractionDelayConfig.VANILLA) ? 0 : Math.min(5, cur + 1);
		double init = idx / 5.0;
		this.addDrawableChild(new SliderWidget(cx - 100, y, 200, 20,
				delayMessage(label, cur), init) {
			@Override
			protected void updateMessage() {
				int i = (int) Math.round(this.value * 5);
				int v = (i <= 0) ? InteractionDelayConfig.VANILLA : (i - 1);
				setMessage(delayMessage(label, v));
			}

			@Override
			protected void applyValue() {
				int i = (int) Math.round(this.value * 5);
				int v = (i <= 0) ? InteractionDelayConfig.VANILLA : Math.min(4, i - 1);
				set.accept(v);
			}
		});
		return y + row;
	}

	private static Text delayMessage(String label, int v) {
		if (v == InteractionDelayConfig.VANILLA) {
			return Text.literal(label + ": Vanilla");
		}
		return Text.literal(label + ": " + v + "t");
	}

	private int addIntSlider(int cx, int y, int row, String label, int min, int max,
			IntSupplier get, IntConsumer set) {
		int cur = Math.max(min, Math.min(max, get.getAsInt()));
		double init = (max == min) ? 0 : (cur - min) / (double) (max - min);
		this.addDrawableChild(new SliderWidget(cx - 100, y, 200, 20,
				Text.literal(label + ": " + cur), init) {
			@Override
			protected void updateMessage() {
				int v = min + (int) Math.round(this.value * (max - min));
				setMessage(Text.literal(label + ": " + v));
			}

			@Override
			protected void applyValue() {
				int v = min + (int) Math.round(this.value * (max - min));
				set.accept(v);
			}
		});
		return y + row;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		super.render(context, mouseX, mouseY, delta);
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 10, 0xFFFFFF);
	}

	@Override
	public void close() {
		config.save();
		if (this.client != null) {
			this.client.setScreen(parent);
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
