package betel.nut.platform;
import java.util.*;
import net.neoforged.neoforge.event.furnace.FurnaceFuelBurnTimeEvent;
import java.util.function.*;
import betel.nut.component.BetelNutEntityComponents;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.*;
/** Small loader boundary: callbacks keep the existing server-authoritative gameplay rules. */
public final class BetelPlatform {
 @FunctionalInterface public interface Join { void accept(ServerGamePacketListenerImpl handler, Object sender, MinecraftServer server); }
 @FunctionalInterface public interface Respawn { void accept(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive); }
 @FunctionalInterface public interface Attack { InteractionResult accept(Player p, Level l, InteractionHand h, Entity e, EntityHitResult hit); }
 @FunctionalInterface public interface UseItem { InteractionResultHolder<ItemStack> accept(Player p, Level l, InteractionHand h); }
 @FunctionalInterface public interface UseBlock { InteractionResult accept(Player p, Level l, InteractionHand h, BlockHitResult hit); }
 @FunctionalInterface public interface Dimensions { void accept(ServerPlayer p, ServerLevel from, ServerLevel to); }
 @FunctionalInterface public interface CommandsCallback { void accept(CommandDispatcher<CommandSourceStack> d, CommandBuildContext c, Commands.CommandSelection s); }
 public static void onServerTick(Consumer<MinecraftServer> c) { NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> c.accept(e.getServer())); }
 public static void onServerTickStart(Consumer<MinecraftServer> c) { NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre e) -> c.accept(e.getServer())); }
 public static void onWorldTick(Consumer<ServerLevel> c) { NeoForge.EVENT_BUS.addListener((LevelTickEvent.Post e) -> { if(e.getLevel() instanceof ServerLevel l) c.accept(l); }); }
 public static void onServerStarting(Consumer<MinecraftServer> c) { NeoForge.EVENT_BUS.addListener((ServerStartingEvent e) -> c.accept(e.getServer())); }
 public static void onServerStarted(Consumer<MinecraftServer> c) { NeoForge.EVENT_BUS.addListener((ServerStartedEvent e) -> c.accept(e.getServer())); }
 public static void onServerStopped(Consumer<MinecraftServer> c) { NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> c.accept(e.getServer())); }
 public static void onWorldLoad(BiConsumer<MinecraftServer,ServerLevel> c) { NeoForge.EVENT_BUS.addListener((LevelEvent.Load e) -> { if(e.getLevel() instanceof ServerLevel l) c.accept(l.getServer(),l); }); }
 public static void onJoin(Join c) { NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent e) -> { if(e.getEntity() instanceof ServerPlayer p)c.accept(p.connection,null,p.server); }); }
 public static void onEntityLoad(BiConsumer<Entity,ServerLevel> c) { NeoForge.EVENT_BUS.addListener((EntityJoinLevelEvent e) -> { if(e.getLevel() instanceof ServerLevel l)c.accept(e.getEntity(),l); }); }
 public static void onRespawn(Respawn c) { NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent e) -> { if(e.getEntity() instanceof ServerPlayer p)c.accept(p,p,e.isEndConquered()); }); }
 public static void onDimensionChange(Dimensions c) { NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent e) -> { if(e.getEntity() instanceof ServerPlayer p)c.accept(p,p.server.getLevel(e.getFrom()),p.server.getLevel(e.getTo())); }); }
 public static void onAttack(Attack c) { NeoForge.EVENT_BUS.addListener((AttackEntityEvent e) -> { if(c.accept(e.getEntity(),e.getEntity().level(),InteractionHand.MAIN_HAND,e.getTarget(),null)==InteractionResult.FAIL)e.setCanceled(true); }); }
 public static void onUseItem(UseItem c) { NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickItem e) -> { var r=c.accept(e.getEntity(),e.getLevel(),e.getHand()); if(r.getResult()!=InteractionResult.PASS){e.setCanceled(true);e.setCancellationResult(r.getResult());} }); }
 public static void onUseBlock(UseBlock c) { NeoForge.EVENT_BUS.addListener((PlayerInteractEvent.RightClickBlock e) -> { var r=c.accept(e.getEntity(),e.getLevel(),e.getHand(),e.getHitVec());if(r!=InteractionResult.PASS){e.setCanceled(true);e.setCancellationResult(r);} }); }
 public static void onCommands(CommandsCallback c) { NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> c.accept(e.getDispatcher(),e.getBuildContext(),e.getCommandSelection())); }
 private static final Map<ResourceKey<CreativeModeTab>, List<Consumer<CreativeModeTab.Output>>> CREATIVE = new HashMap<>();
 public static void creative(ResourceKey<CreativeModeTab> key, Consumer<CreativeModeTab.Output> c) { CREATIVE.computeIfAbsent(key,k->new ArrayList<>()).add(c); }
 public static void creativeContents(BuildCreativeModeTabContentsEvent e) { CREATIVE.getOrDefault(e.getTabKey(),List.of()).forEach(c->c.accept(e)); }
 private static final Map<Item,Integer> FUELS = new IdentityHashMap<>();
 public static void fuel(Item item, int ticks) { FUELS.put(item,ticks); }
 public record Flammability(Block block,int encouragement,int flammability) {}
 public static final class FireEntries {
  private final List<Flammability> entries = new ArrayList<>();
  public void add(Block b,int e,int f) { entries.add(new Flammability(b,e,f)); }
 }
 public static final FireEntries FLAMMABILITY = new FireEntries();
 public static void setupFlammability() { var fire=(FireBlock)Blocks.FIRE; FLAMMABILITY.entries.forEach(e->fire.setFlammable(e.block,e.encouragement,e.flammability)); }
 public static void initialize() {
  NeoForge.EVENT_BUS.addListener((FurnaceFuelBurnTimeEvent e)->{var ticks=FUELS.get(e.getItemStack().getItem());if(ticks!=null)e.setBurnTime(ticks);});
  NeoForge.EVENT_BUS.addListener((PlayerEvent.Clone e)->{
   var from=e.getOriginal();var to=e.getEntity();
   BetelNutEntityComponents.ADDICTION.get(to).copyForRespawn(BetelNutEntityComponents.ADDICTION.get(from),!e.isWasDeath());
   BetelNutEntityComponents.SKYBLOCK_PLAYER.get(to).copyFrom(BetelNutEntityComponents.SKYBLOCK_PLAYER.get(from),to.registryAccess());
  });
 }
 private BetelPlatform() {}
}
