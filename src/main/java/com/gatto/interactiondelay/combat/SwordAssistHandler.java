package com.gatto.interactiondelay.combat;

import com.gatto.interactiondelay.InteractionDelay;
import com.gatto.interactiondelay.config.InteractionDelayConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sword-focused assists:
 * <ul>
 *   <li>Require full attack cooldown (no weak hits)</li>
 *   <li>Sprint-reset after a hit for better knockback</li>
 *   <li>Crit assist — small jump when attacking on ground with full cooldown</li>
 * </ul>
 */
public final class SwordAssistHandler {
	private static int sprintResetTicks = 0;
	private static boolean wasSprinting = false;

	private SwordAssistHandler() {
	}

	public static void tick(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || client.player == null) {
			sprintResetTicks = 0;
			return;
		}

		if (sprintResetTicks > 0) {
			sprintResetTicks--;
			client.player.setSprinting(false);
			if (sprintResetTicks <= 0 && wasSprinting && client.options.sprintKey.isPressed()) {
				client.player.setSprinting(true);
			}
			if (sprintResetTicks <= 0) {
				wasSprinting = false;
			}
		}
	}

	/**
	 * @return true if the attack should be cancelled (e.g. cooldown not full)
	 */
	public static boolean shouldCancelAttack(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || client.player == null) {
			return false;
		}
		if (!isSword(client.player.getMainHandStack())) {
			return false;
		}
		if (config.swordRequireFullCooldown) {
			float progress = client.player.getAttackCooldownProgress(0.5f);
			if (progress < 0.9f) {
				return true;
			}
		}
		return false;
	}

	public static void beforeAttack(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || client.player == null) {
			return;
		}
		if (!isSword(client.player.getMainHandStack())) {
			return;
		}

		if (config.swordCritAssist
				&& client.player.isOnGround()
				&& !client.player.isTouchingWater()
				&& !client.player.isClimbing()
				&& client.player.getAttackCooldownProgress(0.5f) >= 0.9f) {
			client.player.jump();
		}
	}

	public static void afterAttack(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || client.player == null) {
			return;
		}
		if (!isSword(client.player.getMainHandStack())) {
			return;
		}

		if (config.swordSprintReset) {
			wasSprinting = client.player.isSprinting() || client.options.sprintKey.isPressed();
			client.player.setSprinting(false);
			sprintResetTicks = Math.max(1, Math.min(5, config.swordSprintResetTicks));
		}
	}

	private static boolean isSword(ItemStack stack) {
		return !stack.isEmpty() && stack.isIn(ItemTags.SWORDS);
	}
}
