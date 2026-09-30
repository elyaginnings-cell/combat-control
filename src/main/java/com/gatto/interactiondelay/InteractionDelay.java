package com.gatto.interactiondelay;

import com.gatto.interactiondelay.config.InteractionDelayConfig;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Combat Control — client-side interaction delays, anchor/crystal assists,
 * axe switch, spear/mace/sword cooldown assists.
 * Open config with backslash (configurable).
 */
public final class InteractionDelay implements ClientModInitializer {
	public static final String MOD_ID = "combatcontrol";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static InteractionDelayConfig config;

	@Override
	public void onInitializeClient() {
		config = InteractionDelayConfig.load();
		LOGGER.info("Combat Control loaded — press \\ (or configured key) to open menu.");
	}

	public static InteractionDelayConfig getConfig() {
		if (config == null) {
			config = InteractionDelayConfig.load();
		}
		return config;
	}

	public static void reloadConfig() {
		config = InteractionDelayConfig.load();
	}
}
