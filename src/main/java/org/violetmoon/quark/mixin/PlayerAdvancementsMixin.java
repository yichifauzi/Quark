package org.violetmoon.quark.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.violetmoon.quark.base.handler.GeneralConfig;

import net.minecraft.server.PlayerAdvancements;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {

	@ModifyConstant(method = "shouldBeVisible", constant = @Constant(intValue = 2))
	public int visibility(int curr) {
		return GeneralConfig.advancementVisibilityDepth;
	}
	
}