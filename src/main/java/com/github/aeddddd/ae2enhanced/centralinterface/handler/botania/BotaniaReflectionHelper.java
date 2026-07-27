package com.github.aeddddd.ae2enhanced.centralinterface.handler.botania;

import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Botania Handler 的反射辅助类.
 *
 * 缓存对 Botania 全部使用到的类/方法/字段的反射引用,避免重复反射带来的性能损耗.
 * 所有缓存均在静态初始化块中完成,失败时记录警告但不应导致崩溃
 * (对应 handler 会在 isValidTarget 中回退到类型检查).
 *
 * 本类自身不引用任何 vazkii.botania 类型,因此可以安全地被无条件加载.
 */
public class BotaniaReflectionHelper {

    // ---- TilePool ----
    public static final Class<?> CLASS_TILE_POOL;
    public static final Method METHOD_POOL_GET_MATCHING_RECIPE;
    public static final Method METHOD_POOL_GET_CURRENT_MANA;
    public static final Method METHOD_POOL_COLLIDE_ENTITY_ITEM;

    // ---- TileAlfPortal ----
    public static final Class<?> CLASS_TILE_ALF_PORTAL;

    // ---- TileTerraPlate ----
    public static final Class<?> CLASS_TILE_TERRA_PLATE;
    public static final Method METHOD_HAS_VALID_PLATFORM;
    public static final Method METHOD_ARE_ITEMS_VALID;
    public static final Method METHOD_GET_ITEMS;

    // ---- TileRuneAltar ----
    public static final Class<?> CLASS_TILE_RUNE_ALTAR;
    public static final Field FIELD_CURRENT_RECIPE;
    public static final Field FIELD_COOLDOWN;
    public static final Field FIELD_MANA;
    public static final Field FIELD_MANA_TO_GET;
    public static final Method METHOD_RUNE_ALTAR_IS_EMPTY;
    public static final Method METHOD_RUNE_ALTAR_ADD_ITEM;
    public static final Method METHOD_RUNE_ALTAR_GET_ITEM_HANDLER;
    public static final Method METHOD_RUNE_ALTAR_GET_CURRENT_MANA;
    public static final Method METHOD_RUNE_ALTAR_RECIEVE_MANA;
    public static final Method METHOD_RUNE_ALTAR_SAVE_LAST_RECIPE;
    public static final Method METHOD_RUNE_ALTAR_GET_SIZE_INVENTORY;

    // ---- TileAltar ----
    public static final Class<?> CLASS_TILE_ALTAR;
    public static final Method METHOD_ALTAR_IS_EMPTY;
    public static final Method METHOD_ALTAR_HAS_LAVA;
    public static final Method METHOD_ALTAR_HAS_WATER;
    public static final Method METHOD_ALTAR_SET_WATER;
    public static final Method METHOD_ALTAR_COLLIDE_ENTITY_ITEM;
    public static final Method METHOD_ALTAR_GET_ITEM_HANDLER;

    // ---- BotaniaStateProps / AlfPortalState ----
    @SuppressWarnings("rawtypes")
    public static final IProperty ALFPORTAL_STATE_PROP;
    public static final Object ALFPORTAL_STATE_OFF;

    // ---- ModBlocks / ModItems (vazkii.botania.common) ----
    public static final Block BLOCK_LIVINGROCK;
    public static final Block BLOCK_RUNE_ALTAR;
    public static final Item ITEM_MANA_RESOURCE;
    public static final Item ITEM_RUNE;
    public static final Item ITEM_LEXICON;

    // ---- BotaniaAPI 配方列表(static final List,引用稳定,直接缓存) ----
    public static final List<?> PETAL_RECIPES;
    public static final List<?> MANA_INFUSION_RECIPES;
    public static final List<?> RUNE_ALTAR_RECIPES;
    public static final List<?> ELVEN_TRADE_RECIPES;

    // ---- Recipe 类 ----
    public static final Class<?> CLASS_RECIPE_RUNE_ALTAR;
    public static final Method METHOD_MANA_INFUSION_GET_INPUT;
    public static final Method METHOD_MANA_INFUSION_GET_OUTPUT;
    public static final Method METHOD_MANA_INFUSION_GET_MANA_TO_CONSUME;
    public static final Method METHOD_ELVEN_TRADE_GET_INPUTS;
    public static final Method METHOD_ELVEN_TRADE_GET_OUTPUTS;
    public static final Method METHOD_PETALS_MATCHES;
    public static final Method METHOD_PETALS_GET_INPUTS;
    public static final Method METHOD_PETALS_GET_OUTPUT;
    public static final Method METHOD_RUNE_ALTAR_GET_MANA_USAGE;

    static {
        Class<?> tilePool = null;
        Method poolGetMatchingRecipe = null;
        Method poolGetCurrentMana = null;
        Method poolCollideEntityItem = null;

        Class<?> tileAlfPortal = null;

        Class<?> tileTerraPlate = null;
        Method hasValidPlatform = null;
        Method areItemsValid = null;
        Method getItems = null;

        Class<?> tileRuneAltar = null;
        Field currentRecipe = null;
        Field cooldown = null;
        Field mana = null;
        Field manaToGet = null;
        Method runeAltarIsEmpty = null;
        Method runeAltarAddItem = null;
        Method runeAltarGetItemHandler = null;
        Method runeAltarGetCurrentMana = null;
        Method runeAltarRecieveMana = null;
        Method runeAltarSaveLastRecipe = null;
        Method runeAltarGetSizeInventory = null;

        Class<?> tileAltar = null;
        Method altarIsEmpty = null;
        Method altarHasLava = null;
        Method altarHasWater = null;
        Method altarSetWater = null;
        Method altarCollideEntityItem = null;
        Method altarGetItemHandler = null;

        IProperty alfPortalStateProp = null;
        Object alfPortalStateOff = null;

        Block livingrock = null;
        Block runeAltarBlock = null;
        Item manaResource = null;
        Item rune = null;
        Item lexicon = null;

        List<?> petalRecipes = null;
        List<?> manaInfusionRecipes = null;
        List<?> runeAltarRecipes = null;
        List<?> elvenTradeRecipes = null;

        Class<?> recipeRuneAltar = null;
        Method manaInfusionGetInput = null;
        Method manaInfusionGetOutput = null;
        Method manaInfusionGetManaToConsume = null;
        Method elvenTradeGetInputs = null;
        Method elvenTradeGetOutputs = null;
        Method petalsMatches = null;
        Method petalsGetInputs = null;
        Method petalsGetOutput = null;
        Method runeAltarGetManaUsage = null;

        try {
            tilePool = Class.forName("vazkii.botania.common.block.tile.mana.TilePool");
            poolGetMatchingRecipe = tilePool.getMethod("getMatchingRecipe", ItemStack.class, IBlockState.class);
            poolGetCurrentMana = tilePool.getMethod("getCurrentMana");
            poolCollideEntityItem = tilePool.getMethod("collideEntityItem", EntityItem.class);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache TilePool", e);
        }

        try {
            tileAlfPortal = Class.forName("vazkii.botania.common.block.tile.TileAlfPortal");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache TileAlfPortal", e);
        }

        try {
            tileTerraPlate = Class.forName("vazkii.botania.common.block.tile.TileTerraPlate");
            hasValidPlatform = tileTerraPlate.getDeclaredMethod("hasValidPlatform");
            hasValidPlatform.setAccessible(true);
            areItemsValid = tileTerraPlate.getDeclaredMethod("areItemsValid", List.class);
            areItemsValid.setAccessible(true);
            getItems = tileTerraPlate.getDeclaredMethod("getItems");
            getItems.setAccessible(true);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache TileTerraPlate", e);
        }

        try {
            tileRuneAltar = Class.forName("vazkii.botania.common.block.tile.TileRuneAltar");
            currentRecipe = tileRuneAltar.getDeclaredField("currentRecipe");
            currentRecipe.setAccessible(true);
            cooldown = tileRuneAltar.getDeclaredField("cooldown");
            cooldown.setAccessible(true);
            mana = tileRuneAltar.getDeclaredField("mana");
            mana.setAccessible(true);
            manaToGet = tileRuneAltar.getField("manaToGet");
            runeAltarIsEmpty = tileRuneAltar.getMethod("isEmpty");
            runeAltarAddItem = tileRuneAltar.getMethod("addItem", EntityPlayer.class, ItemStack.class, EnumHand.class);
            runeAltarGetItemHandler = tileRuneAltar.getMethod("getItemHandler");
            runeAltarGetCurrentMana = tileRuneAltar.getMethod("getCurrentMana");
            runeAltarRecieveMana = tileRuneAltar.getMethod("recieveMana", int.class);
            runeAltarSaveLastRecipe = tileRuneAltar.getMethod("saveLastRecipe");
            runeAltarGetSizeInventory = tileRuneAltar.getMethod("getSizeInventory");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache TileRuneAltar", e);
        }

        try {
            tileAltar = Class.forName("vazkii.botania.common.block.tile.TileAltar");
            altarIsEmpty = tileAltar.getMethod("isEmpty");
            altarHasLava = tileAltar.getMethod("hasLava");
            altarHasWater = tileAltar.getMethod("hasWater");
            altarSetWater = tileAltar.getMethod("setWater", boolean.class);
            altarCollideEntityItem = tileAltar.getMethod("collideEntityItem", EntityItem.class);
            altarGetItemHandler = tileAltar.getMethod("getItemHandler");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache TileAltar", e);
        }

        try {
            Class<?> stateProps = Class.forName("vazkii.botania.api.state.BotaniaStateProps");
            alfPortalStateProp = (IProperty) stateProps.getField("ALFPORTAL_STATE").get(null);
            Class<?> alfPortalState = Class.forName("vazkii.botania.api.state.enums.AlfPortalState");
            alfPortalStateOff = alfPortalState.getField("OFF").get(null);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache BotaniaStateProps", e);
        }

        try {
            Class<?> modBlocks = Class.forName("vazkii.botania.common.block.ModBlocks");
            livingrock = (Block) modBlocks.getField("livingrock").get(null);
            runeAltarBlock = (Block) modBlocks.getField("runeAltar").get(null);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache ModBlocks", e);
        }

        try {
            Class<?> modItems = Class.forName("vazkii.botania.common.item.ModItems");
            manaResource = (Item) modItems.getField("manaResource").get(null);
            rune = (Item) modItems.getField("rune").get(null);
            lexicon = (Item) modItems.getField("lexicon").get(null);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache ModItems", e);
        }

        try {
            Class<?> botaniaAPI = Class.forName("vazkii.botania.api.BotaniaAPI");
            petalRecipes = (List<?>) botaniaAPI.getField("petalRecipes").get(null);
            manaInfusionRecipes = (List<?>) botaniaAPI.getField("manaInfusionRecipes").get(null);
            runeAltarRecipes = (List<?>) botaniaAPI.getField("runeAltarRecipes").get(null);
            elvenTradeRecipes = (List<?>) botaniaAPI.getField("elvenTradeRecipes").get(null);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache BotaniaAPI recipe lists", e);
        }

        try {
            Class<?> recipeManaInfusion = Class.forName("vazkii.botania.api.recipe.RecipeManaInfusion");
            manaInfusionGetInput = recipeManaInfusion.getMethod("getInput");
            manaInfusionGetOutput = recipeManaInfusion.getMethod("getOutput");
            manaInfusionGetManaToConsume = recipeManaInfusion.getMethod("getManaToConsume");

            Class<?> recipeElvenTrade = Class.forName("vazkii.botania.api.recipe.RecipeElvenTrade");
            elvenTradeGetInputs = recipeElvenTrade.getMethod("getInputs");
            elvenTradeGetOutputs = recipeElvenTrade.getMethod("getOutputs");

            Class<?> recipePetals = Class.forName("vazkii.botania.api.recipe.RecipePetals");
            petalsMatches = recipePetals.getMethod("matches", IItemHandler.class);
            petalsGetInputs = recipePetals.getMethod("getInputs");
            petalsGetOutput = recipePetals.getMethod("getOutput");

            recipeRuneAltar = Class.forName("vazkii.botania.api.recipe.RecipeRuneAltar");
            runeAltarGetManaUsage = recipeRuneAltar.getMethod("getManaUsage");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] BotaniaReflectionHelper failed to cache recipe classes", e);
        }

        CLASS_TILE_POOL = tilePool;
        METHOD_POOL_GET_MATCHING_RECIPE = poolGetMatchingRecipe;
        METHOD_POOL_GET_CURRENT_MANA = poolGetCurrentMana;
        METHOD_POOL_COLLIDE_ENTITY_ITEM = poolCollideEntityItem;

        CLASS_TILE_ALF_PORTAL = tileAlfPortal;

        CLASS_TILE_TERRA_PLATE = tileTerraPlate;
        METHOD_HAS_VALID_PLATFORM = hasValidPlatform;
        METHOD_ARE_ITEMS_VALID = areItemsValid;
        METHOD_GET_ITEMS = getItems;

        CLASS_TILE_RUNE_ALTAR = tileRuneAltar;
        FIELD_CURRENT_RECIPE = currentRecipe;
        FIELD_COOLDOWN = cooldown;
        FIELD_MANA = mana;
        FIELD_MANA_TO_GET = manaToGet;
        METHOD_RUNE_ALTAR_IS_EMPTY = runeAltarIsEmpty;
        METHOD_RUNE_ALTAR_ADD_ITEM = runeAltarAddItem;
        METHOD_RUNE_ALTAR_GET_ITEM_HANDLER = runeAltarGetItemHandler;
        METHOD_RUNE_ALTAR_GET_CURRENT_MANA = runeAltarGetCurrentMana;
        METHOD_RUNE_ALTAR_RECIEVE_MANA = runeAltarRecieveMana;
        METHOD_RUNE_ALTAR_SAVE_LAST_RECIPE = runeAltarSaveLastRecipe;
        METHOD_RUNE_ALTAR_GET_SIZE_INVENTORY = runeAltarGetSizeInventory;

        CLASS_TILE_ALTAR = tileAltar;
        METHOD_ALTAR_IS_EMPTY = altarIsEmpty;
        METHOD_ALTAR_HAS_LAVA = altarHasLava;
        METHOD_ALTAR_HAS_WATER = altarHasWater;
        METHOD_ALTAR_SET_WATER = altarSetWater;
        METHOD_ALTAR_COLLIDE_ENTITY_ITEM = altarCollideEntityItem;
        METHOD_ALTAR_GET_ITEM_HANDLER = altarGetItemHandler;

        ALFPORTAL_STATE_PROP = alfPortalStateProp;
        ALFPORTAL_STATE_OFF = alfPortalStateOff;

        BLOCK_LIVINGROCK = livingrock;
        BLOCK_RUNE_ALTAR = runeAltarBlock;
        ITEM_MANA_RESOURCE = manaResource;
        ITEM_RUNE = rune;
        ITEM_LEXICON = lexicon;

        PETAL_RECIPES = petalRecipes;
        MANA_INFUSION_RECIPES = manaInfusionRecipes;
        RUNE_ALTAR_RECIPES = runeAltarRecipes;
        ELVEN_TRADE_RECIPES = elvenTradeRecipes;

        CLASS_RECIPE_RUNE_ALTAR = recipeRuneAltar;
        METHOD_MANA_INFUSION_GET_INPUT = manaInfusionGetInput;
        METHOD_MANA_INFUSION_GET_OUTPUT = manaInfusionGetOutput;
        METHOD_MANA_INFUSION_GET_MANA_TO_CONSUME = manaInfusionGetManaToConsume;
        METHOD_ELVEN_TRADE_GET_INPUTS = elvenTradeGetInputs;
        METHOD_ELVEN_TRADE_GET_OUTPUTS = elvenTradeGetOutputs;
        METHOD_PETALS_MATCHES = petalsMatches;
        METHOD_PETALS_GET_INPUTS = petalsGetInputs;
        METHOD_PETALS_GET_OUTPUT = petalsGetOutput;
        METHOD_RUNE_ALTAR_GET_MANA_USAGE = runeAltarGetManaUsage;
    }

    // ---- 通用调用封装 ----

    public static boolean isInstance(Class<?> clazz, Object obj) {
        return clazz != null && clazz.isInstance(obj);
    }

    private static Object invoke(Method method, Object target, Object... args) {
        if (method == null) {
            throw new IllegalStateException("[AE2E] BotaniaReflectionHelper: method not cached");
        }
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[AE2E] BotaniaReflectionHelper invoke failed: " + method, e);
        }
    }

    private static boolean invokeBool(Method method, Object target, Object... args) {
        return (Boolean) invoke(method, target, args);
    }

    private static int invokeInt(Method method, Object target, Object... args) {
        return (Integer) invoke(method, target, args);
    }

    // ---- TilePool ----

    public static Object getPoolMatchingRecipe(ItemStack stack, IBlockState state) {
        return invoke(METHOD_POOL_GET_MATCHING_RECIPE, null, stack, state);
    }

    public static int getPoolCurrentMana(Object pool) {
        return invokeInt(METHOD_POOL_GET_CURRENT_MANA, pool);
    }

    public static boolean poolCollideEntityItem(Object pool, EntityItem item) {
        return invokeBool(METHOD_POOL_COLLIDE_ENTITY_ITEM, pool, item);
    }

    // ---- TileRuneAltar ----

    public static int getRuneAltarCooldown(Object altar) {
        if (FIELD_COOLDOWN == null) return 0;
        try {
            return FIELD_COOLDOWN.getInt(altar);
        } catch (IllegalAccessException e) {
            return 0;
        }
    }

    public static void setRuneAltarCooldown(Object altar, int value) {
        if (FIELD_COOLDOWN == null) return;
        try {
            FIELD_COOLDOWN.setInt(altar, value);
        } catch (IllegalAccessException e) {
            AE2Enhanced.LOGGER.warn("[AE2E] Failed to set RuneAltar cooldown", e);
        }
    }

    public static int getRuneAltarMana(Object altar) {
        if (FIELD_MANA == null) return runeAltarGetCurrentMana(altar);
        try {
            return FIELD_MANA.getInt(altar);
        } catch (IllegalAccessException e) {
            return runeAltarGetCurrentMana(altar);
        }
    }

    public static Object getRuneAltarCurrentRecipe(Object altar) {
        if (FIELD_CURRENT_RECIPE == null) return null;
        try {
            return FIELD_CURRENT_RECIPE.get(altar);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static void setRuneAltarCurrentRecipe(Object altar, Object recipe) {
        if (FIELD_CURRENT_RECIPE == null) return;
        try {
            FIELD_CURRENT_RECIPE.set(altar, recipe);
        } catch (IllegalAccessException e) {
            AE2Enhanced.LOGGER.warn("[AE2E] Failed to set RuneAltar currentRecipe", e);
        }
    }

    public static int runeAltarGetManaToGet(Object altar) {
        if (FIELD_MANA_TO_GET == null) {
            throw new IllegalStateException("[AE2E] BotaniaReflectionHelper: manaToGet field not cached");
        }
        try {
            return FIELD_MANA_TO_GET.getInt(altar);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("[AE2E] BotaniaReflectionHelper failed to read manaToGet", e);
        }
    }

    public static boolean runeAltarIsEmpty(Object altar) {
        return invokeBool(METHOD_RUNE_ALTAR_IS_EMPTY, altar);
    }

    public static boolean runeAltarAddItem(Object altar, ItemStack stack) {
        return invokeBool(METHOD_RUNE_ALTAR_ADD_ITEM, altar, null, stack, null);
    }

    public static IItemHandlerModifiable runeAltarGetItemHandler(Object altar) {
        return (IItemHandlerModifiable) invoke(METHOD_RUNE_ALTAR_GET_ITEM_HANDLER, altar);
    }

    public static int runeAltarGetCurrentMana(Object altar) {
        return invokeInt(METHOD_RUNE_ALTAR_GET_CURRENT_MANA, altar);
    }

    public static void runeAltarRecieveMana(Object altar, int mana) {
        invoke(METHOD_RUNE_ALTAR_RECIEVE_MANA, altar, mana);
    }

    public static void runeAltarSaveLastRecipe(Object altar) {
        invoke(METHOD_RUNE_ALTAR_SAVE_LAST_RECIPE, altar);
    }

    public static int runeAltarGetSizeInventory(Object altar) {
        return invokeInt(METHOD_RUNE_ALTAR_GET_SIZE_INVENTORY, altar);
    }

    // ---- TileAltar ----

    public static boolean altarIsEmpty(Object altar) {
        return invokeBool(METHOD_ALTAR_IS_EMPTY, altar);
    }

    public static boolean altarHasLava(Object altar) {
        return invokeBool(METHOD_ALTAR_HAS_LAVA, altar);
    }

    public static boolean altarHasWater(Object altar) {
        return invokeBool(METHOD_ALTAR_HAS_WATER, altar);
    }

    public static void altarSetWater(Object altar, boolean water) {
        invoke(METHOD_ALTAR_SET_WATER, altar, water);
    }

    public static boolean altarCollideEntityItem(Object altar, EntityItem item) {
        return invokeBool(METHOD_ALTAR_COLLIDE_ENTITY_ITEM, altar, item);
    }

    public static IItemHandlerModifiable altarGetItemHandler(Object altar) {
        return (IItemHandlerModifiable) invoke(METHOD_ALTAR_GET_ITEM_HANDLER, altar);
    }

    // ---- BotaniaStateProps ----

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Object getAlfPortalState(IBlockState state) {
        if (ALFPORTAL_STATE_PROP == null) return null;
        return state.getValue(ALFPORTAL_STATE_PROP);
    }

    // ---- Recipe 访问 ----

    public static Object manaInfusionGetInput(Object recipe) {
        return invoke(METHOD_MANA_INFUSION_GET_INPUT, recipe);
    }

    public static ItemStack manaInfusionGetOutput(Object recipe) {
        return (ItemStack) invoke(METHOD_MANA_INFUSION_GET_OUTPUT, recipe);
    }

    public static int manaInfusionGetManaToConsume(Object recipe) {
        return invokeInt(METHOD_MANA_INFUSION_GET_MANA_TO_CONSUME, recipe);
    }

    public static List<?> elvenTradeGetInputs(Object recipe) {
        return (List<?>) invoke(METHOD_ELVEN_TRADE_GET_INPUTS, recipe);
    }

    public static List<?> elvenTradeGetOutputs(Object recipe) {
        return (List<?>) invoke(METHOD_ELVEN_TRADE_GET_OUTPUTS, recipe);
    }

    public static boolean petalsMatches(Object recipe, IItemHandler handler) {
        return invokeBool(METHOD_PETALS_MATCHES, recipe, handler);
    }

    public static List<?> petalsGetInputs(Object recipe) {
        return (List<?>) invoke(METHOD_PETALS_GET_INPUTS, recipe);
    }

    public static ItemStack petalsGetOutput(Object recipe) {
        return (ItemStack) invoke(METHOD_PETALS_GET_OUTPUT, recipe);
    }

    public static int runeAltarRecipeGetManaUsage(Object recipe) {
        return invokeInt(METHOD_RUNE_ALTAR_GET_MANA_USAGE, recipe);
    }
}
