package com.github.aeddddd.ae2enhanced.centralinterface.handler.thaumcraft;

import appeng.api.storage.data.IAEStack;
import com.github.aeddddd.ae2enhanced.AE2Enhanced;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.NonNullList;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Thaumcraft Handler 的反射辅助类.
 *
 * 缓存对 Thaumcraft API 全部使用到的类/方法/构造器的反射引用,避免重复反射带来的性能损耗.
 * 所有缓存均在静态初始化块中完成,失败时记录警告但不应导致崩溃
 * (对应 handler 由 HandlerRegistry 通过 Loader.isModLoaded("thaumcraft") 门控加载,
 *  正常路径下 Thaumcraft 必然存在;ThaumicEnergistics 为可选,缺失时源质消耗返回 null).
 *
 * 本类自身不引用任何 thaumcraft / thaumicenergistics 类型,因此可以安全地被无条件加载.
 */
public class ThaumcraftReflectionHelper {

    // ---- Thaumcraft API 类 ----
    public static final Class<?> CLASS_ASPECT;
    public static final Class<?> CLASS_ASPECT_LIST;
    public static final Class<?> CLASS_INFUSION_RECIPE;
    public static final Class<?> CLASS_CRUCIBLE_RECIPE;

    // ---- ThaumcraftApi ----
    public static final Method METHOD_GET_CRAFTING_RECIPES;
    public static final Method METHOD_GET_INFUSION_RECIPE;
    public static final Method METHOD_GET_CRUCIBLE_RECIPE;

    // ---- ThaumcraftCapabilities / IPlayerKnowledge ----
    public static final Method METHOD_GET_KNOWLEDGE;
    public static final Method METHOD_KNOWLEDGE_ADD_RESEARCH;
    public static final Method METHOD_KNOWLEDGE_REMOVE_RESEARCH;
    public static final Method METHOD_KNOWLEDGE_SET_RESEARCH_STAGE;
    public static final Method METHOD_KNOWLEDGE_SYNC;

    // ---- InfusionRecipe ----
    public static final Method METHOD_INFUSION_GET_RESEARCH;
    public static final Method METHOD_INFUSION_GET_RECIPE_INPUT;
    public static final Method METHOD_INFUSION_GET_COMPONENTS;
    public static final Method METHOD_INFUSION_GET_RECIPE_OUTPUT;
    public static final Method METHOD_INFUSION_GET_ASPECTS;

    // ---- CrucibleRecipe ----
    public static final Method METHOD_CRUCIBLE_GET_RESEARCH;
    public static final Method METHOD_CRUCIBLE_GET_CATALYST;
    public static final Method METHOD_CRUCIBLE_GET_ASPECTS;

    // ---- AspectList ----
    public static final Method METHOD_ASPECT_LIST_SIZE;
    public static final Method METHOD_ASPECT_LIST_GET_ASPECTS;
    public static final Method METHOD_ASPECT_LIST_GET_AMOUNT;

    // ---- ThaumicEnergistics (可选) ----
    public static final Constructor<?> CTOR_ESSENTIA_STACK;
    public static final Method METHOD_FROM_ESSENTIA_STACK;

    static {
        Class<?> aspect = null;
        Class<?> aspectList = null;
        Class<?> infusionRecipe = null;
        Class<?> crucibleRecipe = null;
        Method aspectListSize = null;
        Method aspectListGetAspects = null;
        Method aspectListGetAmount = null;

        Method getCraftingRecipes = null;
        Method getInfusionRecipe = null;
        Method getCrucibleRecipe = null;

        Method getKnowledge = null;
        Method knowledgeAddResearch = null;
        Method knowledgeRemoveResearch = null;
        Method knowledgeSetResearchStage = null;
        Method knowledgeSync = null;

        Method infusionGetResearch = null;
        Method infusionGetRecipeInput = null;
        Method infusionGetComponents = null;
        Method infusionGetRecipeOutput = null;
        Method infusionGetAspects = null;

        Method crucibleGetResearch = null;
        Method crucibleGetCatalyst = null;
        Method crucibleGetAspects = null;

        Constructor<?> essentiaStackCtor = null;
        Method fromEssentiaStack = null;

        try {
            aspect = Class.forName("thaumcraft.api.aspects.Aspect");
            aspectList = Class.forName("thaumcraft.api.aspects.AspectList");
            aspectListSize = aspectList.getMethod("size");
            aspectListGetAspects = aspectList.getMethod("getAspects");
            aspectListGetAmount = aspectList.getMethod("getAmount", aspect);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] ThaumcraftReflectionHelper failed to cache Aspect/AspectList", e);
        }

        try {
            Class<?> thaumcraftApi = Class.forName("thaumcraft.api.ThaumcraftApi");
            getCraftingRecipes = thaumcraftApi.getMethod("getCraftingRecipes");
            getInfusionRecipe = thaumcraftApi.getMethod("getInfusionRecipe", ItemStack.class);
            getCrucibleRecipe = thaumcraftApi.getMethod("getCrucibleRecipe", ItemStack.class);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] ThaumcraftReflectionHelper failed to cache ThaumcraftApi", e);
        }

        try {
            Class<?> capabilities = Class.forName("thaumcraft.api.capabilities.ThaumcraftCapabilities");
            getKnowledge = capabilities.getMethod("getKnowledge", EntityPlayer.class);

            Class<?> playerKnowledge = Class.forName("thaumcraft.api.capabilities.IPlayerKnowledge");
            knowledgeAddResearch = playerKnowledge.getMethod("addResearch", String.class);
            knowledgeRemoveResearch = playerKnowledge.getMethod("removeResearch", String.class);
            knowledgeSetResearchStage = playerKnowledge.getMethod("setResearchStage", String.class, int.class);
            knowledgeSync = playerKnowledge.getMethod("sync", EntityPlayerMP.class);
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] ThaumcraftReflectionHelper failed to cache IPlayerKnowledge", e);
        }

        try {
            infusionRecipe = Class.forName("thaumcraft.api.crafting.InfusionRecipe");
            infusionGetResearch = infusionRecipe.getMethod("getResearch");
            infusionGetRecipeInput = infusionRecipe.getMethod("getRecipeInput");
            infusionGetComponents = infusionRecipe.getMethod("getComponents");
            infusionGetRecipeOutput = infusionRecipe.getMethod("getRecipeOutput");
            infusionGetAspects = infusionRecipe.getMethod("getAspects");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] ThaumcraftReflectionHelper failed to cache InfusionRecipe", e);
        }

        try {
            crucibleRecipe = Class.forName("thaumcraft.api.crafting.CrucibleRecipe");
            crucibleGetResearch = crucibleRecipe.getMethod("getResearch");
            crucibleGetCatalyst = crucibleRecipe.getMethod("getCatalyst");
            crucibleGetAspects = crucibleRecipe.getMethod("getAspects");
        } catch (Exception e) {
            AE2Enhanced.LOGGER.warn("[AE2E] ThaumcraftReflectionHelper failed to cache CrucibleRecipe", e);
        }

        try {
            Class<?> essentiaStack = Class.forName("thaumicenergistics.api.EssentiaStack");
            essentiaStackCtor = essentiaStack.getConstructor(aspect, int.class);
            Class<?> aeEssentiaStack = Class.forName("thaumicenergistics.integration.appeng.AEEssentiaStack");
            fromEssentiaStack = aeEssentiaStack.getMethod("fromEssentiaStack", essentiaStack);
        } catch (Exception e) {
            // ThaumicEnergistics 未安装或版本不兼容,源质消耗将返回 null
            AE2Enhanced.LOGGER.warn("[AE2E] ThaumcraftReflectionHelper failed to cache ThaumicEnergistics classes", e);
        }

        CLASS_ASPECT = aspect;
        CLASS_ASPECT_LIST = aspectList;
        CLASS_INFUSION_RECIPE = infusionRecipe;
        CLASS_CRUCIBLE_RECIPE = crucibleRecipe;

        METHOD_GET_CRAFTING_RECIPES = getCraftingRecipes;
        METHOD_GET_INFUSION_RECIPE = getInfusionRecipe;
        METHOD_GET_CRUCIBLE_RECIPE = getCrucibleRecipe;

        METHOD_GET_KNOWLEDGE = getKnowledge;
        METHOD_KNOWLEDGE_ADD_RESEARCH = knowledgeAddResearch;
        METHOD_KNOWLEDGE_REMOVE_RESEARCH = knowledgeRemoveResearch;
        METHOD_KNOWLEDGE_SET_RESEARCH_STAGE = knowledgeSetResearchStage;
        METHOD_KNOWLEDGE_SYNC = knowledgeSync;

        METHOD_INFUSION_GET_RESEARCH = infusionGetResearch;
        METHOD_INFUSION_GET_RECIPE_INPUT = infusionGetRecipeInput;
        METHOD_INFUSION_GET_COMPONENTS = infusionGetComponents;
        METHOD_INFUSION_GET_RECIPE_OUTPUT = infusionGetRecipeOutput;
        METHOD_INFUSION_GET_ASPECTS = infusionGetAspects;

        METHOD_CRUCIBLE_GET_RESEARCH = crucibleGetResearch;
        METHOD_CRUCIBLE_GET_CATALYST = crucibleGetCatalyst;
        METHOD_CRUCIBLE_GET_ASPECTS = crucibleGetAspects;

        METHOD_ASPECT_LIST_SIZE = aspectListSize;
        METHOD_ASPECT_LIST_GET_ASPECTS = aspectListGetAspects;
        METHOD_ASPECT_LIST_GET_AMOUNT = aspectListGetAmount;

        CTOR_ESSENTIA_STACK = essentiaStackCtor;
        METHOD_FROM_ESSENTIA_STACK = fromEssentiaStack;
    }

    // ---- 通用调用封装 ----

    private static Object invoke(Method method, Object target, Object... args) {
        if (method == null) {
            throw new IllegalStateException("[AE2E] ThaumcraftReflectionHelper: method not cached");
        }
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("[AE2E] ThaumcraftReflectionHelper invoke failed: " + method, e);
        }
    }

    // ---- ThaumcraftApi ----

    public static Object getInfusionRecipe(ItemStack stack) {
        return invoke(METHOD_GET_INFUSION_RECIPE, null, stack);
    }

    public static Object getCrucibleRecipe(ItemStack stack) {
        return invoke(METHOD_GET_CRUCIBLE_RECIPE, null, stack);
    }

    public static Map<?, ?> getCraftingRecipes() {
        return (Map<?, ?>) invoke(METHOD_GET_CRAFTING_RECIPES, null);
    }

    // ---- IPlayerKnowledge ----

    public static Object getKnowledge(EntityPlayer player) {
        return invoke(METHOD_GET_KNOWLEDGE, null, player);
    }

    public static boolean knowledgeAddResearch(Object knowledge, String research) {
        return (Boolean) invoke(METHOD_KNOWLEDGE_ADD_RESEARCH, knowledge, research);
    }

    public static boolean knowledgeRemoveResearch(Object knowledge, String research) {
        return (Boolean) invoke(METHOD_KNOWLEDGE_REMOVE_RESEARCH, knowledge, research);
    }

    public static void knowledgeSetResearchStage(Object knowledge, String research, int stage) {
        invoke(METHOD_KNOWLEDGE_SET_RESEARCH_STAGE, knowledge, research, stage);
    }

    public static void knowledgeSync(Object knowledge, EntityPlayer player) {
        invoke(METHOD_KNOWLEDGE_SYNC, knowledge, player);
    }

    // ---- InfusionRecipe ----

    public static String infusionGetResearch(Object recipe) {
        return (String) invoke(METHOD_INFUSION_GET_RESEARCH, recipe);
    }

    public static Ingredient infusionGetRecipeInput(Object recipe) {
        return (Ingredient) invoke(METHOD_INFUSION_GET_RECIPE_INPUT, recipe);
    }

    @SuppressWarnings("unchecked")
    public static NonNullList<Ingredient> infusionGetComponents(Object recipe) {
        return (NonNullList<Ingredient>) invoke(METHOD_INFUSION_GET_COMPONENTS, recipe);
    }

    public static Object infusionGetRecipeOutput(Object recipe) {
        return invoke(METHOD_INFUSION_GET_RECIPE_OUTPUT, recipe);
    }

    public static Object infusionGetAspects(Object recipe) {
        return invoke(METHOD_INFUSION_GET_ASPECTS, recipe);
    }

    // ---- CrucibleRecipe ----

    public static String crucibleGetResearch(Object recipe) {
        return (String) invoke(METHOD_CRUCIBLE_GET_RESEARCH, recipe);
    }

    public static Ingredient crucibleGetCatalyst(Object recipe) {
        return (Ingredient) invoke(METHOD_CRUCIBLE_GET_CATALYST, recipe);
    }

    public static Object crucibleGetAspects(Object recipe) {
        return invoke(METHOD_CRUCIBLE_GET_ASPECTS, recipe);
    }

    // ---- AspectList ----

    public static int aspectListSize(Object aspects) {
        return (Integer) invoke(METHOD_ASPECT_LIST_SIZE, aspects);
    }

    /**
     * 通过反射创建源质消耗栈，避免在 ThaumicEnergistics 未安装时类加载失败。
     *
     * @return 源质消耗列表；若 ThaumicEnergistics 缺失或任意源质栈无法创建则返回 null
     */
    public static List<IAEStack> createEssentiaCosts(Object aspects, long count) {
        List<IAEStack> costs = new ArrayList<>();
        if (CTOR_ESSENTIA_STACK == null || METHOD_FROM_ESSENTIA_STACK == null
                || METHOD_ASPECT_LIST_GET_ASPECTS == null || METHOD_ASPECT_LIST_GET_AMOUNT == null) {
            return null;
        }
        try {
            for (Object aspect : (Object[]) METHOD_ASPECT_LIST_GET_ASPECTS.invoke(aspects)) {
                if (aspect == null) continue;
                int amount = (Integer) METHOD_ASPECT_LIST_GET_AMOUNT.invoke(aspects, aspect);
                if (amount <= 0) continue;
                Object essStack = CTOR_ESSENTIA_STACK.newInstance(aspect, (int) ((long) amount * count));
                Object aeStack = METHOD_FROM_ESSENTIA_STACK.invoke(null, essStack);
                if (aeStack == null) {
                    return null;
                }
                costs.add((IAEStack) aeStack);
            }
        } catch (Exception e) {
            // ThaumicEnergistics 未安装或版本不兼容
            return null;
        }
        return costs;
    }
}
