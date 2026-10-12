package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.block.ModBlocks;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Captures the unmodified natural terrain created by the dedicated-server probe. */
public class GrovePreviewClient implements ClientModInitializer {
    private int ticks;
    private boolean prepared;
    private volatile RuntimeException failure;
    private volatile List<BlockPos> roots;
    private volatile Vec3 target;
    private volatile Vec3 secondCamera;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (failure != null) throw failure;
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) return;
            if (!prepared) {
                prepared = true;
                client.options.pauseOnLostFocus = false;
                client.options.hideGui = true;
                client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                client.setScreen(null);
                server(client, () -> prepare(client));
            }
            ticks++;
            client.getToasts().clear();
            if (client.screen != null) client.setScreen(null);
            if (ticks == 160) {
                for (var root : roots) if (!client.level.getBlockState(root).is(ModBlocks.BETEL_PALM_LOG))
                    throw new IllegalStateException("Natural trunk missing from client: " + root);
                BetelNutMod.LOGGER.info("GROVE_CLIENT_NATURAL_WORLD_VERIFIED roots={}", roots.size());
                capture(client, "grove-natural-overview.png");
            }
            if (ticks == 180) server(client, () -> camera(client, secondCamera, target));
            if (ticks == 230) capture(client, "grove-natural-side.png");
            if (ticks == 250) client.stop();
        });
    }

    private void prepare(Minecraft client) {
        try {
            var data = JsonParser.parseString(Files.readString(Path.of("../worldgen/natural-world-report.json"))).getAsJsonObject();
            var array = data.getAsJsonArray("samples").get(0).getAsJsonObject().getAsJsonArray("roots");
            List<BlockPos> all = new ArrayList<>();
            for (var item : array) {
                var coordinates = item.getAsJsonArray();
                all.add(new BlockPos(coordinates.get(0).getAsInt(), coordinates.get(1).getAsInt(), coordinates.get(2).getAsInt()));
            }
            if (all.isEmpty()) throw new IllegalStateException("No naturally generated palms to preview");
            roots = List.of();
            for (var center : all) {
                var group = all.stream().filter(pos -> {
                    int dx=pos.getX()-center.getX(),dz=pos.getZ()-center.getZ();return dx*dx+dz*dz<=18*18;
                }).toList();
                if (group.size()>roots.size()) roots=group;
            }
            var server = client.getSingleplayerServer();
            var level = server.overworld();
            level.setDayTime(6000);
            level.setWeatherParameters(0,12000,false,false);
            level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,server);
            var player=server.getPlayerList().getPlayers().getFirst();
            player.setGameMode(GameType.CREATIVE);
            player.getAbilities().flying=true;
            player.onUpdateAbilities();
            List<Vec3> crowns = roots.stream().map(root -> {
                int height = 0;
                while (level.getBlockState(root.above(height)).is(ModBlocks.BETEL_PALM_LOG)) height++;
                return new Vec3(root.getX()+.5, root.getY()+height, root.getZ()+.5);
            }).toList();
            target = new Vec3(crowns.stream().mapToDouble(v -> v.x).average().orElseThrow(),
                    crowns.stream().mapToDouble(v -> v.y).average().orElseThrow(),
                    crowns.stream().mapToDouble(v -> v.z).average().orElseThrow());
            Vec3 firstCamera = chooseCamera(client, crowns, -1);
            double firstAngle = Math.atan2(firstCamera.z-target.z,firstCamera.x-target.x);
            secondCamera = chooseCamera(client, crowns, firstAngle);
            camera(client,firstCamera,target);
            BetelNutMod.LOGGER.info("GROVE_NATURAL_PREVIEW_TARGET {} roots={}",target,roots);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private Vec3 chooseCamera(Minecraft client, List<Vec3> crowns, double previousAngle) {
        var level = client.getSingleplayerServer().overworld();
        var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        Vec3 best = null;
        int bestScore = -1;
        for (int i = 0; i < 8; i++) {
            double angle = i*Math.PI/4;
            if (previousAngle != -1 && Math.cos(angle-previousAngle) > .1) continue;
            Vec3 position = target.add(Math.cos(angle)*34,28,Math.sin(angle)*34);
            int ground = level.getHeight(Heightmap.Types.WORLD_SURFACE,(int)Math.floor(position.x),(int)Math.floor(position.z));
            position = new Vec3(position.x,Math.max(position.y,ground+8),position.z);
            int visible = 0;
            for (Vec3 crown : crowns) {
                var hit = level.clip(new ClipContext(position.add(0,player.getEyeHeight(),0),crown,
                        ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,player));
                if (hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(crown) < 25) visible++;
            }
            if (visible > bestScore) { bestScore = visible; best = position; }
        }
        if (best == null) throw new IllegalStateException("No camera position found");
        BetelNutMod.LOGGER.info("GROVE_CAMERA_SELECTED visibleCrowns={} position={}",bestScore,best);
        return best;
    }

    private static void camera(Minecraft client, Vec3 position, Vec3 target) {
        var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        var direction=target.subtract(position.add(0,player.getEyeHeight(),0));
        float yaw=(float)Math.toDegrees(Math.atan2(-direction.x,direction.z));
        float pitch=(float)-Math.toDegrees(Math.atan2(direction.y,Math.sqrt(direction.x*direction.x+direction.z*direction.z)));
        player.teleportTo(player.serverLevel(),position.x,position.y,position.z,Set.of(),yaw,pitch);
    }

    private void server(Minecraft client,Runnable task) {
        client.getSingleplayerServer().execute(()->{try{task.run();}catch(Exception e){failure=new RuntimeException("Natural grove preview failed",e);}});
    }

    private static void capture(Minecraft client,String name) {
        Screenshot.grab(client.gameDirectory,name,client.getMainRenderTarget(),message->BetelNutMod.LOGGER.info("GROVE_PREVIEW {} {}",name,message.getString()));
    }
}
