package betel.nut.item;

import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.ToIntFunction;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class ModItems {
	public static final Item RAW_BETEL_NUT = register("raw_betel_nut", new Item(new Item.Properties()));
	public static final Item BETEL_LEAF = register("betel_leaf", new Item(new Item.Properties()));
	public static final Item BETEL_NUT_RESIDUE = register("betel_nut_residue", new Item(new Item.Properties()));

	public static final Item ROASTED_BETEL_NUT = register("roasted_betel_nut",
			new BetelNutItem(foodProperties(2, 0.3F), BetelNutConfig.ROASTED_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 300, 1))));

	public static final Item SPICY_BETEL_NUT = register("spicy_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.SPICY_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.FIRE_RESISTANCE, 3600, 1))));

	public static final Item SWEET_BETEL_NUT = register("sweet_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.SWEET_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 2700, 1))));

	public static final Item REFRESHING_BETEL_NUT = register("refreshing_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.REFRESHING_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.WATER_BREATHING, 3600, 1))));

	public static final Item NIGHT_BETEL_NUT = register("night_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.NIGHT_ADDICTION,
					List.of(new BetelNutItem.EffectSpec(MobEffects.NIGHT_VISION, 3600, 1))));

	public static final Item ENERGIZING_BETEL_NUT = register("energizing_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F), BetelNutConfig.ENERGIZING_ADDICTION,
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 1800, 1),
							new BetelNutItem.EffectSpec(MobEffects.DIG_SPEED, 1800, 1))));

	public static final Item HONEY_BETEL_NUT = register("honey_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.honeyBetelAddictionIncrease),
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.REGENERATION, 400, 1),
							new BetelNutItem.EffectSpec(MobEffects.ABSORPTION, 400, 1))));

	public static final Item GLOW_BETEL_NUT = register("glow_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.glowingBetelAddictionIncrease),
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.NIGHT_VISION, 3600, 1),
							new BetelNutItem.EffectSpec(MobEffects.GLOWING, 300, 0))));

	public static final Item PHANTOM_BETEL_NUT = register("phantom_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.phantomBetelAddictionIncrease),
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.SLOW_FALLING, 2400, 1),
							new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 600, 1))));

	public static final Item ENDER_BETEL_NUT = register("ender_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.enderBetelAddictionIncrease),
					List.of()));

	public static final Item LAPIS_BETEL_NUT = register("lapis_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.lapisBetelAddictionIncrease),
					List.of(new BetelNutItem.EffectSpec(MobEffects.LUCK, 2400, 1))));

	public static final Item QUARTZ_BETEL_NUT = register("quartz_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.quartzBetelAddictionIncrease),
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.DIG_SPEED, 1600, 1),
							new BetelNutItem.EffectSpec(MobEffects.DAMAGE_RESISTANCE, 400, 1))));

	public static final Item MAGMA_BETEL_NUT = register("magma_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.magmaBetelAddictionIncrease),
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.FIRE_RESISTANCE, 3600, 1),
							new BetelNutItem.EffectSpec(MobEffects.DAMAGE_BOOST, 400, 1),
							new BetelNutItem.EffectSpec(MobEffects.HUNGER, 160, 0))));

	public static final Item AMETHYST_BETEL_NUT = register("amethyst_betel_nut",
			new BetelNutItem(foodProperties(3, 0.4F),
					configValue(config -> config.amethystBetelAddictionIncrease),
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.DAMAGE_RESISTANCE, 800, 1),
							new BetelNutItem.EffectSpec(MobEffects.LUCK, 1200, 1))));

	public static final Item SYNTHETIC_WORLD_BETEL = register("synthetic_world_betel",
			new BetelNutItem(foodProperties(6, 0.8F).rarity(Rarity.EPIC).fireResistant(),
					BetelNutConfig.SYNTHETIC_WORLD_ADDICTION,
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 6000, 2),
							new BetelNutItem.EffectSpec(MobEffects.DIG_SPEED, 6000, 2),
							new BetelNutItem.EffectSpec(MobEffects.DAMAGE_RESISTANCE, 6000, 1))));

	public static final Item RICH_WORLD_BETEL = register("rich_world_betel",
			new BetelNutItem(foodProperties(6, 0.8F).rarity(Rarity.EPIC).fireResistant(),
					BetelNutConfig.RICH_WORLD_ADDICTION,
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SPEED, 2400, 2),
							new BetelNutItem.EffectSpec(MobEffects.DIG_SPEED, 2400, 2),
							new BetelNutItem.EffectSpec(MobEffects.DAMAGE_RESISTANCE, 1800, 1),
							new BetelNutItem.EffectSpec(MobEffects.NIGHT_VISION, 3600, 1))));

	public static final Item UNDERGROUND_BETEL = register("underground_betel",
			new BetelNutItem(foodProperties(5, 0.6F).rarity(Rarity.EPIC),
					BetelNutConfig.UNDERGROUND_ADDICTION,
					List.of(
							new BetelNutItem.EffectSpec(MobEffects.NIGHT_VISION, 6000, 1),
							new BetelNutItem.EffectSpec(MobEffects.DIG_SPEED, 3600, 2),
							new BetelNutItem.EffectSpec(MobEffects.DAMAGE_RESISTANCE, 2400, 1),
							new BetelNutItem.EffectSpec(MobEffects.MOVEMENT_SLOWDOWN, 200, 0))));

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
