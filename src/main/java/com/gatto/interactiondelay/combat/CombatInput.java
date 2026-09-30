package com.gatto.interactiondelay.combat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.text.Text;

/**
 * Low-level combat input: hotbar + attack as key presses the client understands.
 */
public final class CombatInput {
	private static int releaseAttackTicks = 0;
	private static int releaseHotbarTicks = 0;
	private static int releaseHotbarSlot = -1;

	private CombatInput() {
	}

	public static void tick(MinecraftClient client) {
		if (client == null || client.options == null) {
			return;
		}
		if (releaseAttackTicks > 0) {
			releaseAttackTicks--;
			if (releaseAttackTicks <= 0) {
				client.options.attackKey.setPressed(false);
			}
		}
		if (releaseHotbarTicks > 0) {
			releaseHotbarTicks--;
			if (releaseHotbarTicks <= 0 && releaseHotbarSlot >= 0) {
				KeyBinding[] keys = client.options.hotbarKeys;
				if (keys != null && releaseHotbarSlot < keys.length && keys[releaseHotbarSlot] != null) {
					keys[releaseHotbarSlot].setPressed(false);
				}
				releaseHotbarSlot = -1;
			}
		}
	}

	/** Instant hotbar switch: local + packet + key tap. */
	public static void selectSlot(MinecraftClient client, int slot) {
		if (client == null || client.player == null || slot < 0 || slot > 8) {
			return;
		}
		client.player.getInventory().setSelectedSlot(slot);

		ClientPlayNetworkHandler net = client.getNetworkHandler();
		if (net != null) {
			net.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
		}

		KeyBinding[] keys = client.options.hotbarKeys;
		if (keys != null && slot < keys.length && keys[slot] != null) {
			if (releaseHotbarSlot >= 0 && releaseHotbarSlot != slot && releaseHotbarSlot < keys.length
					&& keys[releaseHotbarSlot] != null) {
				keys[releaseHotbarSlot].setPressed(false);
			}
			keys[slot].setPressed(true);
			releaseHotbarSlot = slot;
			releaseHotbarTicks = 2;
		}

		SafeHotbar.selectNow(client, slot);
	}

	/** Tap attack key (left click) for 1-2 ticks. */
	public static void tapAttack(MinecraftClient client) {
		if (client == null || client.options == null) {
			return;
		}
		client.options.attackKey.setPressed(true);
		releaseAttackTicks = 2;
	}

	/** Direct attackEntity + swing (backup if key tap isn't enough). */
	public static void hardAttack(MinecraftClient client, net.minecraft.entity.Entity target) {
		if (client == null || client.player == null || client.interactionManager == null || target == null) {
			return;
		}
		client.interactionManager.attackEntity(client.player, target);
		client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
		tapAttack(client);
	}

	public static void feedback(MinecraftClient client, String msg) {
		if (client != null && client.player != null) {
			client.player.sendMessage(Text.literal("\u00a7c[CC] \u00a77" + msg), true);
		}
	}
}
