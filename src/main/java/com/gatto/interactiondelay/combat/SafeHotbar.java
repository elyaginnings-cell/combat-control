package com.gatto.interactiondelay.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;

/**
 * Hotbar selection — normal rate-limited select, plus {@link #selectNow} for combat.
 */
public final class SafeHotbar {
	public static final int MIN_INTERVAL = 2;

	private static int lastSentSlot = -1;
	private static int ticksSinceSend = 100;
	private static int pendingSlot = -1;
	private static int keyDownSlot = -1;
	private static int keyDownTicksLeft = 0;

	private SafeHotbar() {
	}

	public static void tick(MinecraftClient client) {
		ticksSinceSend++;
		if (keyDownTicksLeft > 0) {
			keyDownTicksLeft--;
			if (keyDownTicksLeft <= 0 && keyDownSlot >= 0) {
				setHotbarKeyPressed(client, keyDownSlot, false);
				keyDownSlot = -1;
			}
		}
		if (pendingSlot >= 0 && ticksSinceSend >= MIN_INTERVAL) {
			int slot = pendingSlot;
			pendingSlot = -1;
			sendNow(client, slot);
		}
	}

	/** Rate-limited (anchors / general). */
	public static boolean select(MinecraftClient client, int slot) {
		if (client == null || client.player == null || slot < 0 || slot > 8) {
			return false;
		}
		int current = client.player.getInventory().getSelectedSlot();
		if (current == slot && pendingSlot < 0 && lastSentSlot == slot) {
			return true;
		}
		client.player.getInventory().setSelectedSlot(slot);
		if (ticksSinceSend >= MIN_INTERVAL) {
			pendingSlot = -1;
			sendNow(client, slot);
		} else {
			pendingSlot = slot;
		}
		return true;
	}

	/**
	 * Immediate slot change for combat (stun / mace / axe).
	 * Always sends UpdateSelectedSlot this tick.
	 */
	public static boolean selectNow(MinecraftClient client, int slot) {
		if (client == null || client.player == null || slot < 0 || slot > 8) {
			return false;
		}
		pendingSlot = -1;
		client.player.getInventory().setSelectedSlot(slot);
		sendNow(client, slot);
		return true;
	}

	private static void sendNow(MinecraftClient client, int slot) {
		if (client.player == null) {
			return;
		}
		if (client.player.getInventory().getSelectedSlot() != slot) {
			client.player.getInventory().setSelectedSlot(slot);
		}
		ClientPlayNetworkHandler net = client.getNetworkHandler();
		if (net != null) {
			net.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
		}
		if (keyDownSlot >= 0 && keyDownSlot != slot) {
			setHotbarKeyPressed(client, keyDownSlot, false);
		}
		setHotbarKeyPressed(client, slot, true);
		keyDownSlot = slot;
		keyDownTicksLeft = 1;
		lastSentSlot = slot;
		ticksSinceSend = 0;
	}

	private static void setHotbarKeyPressed(MinecraftClient client, int slot, boolean pressed) {
		if (client == null || client.options == null) {
			return;
		}
		KeyBinding[] keys = client.options.hotbarKeys;
		if (keys == null || slot < 0 || slot >= keys.length) {
			return;
		}
		KeyBinding key = keys[slot];
		if (key != null) {
			key.setPressed(pressed);
		}
	}

	public static void resetTracking() {
		lastSentSlot = -1;
		ticksSinceSend = 100;
		pendingSlot = -1;
		keyDownSlot = -1;
		keyDownTicksLeft = 0;
	}
}
