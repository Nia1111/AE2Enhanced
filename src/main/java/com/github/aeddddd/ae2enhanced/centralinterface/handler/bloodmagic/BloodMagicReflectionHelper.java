package com.github.aeddddd.ae2enhanced.centralinterface.handler.bloodmagic;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

/**
 * Blood Magic Handler 的反射辅助类.
 *
 * 缓存对 Blood Magic 全部使用到的类/方法/字段的反射引用,避免重复反射带来的性能损耗.
 * 所有缓存均在静态初始化块中完成,失败时记录警告但不应导致崩溃
 * (对应 handler 会在 isValidTarget 中回退到类型检查).
 *
 * 本类自身不引用任何 WayofTime.bloodmagic 类型,因此可以安全地被无条件加载.
 */
public class BloodMagicReflectionHelper {

    // ---- TileAlchemyTable ----
    public static final Class<?> CLASS_TILE_ALCHEMY_TABLE;
    public static final Method METHOD_ALCHEMY_TABLE_IS_SLAVE;
    public static final Method METHOD_ALCHEMY_TABLE_GET_BURN_TIME;
    public static final Method METHOD_ALCHEMY_TABLE_IS_INPUT_SLOT_ACCESSIBLE;

    // ---- TileSoulForge ----
    public static final Class<?> CLASS_TILE_SOUL_FORGE;
    public static final Field FIELD_SOUL_FORGE_BURN_TIME;

    // ---- TileAltar ----
    public static final Class<?> CLASS_TILE_ALTAR;
    public static final Method METHOD_ALTAR_START_CYCLE;
    public static final Method METHOD_ALTAR_IS_ACTIVE;
    public static final Method METHOD_ALTAR_GET_PROGRESS;

    // ---- BloodMagicAPI / RecipeRegistrar ----
    public static final Field FIELD_API_INSTANCE;
    public static final Method METHOD_API_GET_RECIPE_REGISTRAR;
    public static final Method METHOD_REGISTRAR_GET_ALCHEMY_RECIPES;
    public static final Method METHOD_REGISTRAR_GET_ALTAR_RECIPES;

    // ---- RecipeAlchemyTable ----
    public static final Method METHOD_RECIPE_ALCHEMY_TABLE_GET_INPUT;
    public static final Method METHOD_RECIPE_ALCHEMY_TABLE_GET_OUTPUT;
    public static final Method METHOD_RECIPE_ALCHEMY_TABLE_GET_SYPHON;

    // ---- RecipeBloodAltar ----
    public static final Method METHOD_RECIPE_BLOOD_ALTAR_GET_INPUT;
    public static final Method METHOD_RECIPE_BLOOD_ALTAR_GET_OUTPUT;
    public static final Method METHOD_RECIPE_BLOOD_ALTAR_GET_SYPHON;

    static {
        Class<?> tileAlchemyTable = null;
        Method alchemyTableIsSlave = null;
        Method alchemyTableGetBurnTime = null;
        Method alchemyTableIsInputSlotAccessible = null;

        Class<?> tileSoulForge = null;
        Field soulForgeBurnTime = null;

        Class<?> tileAltar = null;
        Method altarStartCycle = null;
        Method altarIsActive = null;
        Method altarGetProgress = null;

        Field apiInstance = null;
        Method apiGetRecipeRegistrar = null;
        Method registrarGetAlchemyRecipes = null;
        Method registrarGetAltarRecipes = null;

        Method recipeAlchemyTableGetInput = null;
        Method recipeAlchemyTableGetOutput = null;
        Method recipeAlchemyTableGetSyphon = null;

        Method recipeBloodAltarGetInput = null;
        Method recipeBloodAltarGetOutput = null;
        Method recipeBloodAltarGetSyphon = null;

        try {
            tileAlchemyTable = Class.forName("WayofTime.bloodmagic.tile.TileAlchemyTable");
            alchemyTableIsSlave = tileAlchemyTable.getMethod("isSlave");
            alchemyTableGetBurnTime = tileAlchemyTable.getMethod("getBurnTime");
            alchemyTableIsInputSlotAccessible = tileAlchemyTable.getMethod("isInputSlotAccessible", int.class);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BloodMagicReflectionHelper failed to cache TileAlchemyTable", e);
        }

        try {
            tileSoulForge = Class.forName("WayofTime.bloodmagic.tile.TileSoulForge");
            soulForgeBurnTime = tileSoulForge.getField("burnTime");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BloodMagicReflectionHelper failed to cache TileSoulForge", e);
        }

        try {
            tileAltar = Class.forName("WayofTime.bloodmagic.tile.TileAltar");
            altarStartCycle = tileAltar.getMethod("startCycle");
            altarIsActive = tileAltar.getMethod("isActive");
            altarGetProgress = tileAltar.getMethod("getProgress");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BloodMagicReflectionHelper failed to cache TileAltar", e);
        }

        try {
            Class<?> api = Class.forName("WayofTime.bloodmagic.api.impl.BloodMagicAPI");
            apiInstance = api.getField("INSTANCE");
            apiGetRecipeRegistrar = api.getMethod("getRecipeRegistrar");

            Class<?> registrar = Class.forName("WayofTime.bloodmagic.api.impl.BloodMagicRecipeRegistrar");
            registrarGetAlchemyRecipes = registrar.getMethod("getAlchemyRecipes");
            registrarGetAltarRecipes = registrar.getMethod("getAltarRecipes");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BloodMagicReflectionHelper failed to cache BloodMagicAPI", e);
        }

        try {
            Class<?> recipeAlchemyTable = Class.forName("WayofTime.bloodmagic.api.impl.recipe.RecipeAlchemyTable");
            recipeAlchemyTableGetInput = recipeAlchemyTable.getMethod("getInput");
            recipeAlchemyTableGetOutput = recipeAlchemyTable.getMethod("getOutput");
            recipeAlchemyTableGetSyphon = recipeAlchemyTable.getMethod("getSyphon");

            Class<?> recipeBloodAltar = Class.forName("WayofTime.bloodmagic.api.impl.recipe.RecipeBloodAltar");
            recipeBloodAltarGetInput = recipeBloodAltar.getMethod("getInput");
            recipeBloodAltarGetOutput = recipeBloodAltar.getMethod("getOutput");
            recipeBloodAltarGetSyphon = recipeBloodAltar.getMethod("getSyphon");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BloodMagicReflectionHelper failed to cache recipe classes", e);
        }

        CLASS_TILE_ALCHEMY_TABLE = tileAlchemyTable;
        METHOD_ALCHEMY_TABLE_IS_SLAVE = alchemyTableIsSlave;
        METHOD_ALCHEMY_TABLE_GET_BURN_TIME = alchemyTableGetBurnTime;
        METHOD_ALCHEMY_TABLE_IS_INPUT_SLOT_ACCESSIBLE = alchemyTableIsInputSlotAccessible;

        CLASS_TILE_SOUL_FORGE = tileSoulForge;
        FIELD_SOUL_FORGE_BURN_TIME = soulForgeBurnTime;

        CLASS_TILE_ALTAR = tileAltar;
        METHOD_ALTAR_START_CYCLE = altarStartCycle;
        METHOD_ALTAR_IS_ACTIVE = altarIsActive;
        METHOD_ALTAR_GET_PROGRESS = altarGetProgress;

        FIELD_API_INSTANCE = apiInstance;
        METHOD_API_GET_RECIPE_REGISTRAR = apiGetRecipeRegistrar;
        METHOD_REGISTRAR_GET_ALCHEMY_RECIPES = registrarGetAlchemyRecipes;
        METHOD_REGISTRAR_GET_ALTAR_RECIPES = registrarGetAltarRecipes;

        METHOD_RECIPE_ALCHEMY_TABLE_GET_INPUT = recipeAlchemyTableGetInput;
        METHOD_RECIPE_ALCHEMY_TABLE_GET_OUTPUT = recipeAlchemyTableGetOutput;
        METHOD_RECIPE_ALCHEMY_TABLE_GET_SYPHON = recipeAlchemyTableGetSyphon;

        METHOD_RECIPE_BLOOD_ALTAR_GET_INPUT = recipeBloodAltarGetInput;
        METHOD_RECIPE_BLOOD_ALTAR_GET_OUTPUT = recipeBloodAltarGetOutput;
        METHOD_RECIPE_BLOOD_ALTAR_GET_SYPHON = recipeBloodAltarGetSyphon;
    }

    // ---- 通用调用封装 ----

    public static boolean isInstance(Class<?> clazz, Object obj) {
        return clazz != null && clazz.isInstance(obj);
    }

    private static Object invoke(Method method, Object target, Object... args) {
        if (method == null) {
            throw new IllegalStateException("[AE2E] BloodMagicReflectionHelper: method not cached");
        }
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[AE2E] BloodMagicReflectionHelper invoke failed: " + method, e);
        }
    }

    private static boolean invokeBool(Method method, Object target, Object... args) {
        return (Boolean) invoke(method, target, args);
    }

    private static int invokeInt(Method method, Object target, Object... args) {
        return (Integer) invoke(method, target, args);
    }

    // ---- TileAlchemyTable ----

    public static boolean isAlchemyTableSlave(Object table) {
        return invokeBool(METHOD_ALCHEMY_TABLE_IS_SLAVE, table);
    }

    public static int getAlchemyTableBurnTime(Object table) {
        return invokeInt(METHOD_ALCHEMY_TABLE_GET_BURN_TIME, table);
    }

    public static boolean isAlchemyTableInputSlotAccessible(Object table, int slot) {
        return invokeBool(METHOD_ALCHEMY_TABLE_IS_INPUT_SLOT_ACCESSIBLE, table, slot);
    }

    // ---- TileSoulForge ----

    public static int getSoulForgeBurnTime(Object forge) {
        if (FIELD_SOUL_FORGE_BURN_TIME == null) {
            throw new IllegalStateException("[AE2E] BloodMagicReflectionHelper: burnTime field not cached");
        }
        try {
            return FIELD_SOUL_FORGE_BURN_TIME.getInt(forge);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("[AE2E] BloodMagicReflectionHelper failed to read SoulForge burnTime", e);
        }
    }

    // ---- TileAltar ----

    public static void altarStartCycle(Object altar) {
        invoke(METHOD_ALTAR_START_CYCLE, altar);
    }

    public static boolean isAltarActive(Object altar) {
        return invokeBool(METHOD_ALTAR_IS_ACTIVE, altar);
    }

    public static int getAltarProgress(Object altar) {
        return invokeInt(METHOD_ALTAR_GET_PROGRESS, altar);
    }

    // ---- BloodMagicAPI / RecipeRegistrar ----

    private static Object getRecipeRegistrar() {
        if (FIELD_API_INSTANCE == null) {
            throw new IllegalStateException("[AE2E] BloodMagicReflectionHelper: API INSTANCE field not cached");
        }
        try {
            Object api = FIELD_API_INSTANCE.get(null);
            return invoke(METHOD_API_GET_RECIPE_REGISTRAR, api);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("[AE2E] BloodMagicReflectionHelper failed to read API INSTANCE", e);
        }
    }

    public static Set<?> getAlchemyRecipes() {
        return (Set<?>) invoke(METHOD_REGISTRAR_GET_ALCHEMY_RECIPES, getRecipeRegistrar());
    }

    public static Set<?> getAltarRecipes() {
        return (Set<?>) invoke(METHOD_REGISTRAR_GET_ALTAR_RECIPES, getRecipeRegistrar());
    }

    // ---- RecipeAlchemyTable ----

    public static List<?> alchemyRecipeGetInput(Object recipe) {
        return (List<?>) invoke(METHOD_RECIPE_ALCHEMY_TABLE_GET_INPUT, recipe);
    }

    public static ItemStack alchemyRecipeGetOutput(Object recipe) {
        return (ItemStack) invoke(METHOD_RECIPE_ALCHEMY_TABLE_GET_OUTPUT, recipe);
    }

    public static int alchemyRecipeGetSyphon(Object recipe) {
        return invokeInt(METHOD_RECIPE_ALCHEMY_TABLE_GET_SYPHON, recipe);
    }

    // ---- RecipeBloodAltar ----

    public static Ingredient bloodAltarRecipeGetInput(Object recipe) {
        return (Ingredient) invoke(METHOD_RECIPE_BLOOD_ALTAR_GET_INPUT, recipe);
    }

    public static ItemStack bloodAltarRecipeGetOutput(Object recipe) {
        return (ItemStack) invoke(METHOD_RECIPE_BLOOD_ALTAR_GET_OUTPUT, recipe);
    }

    public static int bloodAltarRecipeGetSyphon(Object recipe) {
        return invokeInt(METHOD_RECIPE_BLOOD_ALTAR_GET_SYPHON, recipe);
    }
}
