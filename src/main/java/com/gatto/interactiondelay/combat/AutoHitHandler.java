package com.gatto.interactiondelay.combat;

import com.gatto.interactiondelay.InteractionDelay;
import com.gatto.interactiondelay.config.InteractionDelayConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class AutoHitHandler {
	private enum MacePhase { IDLE, WAIT, HOLD }

	private static int ticksSinceHit = 100;
	private static int critWaitTicks = 0;
	private static MacePhase macePhase = MacePhase.IDLE;
	private static int maceTicks = 0;
	private static int maceSlot = -1;
	private static int maceRestore = -1;
	private static Entity maceTarget = null;
	private static int feedbackCooldown = 0;

	private AutoHitHandler() {
	}

	public static void tick(MinecraftClient client) {
		ticksSinceHit++;
		if (feedbackCooldown > 0) {
			feedbackCooldown--;
		}

		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (client.player == null) {
			return;
		}

		if (macePhase == MacePhase.WAIT) {
			maceTicks++;
			if (maceTicks >= 2) {
				CombatInput.selectSlot(client, maceSlot);
				if (maceTarget != null && maceTarget.isAlive()) {
					CombatInput.hardAttack(client, maceTarget);
					ticksSinceHit = 0;
				}
				macePhase = MacePhase.HOLD;
				maceTicks = 0;
			}
			return;
		}
		if (macePhase == MacePhase.HOLD) {
			maceTicks++;
			if (maceTicks >= Math.max(1, config.autoHitMaceHoldTicks)) {
				if (maceRestore >= 0) {
					CombatInput.selectSlot(client, maceRestore);
				}
				macePhase = MacePhase.IDLE;
				maceSlot = -1;
				maceRestore = -1;
				maceTarget = null;
			}
			return;
		}

		if (!config.modEnabled || !config.autoHitEnabled) {
			return;
		}
		if (client.interactionManager == null || client.currentScreen != null
				|| client.player.isSpectator() || client.player.isSleeping()) {
			return;
		}
		if (client.player.isBlocking()) {
			return;
		}
		if (StunSlamHandler.isActive()) {
			return;
		}

		ItemStack main = client.player.getMainHandStack();
		if (config.autoHitWeaponsOnly && !isWeapon(main)) {
			return;
		}

		Entity target = resolveTarget(client);
		if (target == null) {
			return;
		}

		double range = Math.max(1.0, Math.min(6.0, config.autoHitRange)) + 0.5;
		if (client.player.squaredDistanceTo(target) > range * range) {
			return;
		}

		if (config.autoHitRequireCooldown) {
			float need = Math.max(0.5f, Math.min(1.0f, config.autoHitCooldownPercent / 100.0f));
			if (client.player.getAttackCooldownProgress(0.0f) < need) {
				return;
			}
		} else if (ticksSinceHit < Math.max(0, config.autoHitMinTicks)) {
			return;
		}

		if (config.autoHitWaitForCrit) {
			PlayerEntity p = client.player;
			if (isAirborne(p) && !canCrit(p)) {
				critWaitTicks++;
				if (critWaitTicks <= Math.max(1, config.autoHitCritTimeout)) {
					return;
				}
			} else {
				critWaitTicks = 0;
			}
		} else {
			critWaitTicks = 0;
		}

		if (target instanceof LivingEntity living && isShielding(living)) {
			if (StunSlamHandler.tryStart(client, target, true)) {
				ticksSinceHit = 0;
				return;
			}
			if (config.autoHitShieldDisable) {
				int axe = StunSlamHandler.findAxe(client.player.getInventory());
				if (axe >= 0) {
					int cur = client.player.getInventory().getSelectedSlot();
					CombatInput.selectSlot(client, axe);
					CombatInput.hardAttack(client, target);
					ticksSinceHit = 0;
					if (cur != axe) {
						AxeSwitchHandler.onExternalShieldBreak(client, target, cur);
					}
					return;
				}
			}
		}

		if (config.autoHitMaceSwap) {
			int mace = StunSlamHandler.findMace(client.player.getInventory(), config.autoHitMaceFilter);
			if (mace < 0) {
				if (feedbackCooldown <= 0) {
					CombatInput.feedback(client, "Mace swap: no mace in hotbar (filter=" + config.autoHitMaceFilter + ")");
					feedbackCooldown = 40;
				}
			} else if (client.player.getInventory().getSelectedSlot() != mace) {
				maceRestore = client.player.getInventory().getSelectedSlot();
				maceSlot = mace;
				maceTarget = target;
				CombatInput.selectSlot(client, mace);
				macePhase = MacePhase.WAIT;
				maceTicks = 0;
				if (feedbackCooldown <= 0) {
					CombatInput.feedback(client, "Mace swap to slot " + (mace + 1));
					feedbackCooldown = 20;
				}
				return;
			}
		}

		CombatInput.hardAttack(client, target);
		ticksSinceHit = 0;
		critWaitTicks = 0;
	}

	private static boolean isShielding(LivingEntity e) {
		if (e.isBlocking()) {
			return true;
		}
		ItemStack active = e.getActiveItem();
		if (!active.isEmpty() && active.isOf(Items.SHIELD)) {
			return true;
		}
		return e.isUsingItem() && e.getOffHandStack().isOf(Items.SHIELD);
	}

	private static boolean canCrit(PlayerEntity p) {
		return p.fallDistance > 0.0f && !p.isOnGround() && !p.isClimbing()
				&& !p.isTouchingWater() && !p.hasVehicle();
	}

	private static boolean isAirborne(PlayerEntity p) {
		return !p.isOnGround() && !p.isTouchingWater() && !p.hasVehicle() && !p.isClimbing();
	}

	private static Entity resolveTarget(MinecraftClient client) {
		HitResult hit = client.crosshairTarget;
		if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
			Entity e = ((EntityHitResult) hit).getEntity();
			if (e instanceof LivingEntity living && living.isAlive()
					&& living != client.player && !(living instanceof ArmorStandEntity)) {
				return living;
			}
		}
		return null;
	}

	private static boolean isWeapon(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		if (stack.isIn(ItemTags.SWORDS) || stack.isIn(ItemTags.AXES)) {
			return true;
		}
		return StunSlamHandler.isMace(stack) || stack.isOf(Items.TRIDENT);
	}
}
