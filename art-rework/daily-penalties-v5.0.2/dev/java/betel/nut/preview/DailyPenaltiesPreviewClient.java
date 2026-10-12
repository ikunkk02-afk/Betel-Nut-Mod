package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.client.BetelNutClientConfig;
import betel.nut.client.addiction.AddictionHudRenderer;
import betel.nut.client.addiction.ClientAddictionData;
import betel.nut.client.gui.HudEditScreen;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.effect.ModEffects;
import betel.nut.item.ModItems;
import betel.nut.network.AddictionSyncPayload;
import java.util.ArrayList;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

/** All seven meals and recoveries use actual client use-item packets. */
public final class DailyPenaltiesPreviewClient implements ClientModInitializer {
    private int tick;
    private volatile RuntimeException failure;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (failure != null) throw failure;
            if (c.player == null || c.level == null || c.getSingleplayerServer() == null) return;
            tick++; c.getToasts().clear(); c.gui.clear();
            if (tick == 1) {
                c.options.pauseOnLostFocus=false; c.options.hideGui=false;
                c.options.guiScale().set(3); c.resizeDisplay(); c.setScreen(null);
                var cfg=BetelNutClientConfig.get(); cfg.hudUseCustomPosition=false; cfg.hudScale=1;
                cfg.hudAnchor=BetelNutClientConfig.HudAnchor.BOTTOM_LEFT;
                server(c,p -> {
                    var w=p.serverLevel();w.setDayTime(240006000L);
                    w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);
                    w.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,p.server);
                    w.setWeatherParameters(0,12000,false,false);
                    p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.getInventory().clearContent();
                    p.teleportTo(w,3.5,80,-14,Set.of(),135,12);
                    var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);s.beginCalendarSession(p);
                    hold(p,ModItems.WRAPPED_RAW_BETEL,16);
                });
            }
            if(tick==30) startMeal(c);
            if(tick==70) c.options.keyUse.setDown(false);
            if(tick==75)server(c,p -> checkDay(p,1));
            if(tick==90){checkClient(c,1);capture(c,"day-1-no-symptoms.png");}
            for(int day=2;day<=7;day++) {
                int start=100+(day-2)*90;final int eatingDay=day;
                if(tick==start)server(c,p -> {
                    var w=p.server.overworld();
                    p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4),"time add 24000");
                    require(w.getDayTime()==240006000L+(eatingDay-1)*24000L,"actual time-add command advances one date");
                    p.getFoodData().setFoodLevel(6);
                });
                if(tick==start+10)startMeal(c);
                if(tick==start+50)c.options.keyUse.setDown(false);
                if(tick==start+55)server(c,p -> checkDay(p,eatingDay));
                if(tick==start+65){checkClient(c,day);capture(c,"day-"+day+"-symptoms.png");}
            }
            if(tick==630)c.setScreen(new InventoryScreen(c.player));
            if(tick==645)capture(c,"day-7-effects-inventory.png");
            if(tick==650){c.setScreen(null);server(c,p -> hold(p,Items.GOLDEN_APPLE,1));}
            if(tick==665)startMeal(c);
            if(tick==705)c.options.keyUse.setDown(false);
            if(tick==710)server(c,p -> {require(BetelNutEntityComponents.ADDICTION.get(p).getAddictionValue()==6,"real golden apple removes one day");checkEffect(p,4);});
            if(tick==725){checkClient(c,6);capture(c,"golden-apple-downgrade.png");}
            if(tick==735)server(c,p -> hold(p,Items.MILK_BUCKET,1));
            if(tick==750)startMeal(c);
            if(tick==790)c.options.keyUse.setDown(false);
            if(tick==795)server(c,p -> {checkEffect(p,4);require(p.getMainHandItem().is(Items.BUCKET),"milk packet really consumed the bucket");});
            if(tick==810){checkClient(c,6);capture(c,"milk-keeps-dependence.png");}
            if(tick==820)server(c,p -> hold(p,Items.ENCHANTED_GOLDEN_APPLE,1));
            if(tick==835)startMeal(c);
            if(tick==875)c.options.keyUse.setDown(false);
            if(tick==880)server(c,p -> {require(BetelNutEntityComponents.ADDICTION.get(p).getAddictionValue()==1,"enchanted recovery removes five days");checkEffect(p,0);});
            if(tick==895){checkClient(c,1);capture(c,"recovery-clears-symptoms.png");}
            if(tick==905)server(c,p -> {
                p.removeAllEffects();var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);
                var old=new CompoundTag();old.putInt("dailySchema",2);old.putInt("addictionValue",7);
                s.readFromNbt(old,p.registryAccess());s.beginCalendarSession(p);
                hold(p,ModItems.ANCIENT_SHELL,1);AddictionSyncPayload.send(p);
            });
            if(tick==930)server(c,p -> {checkEffect(p,5);require(BetelNutEntityComponents.ADDICTION.get(p).getOralExposureDays()==0,"old daily save receives no invented exposure");});
            if(tick==945){checkClient(c,7);capture(c,"old-save-seven-days.png");}
            if(tick==955){c.options.guiScale().set(5);c.resizeDisplay();c.setScreen(new HoverPreviewScreen());}
            if(tick==985)capture(c,"symptom-details-small-gui.png");
            if(tick==1000){c.screen.onClose();BetelNutMod.LOGGER.info("DAILY_PENALTIES_CLIENT_ALL_CHECKS_PASS sevenActualMeals=true milk=true recovery=true oldSave=true");c.stop();}
        });
    }
    private static void hold(ServerPlayer p, Item item, int count) {
        p.getFoodData().setFoodLevel(6);p.getInventory().selected=0;
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,count));p.inventoryMenu.broadcastChanges();
    }
    private static void startMeal(Minecraft c) {
        c.options.keyUse.setDown(true);
        c.gameMode.useItem(c.player,InteractionHand.MAIN_HAND);
    }
    private static void checkDay(ServerPlayer p,int day) {
        var s=BetelNutEntityComponents.ADDICTION.get(p);
        require(s.getAddictionValue()==day&&s.getOralExposureDays()==day&&s.hasEatenToday(),
                "real client food records exactly one new date: expected="+day+" actual="+s.getAddictionValue()+" exposure="+s.getOralExposureDays()+" used="+s.hasEatenToday()+" stack="+p.getMainHandItem().getCount());
        require(p.getMainHandItem().getCount()==16-day,"real client consumes exactly one item each date");
        require(s.getWithdrawalValue()==0&&s.getCleanTime()>p.level().getGameTime(),"symptoms appear while continuously eating, without withdrawal");
        int expected=new int[]{0,0,1,2,3,4,4,5}[day];checkEffect(p,expected);
        BetelNutMod.LOGGER.info("DAILY_PENALTIES_MEAL_PASS day={} symptomStage={} withdrawal=0",day,expected);
    }
    private static void checkEffect(ServerPlayer p,int stage) {
        var effect=p.getEffect(ModEffects.BETEL_DEPENDENCE);
        require(stage==0?effect==null:effect!=null&&effect.getAmplifier()==stage-1,"server effect amplifier matches calendar stage");
        for(var e:new ArrayList<>(p.getActiveEffects()))if(e.getEffect().value().getCategory()==MobEffectCategory.BENEFICIAL)p.removeEffect(e.getEffect());
        var speed=p.getAttribute(Attributes.MOVEMENT_SPEED);var mining=p.getAttribute(Attributes.BLOCK_BREAK_SPEED);
        require(Math.abs(speed.getValue()-speed.getBaseValue()*(1-.05*stage))<.00001,"real movement penalty");
        require(Math.abs(mining.getValue()-mining.getBaseValue()*(1-.1*stage))<.00001,"real mining penalty");
    }
    private static void checkClient(Minecraft c,int day) {
        require(ClientAddictionData.getAddictionValue()==day,"calendar state sync");
        int expected=new int[]{0,0,1,2,3,4,4,5}[day];var effect=c.player.getEffect(ModEffects.BETEL_DEPENDENCE);
        require(expected==0?effect==null:effect!=null&&effect.getAmplifier()==expected-1,"actual harmful effect reaches client");
        var pos=AddictionHudRenderer.getConfiguredPosition(c,false);
        require(pos.x()>=0&&pos.y()>=0&&pos.x()+AddictionHudRenderer.getScaledHudWidth()<=c.getWindow().getGuiScaledWidth()
                &&pos.y()+AddictionHudRenderer.getScaledHudHeight()<=c.getWindow().getGuiScaledHeight(),"HUD stays within screen");
    }
    private void server(Minecraft c,java.util.function.Consumer<ServerPlayer> action) {
        c.getSingleplayerServer().execute(() -> {try{action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}
            catch(Exception e){failure=new RuntimeException("Daily penalty client verification failed",e);}});
    }
    private static void require(boolean good,String message){if(!good)throw new IllegalStateException(message);}
    private static void capture(Minecraft c,String name){Screenshot.grab(c.gameDirectory,name,c.getMainRenderTarget(),msg -> BetelNutMod.LOGGER.info("DAILY_PENALTIES_SCREENSHOT {}",name));}
    private static final class HoverPreviewScreen extends Screen {
        private final HudEditScreen editor=new HudEditScreen();
        HoverPreviewScreen(){super(Component.empty());}
        @Override protected void init(){editor.init(minecraft,width,height);}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void onClose(){editor.onClose();}
        @Override public void render(GuiGraphics g,int mx,int my,float delta){var p=AddictionHudRenderer.getConfiguredPosition(minecraft,false);editor.render(g,p.x()+8,p.y()+8,delta);}
    }
}
