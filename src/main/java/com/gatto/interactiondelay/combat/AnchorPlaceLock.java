package com.gatto.interactiondelay.combat;

/**
 * Former hotbar slot lock — removed.
 * Forcing {@code setSelectedSlot} every tick blocked all hotbar swapping.
 * Anchor sequence + place delays handle anti-miss without locking the hotbar.
 */
public final class AnchorPlaceLock {
	private AnchorPlaceLock() {
	}

	public static void clear() {
		// no-op
	}

	public static void onItemUse(net.minecraft.client.MinecraftClient client) {
		// no-op — do not lock hotbar slots
	}

	public static void tick(net.minecraft.client.MinecraftClient client) {
		// no-op
	}
}
