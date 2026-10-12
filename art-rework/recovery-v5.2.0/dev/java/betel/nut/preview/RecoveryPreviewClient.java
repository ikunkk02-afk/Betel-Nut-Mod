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
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

public final class RecoveryPreviewClient implements ClientModInitializer {
    private int tick;
    private volatile RuntimeException failure;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(failure!=null)throw failure;
            if(c.player==null||c.level==null||c.getSingleplayerServer()==null)return;
            tick++;c.getToasts().clear();c.gui.clear();c.gui.getChat().clearMessages(false);
            if(tick==1){
                c.options.pauseOnLostFocus=false;c.options.hideGui=false;c.options.guiScale().set(3);c.resizeDisplay();c.setScreen(null);
                var cfg=BetelNutClientConfig.get();cfg.hudUseCustomPosition=false;cfg.hudScale=1;cfg.hudAnchor=BetelNutClientConfig.HudAnchor.BOTTOM_LEFT;
                server(c,p->{
                    var w=p.serverLevel();w.setDayTime(3300006000L);w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);
                    w.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false,p.server);w.setWeatherParameters(0,12000,false,false);
                    p.setGameMode(GameType.SURVIVAL);p.removeAllEffects();p.getInventory().clearContent();
                    p.teleportTo(w,3.5,80,-14,Set.of(),135,12);
                    var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);s.beginCalendarSession(p);s.setAddictionValue(7);oral(p,5);hold(p,Items.BREAD,2);
                });
            }
            if(tick==30)start(c);if(tick==70)stop(c);
            if(tick==75)server(c,p->require(p.getMainHandItem().getCount()==1,"II bread consumed once"));
            if(tick==78)capture(c,"stage-II-bread-edible.png");
            if(tick==80)server(c,p->hold(p,Items.GOLDEN_APPLE,1));if(tick==90)start(c);if(tick==130)stop(c);
            if(tick==135)server(c,p->require(p.getMainHandItem().is(Items.GOLDEN_APPLE),"II golden apple blocked"));
            if(tick==138)capture(c,"stage-II-golden-apple-blocked.png");
            if(tick==140)server(c,p->hold(p,Items.ENCHANTED_GOLDEN_APPLE,1));if(tick==150)start(c);if(tick==190)stop(c);
            if(tick==195)server(c,p->require(p.getMainHandItem().is(Items.ENCHANTED_GOLDEN_APPLE),"II enchanted apple blocked"));
            if(tick==200)server(c,p->{oral(p,7);hold(p,Items.BREAD,1);});if(tick==210)start(c);if(tick==250)stop(c);
            if(tick==255){require(ClientAddictionData.getMouthRestrictionLevel()==2,"Locked HUD reaches client");capture(c,"stage-III-bread-blocked.png");server(c,p->require(p.getMainHandItem().is(Items.BREAD),"III bread blocked"));}
            if(tick==260)server(c,p->hold(p,Items.HONEY_BOTTLE,1));if(tick==270)start(c);if(tick==310)stop(c);
            if(tick==315)server(c,p->require(p.getMainHandItem().is(Items.HONEY_BOTTLE),"III honey food blocked"));
            if(tick==320)server(c,p->hold(p,ModItems.GOLDEN_APPLE_PASTE,2));if(tick==330)start(c);
            if(tick==346){require(c.player.isUsingItem(),"Golden paste has actual drink animation");capture(c,"golden-paste-drinking-locked.png");}
            if(tick==370)stop(c);if(tick==375)server(c,p->{var s=BetelNutEntityComponents.ADDICTION.get(p);require(s.getAddictionValue()==6&&s.getOralExposureDays()==7&&p.getMainHandItem().getCount()==1,"Golden paste actual use restores once but preserves oral disease");require(p.getInventory().countItem(Items.GLASS_BOTTLE)==1,"Stacked paste returns one bottle");});
            if(tick==390)server(c,p->{hold(p,ModItems.ENCHANTED_GOLDEN_APPLE_PASTE,1);p.getInventory().setItem(1,new ItemStack(ModItems.GOLDEN_APPLE_PASTE));p.getInventory().setItem(2,new ItemStack(ModItems.RELIEF_POTION));p.inventoryMenu.broadcastChanges();});
            if(tick==400)c.setScreen(new InventoryScreen(c.player));if(tick==410)capture(c,"three-remedies-inventory.png");
            if(tick==420){c.setScreen(null);start(c);}if(tick==436){require(c.player.getMainHandItem().hasFoil(),"Actual enchanted glint");capture(c,"enchanted-paste-drinking.png");}
            if(tick==460)stop(c);if(tick==465)server(c,p->{var s=BetelNutEntityComponents.ADDICTION.get(p);require(s.getAddictionValue()==1&&s.getOralExposureDays()==0&&!p.hasEffect(ModEffects.MOUTH_LOCKED)&&p.getMainHandItem().is(Items.GLASS_BOTTLE),"Enchanted paste cures and returns bottle through real client use");});
            if(tick==475){require(ClientAddictionData.getMouthRestrictionLevel()==0,"Recovery sync releases jaw");capture(c,"enchanted-paste-recovery.png");}
            if(tick==480)server(c,p->{var s=BetelNutEntityComponents.ADDICTION.get(p);s.setAddictionValue(7);oral(p,7);hold(p,ModItems.RELIEF_POTION,1);});
            if(tick==490)start(c);if(tick==506)capture(c,"relief-potion-drinking.png");if(tick==530)stop(c);
            if(tick==535)server(c,p->{var s=BetelNutEntityComponents.ADDICTION.get(p);require(s.getAddictionValue()==6&&s.getOralExposureDays()==6&&!p.hasEffect(ModEffects.MOUTH_LOCKED)&&p.getMainHandItem().is(Items.GLASS_BOTTLE),"Affordable relief really downgrades to soft food");});
            if(tick==540){require(ClientAddictionData.getMouthRestrictionLevel()==1,"Partial recovery sync retains soft-food gate");capture(c,"relief-restores-soft-food.png");}
            if(tick==550)server(c,p->hold(p,ModItems.RELIEF_POTION,2));if(tick==560)start(c);if(tick==600)stop(c);
            if(tick==605)server(c,p->require(p.getMainHandItem().getCount()==2&&BetelNutEntityComponents.ADDICTION.get(p).getOralExposureDays()==6,"Repeat daily dose blocked without consumption"));
            if(tick==610)capture(c,"daily-repeat-dose-blocked.png");
            if(tick==620)server(c,p->p.server.overworld().setDayTime(3300030000L));if(tick==630)start(c);if(tick==670)stop(c);
            if(tick==675)server(c,p->require(p.getMainHandItem().getCount()==1&&BetelNutEntityComponents.ADDICTION.get(p).getOralExposureDays()==5,"Next day dose accepted once"));
            if(tick==690)c.setScreen(new RemedyTooltipScreen());if(tick==705)capture(c,"remedy-tooltips.png");
            if(tick==710){c.setScreen(null);server(c,p->oral(p,7));}
            if(tick==730){c.options.guiScale().set(5);c.resizeDisplay();c.setScreen(new HoverPreviewScreen());}
            if(tick==750)capture(c,"locked-jaw-hud-details.png");
            if(tick==770){c.screen.onClose();BetelNutMod.LOGGER.info("RECOVERY_CLIENT_ALL_CHECKS_PASS stageII=true stageIII=true goldenPaste=true enchantedPaste=true reliefDaily=true bottleReturn=true");c.stop();}
        });
    }
    private static void oral(ServerPlayer p,int days){var s=BetelNutEntityComponents.ADDICTION.get(p);s.setOralExposureDays(days);s.maintainOralEffects(p);AddictionSyncPayload.send(p);}
    private static void hold(ServerPlayer p,Item item,int count){p.getFoodData().setFoodLevel(6);p.getInventory().selected=0;p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item,count));p.inventoryMenu.broadcastChanges();}
    private static void start(Minecraft c){c.options.keyUse.setDown(true);c.gameMode.useItem(c.player,InteractionHand.MAIN_HAND);}
    private static void stop(Minecraft c){c.options.keyUse.setDown(false);}
    private static void require(boolean good,String message){if(!good)throw new IllegalStateException(message);}
    private void server(Minecraft c,java.util.function.Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->{try{action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Exception e){failure=new RuntimeException("Recovery verification failed",e);}});}
    private static void capture(Minecraft c,String name){Screenshot.grab(c.gameDirectory,name,c.getMainRenderTarget(),msg->BetelNutMod.LOGGER.info("RECOVERY_SCREENSHOT {}",name));}
    private static final class RemedyTooltipScreen extends Screen {
        RemedyTooltipScreen(){super(Component.empty());}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float delta){
            Item[] items={ModItems.GOLDEN_APPLE_PASTE,ModItems.ENCHANTED_GOLDEN_APPLE_PASTE,ModItems.RELIEF_POTION};
            for(int i=0;i<items.length;i++){var stack=new ItemStack(items[i]);g.renderItem(stack,30,40+i*85);g.renderTooltip(font,stack,55,40+i*85);}
        }
    }
    private static final class HoverPreviewScreen extends Screen {
        private final HudEditScreen editor=new HudEditScreen();
        HoverPreviewScreen(){super(Component.empty());}
        @Override protected void init(){editor.init(minecraft,width,height);}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void onClose(){editor.onClose();}
        @Override public void render(GuiGraphics g,int mx,int my,float delta){var p=AddictionHudRenderer.getConfiguredPosition(minecraft,false);editor.render(g,p.x()+8,p.y()+8,delta);}
    }
}
