package dev.viking602.watut.probe;
import com.corosus.watut.*;
import com.corosus.watut.client.screen.*;
import com.corosus.watut.config.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
public final class ClientProbe {
 public static int particleFrames,dynamicFrames,typingModels,idleModels,itemTransfers,tabIdleFrames;
 private static String role=System.getProperty("watut.probeRole","host");
 private static int total,ticks,phase;private static boolean started,finished,typing,image,idle;
 public static void register(){NeoForge.EVENT_BUS.addListener(ClientProbe::tick);}
 private static void tick(ClientTickEvent.Post event){
  Minecraft mc=Minecraft.getInstance();if(finished)return;
  try{
   if(++total>9000)throw new IllegalStateException("Probe timeout");
   if(!started&&!role.equals("guest")&&mc.getOverlay()==null&&mc.screen instanceof TitleScreen){
    started=true;mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(4);mc.options.simulationDistance().set(5);
    mc.createWorldOpenFlows().createFreshLevel("watut-probe-"+System.currentTimeMillis(),new LevelSettings("WATUT verification",GameType.SURVIVAL,new LevelSettings.DifficultySettings(Difficulty.PEACEFUL,false,false),true,WorldDataConfiguration.DEFAULT),new WorldOptions(602,false,false),WorldPresets::createFlatWorldDimensions,mc.screen);return;
   }
   if(mc.screen instanceof DisconnectedScreen) throw new IllegalStateException(mc.screen.getNarrationMessage().getString());
   if(mc.level==null||mc.player==null||mc.getOverlay()!=null)return;
   mc.options.pauseOnLostFocus=false;
   if(role.equals("guest")){guest(mc);return;}
   if(phase==0){
    codecChecks();
    if(role.equals("solo")){
     var field=PlayerStatusManager.class.getDeclaredField("singleplayerTesting");field.setAccessible(true);field.setBoolean(WatutMod.getPlayerStatusManagerClient(),true);
     mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.setScreen(null);phase=1;return;
    }
    mc.getSingleplayerServer().setUsesAuthentication(false);
    if(!mc.getSingleplayerServer().publishServer(GameType.SURVIVAL,true,26412))throw new IllegalStateException("LAN publish failed");
    mc.setScreen(null);PortProbe.write("lan-ready.json",Map.of("ready",true));phase=1;return;
   }
   if(phase==1){
    if(!role.equals("solo")&&(mc.level.players().size()<2||!Files.exists(PortProbe.DIR.resolve("guest-ready.json"))))return;
    mc.getSingleplayerServer().execute(()->{
     for(var p:mc.getSingleplayerServer().getPlayerList().getPlayers())p.teleportTo(0,-59,p.getGameProfile().name().equals("WATUTHost")?0:3);
    });mc.setScreen(new ChatScreen("",false));phase=2;ticks=0;return;
   }
   ticks++;
   if(role.equals("solo")){
    mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
    var status=WatutMod.getPlayerStatusManagerClient().getStatusLocal();
    typing|=status.getPlayerChatState()==PlayerStatus.PlayerChatState.CHAT_TYPING;
    idle|=status.isIdle();
    var received=WatutMod.getPlayerStatusManagerClient().getStatus(mc.player.getUUID());
    if(phase==3&&received.getScreenData().getImage()!=null&&dynamicFrames>0){if(!image){capture(mc,"full-pack-inventory.png");var pixels=received.getScreenData().getImage().getPixels();var colors=new HashSet<Integer>();int visible=0;for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++){int color=pixels.getPixel(x,y);if((color>>>24)>0){visible++;colors.add(color);}}PortProbe.require(visible>500&&colors.size()>20,"Blank full-pack GUI");pixels.writeToFile(PortProbe.DIR.resolve("full-pack-gui.png"));PortProbe.write("gui-pixels.json",Map.of("passed",true,"visible",visible,"colors",colors.size()));}image=true;}
   }
   if(phase==2){
    if(mc.screen instanceof ChatScreen chat&&ticks%6==0){chat.input.setValue("WATUT typing "+ticks);WatutMod.getPlayerStatusManagerClient().onKey();}
    if(ticks>200){mc.setScreen(new InventoryScreen(mc.player));phase=3;ticks=0;PortProbe.write("phase.json",Map.of("phase","inventory"));}
   }else if(phase==3){
    if(ticks==50)mc.getSingleplayerServer().execute(()->{var player=mc.getSingleplayerServer().getPlayerList().getPlayers().getFirst();WatutMod.getPlayerStatusManagerServer().sendItemMove(player,player.level(),new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND),0,-58,0,0,-58,1);});
    if(ticks>200){mc.setScreen(null);var local=WatutMod.getPlayerStatusManagerClient().getStatusLocal();local.setTicksSinceLastAction(10000);WatutMod.getPlayerStatusManagerClient().sendIdle(local);phase=4;ticks=0;}
   }else if(phase==4){
    if(ticks>200){PortProbe.write("host-done.json",Map.of("passed",true));phase=5;ticks=0;}
   }else if(phase==5&&(role.equals("solo")||Files.exists(PortProbe.DIR.resolve("guest-result.json")))){if(role.equals("solo")){PortProbe.require(image,"Full-pack GUI projection missing");PortProbe.require(typing&&idle,"Full-pack status missing");}finish(mc,true,null);}
  }catch(Throwable t){finish(mc,false,t.toString());}
 }
 private static void guest(Minecraft mc)throws Exception{
  var remote=mc.level.players().stream().filter(p->!p.getUUID().equals(mc.player.getUUID())).findFirst().orElse(null);
  if(remote==null)return;
  if(++ticks<40)return;
  if(!Files.exists(PortProbe.DIR.resolve("guest-ready.json")))PortProbe.write("guest-ready.json",Map.of("ready",true));
  mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,remote.getEyePosition());
  var status=WatutMod.getPlayerStatusManagerClient().getStatus(remote.getUUID());
  if(status.getPlayerChatState()==PlayerStatus.PlayerChatState.CHAT_TYPING){
   if(!Files.exists(PortProbe.DIR.resolve("typing.png"))&&typingModels>5)capture(mc,"typing.png");typing=true;
   if(!(mc.screen instanceof ChatScreen))mc.setScreen(new ChatScreen("",false));
  }
  if(status.getPlayerGuiState()!=PlayerStatus.PlayerGuiState.NONE&&status.getPlayerGuiState()!=PlayerStatus.PlayerGuiState.CHAT_SCREEN&&status.getScreenData().getImage()!=null&&dynamicFrames>30){
   mc.setScreen(null);
   if(!image){capture(mc,"inventory-projection.png");var pixels=status.getScreenData().getImage().getPixels();var colors=new HashSet<Integer>();int visible=0;for(int y=0;y<pixels.getHeight();y++)for(int x=0;x<pixels.getWidth();x++){int color=pixels.getPixel(x,y);if((color>>>24)>0){visible++;colors.add(color);}}PortProbe.require(visible>500&&colors.size()>20,"Blank GUI capture");pixels.writeToFile(PortProbe.DIR.resolve("received-gui.png"));PortProbe.write("gui-pixels.json",Map.of("passed",true,"visible",visible,"colors",colors.size()));}image=true;
  }
  if(status.isIdle()){mc.setScreen(null);mc.options.keyPlayerList.setDown(true);if(!idle)capture(mc,"idle.png");if(tabIdleFrames>5&&!Files.exists(PortProbe.DIR.resolve("idle-tab.png")))capture(mc,"idle-tab.png");idle=true;}
  if(Files.exists(PortProbe.DIR.resolve("host-done.json"))){
   PortProbe.require(typing,"Typing status not received");PortProbe.require(image,"GUI image not received");PortProbe.require(idle,"Idle status not received");
   PortProbe.require(idleModels>0,"Idle model not animated");
   PortProbe.require(dynamicFrames>0,"Projection never extracted");PortProbe.require(typingModels>0,"Typing model not animated");
   PortProbe.require(itemTransfers>0,"Item transfer not received");PortProbe.require(tabIdleFrames>0,"TAB idle icon not rendered");
   finish(mc,true,null);
  }
 }
 private static void capture(Minecraft mc,String name){Screenshot.takeScreenshot(mc.getMainRenderTarget(),image->{try(image){image.writeToFile(PortProbe.DIR.resolve(name));}catch(Exception e){throw new RuntimeException(e);}});}
 private static void codecChecks()throws Exception{
  for(var info:ParticleRegistry.particles)PortProbe.require(info.getSprite()!=null&&!info.getSprite().contents().name().equals(net.minecraft.client.renderer.texture.MissingTextureAtlasSprite.getLocation()),"Missing status sprite: "+info.getName());
  ScreenData data=new ScreenData();byte[] bytes=new byte[256*256*4];new Random(602).nextBytes(bytes);
  var zipped=RenderHelper.compress(ByteBuffer.wrap(bytes));var restored=RenderHelper.decompress(data,zipped,bytes.length);byte[] actual=new byte[restored.remaining()];restored.get(actual);PortProbe.require(Arrays.equals(bytes,actual),"Image roundtrip");
  int rejected=0;
  for(var invalid:List.of(ByteBuffer.wrap(new byte[]{1,2,3}),ByteBuffer.wrap(new byte[0]),zipped.duplicate().limit(zipped.limit()/2))){try{RenderHelper.decompress(data,invalid,bytes.length);}catch(java.util.zip.DataFormatException expected){rejected++;}}
  PortProbe.require(rejected==3,"Malformed image rejection");data.close();PortProbe.write("image-codec.json",Map.of("passed",true,"malformedRejected",rejected));
 }
 private static void finish(Minecraft mc,boolean passed,String error){
  if(finished)return;finished=true;var out=new LinkedHashMap<String,Object>();out.put("passed",passed);out.put("role",role);out.put("error",error);out.put("typing",typing);out.put("image",image);out.put("idle",idle);out.put("particleFrames",particleFrames);out.put("dynamicFrames",dynamicFrames);out.put("typingModels",typingModels);out.put("idleModels",idleModels);out.put("itemTransfers",itemTransfers);out.put("tabIdleFrames",tabIdleFrames);PortProbe.write(role+"-result.json",out);mc.stop();
 }
}
