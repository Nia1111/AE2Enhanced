package com.github.aeddddd.ae2enhanced.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 黑洞合成配方注册表。
 */
public class BlackHoleRecipeRegistry {

    private static final List<BlackHoleRecipe> RECIPES = new ArrayList<>();

    public static void register(BlackHoleRecipe recipe) {
        RECIPES.add(recipe);
    }

    /**
     * 在 found 中寻找第一个匹配的配方。
     */
    public static BlackHoleRecipe findMatching(Map<String, Integer> found) {
        for (BlackHoleRecipe recipe : RECIPES) {
            if (recipe.matches(found)) {
                return recipe;
            }
        }
        return null;
    }

    public static boolean removeById(String id) {
        return RECIPES.removeIf(r -> r.getId().equals(id));
    }

    public static List<BlackHoleRecipe> getRecipes() {
        return new ArrayList<>(RECIPES);
    }
}
