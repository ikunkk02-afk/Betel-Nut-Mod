package betel.nut.skyblock;

import betel.nut.item.ModItems;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Renewable seeds, tools and a small recovery reserve; every call creates fresh stacks. */
public final class SkyblockStarterKit {
    public static List<ItemStack> supplies() {
        return List.of(new ItemStack(Items.SHEARS), new ItemStack(Items.IRON_INGOT, 2),
                new ItemStack(Items.BONE_MEAL, 8), new ItemStack(Items.ICE, 2),
                new ItemStack(Items.LAVA_BUCKET), new ItemStack(Items.OAK_SAPLING),
                new ItemStack(Items.JUNGLE_SAPLING, 4), new ItemStack(Items.COCOA_BEANS),
                new ItemStack(Items.WHEAT_SEEDS, 2), new ItemStack(Items.SUGAR_CANE),
                new ItemStack(Items.MOSS_BLOCK), new ItemStack(Items.DANDELION), new ItemStack(Items.BUDDING_AMETHYST),
                new ItemStack(Items.BREAD, 8), new ItemStack(ModItems.RELIEF_POTION, 2),
                new ItemStack(ModItems.GOLDEN_APPLE_PASTE));
    }

    private SkyblockStarterKit() {}
}
