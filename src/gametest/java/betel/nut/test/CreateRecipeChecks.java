package betel.nut.test;

import betel.nut.BetelNutMod;
import betel.nut.compat.CreateRecipePolicy;
import betel.nut.item.ModItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.kinetics.crafter.MechanicalCraftingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.basin.BasinRecipe;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

/** Loaded reflectively only with Create installed; exercises the actual processing implementations. */
public final class CreateRecipeChecks {
    private static Recipe<?> recipe(GameTestHelper h, String name) {
        return h.getLevel().getRecipeManager().byKey(BetelNutMod.id("compat/create/" + name)).orElseThrow().value();
    }

    public static void validateAll(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager().getRecipes().stream().filter(r ->
                r.id().getNamespace().equals(BetelNutMod.MOD_ID) && r.id().getPath().startsWith("compat/create/")).toList();
        h.assertTrue(recipes.size() == 34, "All 34 dedicated production routes decode");
        for (var holder : recipes) {
            var r = holder.value();
            h.assertTrue(!r.getResultItem(h.getLevel().registryAccess()).isEmpty(), "Every route has a real result: " + holder.id());
            if (r instanceof ProcessingRecipe<?, ?> processing) {
                h.assertTrue(processing.validate().isEmpty(), "Create validates " + holder.id() + processing.validate());
                var results = processing.rollResults(h.getLevel().random);
                if (!holder.id().getPath().endsWith("washing_dirt"))
                    h.assertTrue(results.size() == 1 && !results.getFirst().isEmpty(), "Guaranteed output without random junk");
            } else {
                h.assertTrue(r instanceof MechanicalCraftingRecipe || r instanceof SequencedAssemblyRecipe
                        || r.getType() == RecipeType.SMOKING, "Only supported native recipe types");
            }
        }
    }

    public static void washing(GameTestHelper h) {
        var level = h.getLevel();
        var washing = com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes.SPLASHING;
        for (var input : new Item[]{Items.DIRT, Items.GRAVEL}) {
            var id = input == Items.DIRT ? BetelNutMod.id("compat/create/washing_dirt")
                    : ResourceLocation.parse("create:splashing/gravel");
            var loaded = (ProcessingRecipe<?, ?>) level.getRecipeManager().byKey(id).orElseThrow().value();
            h.assertTrue(loaded.validate().isEmpty(), "Water washing recipe validates");
            h.assertTrue(washing.canProcess(new ItemStack(input), level), "Real water fan recognizes dirt/gravel");
            int shells = 0, flint = 0, iron = 0;
            for (int i = 0; i < 256; i++) {
                // Use the fan's real stack-processing path, including recipe lookup and chance rolls.
                var output = washing.process(new ItemStack(input, 64), level);
                h.assertTrue(output != null, "Real fan finds the washing recipe");
                for (var stack : output) {
                    if (stack.is(ModItems.SHELL)) shells += stack.getCount();
                    else if (stack.is(Items.FLINT)) flint += stack.getCount();
                    else if (stack.is(Items.IRON_NUGGET)) iron += stack.getCount();
                    else h.assertTrue(false, "Washing never produces ancient shells or unapproved items");
                    h.assertTrue(!stack.is(input), "The input is consumed, rather than returned for free repeat washing");
                }
            }
            h.assertTrue(shells > 3500 && shells < 4700, "Batch washing yields shells near 25 percent");
            if (input == Items.GRAVEL)
                h.assertTrue(flint > 3500 && flint < 4700 && iron > 1500 && iron < 2600, "Vanilla Create flint and iron chances remain");
            else h.assertTrue(flint == 0 && iron == 0, "Dirt washing only yields ordinary shells");
        }
    }

    private static Block block(String name) {
        return BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("create", name));
    }

    private static BasinBlockEntity basin(GameTestHelper h, boolean heated, Item... ingredients) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(3, 4, 3));
        // Replace the BE to clear Create's cached heat and inventories between cases.
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.setBlock(pos.below(), heated ? block("blaze_burner").defaultBlockState()
                .setValue(BlazeBurnerBlock.HEAT_LEVEL, HeatLevel.KINDLED) : Blocks.STONE.defaultBlockState(), 3);
        level.setBlock(pos, block("basin").defaultBlockState(), 3);
        var basin = (BasinBlockEntity) level.getBlockEntity(pos);
        h.assertTrue(basin != null, "Actual basin block entity");
        for (int i = 0; i < ingredients.length; i++) basin.getInputInventory().setStackInSlot(i, new ItemStack(ingredients[i]));
        return basin;
    }

    private static int outputCount(BasinBlockEntity basin, Item item) {
        int count = 0;
        var inventory = basin.getOutputInventory();
        for (int i = 0; i < inventory.getSlots(); i++) if (inventory.getStackInSlot(i).is(item)) count += inventory.getStackInSlot(i).getCount();
        return count;
    }

    public static void basins(GameTestHelper h) {
        var sweet = recipe(h, "mixing_sweet_betel_nut");
        Item[] inputs = {ModItems.ROASTED_BETEL_NUT, ModItems.ROASTED_BETEL_NUT, ModItems.BETEL_LEAF, Items.SUGAR};
        var cold = basin(h, false, inputs);
        cold.inputTank.getPrimaryHandler().fill(new FluidStack(Fluids.WATER, 250), FluidAction.EXECUTE);
        h.assertTrue(!BasinRecipe.apply(cold, sweet), "Cold basin cannot bypass sugar glazing heat");
        h.assertTrue(cold.getInputInventory().getStackInSlot(0).getCount() == 1, "Rejected heat consumes nothing");
        var hot = basin(h, true, inputs);
        hot.inputTank.getPrimaryHandler().fill(new FluidStack(Fluids.WATER, 249), FluidAction.EXECUTE);
        h.assertTrue(!BasinRecipe.apply(hot, sweet), "249 mB cannot substitute for 250 mB");
        h.assertTrue(hot.inputTank.getPrimaryHandler().getFluidAmount() == 249, "Rejected fluid amount is not consumed");
        hot.inputTank.getPrimaryHandler().fill(new FluidStack(Fluids.WATER, 1), FluidAction.EXECUTE);
        h.assertTrue(BasinRecipe.apply(hot, sweet), "Heated basin actually processes the complete input batch");
        h.assertTrue(outputCount(hot, ModItems.SWEET_BETEL_NUT) == 2 && hot.getInputInventory().isEmpty()
                && hot.inputTank.getPrimaryHandler().isEmpty(), "Exactly two nuts; all inputs and exactly 250 mB consumed");
        h.assertTrue(!BasinRecipe.apply(hot, sweet), "Consumed inputs cannot be processed again");

        var quartz = recipe(h, "mixing_quartz_betel_nut");
        var wrongTier = basin(h, true, ModItems.ROASTED_BETEL_NUT, ModItems.ROASTED_BETEL_NUT, Items.QUARTZ);
        wrongTier.inputTank.getPrimaryHandler().fill(new FluidStack(Fluids.WATER, 250), FluidAction.EXECUTE);
        h.assertTrue(!BasinRecipe.apply(wrongTier, quartz), "Roasted nuts cannot skip the sweet base stage");
        var tier = basin(h, true, ModItems.SWEET_BETEL_NUT, ModItems.SWEET_BETEL_NUT, Items.QUARTZ);
        tier.inputTank.getPrimaryHandler().fill(new FluidStack(Fluids.WATER, 250), FluidAction.EXECUTE);
        h.assertTrue(BasinRecipe.apply(tier, quartz) && outputCount(tier, ModItems.QUARTZ_BETEL_NUT) == 2, "Prepared sweet base produces two quartz nuts");

        var remedy = recipe(h, "mixing_enchanted_golden_apple_paste");
        var missingBottle = basin(h, true, Items.ENCHANTED_GOLDEN_APPLE);
        missingBottle.inputTank.getPrimaryHandler().fill(honey(250), FluidAction.EXECUTE);
        h.assertTrue(!BasinRecipe.apply(missingBottle, remedy), "Rare recovery paste cannot invent its bottle");
        var bottled = basin(h, true, Items.ENCHANTED_GOLDEN_APPLE, Items.GLASS_BOTTLE);
        bottled.inputTank.getPrimaryHandler().fill(honey(250), FluidAction.EXECUTE);
        h.assertTrue(BasinRecipe.apply(bottled, remedy), "Real basin processes bottled rare remedy");
        h.assertTrue(outputCount(bottled, ModItems.ENCHANTED_GOLDEN_APPLE_PASTE) == 1
                && outputCount(bottled, Items.GLASS_BOTTLE) == 0 && bottled.getInputInventory().isEmpty(), "One rare apple and one bottle give one bottled remedy without an extra empty bottle");
        h.getLevel().setBlock(bottled.getBlockPos(), Blocks.AIR.defaultBlockState(), 3);
    }

    private static FluidStack honey(int amount) {
        return new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse("create:honey")), amount);
    }

    public static void spout(GameTestHelper h) {
        var level = h.getLevel();
        var sweet = new ItemStack(ModItems.SWEET_BETEL_NUT, 2);
        var honey = honey(250);
        h.assertTrue(FillingBySpout.canItemBeFilled(level, sweet), "Spout recognizes the actual sweet base");
        h.assertTrue(FillingBySpout.getRequiredAmountForItem(level, sweet, new FluidStack(Fluids.WATER, 1000)) == -1, "Water cannot act as honey");
        h.assertTrue(FillingBySpout.getRequiredAmountForItem(level, sweet, honey(124)) == 125, "Spout requires a full 125 mB before processing");
        for (int i = 0; i < 2; i++) {
            int amount = FillingBySpout.getRequiredAmountForItem(level, sweet, honey);
            h.assertTrue(amount == 125, "Each nut uses half a honey bottle's fluid");
            var result = FillingBySpout.fillItem(level, amount, sweet, honey);
            h.assertTrue(result.is(ModItems.HONEY_BETEL_NUT) && result.getCount() == 1, "Actual spout execution coats exactly one nut");
        }
        h.assertTrue(sweet.isEmpty() && honey.isEmpty(), "Two base nuts and 250 mB produce exactly two coated nuts");
    }

    public static void wrapping(GameTestHelper h) {
        var level = h.getLevel();
        var inv = new ItemStackHandler(2);
        var input = new RecipeWrapper(inv);
        inv.setStackInSlot(0, new ItemStack(ModItems.RAW_BETEL_NUT));
        inv.setStackInSlot(1, new ItemStack(ModItems.BETEL_LEAF));
        h.assertTrue(SequencedAssemblyRecipe.getRecipe(level, input, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).isEmpty(), "Leaf cannot skip the powder step");
        inv.setStackInSlot(1, new ItemStack(ModItems.SHELL_POWDER));
        var first = SequencedAssemblyRecipe.getRecipe(level, input, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).orElseThrow().value();
        h.assertTrue(!first.shouldKeepHeldItem(), "Powder is consumed by the deployer");
        var progress = first.rollResults(level.random).getFirst();
        h.assertTrue(progress.is(ModItems.RAW_BETEL_NUT) && progress.getCount() == 1, "First step retains one real raw nut while recording assembly progress");
        inv.setStackInSlot(0, progress.copy());
        h.assertTrue(SequencedAssemblyRecipe.getRecipe(level, input, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).isEmpty(), "Powder cannot repeat the finished step");
        inv.setStackInSlot(1, new ItemStack(ModItems.BETEL_LEAF));
        var second = SequencedAssemblyRecipe.getRecipe(level, input, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).orElseThrow().value();
        h.assertTrue(!second.shouldKeepHeldItem(), "Wrapping consumes one leaf");
        var results = second.rollResults(level.random);
        h.assertTrue(results.size() == 1 && results.getFirst().is(ModItems.WRAPPED_RAW_BETEL)
                && results.getFirst().getCount() == 1, "One ordered powder-and-leaf sequence produces exactly one finished wrapped nut");
        inv.setStackInSlot(0, results.getFirst());
        h.assertTrue(SequencedAssemblyRecipe.getRecipe(level, input, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class).isEmpty(), "Finished goods cannot replay the assembly reward");
    }

    private static CraftingInput grid(GameTestHelper h, List<ItemStack> stacks) {
        var tag = new net.minecraft.nbt.CompoundTag();
        var entries = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            var entry = new net.minecraft.nbt.CompoundTag();
            entry.putInt("x", i % 3); entry.putInt("y", 2 - i / 3);
            entry.put("item", stacks.get(i).saveOptional(h.getLevel().registryAccess())); entries.add(entry);
        }
        tag.put("Grid", entries);
        var grouped = com.simibubi.create.content.kinetics.crafter.RecipeGridHandler.GroupedItems.read(tag, h.getLevel().registryAccess());
        grouped.calcStats();
        return com.simibubi.create.content.kinetics.crafter.MechanicalCraftingInput.of(grouped);
    }

    public static void crafting(GameTestHelper h) {
        var pack = (MechanicalCraftingRecipe) recipe(h, "mechanical_kouweiwang_pack");
        var inputs = new ArrayList<ItemStack>();
        for (var item : new Item[]{ModItems.ROASTED_BETEL_NUT, ModItems.SPICY_BETEL_NUT, ModItems.SWEET_BETEL_NUT,
                ModItems.REFRESHING_BETEL_NUT, ModItems.NIGHT_BETEL_NUT, ModItems.ENERGIZING_BETEL_NUT}) inputs.add(new ItemStack(item));
        inputs.add(ItemStack.EMPTY); inputs.add(new ItemStack(ModItems.EMPTY_PACK)); inputs.add(ItemStack.EMPTY);
        h.assertTrue(!pack.matches(grid(h, inputs), h.getLevel()), "Six flavors and empty packaging cannot replace the purchased seal");
        inputs.set(8, new ItemStack(ModItems.BRAND_SEAL));
        h.assertTrue(pack.matches(grid(h, inputs), h.getLevel()), "Real mechanical crafting matches six distinct flavors, packaging and seal");
        var result = pack.assemble(grid(h, inputs), h.getLevel().registryAccess());
        h.assertTrue(result.is(ModItems.KOUWEIWANG_PACK) && result.getCount() == 1, "One full material set produces one pack");
        h.assertTrue(pack.getRemainingItems(grid(h, inputs)).stream().allMatch(ItemStack::isEmpty), "A consumed seal or empty pack is not returned");
        var rare = (MechanicalCraftingRecipe) recipe(h, "mechanical_underground_betel");
        var rareInput = new ArrayList<ItemStack>(List.of(new ItemStack(ModItems.NIGHT_BETEL_NUT), new ItemStack(ModItems.QUARTZ_BETEL_NUT), new ItemStack(ModItems.AMETHYST_BETEL_NUT),
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        h.assertTrue(!rare.matches(grid(h, rareInput), h.getLevel()), "Three flavored nuts cannot omit the rare echo shard");
        rareInput.set(4, new ItemStack(Items.ECHO_SHARD));
        h.assertTrue(rare.matches(grid(h, rareInput), h.getLevel()), "Rare shard and three prepared flavors match their mechanical recipe");
        h.assertTrue(rare.assemble(grid(h, rareInput), h.getLevel().registryAccess()).getCount() == 3, "Three input nuts stay three output nuts");
    }

    public static void automation(GameTestHelper h) {
        int count = 0;
        for (var holder : h.getLevel().getRecipeManager().getRecipes()) {
            if (CreateRecipePolicy.requiresDedicatedRoute(holder)) {
                count++;
                h.assertTrue(AllRecipeTypes.shouldIgnoreInAutomation(holder), "Generic mixer/crafter cannot bypass explicit route: " + holder.id());
                h.assertTrue(holder.value().getType() == RecipeType.CRAFTING, "Original hand-crafting IDs and recipe type are retained");
            } else if (holder.id().getNamespace().equals(BetelNutMod.MOD_ID) && holder.id().getPath().startsWith("compat/create/")) {
                h.assertTrue(!AllRecipeTypes.shouldIgnoreInAutomation(holder), "Dedicated processing routes remain automatable");
            }
        }
        h.assertTrue(count == (ModList.get().isLoaded("farmersdelight") ? 5 : 26), "Both manual-route environments retain exactly the intended recipes");
        var planks = h.getLevel().getRecipeManager().byKey(BetelNutMod.id("betel_palm_planks")).orElseThrow();
        h.assertTrue(!AllRecipeTypes.shouldIgnoreInAutomation(planks), "Unrelated wood processing remains automatable");
    }
    private CreateRecipeChecks() {}
}
