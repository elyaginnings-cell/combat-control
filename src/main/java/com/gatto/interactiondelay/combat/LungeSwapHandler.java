package com.gatto.interactiondelay.combat;

import com.gatto.interactiondelay.InteractionDelay;
import com.gatto.interactiondelay.config.InteractionDelayConfig;
import com.gatto.interactiondelay.mixin.LivingEntityAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.tag.ItemTags;

/**
 * Attack-cooldown assists for spear (lunge), mace, and optional sword.
 */
public final class LungeSwapHandler {
	private LungeSwapHandler() {
	}

	public static void tick(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || client.player == null) {
			return;
		}

		PlayerEntity player = client.player;
		ItemStack main = player.getMainHandStack();
		int minDelay = -1;

		if (config.lungeSwapEnabled && isSpear(main)) {
			minDelay = config.lungeSwapDelay;
		} else if (config.maceSwapEnabled && main.isOf(Items.MACE)) {
			minDelay = config.maceSwapDelay;
		} else if (config.swordCooldownAssist && isSword(main)) {
			minDelay = config.swordCooldownDelay;
		}

		if (minDelay < 0) {
			return;
		}

		LivingEntityAccessor accessor = (LivingEntityAccessor) player;
		int sinceAttack = accessor.interactiondelay$getTicksSinceLastAttack();
		minDelay = Math.max(0, Math.min(20, minDelay));
		if (sinceAttack >= minDelay && sinceAttack < 1000) {
			accessor.interactiondelay$setTicksSinceLastAttack(1000);
		}
	}

	public static boolean isSpear(ItemStack stack) {
		return !stack.isEmpty() && stack.isIn(ItemTags.SPEARS);
	}

	private static boolean isSword(ItemStack stack) {
		return !stack.isEmpty() && stack.isIn(ItemTags.SWORDS);
	}
}
