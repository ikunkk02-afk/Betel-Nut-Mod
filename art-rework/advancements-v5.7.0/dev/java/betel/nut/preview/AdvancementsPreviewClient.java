package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.ModBlocks;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.ModItems;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashSet;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.advancements.AdvancementTab;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class AdvancementsPreviewClient implements ClientModInitializer {
    private int tick, phase;
    private volatile RuntimeException failure;
    private volatile boolean languageReady = true;

    @Override public void onInitializeClient() {
        // The copied level.dat belongs to a test player with an old inventory and
        // XP. Clear that fixture data before loading our advancement snapshot.
        try {
            var path = Minecraft.getInstance().gameDirectory.toPath().resolve("saves/AdvancementsPreview/level.dat");
            var data = net.minecraft.nbt.NbtIo.readCompressed(path, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            var player = data.getCompound("Data").getCompound("Player");
            player.putInt("XpTotal", 0); player.putInt("XpLevel", 0); player.putFloat("XpP", 0);
            player.put("Inventory", new net.minecraft.nbt.ListTag());
            net.minecraft.nbt.NbtIo.writeCompressed(data, path);
        } catch (Exception e) { throw new RuntimeException("Cannot prepare isolated player fixture", e); }
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (failure != null) throw failure;
            if (c.player == null || c.level == null || c.getSingleplayerServer() == null
                    || !languageReady || c.getOverlay() != null) return;
            tick++; c.getToasts().clear(); c.options.pauseOnLostFocus = false;
            if (tick == 1) { c.options.guiScale().set(2); c.resizeDisplay(); c.setScreen(null); }
            if (phase == 0 && tick == 10) server(c, p -> {
                for (String id : new String[]{"root", "underground_betel", "skyblock/first_compost", "skyblock/enter_end"})
                    require(done(p, id), "Saved v5.6.0 completion survives: " + id);
                require(p.totalExperience == 0, "Existing challenge XP is not reissued on load");
                p.setInvulnerable(true);
                p.serverLevel().setBlock(new BlockPos(0, 139, 0), Blocks.STONE.defaultBlockState(), 3);
                p.teleportTo(0.5, 140, 0.5);
                BetelNutMod.LOGGER.info("ADVANCEMENT_LEGACY_COMPLETION_PASS");
                var level = p.serverLevel(); var pos = new BlockPos(4, 140, 4);
                level.setBlock(pos, Blocks.COMPOSTER.defaultBlockState().setValue(ComposterBlock.LEVEL, 8), 3);
                level.getBlockState(pos).useWithoutItem(level, p, hit(pos));
                require(done(p, "cultivation/compost"), "Actual composter extraction awards new node");
                level.setBlock(pos.south(), ModBlocks.BETEL_PALM_LOG.defaultBlockState(), 3);
                var bunch = ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.AGE, 2);
                level.setBlock(pos, bunch, 3);
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.SHEARS));
                bunch.useItemOn(p.getOffhandItem(), level, p, InteractionHand.OFF_HAND, hit(pos));
                require(done(p, "cultivation/ripe_harvest"), "Actual ripe harvest awards node");
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KOUWEIWANG_PACK));
                p.getMainHandItem().use(level, p, InteractionHand.MAIN_HAND);
                require(done(p, "trade/open_pack"), "Actual unpacking awards node");
                var health = BetelNutEntityComponents.ADDICTION.get(p);
                health.setAddictionValue(2); health.setOralExposureDays(1);
                new ItemStack(ModItems.ENCHANTED_GOLDEN_APPLE_PASTE).finishUsingItem(level, p);
                require(done(p, "recovery/recovered"), "Actual recovery awards node");
                BetelNutMod.LOGGER.info("ADVANCEMENT_REAL_CLIENT_EVENTS_PASS");
                p.getInventory().clearContent();
                // Reveal the complete graph for layout review, leaving the 17-item
                // collector partially complete so its counter can also be reviewed.
                for (var holder : p.server.getAdvancements().getAllAdvancements()) {
                    if (!holder.id().getNamespace().equals(BetelNutMod.MOD_ID) || holder.value().display().isEmpty()) continue;
                    if (holder.id().getPath().equals("processing/all_flavors")) {
                        for (String criterion : holder.value().criteria().keySet()) p.getAdvancements().revoke(holder, criterion);
                        for (String name : new String[]{"spicy_betel_nut", "sweet_betel_nut", "honey_betel_nut"})
                            p.getAdvancements().award(holder, name);
                    } else for (String criterion : holder.value().criteria().keySet()) p.getAdvancements().award(holder, criterion);
                }
                p.getAdvancements().save();
            });
            if (phase <= 1) {
                if (tick == 25) language(c, phase == 0 ? "zh_cn" : "en_us");
                if (tick == 35) show(c, "root");
                if (tick == 40) capture(c, "main-" + language() + "-center.png");
                if (tick == 45) selected(c).scroll(0, 10000);
                if (tick == 50) capture(c, "main-" + language() + "-top.png");
                if (tick == 55) selected(c).scroll(0, -10000);
                if (tick == 60) capture(c, "main-" + language() + "-bottom.png");
                if (tick == 65) show(c, "skyblock/enter_betel_skyblock");
                if (tick == 70) capture(c, "skyblock-" + language() + ".png");
                if (tick == 75) {
                    if (phase == 0) { phase = 1; tick = 20; }
                    else {
                        c.level.disconnect(); c.disconnect(); phase = 2; tick = 0;
                        c.createWorldOpenFlows().openWorld("AdvancementsPreview", () -> {});
                    }
                }
            } else {
                if (tick == 10) server(c, p -> {
                    require(done(p, "cultivation/ripe_harvest") && done(p, "cultivation/compost")
                            && done(p, "trade/open_pack") && done(p, "recovery/recovered"), "New completions survive reload");
                    var holder = p.server.getAdvancements().get(BetelNutMod.id("processing/all_flavors"));
                    var progress = p.getAdvancements().getOrStartProgress(holder);
                    int count = 0; for (String criterion : progress.getCompletedCriteria()) count++;
                    require(!progress.isDone() && count == 3, "Partial 3/17 collection survives reload");
                    BetelNutMod.LOGGER.info("ADVANCEMENT_RELOAD_AND_PARTIAL_PROGRESS_PASS");
                });
                if (tick == 20) { BetelNutMod.LOGGER.info("ADVANCEMENT_CLIENT_ALL_CHECKS_PASS"); c.stop(); }
            }
        });
    }
    private String language() { return phase == 0 ? "zh_cn" : "en_us"; }
    private void language(Minecraft c, String language) {
        languageReady = false; c.getLanguageManager().setSelected(language); c.options.languageCode = language;
        c.reloadResourcePacks().thenRun(() -> languageReady = true);
    }
    private static boolean done(ServerPlayer p, String id) {
        return p.getAdvancements().getOrStartProgress(p.server.getAdvancements().get(BetelNutMod.id(id))).isDone();
    }
    private static BlockHitResult hit(BlockPos pos) { return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false); }
    @SuppressWarnings("unchecked")
    private void show(Minecraft c, String root) {
        var data = c.player.connection.getAdvancements(); var screen = new AdvancementsScreen(data); c.setScreen(screen);
        screen.onSelectedTabChanged(data.get(BetelNutMod.id(root)));
        try {
            Field field = AdvancementsScreen.class.getDeclaredField("tabs"); field.setAccessible(true);
            var tabs = (Map<AdvancementHolder, AdvancementTab>) field.get(screen);
            for (var tab : tabs.values()) if (tab.getRootNode().holder().id().getNamespace().equals(BetelNutMod.MOD_ID)) {
                var coordinates = new HashSet<String>(); int count = 0;
                for (var node : data.getTree().nodes()) {
                    if (node.root() != tab.getRootNode() || node.advancement().display().isEmpty()) continue;
                    var display = node.advancement().display().orElseThrow();
                    require(coordinates.add(display.getX() + ":" + display.getY()), "Visible nodes never overlap");
                    require(screen.getAdvancementWidget(node) != null, "Visible node has a real rendered widget");
                    count++;
                }
                BetelNutMod.LOGGER.info("ADVANCEMENT_CLIENT_LAYOUT language={} root={} nodes={} overlap=false", language(), tab.getRootNode().holder().id(), count);
                require(count == (tab.getRootNode().holder().id().getPath().equals("root") ? 24 : 4), "Both tabs contain every visible node");
            }
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static AdvancementTab selected(Minecraft c) {
        try {
            Field field = AdvancementsScreen.class.getDeclaredField("selectedTab"); field.setAccessible(true);
            return (AdvancementTab) field.get(c.screen);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private void server(Minecraft c, java.util.function.Consumer<ServerPlayer> task) {
        c.getSingleplayerServer().execute(() -> {
            try { task.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()); }
            catch (Exception e) { failure = new RuntimeException("Advancement client verification failed", e); }
        });
    }
    private static void capture(Minecraft c, String name) {
        Screenshot.grab(c.gameDirectory, name, c.getMainRenderTarget(), msg -> BetelNutMod.LOGGER.info("ADVANCEMENT_SCREENSHOT {}", name));
    }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }
}
