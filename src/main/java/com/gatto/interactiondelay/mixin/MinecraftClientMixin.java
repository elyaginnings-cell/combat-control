package com.gatto.interactiondelay.mixin;

import com.gatto.interactiondelay.combat.AxeSwitchHandler;
import com.gatto.interactiondelay.combat.AnchorAssist;
import com.gatto.interactiondelay.combat.SafeHotbar;
import com.gatto.interactiondelay.combat.CombatInput;
import com.gatto.interactiondelay.combat.AnchorPlaceLock;
import com.gatto.interactiondelay.combat.LungeSwapHandler;
import com.gatto.interactiondelay.combat.SwordAssistHandler;
import com.gatto.interactiondelay.combat.AutoHitHandler;
import com.gatto.interactiondelay.combat.StunSlamHandler;
import com.gatto.interactiondelay.config.InteractionDelayConfig;
import com.gatto.interactiondelay.config.ConfigScreens;
import com.gatto.interactiondelay.interaction.InteractionCategory;
import com.gatto.interactiondelay.interaction.InteractionManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Shadow
	private int itemUseCooldown;

	@Unique
	private boolean interactiondelay$configKeyWasDown;

	@Unique
	private int interactiondelay$lastSelectedSlot = -1;

	@Inject(method = "tick", at = @At("HEAD"))
	private void interactiondelay$onTickHead(CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		InteractionDelayConfig config = com.gatto.interactiondelay.InteractionDelay.getConfig();

		long handle = client.getWindow().getHandle();
		boolean down = GLFW.glfwGetKey(handle, config.openConfigKey) == GLFW.GLFW_PRESS;
		if (down && !this.interactiondelay$configKeyWasDown) {
			Screen current = client.currentScreen;
			if (!(current instanceof ChatScreen) && client.player != null) {
				if (ConfigScreens.isConfigScreen(current)) {
					client.setScreen(null);
				} else {
					client.setScreen(ConfigScreens.open(current));
				}
			}
		}
		this.interactiondelay$configKeyWasDown = down;

		AxeSwitchHandler.tick(client);
		LungeSwapHandler.tick(client);
		SwordAssistHandler.tick(client);
		AutoHitHandler.tick(client);
		StunSlamHandler.tick(client);
		CombatInput.tick(client);
		SafeHotbar.tick(client);
		AnchorAssist.tick(client);
		AnchorPlaceLock.tick(client);
		if (AnchorAssist.clearCooldownNext) {
			this.itemUseCooldown = 0;
			AnchorAssist.clearCooldownNext = false;
		}

		if (client.player == null) {
			this.interactiondelay$lastSelectedSlot = -1;
			return;
		}

		int selected = client.player.getInventory().getSelectedSlot();

		if (config.resetCooldownOnSwap
				&& this.interactiondelay$lastSelectedSlot != -1
				&& selected != this.interactiondelay$lastSelectedSlot) {
			this.itemUseCooldown = 0;
		}
		this.interactiondelay$lastSelectedSlot = selected;

		interactiondelay$applyCooldownClamp(client);
	}

	@Inject(method = "tick", at = @At("RETURN"))
	private void interactiondelay$onTickTail(CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		if (client.player == null) {
			return;
		}
		interactiondelay$applyCooldownClamp(client);
	}

	@Inject(method = "doItemUse", at = @At("HEAD"))
	private void interactiondelay$beforeItemUse(CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		if (AnchorAssist.shouldBlockExtraAnchorPlace(client)) {
			this.itemUseCooldown = Math.max(this.itemUseCooldown, 4);
		}
		AnchorAssist.beforeItemUse(client);
	}

	@Inject(method = "doItemUse", at = @At("RETURN"))
	private void interactiondelay$afterItemUse(CallbackInfo ci) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		if (client.player == null) {
			return;
		}
		AnchorAssist.afterItemUse(client);
		AnchorPlaceLock.onItemUse(client);
		interactiondelay$applyCooldownClamp(client);
	}

	@Unique
	private void interactiondelay$applyCooldownClamp(MinecraftClient client) {
		if (this.itemUseCooldown <= 0) {
			return;
		}
		if (!com.gatto.interactiondelay.InteractionDelay.getConfig().modEnabled) {
			return;
		}

		InteractionCategory category = InteractionManager.resolveCategory(client);
		int delay = InteractionManager.resolveDelay(client);

		boolean anchorKit = category == InteractionCategory.RESPAWN_ANCHOR_PLACE
				|| category == InteractionCategory.GLOWSTONE_CHARGE
				|| category == InteractionCategory.ANCHOR_EXPLODE;

		if (anchorKit && AnchorAssist.isSafeMode()) {
			delay = 2;
		}

		if (InteractionDelayConfig.isVanilla(delay) && !anchorKit) {
			return;
		}
		if (InteractionDelayConfig.isVanilla(delay)) {
			return;
		}

		if (this.itemUseCooldown > delay) {
			this.itemUseCooldown = delay;
		}
	}

	@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
	private void interactiondelay$axeSwitchBefore(CallbackInfoReturnable<Boolean> cir) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		if (SwordAssistHandler.shouldCancelAttack(client)) {
			cir.setReturnValue(false);
			return;
		}
		SwordAssistHandler.beforeAttack(client);
		if (StunSlamHandler.beforeAttack(client)) {
			if (StunSlamHandler.shouldCancelAttack()) {
				cir.setReturnValue(false);
				return;
			}
		} else {
			AxeSwitchHandler.beforeAttack(client);
		}
	}

	@Inject(method = "doAttack", at = @At("RETURN"))
	private void interactiondelay$axeSwitchAfter(CallbackInfoReturnable<Boolean> cir) {
		MinecraftClient client = (MinecraftClient) (Object) this;
		StunSlamHandler.afterAttack(client);
		AxeSwitchHandler.afterAttack(client);
		SwordAssistHandler.afterAttack(client);
	}
}
