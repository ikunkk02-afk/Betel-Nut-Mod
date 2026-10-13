package betel.nut.compat;

import betel.nut.BetelNutMod;
import java.util.Set;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/** Prevents Create's generic crafting adapters from bypassing the explicit production routes. */
public final class CreateRecipePolicy {
    private static final Set<String> MANUAL_RECIPES = Set.of(
            "amethyst_betel_nut",
            "echo_betel",
            "empty_pack",
            "enchanted_golden_apple_paste",
            "ender_betel_nut",
            "energizing_betel_nut",
            "glow_betel_nut",
            "golden_apple_paste",
            "hecheng_tianxia_pack",
            "honey_betel_nut",
            "kouweiwang_pack",
            "lapis_betel_nut",
            "magma_betel_nut",
            "night_betel_nut",
            "phantom_betel_nut",
            "quartz_betel_nut",
            "refreshing_betel_nut_from_prismarine_crystals",
            "refreshing_betel_nut_from_prismarine_shard",
            "relief_potion",
            "rich_world_betel",
            "spicy_betel_nut",
            "sweet_betel_nut",
            "synthetic_world_betel",
            "underground_betel",
            "wrapped_raw_betel",
            "xiangtan_puzi_pack");

    public static boolean requiresDedicatedRoute(RecipeHolder<?> recipe) {
        return recipe.value().getType() == RecipeType.CRAFTING
                && recipe.id().getNamespace().equals(BetelNutMod.MOD_ID)
                && MANUAL_RECIPES.contains(recipe.id().getPath());
    }
    private CreateRecipePolicy() {}
}
