package betel.nut.test;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import vectorwing.farmersdelight.common.block.entity.CookingPotBlockEntity;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.common.registry.ModBlocks;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/** Runs against the actual optional mod, never a mock recipe serializer or pot. */
public final class FarmerRecipeChecks {
    public static void run(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(2, 3, 2));
        level.setBlock(pos.below(), Blocks.CAMPFIRE.defaultBlockState(), 3);
        level.setBlock(pos, ModBlocks.COOKING_POT.get().defaultBlockState(), 3);
        var pot = (CookingPotBlockEntity) level.getBlockEntity(pos);
        h.assertTrue(pot != null && pot.isHeated(), "An actual lit campfire heats the actual cooking pot");
        for (var holder : BetelRecipeGameTests.variants(h)) {
            var recipe = (CookingPotRecipe) holder.value();
            var inventory = pot.getInventory();
            for (int slot = 0; slot < 9; slot++) inventory.setStackInSlot(slot, ItemStack.EMPTY);
            var ingredients = BetelRecipeGameTests.inputs(holder);
            for (int slot = 0; slot < ingredients.size(); slot++) inventory.setStackInSlot(slot, ingredients.get(slot).copy());
            h.assertTrue(recipe.matches(new RecipeWrapper(inventory), level), "The actual six-slot pot matches " + holder.id());
            h.assertTrue(recipe.getOutputContainer().isEmpty(), "Solid betel has no artificial paper or bowl container");
            ItemStack first = inventory.getStackInSlot(0);
            inventory.setStackInSlot(0, ItemStack.EMPTY);
            h.assertTrue(!recipe.matches(new RecipeWrapper(inventory), level), "A missing nut rejects the pot recipe");
            inventory.setStackInSlot(0, first);
            int bottlesBefore = bottles(h, pos);
            long expectedBottles = ingredients.stream().filter(s -> s.is(Items.HONEY_BOTTLE)).count();
            for (int tick = 0; tick <= recipe.getCookTime(); tick++)
                CookingPotBlockEntity.cookingTick(level, pos, level.getBlockState(pos), pot);
            ItemStack result = inventory.getStackInSlot(CookingPotBlockEntity.OUTPUT_SLOT);
            h.assertTrue(ItemStack.matches(result, recipe.getResultItem(level.registryAccess())), "Actual cooking consumes ingredients and transfers the full batch to the takeable output slot: " + holder.id());
            h.assertTrue(inventory.getStackInSlot(CookingPotBlockEntity.MEAL_DISPLAY_SLOT).isEmpty(), "No result is stranded waiting for paper");
            for (int slot = 0; slot < 6; slot++) h.assertTrue(inventory.getStackInSlot(slot).isEmpty(), "Every input is consumed once");
            h.assertTrue(bottles(h, pos) - bottlesBefore == expectedBottles, "Actual honey cooking returns each glass bottle once");
        }
        // Avoid leaking a warmed pot into neighboring tests.
        pot.getInventory().setStackInSlot(CookingPotBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
    }

    public static void runRecovery(GameTestHelper h) {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(2, 3, 2));
        level.setBlock(pos.below(), Blocks.CAMPFIRE.defaultBlockState(), 3);
        level.setBlock(pos, ModBlocks.COOKING_POT.get().defaultBlockState(), 3);
        var pot = (CookingPotBlockEntity) level.getBlockEntity(pos);
        h.assertTrue(pot != null && pot.isHeated(), "Real heated cooking pot for bottled remedies");
        for (var holder : BetelRecoveryGameTests.recipes(h)) {
            var recipe = (CookingPotRecipe) holder.value();
            var inventory = pot.getInventory();
            for (int slot = 0; slot < 9; slot++) inventory.setStackInSlot(slot, ItemStack.EMPTY);
            var ingredients = BetelRecipeGameTests.inputs(holder);
            for (int slot = 0; slot < ingredients.size(); slot++) inventory.setStackInSlot(slot, ingredients.get(slot).copy());
            h.assertTrue(recipe.matches(new RecipeWrapper(inventory), level), "Actual pot accepts remedy ingredients");
            h.assertTrue(recipe.getOutputContainer().is(Items.GLASS_BOTTLE), "Remedy requires a glass bottle container");
            int bottlesBefore = bottles(h, pos);
            for (int tick = 0; tick <= recipe.getCookTime(); tick++)
                CookingPotBlockEntity.cookingTick(level, pos, level.getBlockState(pos), pot);
            h.assertTrue(inventory.getStackInSlot(CookingPotBlockEntity.OUTPUT_SLOT).isEmpty(), "Unbottled remedy cannot be taken");
            h.assertTrue(ItemStack.matches(inventory.getStackInSlot(CookingPotBlockEntity.MEAL_DISPLAY_SLOT), recipe.getResultItem(level.registryAccess())), "Remedy waits inside pot for its bottle");
            inventory.setStackInSlot(CookingPotBlockEntity.CONTAINER_SLOT, new ItemStack(Items.GLASS_BOTTLE));
            CookingPotBlockEntity.cookingTick(level, pos, level.getBlockState(pos), pot);
            h.assertTrue(ItemStack.matches(inventory.getStackInSlot(CookingPotBlockEntity.OUTPUT_SLOT), recipe.getResultItem(level.registryAccess())), "Glass bottle transfers exactly one finished remedy");
            h.assertTrue(inventory.getStackInSlot(CookingPotBlockEntity.CONTAINER_SLOT).isEmpty(), "Packaging consumes exactly one empty bottle");
            h.assertTrue(inventory.getStackInSlot(CookingPotBlockEntity.MEAL_DISPLAY_SLOT).isEmpty(), "No duplicate remedy remains in pot");
            for (int slot = 0; slot < 6; slot++) h.assertTrue(inventory.getStackInSlot(slot).isEmpty(), "Each recovery ingredient consumed once");
            h.assertTrue(bottles(h, pos) - bottlesBefore == 1, "Honey returns its original glass bottle once");
        }
        pot.getInventory().setStackInSlot(CookingPotBlockEntity.OUTPUT_SLOT, ItemStack.EMPTY);
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static int bottles(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2)).stream()
                .filter(e -> e.getItem().is(Items.GLASS_BOTTLE)).mapToInt(e -> e.getItem().getCount()).sum();
    }
}
