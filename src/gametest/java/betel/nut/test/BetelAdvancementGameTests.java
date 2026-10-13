package betel.nut.test;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.ModBlocks;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.ModItems;
import betel.nut.villager.ModVillagerTrades;
import com.mojang.authlib.GameProfile;
import java.util.HashSet;
import java.util.UUID;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("betel_nut_mod")
@PrefixGameTestTemplate(false)
public final class BetelAdvancementGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "adv-test"), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        return p;
    }
    private static AdvancementHolder advancement(ServerPlayer p, String id) {
        var holder = p.server.getAdvancements().get(BetelNutMod.id(id));
        if (holder == null) throw new AssertionError("Missing advancement " + id);
        return holder;
    }
    private static boolean done(ServerPlayer p, String id) {
        return p.getAdvancements().getOrStartProgress(advancement(p, id)).isDone();
    }
    private static void obtain(ServerPlayer p, Item item) {
        p.getInventory().clearContent();
        var stack = new ItemStack(item);
        p.getInventory().setItem(0, stack);
        CriteriaTriggers.INVENTORY_CHANGED.trigger(p, p.getInventory(), stack);
    }
    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    @GameTest(template = "empty")
    public void allVisibleNodesAndLegacyCriteriaLoad(GameTestHelper h) {
        var manager = h.getLevel().getServer().getAdvancements();
        int visible = 0, roots = 0;
        for (var holder : manager.getAllAdvancements()) {
            if (!holder.id().getNamespace().equals(BetelNutMod.MOD_ID) || holder.value().display().isEmpty()) continue;
            visible++;
            if (holder.value().parent().isEmpty()) roots++;
            var seen = new HashSet<net.minecraft.resources.ResourceLocation>();
            var cursor = holder;
            while (cursor.value().parent().isPresent()) {
                h.assertTrue(seen.add(cursor.id()), "No cycle in visible progression");
                cursor = manager.get(cursor.value().parent().orElseThrow());
                h.assertTrue(cursor != null, "Every parent loads");
            }
        }
        h.assertTrue(visible == 28 && roots == 2, "Two tabs contain all 28 visible progression nodes");
        h.assertTrue(manager.get(BetelNutMod.id("root")).value().criteria().containsKey("has_raw_betel_nut")
                && manager.get(BetelNutMod.id("skyblock/first_compost")).value().criteria().containsKey("has_bone_meal")
                && manager.get(BetelNutMod.id("skyblock/enter_end")).value().criteria().containsKey("entered_the_end"),
                "Old criterion names preserve saved completion");
        for (String name : new String[]{"eat_first_betel_nut", "get_first_residue", "addiction_started", "withdrawal_started", "eat_hecheng_tianxia"})
            h.assertTrue(manager.get(BetelNutMod.id("skyblock/" + name)).value().display().isEmpty(), "Legacy quest helpers remain available");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void collectionAccumulatesWithoutEatingOrKeepingEveryItem(GameTestHelper h) {
        var p = player(h);
        try {
            obtain(p, ModItems.SPICY_BETEL_NUT);
            h.assertTrue(!done(p, "processing/basic_flavors"), "One flavor is insufficient");
            obtain(p, ModItems.SWEET_BETEL_NUT);
            obtain(p, ModItems.HONEY_BETEL_NUT);
            h.assertTrue(done(p, "processing/basic_flavors"), "Distinct acquisitions accumulate even after inventory is cleared");
            var holder = advancement(p, "processing/all_flavors");
            for (String name : holder.value().criteria().keySet())
                obtain(p, net.minecraft.core.registries.BuiltInRegistries.ITEM.get(BetelNutMod.id(name)));
            h.assertTrue(done(p, "processing/all_flavors") && holder.value().criteria().size() == 17,
                    "All seventeen variants are tracked by actual inventory criteria");
            h.assertTrue(BetelNutEntityComponents.ADDICTION.get(p).getAddictionValue() == 0, "Collection requires no eating");
        } finally { p.server.getPlayerList().remove(p); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public void obtainingASaplingDoesNotCountAsPlanting(GameTestHelper h) {
        var p = player(h); var pos = h.absolutePos(new BlockPos(2, 2, 2)); var level = h.getLevel();
        try {
            obtain(p, ModBlocks.BETEL_PALM_SAPLING_ITEM);
            h.assertTrue(done(p, "root") && done(p, "cultivation/sapling") && !done(p, "cultivation/plant"), "Inventory and placement are distinct");
            level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), 3);
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            p.teleportTo(pos.getX() + 2, pos.getY(), pos.getZ() + 2);
            p.getMainHandItem().useOn(new UseOnContext(p, InteractionHand.MAIN_HAND, hit(pos.below())));
            h.assertTrue(level.getBlockState(pos).is(ModBlocks.BETEL_PALM_SAPLING) && done(p, "cultivation/plant"), "A real successful placement awards planting");
        } finally { p.server.getPlayerList().remove(p); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public void harvestRequiresShearsAndTracksRipeness(GameTestHelper h) {
        var p = player(h); var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(2, 2, 2));
        try {
            obtain(p, ModBlocks.BETEL_NUT_CLUSTER_ITEM);
            h.assertTrue(!done(p, "cultivation/harvest"), "Owning a bunch does not count as harvesting");
            level.setBlock(pos.south(), ModBlocks.BETEL_PALM_LOG.defaultBlockState(), 3);
            for (int age = 1; age <= 2; age++) {
                var state = ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.AGE, age);
                level.setBlock(pos, state, 3);
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHEARS));
                state.useItemOn(p.getOffhandItem(), level, p, InteractionHand.OFF_HAND, hit(pos));
                h.assertTrue(done(p, "cultivation/harvest") && done(p, "cultivation/ripe_harvest") == (age == 2), "Only ripe fruit awards the ripe milestone");
                int xp = p.totalExperience;
                state.useItemOn(p.getOffhandItem(), level, p, InteractionHand.OFF_HAND, hit(pos));
                h.assertTrue(p.totalExperience == xp, "Stale interactions cannot repeat advancement rewards");
            }
        } finally { p.server.getPlayerList().remove(p); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public void boneMealOwnershipCannotCompleteComposting(GameTestHelper h) {
        var p = player(h); var level = h.getLevel(); var pos = h.absolutePos(new BlockPos(2, 2, 2));
        try {
            obtain(p, Items.BONE_MEAL);
            h.assertTrue(!done(p, "cultivation/compost") && !done(p, "skyblock/first_compost"), "Starter, traded and looted bone meal cannot complete composting");
            level.setBlock(pos, Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, 7), 3);
            level.getBlockState(pos).useWithoutItem(level, p, hit(pos));
            h.assertTrue(!done(p, "cultivation/compost"), "Unready compost cannot complete the milestone");
            level.setBlock(pos, Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, 8), 3);
            level.getBlockState(pos).useWithoutItem(level, p, hit(pos));
            h.assertTrue(done(p, "cultivation/compost") && !done(p, "skyblock/first_compost")
                    && level.getBlockState(pos).getValue(ComposterBlock.LEVEL) == 0, "Actual extraction awards only the ordinary-world milestone");
        } finally { p.server.getPlayerList().remove(p); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public void owningAPackDoesNotCountAsOpeningIt(GameTestHelper h) {
        var p = player(h);
        try {
            obtain(p, ModItems.KOUWEIWANG_PACK);
            h.assertTrue(!done(p, "trade/open_pack"), "An unopened pack is insufficient");
            p.getMainHandItem().use(p.level(), p, InteractionHand.MAIN_HAND);
            h.assertTrue(done(p, "trade/open_pack") && !done(p, "trade/seal"), "Opening awards its milestone independently of buying a seal");
        } finally { p.server.getPlayerList().remove(p); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public void sealRequiresARealCompletedMerchantTransaction(GameTestHelper h) {
        var p = player(h);
        try {
            obtain(p, ModItems.BRAND_SEAL);
            h.assertTrue(!done(p, "trade/seal"), "Found or crafted seals do not count as trading");
            var entry = ModVillagerTrades.catalog().stream().filter(e -> e.createOffer(new BetelNutConfig()).getResult().is(ModItems.BRAND_SEAL)).findFirst().orElseThrow();
            var offer = entry.createOffer(new BetelNutConfig());
            var v = new Villager(EntityType.VILLAGER, h.getLevel());
            var offers = new MerchantOffers(); offers.add(offer); v.setOffers(offers); v.setTradingPlayer(p);
            var menu = new MerchantMenu(1, p.getInventory(), v); menu.setSelectionHint(0);
            menu.getSlot(0).set(offer.getCostA().copy()); menu.getSlot(1).set(offer.getCostB().copy());
            h.assertTrue(!done(p, "trade/seal"), "Preparing payment is insufficient");
            menu.quickMoveStack(p, 2);
            h.assertTrue(done(p, "trade/seal") && offer.getUses() == 1, "Taking the paid output awards the trade milestone");
            menu.removed(p);
        } finally { p.server.getPlayerList().remove(p); }
        h.succeed();
    }

    @GameTest(template = "empty")
    public void recoveryRequiresAnActualImprovement(GameTestHelper h) {
        var p = player(h); var data = BetelNutEntityComponents.ADDICTION.get(p);
        try {
            obtain(p, ModItems.RELIEF_POTION);
            p.getMainHandItem().finishUsingItem(p.level(), p);
            h.assertTrue(done(p, "recovery/supplies") && !done(p, "recovery/relief"), "Healthy players cannot earn recovery just by holding or attempting a remedy");
            data.setAddictionValue(3); data.setOralExposureDays(2);
            new ItemStack(ModItems.RELIEF_POTION).finishUsingItem(p.level(), p);
            h.assertTrue(done(p, "recovery/relief") && !done(p, "recovery/recovered"), "Partial improvement awards only relief");
            new ItemStack(ModItems.ENCHANTED_GOLDEN_APPLE_PASTE).finishUsingItem(p.level(), p);
            h.assertTrue(done(p, "recovery/recovered") && data.getAddictionValue() == 0 && data.getOralExposureDays() == 0, "Effective recovery to zero awards a fresh start");
        } finally { p.server.getPlayerList().remove(p); }
        h.succeed();
    }
}
