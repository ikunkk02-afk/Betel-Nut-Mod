package betel.nut.villager;

import java.util.Optional;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;

public final class ModVillagerTrades {
	private static final float PRICE_MULTIPLIER = 0.05F;
	private static boolean registered;

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> registerTrades());
	}

	private static void registerTrades() {
		if (registered) {
			return;
		}
		registered = true;

		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableFarmerTrades) {
			BetelNutMod.LOGGER.info("Betel nut farmer villager trades are disabled by config");
			return;
		}

		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 1, factories -> {
			factories.add(itemForEmeralds(ModItems.RAW_BETEL_NUT, 1, config.farmerTradeRawBetelBuyCount, 16, 2));
			factories.add(emeraldForItems(ModItems.RAW_BETEL_NUT, config.farmerTradeRawBetelSellCount, 12, 2));
		});

		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 2, factories -> factories
				.add(itemForEmeralds(ModItems.BETEL_LEAF, 1, config.farmerTradeLeafBuyCount, 12, 5)));

		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 3, factories -> factories
				.add(itemForEmeralds(ModItems.ROASTED_BETEL_NUT, 2, config.farmerTradeRoastedBetelBuyCount, 10, 10)));

		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 4, factories -> {
			factories.add(itemForEmeralds(ModItems.SPICY_BETEL_NUT, config.farmerTradeFlavorEmeraldCost, 1, 6, 15));
			factories.add(itemForEmeralds(ModItems.SWEET_BETEL_NUT, config.farmerTradeFlavorEmeraldCost, 1, 6, 15));
			factories.add(itemForEmeralds(ModItems.REFRESHING_BETEL_NUT, config.farmerTradeFlavorEmeraldCost, 1, 6, 15));
			factories.add(itemForEmeralds(ModItems.NIGHT_BETEL_NUT, config.farmerTradeFlavorEmeraldCost, 1, 6, 15));
			factories.add(itemForEmeralds(ModItems.ENERGIZING_BETEL_NUT, config.farmerTradeFlavorEmeraldCost, 1, 6, 15));
		});

		TradeOfferHelper.registerVillagerOffers(VillagerProfession.FARMER, 5,
				factories -> factories.add(syntheticWorldTrade(config.farmerTradeSyntheticWorldEmeraldCost, 2, 30)));

		BetelNutMod.LOGGER.info("Registered betel nut farmer villager trades");
	}

	private static VillagerTrades.ItemListing itemForEmeralds(ItemLike item, int emeraldCost, int resultCount,
			int maxUses, int experience) {
		return (entity, random) -> new MerchantOffer(
				new ItemCost(Items.EMERALD, emeraldCost),
				new ItemStack(item, resultCount),
				maxUses,
				experience,
				PRICE_MULTIPLIER);
	}

	private static VillagerTrades.ItemListing emeraldForItems(ItemLike item, int itemCost, int maxUses, int experience) {
		return (entity, random) -> new MerchantOffer(
				new ItemCost(item, itemCost),
				new ItemStack(Items.EMERALD),
				maxUses,
				experience,
				PRICE_MULTIPLIER);
	}

	private static VillagerTrades.ItemListing syntheticWorldTrade(int emeraldCost, int maxUses, int experience) {
		return (entity, random) -> new MerchantOffer(
				new ItemCost(Items.EMERALD, emeraldCost),
				Optional.of(new ItemCost(ModItems.ROASTED_BETEL_NUT)),
				new ItemStack(ModItems.SYNTHETIC_WORLD_BETEL),
				maxUses,
				experience,
				PRICE_MULTIPLIER);
	}

	private ModVillagerTrades() {
	}
}
