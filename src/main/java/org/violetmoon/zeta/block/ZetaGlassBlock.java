package org.violetmoon.zeta.block;

import javax.annotation.Nonnull;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.violetmoon.zeta.module.ZetaModule;
import org.violetmoon.zeta.registry.RenderLayerRegistry;

/**
 * @author WireSegal
 * Created at 12:46 PM on 8/24/19.
 */
public class ZetaGlassBlock extends ZetaBlock {

	public ZetaGlassBlock(String regname, ZetaModule module, CreativeModeTab creativeTab, boolean translucent, Properties properties) {
		super(regname, module, creativeTab, properties
				.noOcclusion()
				.isValidSpawn((state, world, pos, entityType) -> false)
				.isRedstoneConductor((state, world, pos) -> false)
				.isSuffocating((state, world, pos) -> false)
				.isViewBlocking((state, world, pos) -> false));

		module.zeta.renderLayerRegistry.put(this, translucent ? RenderLayerRegistry.Layer.TRANSLUCENT : RenderLayerRegistry.Layer.CUTOUT);
	}

	@Override
	public boolean skipRendering(@Nonnull BlockState state, BlockState adjacentBlockState, @Nonnull Direction side) {
		return adjacentBlockState.is(this) || super.skipRendering(state, adjacentBlockState, side);
	}

	@Override
	@Nonnull
	public VoxelShape getVisualShape(@Nonnull BlockState state, @Nonnull BlockGetter worldIn, @Nonnull BlockPos pos, @Nonnull CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	public float getShadeBrightness(@Nonnull BlockState state, @Nonnull BlockGetter worldIn, @Nonnull BlockPos pos) {
		return 1.0F;
	}

	@Override
	public boolean propagatesSkylightDown(@Nonnull BlockState state, @Nonnull BlockGetter reader, @Nonnull BlockPos pos) {
		return true;
	}

	@Override
	public boolean shouldDisplayFluidOverlay(BlockState state, BlockAndTintGetter world, BlockPos pos, FluidState fluidState) {
		return true;
	}

}