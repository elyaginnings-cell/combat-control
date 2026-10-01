package com.gatto.interactiondelay.combat;

import com.gatto.interactiondelay.InteractionDelay;
import com.gatto.interactiondelay.config.InteractionDelayConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * 2.9.0-style sequence with packet-safe timing:
 * never zero use-cooldown, never cancel uses, min 2 ticks before/between hotbar swaps.
 */
public final class AnchorAssist {
	private enum Phase {
		IDLE,
		WAIT_PLACED,
		WAIT_CHARGED,
		WAIT_GONE,
		RESTORE
	}

	private static final int MAX_AGE = 80;
	private static final int MIN_PLACE_GAP = 1;

	private static Phase phase = Phase.IDLE;
	private static int age = 0;
	private static int timer = 0;
	private static int originalSlot = -1;
	private static BlockPos anchorPos = null;
	private static BlockPos predictedPos = null;
	private static int chargesAtSwap = -1;
	private static int minOnGs = 1;
	private static int minOnTotem = 1;

	public static boolean clearCooldownNext = false;
	public static int forceUseCooldown = 0;

	private AnchorAssist() {
	}

	public static boolean shouldBlockExtraAnchorPlace(MinecraftClient client) {
		if (phase == Phase.IDLE || client.player == null || isSafeMode()) {
			return false;
		}
		return client.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR);
	}

	public static boolean isBusy() {
		return phase != Phase.IDLE;
	}

	public static boolean isSequenceRunning() {
		return phase != Phase.IDLE;
	}

	public static boolean isSafeMode() {
		return InteractionDelay.getConfig().anchorComboSafe;
	}

	public static void tick(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || !config.anchorComboEnabled || config.anchorComboSafe) {
			if (phase != Phase.IDLE) {
				reset();
			}
			return;
		}
		if (client.player == null || client.world == null) {
			if (phase != Phase.IDLE) {
				reset();
			}
			return;
		}
		if (phase == Phase.IDLE) {
			return;
		}

		age++;
		if (age > MAX_AGE) {
			finish(client);
			return;
		}

		timer++;
		int placeMin = Math.max(MIN_PLACE_GAP, config.anchorComboPlaceTicks);
		int chargeMin = Math.max(1, config.anchorComboChargeTicks);
		int stepMin = Math.max(1, config.anchorComboStepTicks);

		switch (phase) {
			case WAIT_PLACED -> {
				if (!locateAnchor(client)) {
					if (timer > placeMin + 25) {
						finish(client);
					}
					return;
				}
				if (timer >= placeMin) {
					int gs = findItem(client.player.getInventory(), Items.GLOWSTONE);
					if (gs >= 0) {
						select(client, gs);
						chargesAtSwap = getCharges(client, anchorPos);
						if (chargesAtSwap < 0) {
							chargesAtSwap = 0;
						}
						minOnGs = Math.max(1, chargeMin);
						go(Phase.WAIT_CHARGED);
					} else {
						finish(client);
					}
				}
			}
			case WAIT_CHARGED -> {
				if (anchorPos == null || !client.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) {
					if (!locateAnchor(client)) {
						finish(client);
						return;
					}
				}
				int charges = getCharges(client, anchorPos);
				boolean charged = charges > chargesAtSwap;
				if (timer >= minOnGs && charged) {
					int totem = findItem(client.player.getInventory(), Items.TOTEM_OF_UNDYING);
					select(client, totem >= 0 ? totem : 8);
					minOnTotem = Math.max(1, stepMin);
					go(Phase.WAIT_GONE);
				} else if (timer > minOnGs + 40) {
					finish(client);
				}
			}
			case WAIT_GONE -> {
				boolean gone = anchorPos == null
						|| !client.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR);
				if (!gone) {
					gone = findNearestAnchor(client) == null;
				} else {
					BlockPos near = findNearestAnchor(client);
					if (near != null && near.equals(anchorPos)) {
						gone = false;
					} else if (near != null && predictedPos != null && near.equals(predictedPos)) {
						gone = false;
						anchorPos = near;
					} else if (near == null) {
						gone = true;
					}
				}
				if (timer >= minOnTotem && gone) {
					go(Phase.RESTORE);
				} else if (timer > minOnTotem + 50) {
					finish(client);
				}
			}
			case RESTORE -> {
				if (originalSlot >= 0 && originalSlot < 9) {
					select(client, originalSlot);
				} else {
					int a = findItem(client.player.getInventory(), Items.RESPAWN_ANCHOR);
					if (a >= 0) {
						select(client, a);
					}
				}
				reset();
			}
			default -> reset();
		}
	}

	public static void afterItemUse(MinecraftClient client) {
		InteractionDelayConfig config = InteractionDelay.getConfig();
		if (!config.modEnabled || !config.anchorComboEnabled || config.anchorComboSafe) {
			return;
		}
		if (client.player == null) {
			return;
		}
		if (phase != Phase.IDLE) {
			return;
		}
		if (!client.player.getMainHandStack().isOf(Items.RESPAWN_ANCHOR)) {
			return;
		}
		if (!(client.crosshairTarget instanceof BlockHitResult bhr)
				|| bhr.getType() != HitResult.Type.BLOCK) {
			return;
		}

		originalSlot = client.player.getInventory().getSelectedSlot();
		predictedPos = bhr.getBlockPos().offset(bhr.getSide());
		anchorPos = predictedPos;
		age = 0;
		chargesAtSwap = -1;
		go(Phase.WAIT_PLACED);
		if (client.world != null
				&& client.world.getBlockState(predictedPos).isOf(Blocks.RESPAWN_ANCHOR)) {
			timer = Math.max(0, Math.max(MIN_PLACE_GAP, config.anchorComboPlaceTicks) - 1);
		}
	}

	public static void beforeItemUse(MinecraftClient client) {
	}

	private static void go(Phase next) {
		phase = next;
		timer = 0;
	}

	private static void finish(MinecraftClient client) {
		if (client != null && client.player != null && originalSlot >= 0 && originalSlot < 9) {
			select(client, originalSlot);
		}
		reset();
	}

	private static void reset() {
		phase = Phase.IDLE;
		age = 0;
		timer = 0;
		originalSlot = -1;
		anchorPos = null;
		predictedPos = null;
		chargesAtSwap = -1;
		minOnGs = 1;
		minOnTotem = 1;
		forceUseCooldown = 0;
		clearCooldownNext = false;
	}

	private static boolean locateAnchor(MinecraftClient client) {
		if (predictedPos != null && client.world.getBlockState(predictedPos).isOf(Blocks.RESPAWN_ANCHOR)) {
			anchorPos = predictedPos;
			return true;
		}
		if (anchorPos != null && client.world.getBlockState(anchorPos).isOf(Blocks.RESPAWN_ANCHOR)) {
			return true;
		}
		BlockPos near = findNearestAnchor(client);
		if (near != null) {
			anchorPos = near;
			return true;
		}
		return false;
	}

	private static BlockPos findNearestAnchor(MinecraftClient client) {
		BlockPos base = client.player.getBlockPos();
		BlockPos best = null;
		double bestDist = 36.0;
		for (int dx = -4; dx <= 4; dx++) {
			for (int dy = -3; dy <= 4; dy++) {
				for (int dz = -4; dz <= 4; dz++) {
					BlockPos p = base.add(dx, dy, dz);
					if (client.world.getBlockState(p).isOf(Blocks.RESPAWN_ANCHOR)) {
						double d = client.player.squaredDistanceTo(p.toCenterPos());
						if (d < bestDist) {
							bestDist = d;
							best = p;
						}
					}
				}
			}
		}
		return best;
	}

	private static int getCharges(MinecraftClient client, BlockPos pos) {
		if (pos == null) {
			return -1;
		}
		BlockState state = client.world.getBlockState(pos);
		if (!state.isOf(Blocks.RESPAWN_ANCHOR)) {
			return -1;
		}
		try {
			return state.get(RespawnAnchorBlock.CHARGES);
		} catch (Exception e) {
			return -1;
		}
	}

	private static int findItem(PlayerInventory inv, net.minecraft.item.Item item) {
		for (int i = 0; i < 9; i++) {
			if (inv.getStack(i).isOf(item)) {
				return i;
			}
		}
		return -1;
	}

	private static void select(MinecraftClient client, int slot) {
		SafeHotbar.select(client, slot);
	}
}
