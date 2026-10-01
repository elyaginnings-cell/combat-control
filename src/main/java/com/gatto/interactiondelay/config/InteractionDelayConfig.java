package com.gatto.interactiondelay.config;

import com.gatto.interactiondelay.InteractionDelay;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Combat Control configuration.
 * Delay: 0-4 clamp ticks, -1 = vanilla (untouched).
 */
public final class InteractionDelayConfig {
	public static final int VANILLA = -1;
	public static final int MIN_DELAY = 0;
	public static final int MAX_DELAY = 4;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path CONFIG_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("combatcontrol.json");
	private static final Path LEGACY_PATH = FabricLoader.getInstance()
			.getConfigDir()
			.resolve("interactiondelay.json");

	public boolean modEnabled = true;

	public int blockPlacementDelay = 2;
	public int obsidianPlaceDelay = 2;
	public int enderPearlDelay = 2;
	public int windChargeDelay = 2;
	public int endCrystalDelay = 2;
	public int fireworkDelay = VANILLA;
	public int chorusFruitDelay = VANILLA;
	public int xpBottleDelay = VANILLA;
	public int splashPotionDelay = VANILLA;
	public int armorDelay = 4;
	public int elytraDelay = 4;
	public int otherItemDelay = VANILLA;
	public int blockInteractionDelay = VANILLA;

	public int respawnAnchorPlaceDelay = 1;
	public int glowstoneChargeDelay = 1;
	public int anchorExplodeDelay = 1;
	public boolean anchorPlaceLockEnabled = false;
	public boolean anchorComboEnabled = true;
	public int anchorComboStepTicks = 1;
	public int anchorComboPlaceTicks = 1;
	public int anchorComboChargeTicks = 1;
	public boolean anchorComboSafe = false;
	public int anchorPlaceLockTicks = 2;
	public boolean anchorPreferOverGlowstone = false;
	public int anchorPreferSuppressTicks = 6;
	public int anchorPlaceKey = 86;
	public int anchorChargeKey = 71;
	public int anchorTotemKey = 82;
	public boolean anchorAutoGlowstone = false;
	public int anchorAutoGlowstoneDelay = 2;
	public boolean anchorAutoCharge = false;
	public int anchorAutoChargeWindow = 4;

	public boolean crystalPlaceLockEnabled = false;
	public int crystalPlaceLockTicks = 2;

	public boolean axeSwitchEnabled = true;
	public int axeSwitchChance = 100;
	public boolean doubleHitEnabled = true;
	public int doubleHitCps = 10;
	public int axeSwitchHoldTicks = 2;
	public boolean axeSwitchPreemptive = true;

	public boolean lungeSwapEnabled = true;
	public int lungeSwapDelay = 0;
	public boolean maceSwapEnabled = true;
	public int maceSwapDelay = 0;
	public boolean swordCooldownAssist = false;
	public int swordCooldownDelay = 0;
	public boolean swordRequireFullCooldown = false;
	public boolean swordSprintReset = false;
	public int swordSprintResetTicks = 1;
	public boolean swordCritAssist = false;
	public boolean stunSlamEnabled = true;

	public boolean autoHitEnabled = false;
	public int autoHitRange = 3;
	public boolean autoHitRequireCooldown = true;
	public int autoHitCooldownPercent = 80;
	public int autoHitMinTicks = 1;
	public boolean autoHitWeaponsOnly = true;
	public boolean autoHitWaitForCrit = true;
	public int autoHitCritTimeout = 12;
	public boolean autoHitShieldDisable = true;
	public boolean autoHitMaceSwap = true;
	public int autoHitMaceFilter = 0;
	public int autoHitMaceHoldTicks = 2;

	public int stunSlamSlotToAxeTicks = 0;
	public int stunSlamAxeToMaceTicks = 2;
	public int stunSlamMaceToRestoreTicks = 2;
	public int stunSlamMaceFilter = 0;

	public boolean resetCooldownOnSwap = true;
	public int openConfigKey = 92;

	public static InteractionDelayConfig load() {
		Path path = Files.isRegularFile(CONFIG_PATH) ? CONFIG_PATH
				: (Files.isRegularFile(LEGACY_PATH) ? LEGACY_PATH : CONFIG_PATH);
		if (Files.isRegularFile(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				InteractionDelayConfig loaded = GSON.fromJson(reader, InteractionDelayConfig.class);
				if (loaded != null) {
					loaded.validate();
					return loaded;
				}
			} catch (IOException | JsonSyntaxException e) {
				InteractionDelay.LOGGER.warn("Failed to load config, using defaults: {}", e.toString());
			}
		}
		InteractionDelayConfig defaults = new InteractionDelayConfig();
		defaults.validate();
		defaults.save();
		return defaults;
	}

	public void save() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			InteractionDelay.LOGGER.error("Failed to save config: {}", e.toString());
		}
	}

	public void validate() {
		blockPlacementDelay = clamp(blockPlacementDelay);
		obsidianPlaceDelay = clamp(obsidianPlaceDelay);
		enderPearlDelay = clamp(enderPearlDelay);
		windChargeDelay = clamp(windChargeDelay);
		endCrystalDelay = clamp(endCrystalDelay);
		fireworkDelay = clamp(fireworkDelay);
		chorusFruitDelay = clamp(chorusFruitDelay);
		xpBottleDelay = clamp(xpBottleDelay);
		splashPotionDelay = clamp(splashPotionDelay);
		respawnAnchorPlaceDelay = clamp(respawnAnchorPlaceDelay);
		glowstoneChargeDelay = clamp(glowstoneChargeDelay);
		anchorExplodeDelay = clamp(anchorExplodeDelay);
		armorDelay = clamp(armorDelay);
		elytraDelay = clamp(elytraDelay);
		otherItemDelay = clamp(otherItemDelay);
		blockInteractionDelay = clamp(blockInteractionDelay);
		axeSwitchChance = clampRange(axeSwitchChance, 0, 100);
		doubleHitCps = clampRange(doubleHitCps, 1, 20);
		lungeSwapDelay = clampRange(lungeSwapDelay, 0, 20);
		maceSwapDelay = clampRange(maceSwapDelay, 0, 20);
		swordCooldownDelay = clampRange(swordCooldownDelay, 0, 20);
		axeSwitchHoldTicks = clampRange(axeSwitchHoldTicks, 0, 10);
		swordSprintResetTicks = clampRange(swordSprintResetTicks, 1, 5);
		stunSlamSlotToAxeTicks = clampRange(stunSlamSlotToAxeTicks, 0, 20);
		stunSlamAxeToMaceTicks = clampRange(stunSlamAxeToMaceTicks, 0, 20);
		stunSlamMaceToRestoreTicks = clampRange(stunSlamMaceToRestoreTicks, 0, 20);
		stunSlamMaceFilter = clampRange(stunSlamMaceFilter, 0, 4);
		autoHitRange = clampRange(autoHitRange, 1, 6);
		autoHitCooldownPercent = clampRange(autoHitCooldownPercent, 50, 100);
		autoHitMinTicks = clampRange(autoHitMinTicks, 0, 10);
		autoHitCritTimeout = clampRange(autoHitCritTimeout, 1, 40);
		autoHitMaceFilter = clampRange(autoHitMaceFilter, 0, 4);
		autoHitMaceHoldTicks = clampRange(autoHitMaceHoldTicks, 0, 10);
		anchorPlaceLockTicks = clampRange(anchorPlaceLockTicks, 1, 10);
		anchorComboStepTicks = clampRange(anchorComboStepTicks, 0, 5);
		anchorComboPlaceTicks = clampRange(anchorComboPlaceTicks, 0, 10);
		anchorComboChargeTicks = clampRange(anchorComboChargeTicks, 0, 10);
		anchorAutoGlowstoneDelay = clampRange(anchorAutoGlowstoneDelay, 1, 10);
		anchorAutoChargeWindow = clampRange(anchorAutoChargeWindow, 2, 20);
		anchorPreferSuppressTicks = clampRange(anchorPreferSuppressTicks, 1, 20);
		crystalPlaceLockTicks = clampRange(crystalPlaceLockTicks, 1, 10);
	}

	private static int clampRange(int value, int min, int max) {
		if (value < min) return min;
		if (value > max) return max;
		return value;
	}

	private static int clamp(int value) {
		if (value == VANILLA) return VANILLA;
		if (value < MIN_DELAY) return MIN_DELAY;
		if (value > MAX_DELAY) return MAX_DELAY;
		return value;
	}

	public static boolean isVanilla(int delay) {
		return delay == VANILLA;
	}
}
