package com.gatto.interactiondelay.config;

import net.minecraft.client.gui.screen.Screen;

public final class ConfigScreens {
	private ConfigScreens() {
	}

	public static Screen open(Screen parent) {
		if (ClothConfigFactory.isAvailable()) {
			return ClothConfigFactory.create(parent);
		}
		return new InteractionDelayConfigScreen(parent);
	}

	public static boolean isConfigScreen(Screen screen) {
		if (screen == null) {
			return false;
		}
		if (screen instanceof InteractionDelayConfigScreen) {
			return true;
		}
		String n = screen.getClass().getName();
		return n.contains("clothconfig") || n.contains("ClothConfig");
	}
}
