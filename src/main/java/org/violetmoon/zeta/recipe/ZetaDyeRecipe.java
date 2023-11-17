package org.violetmoon.zeta.recipe;

import java.util.List;
import java.util.function.Supplier;

import org.violetmoon.zeta.registry.DyeablesRegistry;

import com.google.common.collect.Lists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleRecipeSerializer;
import net.minecraft.world.level.Level;

// copy of ArmorDyeRecipe
public class ZetaDyeRecipe extends CustomRecipe {
	protected final DyeablesRegistry dyeablesRegistry;
	protected final RecipeSerializer<?> serializer;

	public ZetaDyeRecipe(ResourceLocation id, DyeablesRegistry dyeablesRegistry) {
		super(id);
		this.dyeablesRegistry = dyeablesRegistry;
		// We need to plug the serializer into itself, so that when a fresh copy of this ZetaDyeRecipe is constructed
		// it will have the exact same serializer instance already registered in the recipe serializer registry.
		// Yeah, it's weird i know. Don't try this at home, it's like searching Google into Google.
		// Btw, the serializer can't be made `static` because constructing this recipe type requires a DyeablesRegistry.
		this.serializer = new SimpleRecipeSerializer<>(id_ -> new ZetaDyeRecipe(id_, dyeablesRegistry, this::getSerializer));
	}

	protected ZetaDyeRecipe(ResourceLocation id, DyeablesRegistry dyeablesRegistry, Supplier<RecipeSerializer<?>> loopCloser) {
		super(id);
		this.dyeablesRegistry = dyeablesRegistry;
		this.serializer = loopCloser.get();
	}

	@Override
	public boolean matches(CraftingContainer p_43769_, Level p_43770_) {
		ItemStack itemstack = ItemStack.EMPTY;
		List<ItemStack> list = Lists.newArrayList();

		for(int i = 0; i < p_43769_.getContainerSize(); ++i) {
			ItemStack itemstack1 = p_43769_.getItem(i);
			if (!itemstack1.isEmpty()) {
				if (dyeablesRegistry.isDyeable(itemstack1)) { // <- changed
					if (!itemstack.isEmpty()) {
						return false;
					}

					itemstack = itemstack1;
				} else {
					if (!(itemstack1.getItem() instanceof DyeItem)) {
						return false;
					}

					list.add(itemstack1);
				}
			}
		}

		return !itemstack.isEmpty() && !list.isEmpty();
	}

	@Override
	public ItemStack assemble(CraftingContainer p_43767_) {
		List<DyeItem> list = Lists.newArrayList();
		ItemStack itemstack = ItemStack.EMPTY;

		for(int i = 0; i < p_43767_.getContainerSize(); ++i) {
			ItemStack itemstack1 = p_43767_.getItem(i);
			if (!itemstack1.isEmpty()) {
				Item item = itemstack1.getItem();
				if (dyeablesRegistry.isDyeable(itemstack1)) { // <- changed
					if (!itemstack.isEmpty()) {
						return ItemStack.EMPTY;
					}

					itemstack = itemstack1.copy();
				} else {
					if (!(item instanceof DyeItem)) {
						return ItemStack.EMPTY;
					}

					list.add((DyeItem)item);
				}
			}
		}

		return !itemstack.isEmpty() && !list.isEmpty() ?
			dyeablesRegistry.dyeItem(itemstack, list) : // <- changed
			ItemStack.EMPTY;
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return width * height >= 2;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return serializer;
	}
}