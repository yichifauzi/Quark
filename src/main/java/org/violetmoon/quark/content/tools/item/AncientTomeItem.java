package org.violetmoon.quark.content.tools.item;

import java.util.List;

import javax.annotation.Nonnull;

import net.minecraft.ChatFormatting;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.violetmoon.quark.content.experimental.module.EnchantmentsBegoneModule;
import org.violetmoon.quark.content.tools.module.AncientTomesModule;
import org.violetmoon.zeta.item.ZetaItem;
import org.violetmoon.zeta.module.ZetaModule;

public class AncientTomeItem extends ZetaItem {

	public AncientTomeItem(ZetaModule module) {
		super("ancient_tome", module,
				new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	}

	@Override
	public boolean isEnchantable(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean isFoil(@Nonnull ItemStack stack) {
		return true;
	}

	@Override
	public boolean canApplyAtEnchantingTableZeta(ItemStack stack, Enchantment enchantment) {
		return false;
	}

	public static ItemStack getEnchantedItemStack(Enchantment ench) {
		ItemStack newStack = new ItemStack(AncientTomesModule.ancient_tome);
		EnchantedBookItem.addEnchantment(newStack, new EnchantmentInstance(ench, ench.getMaxLevel()));
		return newStack;
	}

	@Override
	public void fillItemCategory(@Nonnull CreativeModeTab group, @Nonnull NonNullList<ItemStack> items) {
		if (isEnabled() || group == CreativeModeTab.TAB_SEARCH) {
			if (group == CreativeModeTab.TAB_SEARCH || group.getEnchantmentCategories().length != 0) {
				BuiltInRegistries.ENCHANTMENT.forEach(ench -> {
					if (!EnchantmentsBegoneModule.shouldBegone(ench) && (!AncientTomesModule.sanityCheck || ench.getMaxLevel() != 1)) {
						if (!AncientTomesModule.isInitialized() || AncientTomesModule.validEnchants.contains(ench)) {
							if (group == CreativeModeTab.TAB_SEARCH || group.hasEnchantmentCategory(ench.category)) {
								items.add(getEnchantedItemStack(ench));
							}
						}
					}
				});
			}
		}
	}

	public static Component getFullTooltipText(Enchantment ench) {
		return Component.translatable("quark.misc.ancient_tome_tooltip", Component.translatable(ench.getDescriptionId()), Component.translatable("enchantment.level." + (ench.getMaxLevel() + 1))).withStyle(ChatFormatting.GRAY);
	}

	@Override
	public void appendHoverText(@Nonnull ItemStack stack, Level worldIn, @Nonnull List<Component> tooltip, @Nonnull TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);

		Enchantment ench = AncientTomesModule.getTomeEnchantment(stack);
		if(ench != null)
			tooltip.add(getFullTooltipText(ench));
		else
			tooltip.add(Component.translatable("quark.misc.ancient_tome_tooltip_any").withStyle(ChatFormatting.GRAY));

		if(AncientTomesModule.curseGear){
			tooltip.add(Component.translatable("quark.misc.ancient_tome_tooltip_curse").withStyle(ChatFormatting.RED));
		}
	}

}