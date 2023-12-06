package org.violetmoon.quark.integration.lootr;

import java.util.function.BooleanSupplier;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;
import org.violetmoon.zeta.module.ZetaModule;

/**
 * @author WireSegal
 * Created at 11:40 AM on 7/3/23.
 */
public interface ILootrIntegration {

	default BlockEntityType<? extends ChestBlockEntity> chestTE() {
		return null;
	}

	default BlockEntityType<? extends ChestBlockEntity> trappedChestTE() {
		return null;
	}

	default void makeChestBlocks(ZetaModule module, String name, Block base, BooleanSupplier condition, Block superRegularChest, Block superTrappedChest) {
		// NO-OP
	}

	@Nullable
	default Block lootrVariant(Block base) {
		return null;
	}

	default void postRegister() {
		// NO-OP
	}

	@OnlyIn(Dist.CLIENT)
	default void clientSetup() {
		// NO-OP
	}

	class Dummy implements ILootrIntegration { }

}
