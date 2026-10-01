package com.gatto.interactiondelay.combat;

import com.gatto.interactiondelay.InteractionDelay;
import com.gatto.interactiondelay.config.InteractionDelayConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public final class StunSlamHandler {
	private enum Phase {
		IDLE, AXE_WAIT, MACE_WAIT, MACE_HIT, RESTORE
	}

	private static Phase phase = Phase.IDLE;
	private static int ticks = 0;
	private static int age = 0;
	private static int originalSlot = -1;
	private static int axeSlot = -1;
	private static int maceSlot = -1;
	private static Entity target = null;
	private static boolean cancelVanilla = false;
	private static boolean silent = false;

	private StunSlamHandler() {
	}

	public static boolean isActive() {
		return phase != Phase.IDLE;
	}

	public static boolean tryStart(MinecraftClient client, Entity hit) {
		return tryStart(client, hit, false);
	}

	public static boolean tryStart(MinecraftClient client, Entity hit, boolean quiet) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || !config.stunSlamEnabled) {
			if (!quiet) {
				CombatInput.feedback(client, "Stun off");
			}
			return false;
		}
		if (phase != Phase.IDLE || client.player == null) {
			return false;
		}
		if (!(hit instanceof LivingEntity living) || !isShielding(living)) {
			if (!quiet) {
				CombatInput.feedback(client, "Stun: target not blocking");
			}
			return false;
		}
		PlayerInventory inv = client.player.getInventory();
		axeSlot = findAxe(inv);
		maceSlot = findMace(inv, config.stunSlamMaceFilter);
		if (axeSlot < 0) {
			CombatInput.feedback(client, "Stun: no axe in hotbar");
			return false;
		}
		if (maceSlot < 0) {
			CombatInput.feedback(client, "Stun: no mace (check filter)");
			return false;
		}
		originalSlot = inv.getSelectedSlot();
		target = hit;
		ticks = 0;
		age = 0;
		silent = quiet;
		CombatInput.selectSlot(client, axeSlot);
		phase = Phase.AXE_WAIT;
		if (!quiet) {
			CombatInput.feedback(client, "Stun: axe->mace");
		}
		return true;
	}

	public static void tick(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || !config.stunSlamEnabled) {
			if (phase != Phase.IDLE) {
				finish(client);
			}
			return;
		}
		if (client.player == null || phase == Phase.IDLE) {
			return;
		}
		age++;
		if (age > 50) {
			finish(client);
			return;
		}
		ticks++;
		int axeWait = Math.max(2, config.stunSlamSlotToAxeTicks + 2);
		int maceGap = Math.max(2, config.stunSlamAxeToMaceTicks);
		int restoreGap = Math.max(1, config.stunSlamMaceToRestoreTicks);

		switch (phase) {
			case AXE_WAIT -> {
				if (ticks >= axeWait) {
					if (!alive(target)) {
						finish(client);
						return;
					}
					CombatInput.selectSlot(client, axeSlot);
					CombatInput.hardAttack(client, target);
					phase = Phase.MACE_WAIT;
					ticks = 0;
				}
			}
			case MACE_WAIT -> {
				if (ticks >= maceGap) {
					if (!alive(target)) {
						finish(client);
						return;
					}
					CombatInput.selectSlot(client, maceSlot);
					phase = Phase.MACE_HIT;
					ticks = 0;
				}
			}
			case MACE_HIT -> {
				if (ticks >= 2) {
					if (!alive(target)) {
						finish(client);
						return;
					}
					CombatInput.selectSlot(client, maceSlot);
					CombatInput.hardAttack(client, target);
					phase = Phase.RESTORE;
					ticks = 0;
				}
			}
			case RESTORE -> {
				if (ticks >= restoreGap) {
					finish(client);
				}
			}
			default -> reset();
		}
	}

	public static boolean beforeAttack(MinecraftClient client) {
		cancelVanilla = false;
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || !config.stunSlamEnabled) {
			return false;
		}
		if (phase != Phase.IDLE) {
			cancelVanilla = true;
			return true;
		}
		Entity hit = crosshairEntity(client);
		if (tryStart(client, hit, true)) {
			cancelVanilla = true;
			return true;
		}
		return false;
	}

	public static boolean shouldCancelAttack() {
		return cancelVanilla;
	}

	public static void afterAttack(MinecraftClient client) {
	}

	private static void finish(MinecraftClient client) {
		if (client != null && client.player != null && originalSlot >= 0 && originalSlot < 9) {
			CombatInput.selectSlot(client, originalSlot);
		}
		reset();
	}

	private static void reset() {
		phase = Phase.IDLE;
		ticks = 0;
		age = 0;
		originalSlot = -1;
		axeSlot = -1;
		maceSlot = -1;
		target = null;
		cancelVanilla = false;
		silent = false;
	}

	private static boolean isShielding(LivingEntity e) {
		if (e.isBlocking()) {
			return true;
		}
		ItemStack active = e.getActiveItem();
		if (!active.isEmpty() && active.isOf(Items.SHIELD)) {
			return true;
		}
		ItemStack off = e.getOffHandStack();
		return e.isUsingItem() && off.isOf(Items.SHIELD);
	}

	private static boolean alive(Entity t) {
		return t != null && t.isAlive();
	}

	private static Entity crosshairEntity(MinecraftClient client) {
		HitResult hit = client.crosshairTarget;
		if (hit != null && hit.getType() == HitResult.Type.ENTITY) {
			return ((EntityHitResult) hit).getEntity();
		}
		return null;
	}

	static int findAxe(PlayerInventory inv) {
		for (int i = 0; i < 9; i++) {
			ItemStack s = inv.getStack(i);
			if (!s.isEmpty() && s.isIn(ItemTags.AXES)) {
				return i;
			}
		}
		return -1;
	}

	static int findMace(PlayerInventory inv, int filter) {
		for (int i = 0; i < 9; i++) {
			ItemStack s = inv.getStack(i);
			if (isMace(s) && maceMatchesFilter(s, filter)) {
				return i;
			}
		}
		return -1;
	}

	static boolean isMace(ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		try {
			if (stack.isOf(Items.MACE)) {
				return true;
			}
		} catch (Throwable ignored) {
		}
		Identifier id = Registries.ITEM.getId(stack.getItem());
		return id != null && "mace".equals(id.getPath());
	}

	static boolean maceMatchesFilter(ItemStack stack, int filter) {
		if (filter <= 0) {
			return true;
		}
		boolean breach = hasEnchant(stack, "breach");
		boolean density = hasEnchant(stack, "density");
		return switch (filter) {
			case 1 -> breach;
			case 2 -> density;
			case 3 -> breach || density;
			case 4 -> breach && density;
			default -> true;
		};
	}

	private static boolean hasEnchant(ItemStack stack, String path) {
		ItemEnchantmentsComponent enchants = stack.getOrDefault(
				DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT);
		for (var entry : enchants.getEnchantmentEntries()) {
			if (entry.getIntValue() <= 0) {
				continue;
			}
			RegistryEntry<Enchantment> ench = entry.getKey();
			var key = ench.getKey();
			if (key.isPresent()) {
				String p = key.get().getValue().getPath();
				if (p.equals(path) || p.contains(path)) {
					return true;
				}
			}
			if (ench.toString().toLowerCase().contains(path)) {
				return true;
			}
		}
		return false;
	}
}
