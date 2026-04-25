package com.github.aeddddd.ae2enhanced.integration.jei;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import com.github.aeddddd.ae2enhanced.crafting.BlackHoleRecipe;
import com.github.aeddddd.ae2enhanced.crafting.BlackHoleRecipeRegistry;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 插件：注册黑洞合成配方类别与配方显示。
 */
@JEIPlugin
public class AE2EnhancedJEIPlugin implements IModPlugin {

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        registry.addRecipeCategories(new BlackHoleRecipeCategory(registry.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void register(IModRegistry registry) {
        IIngredientRegistry ingredientRegistry = registry.getIngredientRegistry();

        // 必须将 BlackHoleRecipe 包装为 BlackHoleRecipeWrapper，与 IRecipeCategory 的泛型匹配
        List<BlackHoleRecipeWrapper> wrappers = new ArrayList<>();
        for (BlackHoleRecipe recipe : BlackHoleRecipeRegistry.getRecipes()) {
            wrappers.add(new BlackHoleRecipeWrapper(recipe));
        }
        registry.addRecipes(wrappers, BlackHoleRecipeCategory.UID);
        AE2Enhanced.LOGGER.info("JEI 集成已注册，黑洞合成配方数量: {}", wrappers.size());
    }
}
