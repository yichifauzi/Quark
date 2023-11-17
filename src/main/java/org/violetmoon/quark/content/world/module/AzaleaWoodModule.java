package org.violetmoon.quark.content.world.module;

import net.minecraft.world.level.material.MapColor;
import org.violetmoon.quark.base.handler.WoodSetHandler;
import org.violetmoon.quark.base.handler.WoodSetHandler.WoodSet;
import org.violetmoon.zeta.event.bus.LoadEvent;
import org.violetmoon.zeta.event.load.ZRegister;
import org.violetmoon.zeta.module.ZetaLoadModule;
import org.violetmoon.zeta.module.ZetaModule;

import net.minecraft.data.worldgen.features.TreeFeatures;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

@ZetaLoadModule(category = "world", antiOverlap = { "caverns_and_chasms" })
public class AzaleaWoodModule extends ZetaModule {

	public static WoodSet woodSet;

	@LoadEvent
	public final void register(ZRegister event) {
		woodSet = WoodSetHandler.addWoodSet(event, this, "azalea", MapColor.COLOR_LIGHT_GREEN, MapColor.COLOR_BROWN, true);
		//ugly I know but config is fired before this now
		//TODO: not actually fired by the config lol
		enabledStatusChanged(true, this.enabled, this.enabled);
	}

	public void enabledStatusChanged(boolean firstLoad, boolean oldStatus, boolean newStatus) {
		ConfiguredFeature<TreeConfiguration, ?> configured = null;
		try {
			configured = TreeFeatures.AZALEA_TREE.value();
		} catch(IllegalStateException e) {
			e.printStackTrace();
		}

		if(configured != null) {
			TreeConfiguration config = configured.config();

			if(newStatus && woodSet != null)
				config.trunkProvider = BlockStateProvider.simple(woodSet.log);
			else if(!firstLoad)
				config.trunkProvider = BlockStateProvider.simple(Blocks.OAK_LOG);
		}
	}

}