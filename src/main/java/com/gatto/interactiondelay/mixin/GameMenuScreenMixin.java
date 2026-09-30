package com.gatto.interactiondelay.mixin;

import com.gatto.interactiondelay.config.ConfigScreens;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds an "Combat Control..." button to the pause menu.
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
	protected GameMenuScreenMixin(Text title) {
		super(title);
	}

	@Inject(method = "initWidgets", at = @At("RETURN"))
	private void interactiondelay$addConfigButton(CallbackInfo ci) {
		this.addDrawableChild(ButtonWidget.builder(
						Text.literal("Combat Control..."),
						btn -> {
							if (this.client != null) {
								this.client.setScreen(ConfigScreens.open(this));
							}
						})
				.dimensions(this.width / 2 - 102, this.height - 28, 204, 20)
				.build());
	}
}
