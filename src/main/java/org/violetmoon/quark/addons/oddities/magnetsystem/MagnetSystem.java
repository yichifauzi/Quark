package org.violetmoon.quark.addons.oddities.magnetsystem;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import org.violetmoon.quark.addons.oddities.block.be.MagnetBlockEntity;
import org.violetmoon.quark.addons.oddities.module.MagnetsModule;
import org.violetmoon.quark.api.IMagnetMoveAction;
import org.violetmoon.quark.api.IMagnetTracker;
import org.violetmoon.quark.api.QuarkCapabilities;
import org.violetmoon.quark.base.Quark;
import org.violetmoon.zeta.util.RegistryUtil;
import org.violetmoon.zeta.util.handler.RecipeCrawlHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

public class MagnetSystem {

	private static final HashSet<Block> magnetizableBlocks = new HashSet<>();
	private static final HashSet<Item> magnetizableItems = new HashSet<>();

	private static final HashMap<Block, IMagnetMoveAction> BLOCK_MOVE_ACTIONS = new HashMap<>();

	static {
		DefaultMoveActions.addActions(BLOCK_MOVE_ACTIONS);
	}

	public static IMagnetMoveAction getMoveAction(Block block) {
		if (block instanceof IMagnetMoveAction ma)return ma;
		return BLOCK_MOVE_ACTIONS.get(block);
	}

	public static @Nullable IMagnetTracker getTracker(Level level) {
		return Quark.ZETA.capabilityManager.getCapability(QuarkCapabilities.MAGNET_TRACKER_CAPABILITY, level);
	}

	public static void tick(boolean start, Level level) {
		IMagnetTracker tracker = getTracker(level);
		if(tracker == null)
			return;

		if(!start) {
			for(BlockPos pos : tracker.getTrackedPositions())
				tracker.actOnForces(pos);
		}
		tracker.clear();
	}

	public static void onRecipeReset() {
		magnetizableBlocks.clear();
	}

	public static void onDigest() {
		//TODO(Zeta): Eschew the built-in whitelist/blacklist system... (https://github.com/VazkiiMods/Zeta/issues/2)
		RecipeCrawlHandler.recursivelyFindCraftedItemsFromStrings(MagnetsModule.magneticDerivationList, Collections.emptyList(), Collections.emptyList(), i -> {
			if(i instanceof BlockItem bi)
				magnetizableBlocks.add(bi.getBlock());
			if(i instanceof ArmorItem) //this could use a better check... And whitelist
				magnetizableItems.add(i);
		});

		//...in favor of manual fixup
		List<Block> magneticWhitelist = RegistryUtil.massRegistryGet(MagnetsModule.magneticWhitelist, BuiltInRegistries.BLOCK);
		List<Block> magneticBlacklist = RegistryUtil.massRegistryGet(MagnetsModule.magneticBlacklist, BuiltInRegistries.BLOCK);

		magnetizableBlocks.addAll(magneticWhitelist);
		magneticBlacklist.forEach(magnetizableBlocks::remove);
	}

	public static void applyForce(Level world, BlockPos pos, int magnitude, boolean pushing, Direction dir, int distance, BlockPos origin) {
		IMagnetTracker tracker = getTracker(world);
		if(tracker != null)
			tracker.applyForce(pos, magnitude, pushing, dir, distance, origin);
	}

	public static PushReaction getPushAction(MagnetBlockEntity magnet, BlockPos pos, BlockState state, Direction moveDir) {
		Level world = magnet.getLevel();
		if(world != null && canBlockBeMagneticallyMoved(state, pos, world, moveDir, magnet)) {
			BlockPos targetLocation = pos.relative(moveDir);
			BlockState stateAtTarget = world.getBlockState(targetLocation);
			if(stateAtTarget.isAir())
				return PushReaction.IGNORE;
			else if(stateAtTarget.getPistonPushReaction() == PushReaction.DESTROY)
				return PushReaction.DESTROY;
		}

		return PushReaction.BLOCK;
	}

	public static boolean isItemMagnetic(Item item) {
		return magnetizableItems.contains(item);
	}

	// Just checks if its magnetic. Not if it can be moved
	public static boolean isBlockMagnetic(BlockState state) {
		Block block = state.getBlock();
		if (block == MagnetsModule.magnet) return false;
		return (magnetizableBlocks.contains(block) || BLOCK_MOVE_ACTIONS.containsKey(block));
	}

	public static boolean canBlockBeMagneticallyMoved(BlockState state, BlockPos pos, Level level, Direction moveDir, BlockEntity magnet) {
		Block block = state.getBlock();

		if (block == Blocks.PISTON || block == Blocks.STICKY_PISTON) {
			if (state.getValue(PistonBaseBlock.EXTENDED))
				return false;
		}
		if (block == MagnetsModule.magnet) return false;

		IMagnetMoveAction action = getMoveAction(block);
		if (action != null) return action.canMagnetMove(level, pos, moveDir, state, magnet);
		return (magnetizableBlocks.contains(block));
	}
}
