package com.gatto.interactiondelay.config;

import com.gatto.interactiondelay.InteractionDelay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Builds a Cloth Config screen via reflection so we compile without
 * intermediary-mapped Cloth jars. Works with Cloth Config 19/20/21 at runtime.
 */
public final class ClothConfigFactory {
	private ClothConfigFactory() {
	}

	public static boolean isAvailable() {
		try {
			Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
			return true;
		} catch (Throwable t) {
			return false;
		}
	}

	public static Screen create(Screen parent) {
		try {
			return buildReflective(parent);
		} catch (Throwable t) {
			InteractionDelay.LOGGER.warn("Cloth Config UI failed, falling back: {}", t.toString());
			return new InteractionDelayConfigScreen(parent);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static Screen buildReflective(Screen parent) throws Exception {
		InteractionDelayConfig c = InteractionDelay.getConfig();

		Class<?> builderCl = Class.forName("me.shedaniel.clothconfig2.api.ConfigBuilder");
		Object builder = builderCl.getMethod("create").invoke(null);
		builderCl.getMethod("setParentScreen", Screen.class).invoke(builder, parent);
		builderCl.getMethod("setTitle", Text.class).invoke(builder, Text.literal("Combat Control"));
		try {
			builderCl.getMethod("setTransparentBackground", boolean.class).invoke(builder, true);
		} catch (NoSuchMethodException ignored) {
		}
		builderCl.getMethod("setSavingRunnable", Runnable.class).invoke(builder, (Runnable) c::save);

		Object entryBuilder = builderCl.getMethod("entryBuilder").invoke(builder);
		Class<?> entryApi = Class.forName("me.shedaniel.clothconfig2.api.ConfigEntryBuilder");

		Object general = cat(builder, "General");
		addToggle(entryApi, entryBuilder, general, "Master Enable", c.modEnabled, true, v -> c.modEnabled = v,
				"Enable or disable the entire mod.");
		addToggle(entryApi, entryBuilder, general, "Reset CD On Hotbar Swap", c.resetCooldownOnSwap, true,
				v -> c.resetCooldownOnSwap = v, "Clears item-use cooldown when you change hotbar slots.");

		Object delays = cat(builder, "Delays");
		addDesc(entryApi, entryBuilder, delays, "-1 = Vanilla · 0-4 = ticks between uses");
		addDelay(entryApi, entryBuilder, delays, "Block Placement", c.blockPlacementDelay, 2, v -> c.blockPlacementDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Obsidian", c.obsidianPlaceDelay, 2, v -> c.obsidianPlaceDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Ender Pearl", c.enderPearlDelay, 2, v -> c.enderPearlDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Wind Charge", c.windChargeDelay, 2, v -> c.windChargeDelay = v);
		addDelay(entryApi, entryBuilder, delays, "End Crystal", c.endCrystalDelay, 2, v -> c.endCrystalDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Firework", c.fireworkDelay, -1, v -> c.fireworkDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Chorus Fruit", c.chorusFruitDelay, -1, v -> c.chorusFruitDelay = v);
		addDelay(entryApi, entryBuilder, delays, "XP Bottle", c.xpBottleDelay, -1, v -> c.xpBottleDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Splash Potion", c.splashPotionDelay, -1, v -> c.splashPotionDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Armor Equip", c.armorDelay, 4, v -> c.armorDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Elytra Equip", c.elytraDelay, 4, v -> c.elytraDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Other Items", c.otherItemDelay, -1, v -> c.otherItemDelay = v);
		addDelay(entryApi, entryBuilder, delays, "Block Interaction", c.blockInteractionDelay, -1, v -> c.blockInteractionDelay = v);

		Object anchors = cat(builder, "Anchors");
		addDelay(entryApi, entryBuilder, anchors, "Anchor Place Delay", c.respawnAnchorPlaceDelay, 1, v -> c.respawnAnchorPlaceDelay = v);
		addDelay(entryApi, entryBuilder, anchors, "Glowstone Charge Delay", c.glowstoneChargeDelay, 1, v -> c.glowstoneChargeDelay = v);
		addDelay(entryApi, entryBuilder, anchors, "Anchor Explode Delay", c.anchorExplodeDelay, 1, v -> c.anchorExplodeDelay = v);
		addToggle(entryApi, entryBuilder, anchors, "Anchor Sequence", c.anchorComboEnabled, true,
				v -> c.anchorComboEnabled = v, "Place -> GS -> charge -> totem -> boom -> restore.");
		addToggle(entryApi, entryBuilder, anchors, "Safe Anchoring", c.anchorComboSafe, false,
				v -> c.anchorComboSafe = v, "No auto swaps; 2-tick delays on anchor kit only.");
		addSlider(entryApi, entryBuilder, anchors, "Min Ticks After Place", c.anchorComboPlaceTicks, 0, 10, 0, v -> c.anchorComboPlaceTicks = v);
		addSlider(entryApi, entryBuilder, anchors, "Min Ticks On Glowstone", c.anchorComboChargeTicks, 0, 10, 1, v -> c.anchorComboChargeTicks = v);
		addSlider(entryApi, entryBuilder, anchors, "Min Ticks On Totem", c.anchorComboStepTicks, 0, 10, 1, v -> c.anchorComboStepTicks = v);

		Object combat = cat(builder, "Combat");
		addToggle(entryApi, entryBuilder, combat, "Auto Axe-Switch", c.axeSwitchEnabled, true, v -> c.axeSwitchEnabled = v, null);
		addToggle(entryApi, entryBuilder, combat, "Axe Pre-Switch", c.axeSwitchPreemptive, true, v -> c.axeSwitchPreemptive = v, null);
		addSlider(entryApi, entryBuilder, combat, "Axe Hold Ticks", c.axeSwitchHoldTicks, 0, 10, 2, v -> c.axeSwitchHoldTicks = v);
		addSlider(entryApi, entryBuilder, combat, "Shield-Break Chance %", c.axeSwitchChance, 0, 100, 100, v -> c.axeSwitchChance = v);
		addToggle(entryApi, entryBuilder, combat, "Double Hit", c.doubleHitEnabled, false, v -> c.doubleHitEnabled = v, null);
		addSlider(entryApi, entryBuilder, combat, "Double-Hit CPS", c.doubleHitCps, 1, 20, 10, v -> c.doubleHitCps = v);
		addToggle(entryApi, entryBuilder, combat, "Stun Slam", c.stunSlamEnabled, true, v -> c.stunSlamEnabled = v,
				"Axe -> mace -> restore on shielded targets.");
		addSlider(entryApi, entryBuilder, combat, "Stun: Slot -> Axe", c.stunSlamSlotToAxeTicks, 0, 20, 0, v -> c.stunSlamSlotToAxeTicks = v);
		addSlider(entryApi, entryBuilder, combat, "Stun: Axe -> Mace", c.stunSlamAxeToMaceTicks, 0, 20, 2, v -> c.stunSlamAxeToMaceTicks = v);
		addSlider(entryApi, entryBuilder, combat, "Stun: Mace -> Restore", c.stunSlamMaceToRestoreTicks, 0, 20, 2, v -> c.stunSlamMaceToRestoreTicks = v);

		Object auto = cat(builder, "Auto-Hit");
		addToggle(entryApi, entryBuilder, auto, "Enable Auto-Hit", c.autoHitEnabled, false, v -> c.autoHitEnabled = v,
				"Attack when crosshair is on a living target in range.");
		addSlider(entryApi, entryBuilder, auto, "Range (blocks)", c.autoHitRange, 1, 6, 3, v -> c.autoHitRange = v);
		addToggle(entryApi, entryBuilder, auto, "Cooldown Gate", c.autoHitRequireCooldown, true, v -> c.autoHitRequireCooldown = v, null);
		addSlider(entryApi, entryBuilder, auto, "Cooldown Threshold %", c.autoHitCooldownPercent, 50, 100, 80, v -> c.autoHitCooldownPercent = v);
		addSlider(entryApi, entryBuilder, auto, "Min Ticks Between Hits", c.autoHitMinTicks, 0, 10, 1, v -> c.autoHitMinTicks = v);
		addToggle(entryApi, entryBuilder, auto, "Weapons Only", c.autoHitWeaponsOnly, true, v -> c.autoHitWeaponsOnly = v, null);
		addToggle(entryApi, entryBuilder, auto, "Wait For Crit", c.autoHitWaitForCrit, true, v -> c.autoHitWaitForCrit = v,
				"When jumping, wait until falling to crit.");
		addSlider(entryApi, entryBuilder, auto, "Crit Wait Timeout", c.autoHitCritTimeout, 1, 40, 12, v -> c.autoHitCritTimeout = v);
		addToggle(entryApi, entryBuilder, auto, "Shield Disable", c.autoHitShieldDisable, true, v -> c.autoHitShieldDisable = v,
				"Swap to axe when target is blocking, then restore.");
		addToggle(entryApi, entryBuilder, auto, "Mace Swap", c.autoHitMaceSwap, false, v -> c.autoHitMaceSwap = v,
				"Swap to a mace before auto-hitting (when not stun-slamming).");
		addSlider(entryApi, entryBuilder, auto, "Mace Filter (0any 1B 2D 3B|D 4B&D)", c.autoHitMaceFilter, 0, 4, 0, v -> c.autoHitMaceFilter = v);
		addSlider(entryApi, entryBuilder, auto, "Mace Hold Ticks", c.autoHitMaceHoldTicks, 0, 10, 2, v -> c.autoHitMaceHoldTicks = v);

		Object weapons = cat(builder, "Weapons");
		addToggle(entryApi, entryBuilder, weapons, "Spear Lunge Assist", c.lungeSwapEnabled, false, v -> c.lungeSwapEnabled = v, null);
		addSlider(entryApi, entryBuilder, weapons, "Lunge Delay", c.lungeSwapDelay, 0, 20, 0, v -> c.lungeSwapDelay = v);
		addToggle(entryApi, entryBuilder, weapons, "Mace Cooldown Assist", c.maceSwapEnabled, false, v -> c.maceSwapEnabled = v, null);
		addSlider(entryApi, entryBuilder, weapons, "Mace Delay", c.maceSwapDelay, 0, 20, 0, v -> c.maceSwapDelay = v);
		addToggle(entryApi, entryBuilder, weapons, "Sword CD Assist", c.swordCooldownAssist, false, v -> c.swordCooldownAssist = v, null);
		addSlider(entryApi, entryBuilder, weapons, "Sword CD Delay", c.swordCooldownDelay, 0, 20, 0, v -> c.swordCooldownDelay = v);
		addToggle(entryApi, entryBuilder, weapons, "Sword Full CD Only", c.swordRequireFullCooldown, false, v -> c.swordRequireFullCooldown = v, null);
		addToggle(entryApi, entryBuilder, weapons, "Sword Sprint Reset", c.swordSprintReset, false, v -> c.swordSprintReset = v, null);
		addSlider(entryApi, entryBuilder, weapons, "Sprint Reset Ticks", c.swordSprintResetTicks, 1, 5, 1, v -> c.swordSprintResetTicks = v);
		addToggle(entryApi, entryBuilder, weapons, "Sword Crit Assist", c.swordCritAssist, false, v -> c.swordCritAssist = v, null);

		Object screen = builderCl.getMethod("build").invoke(builder);
		return (Screen) screen;
	}

	private static Object cat(Object builder, String name) throws Exception {
		return builder.getClass().getMethod("getOrCreateCategory", Text.class)
				.invoke(builder, Text.literal(name));
	}

	private static void addDesc(Class<?> entryApi, Object eb, Object cat, String text) throws Exception {
		Object entry = entryApi.getMethod("startTextDescription", Text.class)
				.invoke(eb, Text.literal(text));
		Object built = entry.getClass().getMethod("build").invoke(entry);
		cat.getClass().getMethod("addEntry", Class.forName("me.shedaniel.clothconfig2.api.AbstractConfigListEntry"))
				.invoke(cat, built);
	}

	private static void addToggle(Class<?> entryApi, Object eb, Object cat, String name, boolean val,
			boolean def, Consumer<Boolean> save, String tip) throws Exception {
		Object builder = entryApi.getMethod("startBooleanToggle", Text.class, boolean.class)
				.invoke(eb, Text.literal(name), val);
		Class<?> bCl = builder.getClass();
		bCl.getMethod("setDefaultValue", boolean.class).invoke(builder, def);
		bCl.getMethod("setSaveConsumer", Consumer.class).invoke(builder, save);
		if (tip != null) {
			try {
				bCl.getMethod("setTooltip", Text[].class).invoke(builder, (Object) new Text[]{Text.literal(tip)});
			} catch (NoSuchMethodException ex) {
				try {
					bCl.getMethod("setTooltip", Text.class).invoke(builder, Text.literal(tip));
				} catch (NoSuchMethodException ignored) {
				}
			}
		}
		Object built = bCl.getMethod("build").invoke(builder);
		addEntry(cat, built);
	}

	private static void addSlider(Class<?> entryApi, Object eb, Object cat, String name, int val,
			int min, int max, int def, Consumer<Integer> save) throws Exception {
		Object builder = entryApi.getMethod("startIntSlider", Text.class, int.class, int.class, int.class)
				.invoke(eb, Text.literal(name), val, min, max);
		Class<?> bCl = builder.getClass();
		bCl.getMethod("setDefaultValue", int.class).invoke(builder, def);
		bCl.getMethod("setSaveConsumer", Consumer.class).invoke(builder, save);
		Object built = bCl.getMethod("build").invoke(builder);
		addEntry(cat, built);
	}

	private static void addDelay(Class<?> entryApi, Object eb, Object cat, String name, int val,
			int def, Consumer<Integer> save) throws Exception {
		Object builder = entryApi.getMethod("startIntSlider", Text.class, int.class, int.class, int.class)
				.invoke(eb, Text.literal(name), val, -1, 4);
		Class<?> bCl = builder.getClass();
		bCl.getMethod("setDefaultValue", int.class).invoke(builder, def);
		bCl.getMethod("setSaveConsumer", Consumer.class).invoke(builder, save);
		try {
			Function<Integer, Text> getter = v -> Text.literal(v < 0 ? name + ": Vanilla" : name + ": " + v + "t");
			bCl.getMethod("setTextGetter", Function.class).invoke(builder, getter);
		} catch (NoSuchMethodException ignored) {
		}
		Object built = bCl.getMethod("build").invoke(builder);
		addEntry(cat, built);
	}

	private static void addEntry(Object cat, Object entry) throws Exception {
		Class<?> entryType = Class.forName("me.shedaniel.clothconfig2.api.AbstractConfigListEntry");
		cat.getClass().getMethod("addEntry", entryType).invoke(cat, entry);
	}
}
