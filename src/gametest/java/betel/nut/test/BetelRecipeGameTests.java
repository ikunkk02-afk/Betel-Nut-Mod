package betel.nut.test;

import betel.nut.BetelNutMod;
import betel.nut.item.BetelNutItem;
import betel.nut.item.ModItems;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;

public final class BetelRecipeGameTests implements FabricGameTest {
    private static boolean isVariant(Item item) {
        return item instanceof BetelNutItem && item != ModItems.ROASTED_BETEL_NUT && item != ModItems.WRAPPED_RAW_BETEL;
    }

    public static List<RecipeHolder<?>> variants(GameTestHelper h) {
        return h.getLevel().getRecipeManager().getRecipes().stream()
                .filter(r -> r.id().getNamespace().equals(BetelNutMod.MOD_ID)
                        && isVariant(r.value().getResultItem(h.getLevel().registryAccess()).getItem()))
                .sorted(java.util.Comparator.comparing(r -> r.id().toString())).toList();
    }

    public static List<ItemStack> inputs(RecipeHolder<?> holder) {
        return holder.value().getIngredients().stream().map(i -> i.getItems()[0].copyWithCount(1)).toList();
    }

    private static CraftingInput grid(List<ItemStack> ingredients) {
        List<ItemStack> stacks = new ArrayList<>(ingredients);
        while (stacks.size() < 9) stacks.add(ItemStack.EMPTY);
        return CraftingInput.of(3, 3, stacks);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void everyVariantHasExactlyOneEnvironmentRouteAndConservesNuts(GameTestHelper h) {
        boolean delight = FabricLoader.getInstance().isModLoaded("farmersdelight");
        var recipes = variants(h);
        h.assertTrue(recipes.size() == 18, "All 17 variants load, with exactly two refreshing alternatives");
        Set<Item> outputs = new HashSet<>();
        for (var holder : recipes) {
            var recipe = holder.value();
            var result = recipe.getResultItem(h.getLevel().registryAccess());
            outputs.add(result.getItem());
            var ingredients = inputs(holder);
            long nuts = ingredients.stream().filter(s -> s.getItem() instanceof BetelNutItem).count();
            h.assertTrue(nuts == result.getCount(), holder.id() + " cannot duplicate or destroy the nut base");
            h.assertTrue(ingredients.size() <= 6, "Every recipe fits the real cooking pot's six input slots");
            h.assertTrue(ingredients.stream().noneMatch(s -> s.is(Items.PAPER) || s.is(Items.CRAFTING_TABLE)
                    || s.is(Items.MAP) || s.is(Items.DEEPSLATE)), "No paper container, furniture, map or stone filler");
            String type = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()).toString();
            h.assertTrue(type.equals(delight ? "farmersdelight:cooking" : "minecraft:crafting"), "Only the selected environment's route loads");
            if (recipe instanceof CraftingRecipe crafting) {
                var input = grid(ingredients);
                var actual = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel());
                h.assertTrue(actual.isPresent() && actual.get().id().equals(holder.id()), "Real recipe lookup selects the intended flavor");
                h.assertTrue(ItemStack.matches(crafting.assemble(input, h.getLevel().registryAccess()), result), "Assembly preserves result item and count");
                List<ItemStack> missing = new ArrayList<>(ingredients);
                missing.removeFirst();
                h.assertTrue(!crafting.matches(grid(missing), h.getLevel()), "One missing nut cannot produce a full batch");
                var remaining = crafting.getRemainingItems(input);
                long bottles = remaining.stream().filter(s -> s.is(Items.GLASS_BOTTLE)).mapToInt(ItemStack::getCount).sum();
                long honey = ingredients.stream().filter(s -> s.is(Items.HONEY_BOTTLE)).count();
                h.assertTrue(bottles == honey, "Honey bottle containers are returned exactly once");
            }
            h.assertTrue(h.getLevel().getServer().getAdvancements().get(BetelNutMod.id("recipes/" + holder.id().getPath())) != null,
                    "The active recipe has a loaded recipe-book advancement");
        }
        h.assertTrue(outputs.size() == 17, "Every registered variant is represented");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void processingChainIsReachableAndRejectsOldPaperShortcuts(GameTestHelper h) {
        Map<Item, Set<Item>> dependencies = new HashMap<>();
        for (var holder : variants(h)) {
            Item output = holder.value().getResultItem(h.getLevel().registryAccess()).getItem();
            Set<Item> bases = dependencies.computeIfAbsent(output, ignored -> new HashSet<>());
            inputs(holder).stream().map(ItemStack::getItem).filter(BetelRecipeGameTests::isVariant).forEach(bases::add);
            if (holder.value() instanceof CraftingRecipe recipe) {
                var old = grid(List.of(new ItemStack(ModItems.ROASTED_BETEL_NUT), new ItemStack(Items.PAPER), new ItemStack(Items.SUGAR)));
                h.assertTrue(!recipe.matches(old, h.getLevel()), "Legacy single-nut paper shortcuts are removed");
            }
        }
        for (Item item : dependencies.keySet()) visit(h, item, dependencies, new HashSet<>(), new HashSet<>());
        h.assertTrue(dependencies.get(ModItems.ENERGIZING_BETEL_NUT).equals(Set.of(ModItems.SWEET_BETEL_NUT)), "Energizing advances from sweet betel");
        h.assertTrue(dependencies.get(ModItems.MAGMA_BETEL_NUT).equals(Set.of(ModItems.SPICY_BETEL_NUT)), "Magma advances from spicy betel");
        for (Item compound : List.of(ModItems.SYNTHETIC_WORLD_BETEL, ModItems.RICH_WORLD_BETEL, ModItems.UNDERGROUND_BETEL))
            h.assertTrue(dependencies.get(compound).size() == 3, "Compound flavors require three distinct processed variants");
        h.succeed();
    }

    private static void visit(GameTestHelper h, Item item, Map<Item, Set<Item>> graph, Set<Item> visiting, Set<Item> done) {
        if (done.contains(item)) return;
        h.assertTrue(visiting.add(item), "No circular crafting dependency");
        h.assertTrue(graph.containsKey(item), "Every intermediate flavor has an enabled recipe");
        for (Item base : graph.get(item)) visit(h, base, graph, visiting, done);
        visiting.remove(item);
        done.add(item);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void ordinaryProcessingStaysUsableAndRealPotCookingWorksWhenInstalled(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var roasted = level.getRecipeManager().byKey(BetelNutMod.id("roasted_betel_nut_from_campfire")).orElseThrow().value();
        h.assertTrue(roasted instanceof CampfireCookingRecipe && roasted.getType() == RecipeType.CAMPFIRE_COOKING, "Ordinary betel still uses a campfire");
        var campfire = (CampfireCookingRecipe) roasted;
        var raw = new SingleRecipeInput(new ItemStack(ModItems.RAW_BETEL_NUT));
        h.assertTrue(campfire.matches(raw, level) && campfire.getCookingTime() == 600 && Math.abs(campfire.getExperience() - .35F) < .001F,
                "Ordinary roasting input, duration and experience stay unchanged");
        var result = campfire.assemble(raw, level.registryAccess());
        h.assertTrue(result.is(ModItems.ROASTED_BETEL_NUT) && result.getCount() == 1, "Ordinary roasting still produces one nut");
        var wrap = grid(List.of(new ItemStack(ModItems.RAW_BETEL_NUT), new ItemStack(ModItems.BETEL_LEAF), new ItemStack(ModItems.SHELL_POWDER)));
        var wrapped = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, wrap, level).orElseThrow().value().assemble(wrap, level.registryAccess());
        h.assertTrue(wrapped.is(ModItems.WRAPPED_RAW_BETEL) && wrapped.getCount() == 1, "Ordinary raw leaf-wrapping remains available with or without Delight");
        if (FabricLoader.getInstance().isModLoaded("farmersdelight")) {
            Class.forName("betel.nut.test.FarmerRecipeChecks").getMethod("run", GameTestHelper.class).invoke(null, h);
        }
        h.succeed();
    }
}
