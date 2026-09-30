package com.gatto.interactiondelay.mixin;

import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
	@Accessor("ticksSinceLastAttack")
	int interactiondelay$getTicksSinceLastAttack();

	@Accessor("ticksSinceLastAttack")
	void interactiondelay$setTicksSinceLastAttack(int ticks);
}
