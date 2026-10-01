package com.gatto.interactiondelay.combat;

import com.gatto.interactiondelay.InteractionDelay;
import com.gatto.interactiondelay.config.InteractionDelayConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.item.ItemStack;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Fast axe-switch for shield breaks.
 * <ul>
 *   <li>Preemptive: switches to axe as soon as attack is held on a blocking target</li>
 *   <li>Hold: keeps axe equipped for {@code axeSwitchHoldTicks} after the hit</li>
 *   <li>Double-hit: optional follow-up with restored weapon</li>
 * </ul>
 */
public final class AxeSwitchHandler {
	private static int slotToRestore = -1;
	private static int holdTicksLeft = 0;
	private static boolean performingDoubleHit = false;
	private static int doubleHitTicksLeft = -1;
	private static Entity doubleHitTarget = null;
	/** True after a successful axe switch this swing cycle. */
	private static boolean switchedThisSwing = false;
	/** Target from auto-hit shield break (crosshair may change before double-hit). */
	private static Entity preferredDoubleHitTarget = null;

	private AxeSwitchHandler() {
	}

	/**
	 * Called every client tick — preemptive switch + hold countdown + double-hit.
	 */
	public static void tick(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled) {
			return;
		}
		if (client.player == null || client.getNetworkHandler() == null) {
			return;
		}

		// Hold axe after hit (also used by Auto-Hit shield disable)
		if (holdTicksLeft > 0) {
			holdTicksLeft--;
			if (holdTicksLeft <= 0 && slotToRestore >= 0) {
				int restore = slotToRestore;
				slotToRestore = -1;
				switchSlot(client, restore);
				maybeQueueDoubleHit(client, config);
			}
		}

		// Preemptive: attack key held + looking at blocker → already on axe
		if (config.axeSwitchEnabled
				&& config.axeSwitchPreemptive
				&& client.options.attackKey.isPressed()
				&& slotToRestore < 0
				&& holdTicksLeft <= 0
				&& !performingDoubleHit) {
			tryPreemptiveSwitch(client, config);
		}

		// Double-hit countdown
		if (doubleHitTicksLeft >= 0) {
			doubleHitTicksLeft--;
			if (doubleHitTicksLeft <= 0) {
				doubleHitTicksLeft = -1;
				Entity target = doubleHitTarget;
				doubleHitTarget = null;
				if (target != null && target.isAlive()
						&& client.interactionManager != null
						&& !client.player.isSpectator()) {
					// Attack only — slot was restored on a previous tick
					performingDoubleHit = true;
					try {
						client.interactionManager.attackEntity(client.player, target);
						client.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
					} finally {
						performingDoubleHit = false;
					}
				}
			}
		}
	}

	public static void beforeAttack(MinecraftClient client) {
		switchedThisSwing = false;
		if (performingDoubleHit) {
			return;
		}

		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || !config.axeSwitchEnabled) {
			return;
		}
		if (client.player == null || client.getNetworkHandler() == null) {
			return;
		}

		// Already pre-switched
		if (slotToRestore >= 0) {
			switchedThisSwing = true;
			return;
		}

		Entity target = getAttackTarget(client);
		if (!(target instanceof LivingEntity living) || !living.isBlocking()) {
			return;
		}

		if (!rollChance(config)) {
			return;
		}

		if (isAxe(client.player.getMainHandStack())) {
			return;
		}

		int axeSlot = findAxeHotbarSlot(client.player.getInventory());
		if (axeSlot < 0) {
			return;
		}

		PlayerInventory inv = client.player.getInventory();
		int current = inv.getSelectedSlot();
		if (current == axeSlot) {
			return;
		}

		slotToRestore = current;
		switchSlot(client, axeSlot);
		// Send twice so the server is more likely to see axe before the attack packet
		switchSlot(client, axeSlot);
		switchedThisSwing = true;
	}

	public static void afterAttack(MinecraftClient client) {
		if (performingDoubleHit) {
			performingDoubleHit = false;
			return;
		}

		if (!switchedThisSwing && slotToRestore < 0) {
			return;
		}

		InteractionDelayConfig config = InteractionDelay.getConfig();
		int hold = Math.max(0, Math.min(10, config.axeSwitchHoldTicks));

		if (hold <= 0) {
			// Instant restore (old behavior)
			if (slotToRestore >= 0) {
				int restore = slotToRestore;
				slotToRestore = -1;
				switchSlot(client, restore);
				maybeQueueDoubleHit(client, config);
			}
		} else {
			// Keep axe for a few ticks so the shield-break registers server-side
			holdTicksLeft = hold;
		}
	}

	private static void tryPreemptiveSwitch(MinecraftClient client, InteractionDelayConfig config) {
		Entity target = getAttackTarget(client);
		if (!(target instanceof LivingEntity living) || !living.isBlocking()) {
			return;
		}
		if (isAxe(client.player.getMainHandStack())) {
			return;
		}
		if (!rollChance(config)) {
			return;
		}
		int axeSlot = findAxeHotbarSlot(client.player.getInventory());
		if (axeSlot < 0) {
			return;
		}
		int current = client.player.getInventory().getSelectedSlot();
		if (current == axeSlot) {
			return;
		}
		slotToRestore = current;
		switchSlot(client, axeSlot);
		switchedThisSwing = true;
		// Hold at least 1 tick even before the hit lands
		holdTicksLeft = Math.max(1, config.axeSwitchHoldTicks);
	}

	private static void maybeQueueDoubleHit(MinecraftClient client, InteractionDelayConfig config) {
		if (!config.doubleHitEnabled) {
			return;
		}
		Entity target = preferredDoubleHitTarget;
		preferredDoubleHitTarget = null;
		if (target == null || !target.isAlive()) {
			target = getAttackTarget(client);
		}
		if (target == null || !target.isAlive()) {
			return;
		}
		int cps = Math.max(1, Math.min(20, config.doubleHitCps));
		doubleHitTicksLeft = Math.max(1, Math.round(20.0f / cps));
		doubleHitTarget = target;
	}

	/**
	 * Called by Auto-Hit after an axe shield-break so restore + double-hit
	 * use the same path as a normal axe-switch swing.
	 */
	public static void onExternalShieldBreak(MinecraftClient client, Entity target, int restoreSlot) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled) {
			return;
		}
		slotToRestore = restoreSlot;
		preferredDoubleHitTarget = target;
		int hold = Math.max(1, Math.min(10, config.axeSwitchHoldTicks));
		holdTicksLeft = hold;
		switchedThisSwing = true;
	}

	private static boolean rollChance(InteractionDelayConfig config) {
		int chance = Math.max(0, Math.min(100, config.axeSwitchChance));
		if (chance <= 0) return false;
		if (chance >= 100) return true;
		return ThreadLocalRandom.current().nextInt(100) < chance;
	}

	private static Entity getAttackTarget(MinecraftClient client) {
		HitResult hit = client.crosshairTarget;
		if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
			return ((EntityHitResult) hit).getEntity();
		}
		return null;
	}

	private static int findAxeHotbarSlot(PlayerInventory inv) {
		for (int i = 0; i < 9; i++) {
			if (isAxe(inv.getStack(i))) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isAxe(ItemStack stack) {
		return !stack.isEmpty() && stack.isIn(ItemTags.AXES);
	}

	private static void switchSlot(MinecraftClient client, int slot) {
		SafeHotbar.selectNow(client, slot);
	}
}
