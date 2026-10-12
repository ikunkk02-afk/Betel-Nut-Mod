package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.BetelPalmLogBlock;
import betel.nut.block.ModBlocks;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.component.BetelNutWorldComponents;
import betel.nut.item.ModItems;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.inventory.BookViewScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class SkyblockPreviewClient implements ClientModInitializer {
    private int phase, tick;
    private volatile RuntimeException failure;
    private final boolean guideOnly = Boolean.getBoolean("betel.skyblock.guideOnly");
    private final boolean mobOnly = Boolean.getBoolean("betel.skyblock.mobOnly");
    private final List<List<Component>> guidePages = new ArrayList<>();
    private static final BlockPos CENTER = new BlockPos(0,128,0), COMPOST = CENTER.east().above();

    @Override public void onInitializeClient() {
        if (!guideOnly && !mobOnly) { seed("SkyblockClassic","betel_skyblock"); seed("SkyblockOneBlock","betel_one_block_skyblock"); }
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (failure != null) throw failure;
            if (c.player == null || c.level == null || c.getSingleplayerServer() == null) return;
            tick++; c.getToasts().clear();
            c.options.pauseOnLostFocus = false;
            if (tick == 1) { c.options.guiScale().set(2); c.resizeDisplay(); c.setScreen(null); }
            if (guideOnly) { guide(c); return; }
            if (mobOnly) { mobs(c); return; }
            if (phase == 0) classic(c);
            else if (phase == 1) reloadClassic(c);
            else if (phase == 2) oneBlock(c);
            else reloadOneBlock(c);
        });
    }

    private void mobs(Minecraft c) {
        var center = CENTER.east(64);
        if (tick == 10) server(c, p -> {
            var level = p.serverLevel();
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
            level.setDayTime(18000);
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-16, 0, -16), center.offset(16, 4, 16)))
                level.setBlock(pos, pos.getY() == 128 || pos.getY() == 132
                        ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(CENTER.east(16), Blocks.STONE.defaultBlockState(), 3);
            p.teleportTo(16.5, 129, .5); p.setInvulnerable(true);
        });
        if (tick == 30) server(c, p -> {
            var level = p.serverLevel();
            var bounds = new AABB(center).inflate(22, 5, 22);
            for (int attempt = 0; attempt < 200 && level.getEntitiesOfClass(
                    net.minecraft.world.entity.monster.Monster.class, bounds).isEmpty(); attempt++)
                net.minecraft.world.level.NaturalSpawner.spawnCategoryForPosition(
                        net.minecraft.world.entity.MobCategory.MONSTER, level, center.above());
            var spawned = level.getEntitiesOfClass(net.minecraft.world.entity.monster.Monster.class, bounds);
            require(!spawned.isEmpty(), "Vanilla natural spawning creates real monsters in the void world");
            BetelNutMod.LOGGER.info("SKYBLOCK_NATURAL_MOBS_PASS mode={} count={} types={}",
                    phase == 0 ? "classic" : "one_block", spawned.size(),
                    spawned.stream().map(entity -> entity.getType().toShortString()).toList());
            p.teleportTo(level, 49.5, 129, .5, Set.of(), -90, 0);
        });
        if (tick == 40) capture(c, "natural-mobs-" + (phase == 0 ? "classic" : "one-block") + ".png");
        if (tick == 50) {
            if (phase == 0) switchWorld(c, 1, "SkyblockOneBlock");
            else { BetelNutMod.LOGGER.info("SKYBLOCK_MOBS_ALL_CHECKS_PASS"); c.stop(); }
        }
    }

    private void guide(Minecraft c) {
        if (tick == 10) {
            boolean fits = true;
            for (String language : List.of("en_us", "zh_cn")) {
                try (var reader = new InputStreamReader(SkyblockPreviewClient.class.getResourceAsStream(
                        "/assets/betel-nut-mod/lang/" + language + ".json"), java.nio.charset.StandardCharsets.UTF_8)) {
                    var translations = JsonParser.parseReader(reader).getAsJsonObject();
                    List<Component> pages = new ArrayList<>();
                    for (int page = 1; page <= 9; page++) {
                        var text = Component.literal(translations.get("book.betel-nut-mod.skyblock_guide.page" + page).getAsString());
                        int lines = c.font.split(text, 114).size();
                        BetelNutMod.LOGGER.info("SKYBLOCK_GUIDE_LAYOUT language={} page={} lines={} height={}", language, page, lines, lines * 9);
                        fits &= lines * 9 <= 128;
                        pages.add(text);
                    }
                    guidePages.add(pages);
                } catch (Exception e) { throw new RuntimeException("Guide verification failed", e); }
            }
            require(fits, "Every guide page fits the actual 114 by 128 book text area");
        }
        int sample = (tick - 20) / 5;
        if (tick >= 20 && sample < 18) {
            if ((tick - 20) % 5 == 0) {
                var screen = new BookViewScreen(new BookViewScreen.BookAccess(guidePages.get(sample / 9)));
                c.setScreen(screen); screen.setPage(sample % 9);
            }
            if ((tick - 20) % 5 == 3) capture(c, "guide-" + (sample < 9 ? "en_us" : "zh_cn") + "-page" + (sample % 9 + 1) + ".png");
        }
        if (tick == 115) { BetelNutMod.LOGGER.info("SKYBLOCK_GUIDE_ALL_CHECKS_PASS"); c.stop(); }
    }

    private static void seed(String name, String preset) {
        try {
            var save = Minecraft.getInstance().gameDirectory.toPath().resolve("saves").resolve(name);
            if (Files.exists(save.resolve("data/cardinal_world_components.dat"))) return;
            var path = save.resolve("level.dat");
            CompoundTag root = NbtIo.readCompressed(path,NbtAccounter.unlimitedHeap());
            CompoundTag data = root.getCompound("Data");
            try (var reader = new InputStreamReader(SkyblockPreviewClient.class.getResourceAsStream(
                    "/data/betel-nut-mod/worldgen/world_preset/" + preset + ".json"), java.nio.charset.StandardCharsets.UTF_8)) {
                var json = JsonParser.parseReader(reader).getAsJsonObject();
                data.getCompound("WorldGenSettings").put("dimensions", JsonOps.INSTANCE.convertTo(NbtOps.INSTANCE,json.get("dimensions")));
            }
            data.remove("Player"); data.putInt("GameType",0); data.putBoolean("allowCommands",true);
            data.putInt("SpawnX",0); data.putInt("SpawnY",132); data.putInt("SpawnZ",0); data.putLong("DayTime",6000);
            data.getCompound("GameRules").putString("doMobSpawning","false");
            data.getCompound("GameRules").putString("doDaylightCycle","false");
            data.getCompound("GameRules").putString("spawnRadius","0");
            NbtIo.writeCompressed(root,path);
        } catch (Exception e) { throw new RuntimeException("Unable to seed isolated skyblock test world",e); }
    }

    private void classic(Minecraft c) {
        if (tick == 20) server(c,p -> {
            var world = BetelNutWorldComponents.SKYBLOCK_WORLD.get(p.serverLevel());
            require(world.hasGeneratedBetelSkyIsland() && !world.hasGeneratedBetelOneBlockIsland(),"Correct classic preset initializes automatically");
            var chest = (ChestBlockEntity)p.serverLevel().getBlockEntity(CENTER.offset(2,1,0));
            require(chest.countItem(Items.SHEARS)==1 && chest.countItem(Items.ICE)==2 && chest.countItem(Items.BREAD)==8,"Client-world chest has new survival resources");
            p.openMenu(chest);
            BetelNutMod.LOGGER.info("SKYBLOCK_CLASSIC_START_PASS");
        });
        if (tick == 35) capture(c,"classic-starter-chest.png");
        if (tick == 45) { c.player.closeContainer(); server(c,p -> {
            ((ChestBlockEntity)p.serverLevel().getBlockEntity(CENTER.offset(2,1,0))).setItem(0,new ItemStack(Items.DIAMOND,7));
            for (int y=0;y<27;y++) p.serverLevel().setBlock(CENTER.above(1+y),Blocks.GLASS.defaultBlockState(),3);
            p.teleportTo(p.serverLevel(),.5,-20,.5,Set.of(),0,0);
        }); }
        if (tick == 65) server(c,p -> {
            require(p.getY()>100 && (p.getX()!=.5 || p.getZ()!=.5),"Actual server tick rescues away from an obstructed spawn");
            require(p.serverLevel().getBlockState(CENTER.above(4)).is(Blocks.GLASS),"Rescue preserves obstruction");
            BetelNutMod.LOGGER.info("SKYBLOCK_CLASSIC_RESCUE_PASS");
        });
        if (tick == 75) switchWorld(c,1,"SkyblockClassic");
    }

    private void reloadClassic(Minecraft c) {
        if (tick == 20) server(c,p -> {
            var chest = (ChestBlockEntity)p.serverLevel().getBlockEntity(CENTER.offset(2,1,0));
            require(chest.getItem(0).is(Items.DIAMOND) && chest.getItem(0).getCount()==7,"Classic chest contents survive restart");
            require(p.serverLevel().getBlockState(CENTER.above(4)).is(Blocks.GLASS),"Classic terrain edits survive restart");
            require(BetelNutWorldComponents.SKYBLOCK_WORLD.get(p.serverLevel()).hasSurvivalSuppliesProvided(),"Upgrade flag persists");
            BetelNutMod.LOGGER.info("SKYBLOCK_CLASSIC_RELOAD_PASS");
        });
        if (tick == 30) switchWorld(c,2,"SkyblockOneBlock");
    }

    private void oneBlock(Minecraft c) {
        if (tick == 20) server(c,p -> {
            var level = p.serverLevel(); var world = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
            require(world.hasGeneratedBetelOneBlockIsland() && !world.hasGeneratedBetelSkyIsland(),"Correct one-block preset initializes automatically");
            require(BlockPos.betweenClosedStream(CENTER.offset(-2,-1,-2),CENTER.offset(2,5,2)).filter(b -> !level.getBlockState(b).isAir()).count()==1,"Exactly one initial world block");
            require(p.getInventory().countItem(Items.SHEARS)==1 && p.getInventory().countItem(Items.ICE)==2
                    && p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE)==18,"One-block player receives new kit once");
            BetelNutMod.LOGGER.info("SKYBLOCK_ONE_BLOCK_START_PASS");
        });
        if (tick == 25) c.setScreen(new InventoryScreen(c.player));
        if (tick == 30) capture(c,"one-block-survival-kit.png");
        if (tick == 40) { c.setScreen(null); server(c,this::harvest); }
        if (tick == 55) server(c,p -> {
            p.serverLevel().setBlock(COMPOST,Blocks.COMPOSTER.defaultBlockState(),3);
            p.teleportTo(p.serverLevel(),.5,129,.5,Set.of(),-90,30);
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.BETEL_LEAF,4));
            p.inventoryMenu.broadcastChanges();
        });
        if (tick == 70) c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(COMPOST),Direction.UP,COMPOST,false));
        if (tick == 80) server(c,p -> {
            require(p.serverLevel().getBlockState(COMPOST).getValue(ComposterBlock.LEVEL)==1
                    && p.getMainHandItem().getCount()==3,"Real use-on packet composts a leaf and consumes one");
            BetelNutMod.LOGGER.info("SKYBLOCK_COMPOST_PACKET_PASS");
        });
        if (tick == 90) server(c,p -> {
            for (BlockPos pos : BlockPos.betweenClosed(CENTER.offset(-18,-16,-18),CENTER.offset(18,24,18)))
                p.serverLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),2);
            p.teleportTo(p.serverLevel(),.5,-20,.5,Set.of(),0,0);
        });
        if (tick == 110) server(c,p -> {
            require(p.getY()>100 && p.serverLevel().getBlockState(p.blockPosition().below()).is(Blocks.BEDROCK),"All terrain removed: tick rescue creates safe bedrock foothold");
            BetelNutMod.LOGGER.info("SKYBLOCK_EMERGENCY_RESCUE_PASS");
        });
        if (tick == 115) capture(c,"one-block-emergency-rescue.png");
        if (tick == 125) switchWorld(c,3,"SkyblockOneBlock");
    }

    private void harvest(ServerPlayer p) {
        var level = p.serverLevel();
        // Controlled tree fixture verifies the actual new-mode harvest and regrowth behavior.
        BlockPos base = CENTER.east(5).above(); level.setBlock(base.below(),Blocks.DIRT.defaultBlockState(),3);
        require(BetelPalmTreeGenerator.generateVariant(level,base,RandomSource.create(7),9,9,0,false),"Tree grows in the actual void world");
        BlockPos node = base.above(6); var random = RandomSource.create(17);
        for(int i=0;i<500;i++) level.getBlockState(node).randomTick(level,node,random);
        BlockPos fruit = node.north();
        for(Direction d:Direction.Plane.HORIZONTAL) if(level.getBlockState(node.relative(d)).is(ModBlocks.BETEL_NUT_CLUSTER)) {fruit=node.relative(d);break;}
        for(int i=0;i<100;i++) level.getBlockState(fruit).randomTick(level,fruit,random);
        var old = level.getBlockState(fruit); require(old.is(ModBlocks.BETEL_NUT_CLUSTER) && old.getValue(BetelNutClusterBlock.AGE)==2,"Renewed fruit ripens");
        var shears = new ItemStack(Items.SHEARS); p.setItemInHand(InteractionHand.OFF_HAND,shears);
        old.useItemOn(shears,level,p,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(fruit),Direction.UP,fruit,false));
        old.useItemOn(shears,level,p,InteractionHand.OFF_HAND,new BlockHitResult(Vec3.atCenterOf(fruit),Direction.UP,fruit,false));
        require(level.getBlockState(fruit).isAir() && shears.getDamageValue()==1,"Shearing removes the bunch once");
        require(level.getEntitiesOfClass(ItemEntity.class,new AABB(fruit).inflate(2),e -> e.getItem().is(ModBlocks.BETEL_NUT_CLUSTER_ITEM)).stream().mapToInt(e -> e.getItem().getCount()).sum()==1,"One whole bunch, no duplicated harvest");
        BetelNutMod.LOGGER.info("SKYBLOCK_TREE_RENEWAL_HARVEST_PASS");
    }

    private void reloadOneBlock(Minecraft c) {
        if (tick == 20) server(c,p -> {
            require(BetelNutEntityComponents.SKYBLOCK_PLAYER.get(p).hasReceivedBetelOneBlockStarterItems(),"Personal first-join flag survives reload");
            require(p.getInventory().countItem(Items.ICE)==2 && p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE)==18,"Reload does not issue another kit");
            require(p.serverLevel().getBlockState(p.blockPosition().below()).is(Blocks.BEDROCK),"Rescue platform persists without restoring original terrain");
            BetelNutMod.LOGGER.info("SKYBLOCK_CLIENT_ALL_CHECKS_PASS");
        });
        if (tick == 30) c.stop();
    }

    private void switchWorld(Minecraft c,int next,String name) {
        c.level.disconnect(); c.disconnect(); phase=next; tick=0; c.createWorldOpenFlows().openWorld(name,() -> {});
    }
    private void server(Minecraft c,java.util.function.Consumer<ServerPlayer> action) {
        c.getSingleplayerServer().execute(() -> {
            try { action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()); }
            catch(Exception e) { failure = new RuntimeException("Skyblock client verification failed",e); }
        });
    }
    private static void require(boolean good,String message) { if(!good) throw new IllegalStateException(message); }
    private static void capture(Minecraft c,String name) {
        Screenshot.grab(c.gameDirectory,name,c.getMainRenderTarget(),msg -> BetelNutMod.LOGGER.info("SKYBLOCK_SCREENSHOT {}",name));
    }
}
