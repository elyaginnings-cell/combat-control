package com.gatto.interactiondelay.interaction;

import com.gatto.interactiondelay.InteractionDelay;
import com.gatto.interactiondelay.config.InteractionDelayConfig;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

public final class InteractionManager {
	private InteractionManager() {
	}

	public static int resolveDelay(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled) {
			return InteractionDelayConfig.VANILLA;
		}
		return switch (resolveCategory(client)) {
			case BLOCK_PLACEMENT -> config.blockPlacementDelay;
			case OBSIDIAN_PLACE -> config.obsidianPlaceDelay;
			case ENDER_PEARL -> config.enderPearlDelay;
			case WIND_CHARGE -> config.windChargeDelay;
			case END_CRYSTAL -> config.endCrystalDelay;
			case FIREWORK -> config.fireworkDelay;
			case CHORUS_FRUIT -> config.chorusFruitDelay;
			case XP_BOTTLE -> config.xpBottleDelay;
			case SPLASH_POTION -> config.splashPotionDelay;
			case RESPAWN_ANCHOR_PLACE -> config.respawnAnchorPlaceDelay;
			case GLOWSTONE_CHARGE -> config.glowstoneChargeDelay;
			case ANCHOR_EXPLODE -> config.anchorExplodeDelay;
			case ARMOR -> config.armorDelay;
			case ELYTRA -> config.elytraDelay;
			case OTHER_ITEM -> config.otherItemDelay;
			case BLOCK_INTERACTION -> config.blockInteractionDelay;
		};
	}

	public static InteractionCategory resolveCategory(MinecraftClient client) {
		if (client.player == null) {
			return InteractionCategory.OTHER_ITEM;
		}

		ItemStack main = client.player.getMainHandStack();
		ItemStack off = client.player.getOffHandStack();
		ItemStack active = main.isEmpty() ? off : main;
		Item item = active.getItem();

		boolean lookingAtAnchor = isLookingAtRespawnAnchor(client);

		if (item == Items.GLOWSTONE && lookingAtAnchor) {
			return InteractionCategory.GLOWSTONE_CHARGE;
		}
		if (item == Items.RESPAWN_ANCHOR) {
			return InteractionCategory.RESPAWN_ANCHOR_PLACE;
		}
		if (lookingAtAnchor) {
			return InteractionCategory.ANCHOR_EXPLODE;
		}
		if (item == Items.ENDER_PEARL) return InteractionCategory.ENDER_PEARL;
		if (item == Items.WIND_CHARGE) return InteractionCategory.WIND_CHARGE;
		if (item == Items.END_CRYSTAL) return InteractionCategory.END_CRYSTAL;
		if (item == Items.FIREWORK_ROCKET) return InteractionCategory.FIREWORK;
		if (item == Items.CHORUS_FRUIT) return InteractionCategory.CHORUS_FRUIT;
		if (item == Items.EXPERIENCE_BOTTLE) return InteractionCategory.XP_BOTTLE;
		if (item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
			return InteractionCategory.SPLASH_POTION;
		}
		if (item == Items.ELYTRA) return InteractionCategory.ELYTRA;
		if (isArmorItem(active)) return InteractionCategory.ARMOR;
		if (item == Items.OBSIDIAN) return InteractionCategory.OBSIDIAN_PLACE;
		if (item instanceof BlockItem) return InteractionCategory.BLOCK_PLACEMENT;

		HitResult hit = client.crosshairTarget;
		if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
			if (active.isEmpty() || !(item instanceof BlockItem)) {
				return InteractionCategory.BLOCK_INTERACTION;
			}
		}
		return InteractionCategory.OTHER_ITEM;
	}

	private static boolean isLookingAtRespawnAnchor(MinecraftClient client) {
		HitResult hit = client.crosshairTarget;
		if (hit == null || hit.getType() != HitResult.Type.BLOCK || client.world == null) {
			return false;
		}
		BlockPos pos = ((BlockHitResult) hit).getBlockPos();
		return client.world.getBlockState(pos).isOf(Blocks.RESPAWN_ANCHOR);
	}

	private static boolean isArmorItem(ItemStack stack) {
		if (stack.isEmpty() || stack.isOf(Items.ELYTRA)) return false;
		var equippable = stack.get(DataComponentTypes.EQUIPPABLE);
		if (equippable == null) return false;
		EquipmentSlot slot = equippable.slot();
		return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
				|| slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
	}
}
