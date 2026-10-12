package betel.nut.item;

import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
    public static final Item GOLDEN_APPLE_PASTE = register("golden_apple_paste", new RecoveryDrinkItem(RecoveryDrinkItem.Kind.GOLDEN_PASTE));
    public static final Item ENCHANTED_GOLDEN_APPLE_PASTE = register("enchanted_golden_apple_paste", new RecoveryDrinkItem(RecoveryDrinkItem.Kind.ENCHANTED_PASTE));
    public static final Item RELIEF_POTION = register("relief_potion", new RecoveryDrinkItem(RecoveryDrinkItem.Kind.RELIEF));
	public static final Item SHELL = register("shell", new Item(new Item.Properties()));
	public static final Item SHELL_POWDER = register("shell_powder", new Item(new Item.Properties()));
	public static final Item ANCIENT_SHELL = register("ancient_shell", new Item(new Item.Properties().rarity(Rarity.RARE)));
	public static final Item GOLDEN_SHELL = register("golden_shell", new Item(new Item.Properties().rarity(Rarity.EPIC)));
	public static final Item EMPTY_PACK = register("empty_pack", new Item(new Item.Properties()));
	public static final Item BRAND_SEAL = register("brand_seal", new Item(new Item.Properties()));
	public static final Item KOUWEIWANG_PACK = register("kouweiwang_pack", new BetelPackItem(0));
	public static final Item HECHENG_TIANXIA_PACK = register("hecheng_tianxia_pack", new BetelPackItem(1));
	public static final Item XIANGTAN_PUZI_PACK = register("xiangtan_puzi_pack", new BetelPackItem(2));
	public static final Item LOTTERY_TICKET = register("lottery_ticket", new LotteryTicketItem());
	public static final Item WRAPPED_RAW_BETEL = register("wrapped_raw_betel",
			new BetelNutItem(foodProperties(2, 0.2F), 1,
					BetelEffectProfile.WRAPPED));
	public static final Item ECHO_BETEL = register("echo_betel",
			new BetelNutItem(foodProperties(3, 0.3F).rarity(Rarity.RARE), 1,
					BetelEffectProfile.ECHO));
	public static final Item RAW_BETEL_NUT = register("raw_betel_nut", new Item(new Item.Properties()));
	public static final Item BETEL_LEAF = register("betel_leaf", new Item(new Item.Properties()));
	public static final Item BETEL_NUT_RESIDUE = register("betel_nut_residue", new Item(new Item.Properties()));

	public static final Item ROASTED_BETEL_NUT = register("roasted_betel_nut",
			new BetelNutItem(foodProperties(2, 0.3F), BetelNutConfig.ROASTED_ADDICTION,
					BetelEffectProfile.ROASTED));

	public static final Item SPICY_BETEL_NUT = register("spicy_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.SPICY_ADDICTION,
					BetelEffectProfile.SPICY));

	public static final Item SWEET_BETEL_NUT = register("sweet_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.SWEET_ADDICTION,
					BetelEffectProfile.SWEET));

	public static final Item REFRESHING_BETEL_NUT = register("refreshing_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.REFRESHING_ADDICTION,
					BetelEffectProfile.REFRESHING));

	public static final Item NIGHT_BETEL_NUT = register("night_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.NIGHT_ADDICTION,
					BetelEffectProfile.NIGHT));

	public static final Item ENERGIZING_BETEL_NUT = register("energizing_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.ENERGIZING_ADDICTION,
					BetelEffectProfile.ENERGIZING));

	public static final Item HONEY_BETEL_NUT = register("honey_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.honeyBetelAddictionIncrease),
					BetelEffectProfile.HONEY));

	public static final Item GLOW_BETEL_NUT = register("glow_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.glowingBetelAddictionIncrease),
					BetelEffectProfile.GLOW));

	public static final Item PHANTOM_BETEL_NUT = register("phantom_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.phantomBetelAddictionIncrease),
					BetelEffectProfile.PHANTOM));

	public static final Item ENDER_BETEL_NUT = register("ender_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.enderBetelAddictionIncrease),
					BetelEffectProfile.ENDER));

	public static final Item LAPIS_BETEL_NUT = register("lapis_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.lapisBetelAddictionIncrease),
					BetelEffectProfile.LAPIS));

	public static final Item QUARTZ_BETEL_NUT = register("quartz_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.quartzBetelAddictionIncrease),
					BetelEffectProfile.QUARTZ));

	public static final Item MAGMA_BETEL_NUT = register("magma_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.magmaBetelAddictionIncrease),
					BetelEffectProfile.MAGMA));

	public static final Item AMETHYST_BETEL_NUT = register("amethyst_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.amethystBetelAddictionIncrease),
					BetelEffectProfile.AMETHYST));

	public static final Item SYNTHETIC_WORLD_BETEL = register("synthetic_world_betel",
			new BetelNutItem(foodProperties(3, 0.5F).rarity(Rarity.EPIC).fireResistant(),
					BetelNutConfig.SYNTHETIC_WORLD_ADDICTION,
					BetelEffectProfile.SYNTHETIC));

	public static final Item RICH_WORLD_BETEL = register("rich_world_betel",
			new BetelNutItem(foodProperties(3, 0.5F).rarity(Rarity.EPIC).fireResistant(),
					BetelNutConfig.RICH_WORLD_ADDICTION,
					BetelEffectProfile.RICH));

	public static final Item UNDERGROUND_BETEL = register("underground_betel",
			new BetelNutItem(foodProperties(3, 0.4F).rarity(Rarity.EPIC),
					BetelNutConfig.UNDERGROUND_ADDICTION,
					BetelEffectProfile.UNDERGROUND));

	private static <T extends Item> T register(String path, T item) {
		return Registry.register(BuiltInRegistries.ITEM, BetelNutMod.id(path), item);
	}

	private static Item.Properties foodProperties(int nutrition, float saturationModifier) {
		FoodProperties food = new FoodProperties.Builder()
				.nutrition(nutrition)
				.saturationModifier(saturationModifier)
				.alwaysEdible()
				.build();
		return new Item.Properties().food(food);
	}

	private static IntSupplier configValue(ToIntFunction<BetelNutConfig> getter) {
		return () -> getter.applyAsInt(BetelNutConfig.get());
	}

	public static void register() {
		BetelNutMod.LOGGER.info("Registering betel nut items");
	}

	private ModItems() {
	}
}
