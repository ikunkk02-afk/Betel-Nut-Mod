package betel.nut.item;

import betel.nut.BetelNutMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
	public static final TagKey<Item> BETEL_FOODS = create("betel_foods");
	public static final TagKey<Item> BETEL_NUTS = create("betel_nuts");
	public static final TagKey<Item> WITHDRAWAL_STAGE2_ALLOWED_FOODS = create(
			"withdrawal_stage2_allowed_foods");
	public static final TagKey<Item> WITHDRAWAL_STAGE3_ALLOWED_FOODS = create(
			"withdrawal_stage3_allowed_foods");

	private static TagKey<Item> create(String path) {
		return TagKey.create(Registries.ITEM, BetelNutMod.id(path));
	}

	private ModItemTags() {
	}
}
