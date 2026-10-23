package dev.viking602.watut.probe;
import com.google.gson.GsonBuilder;
import com.corosus.watut.*;
import com.corosus.watut.config.*;
import com.corosus.watut.network.*;
import java.nio.file.*;
import java.util.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import io.netty.buffer.Unpooled;
@Mod("watut_port_probe")
public final class PortProbe {
 public static final Path DIR=Path.of(System.getProperty("watut.probeDir","."));
 public PortProbe(){
  NeoForge.EVENT_BUS.addListener(PortProbe::serverStarted);
  if(FMLEnvironment.getDist().isClient()) ClientProbe.register();
  else NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event)->{
   if(event.getServer().getTickCount()>60) event.getServer().halt(false);
  });
 }
 public static void require(boolean condition,String message){if(!condition)throw new IllegalStateException(message);}
 public static void write(String name,Object value){try{Files.createDirectories(DIR);Files.writeString(DIR.resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(value));}catch(Exception e){throw new RuntimeException(e);}}
 private static void serverStarted(ServerStartedEvent event){
  try{
   CompoundTag data=new CompoundTag();data.putInt(WatutNetworking.NBTDataPlayerGuiStatus,2);data.putBoolean(WatutNetworking.NBTDataPlayerGuiDontSendDetailedGUIInfo,true);data.putFloat(WatutNetworking.NBTDataPlayerTypingAmp,3.5F);
   var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),event.getServer().registryAccess());
   try{
    PacketNBTFromClient.STREAM_CODEC.encode(buffer,new PacketNBTFromClient(data));require(PacketNBTFromClient.STREAM_CODEC.decode(buffer).nbt().equals(data),"Client packet roundtrip");buffer.clear();
    PacketNBTFromServer.STREAM_CODEC.encode(buffer,new PacketNBTFromServer(data));require(PacketNBTFromServer.STREAM_CODEC.decode(buffer).nbt().equals(data),"Server packet roundtrip");
   }finally{buffer.release();}
   data.putBoolean(WatutNetworking.NBTDataServerConfig,true);data.putString(WatutNetworking.NBTDataPlayerUUID,"spoofed");data.putFloat(WatutNetworking.NBTDataPlayerMouseX,Float.NaN);
   var clean=ClientStatusPayload.sanitize(data);require(!clean.contains(WatutNetworking.NBTDataServerConfig)&&!clean.contains(WatutNetworking.NBTDataPlayerUUID)&&!clean.contains(WatutNetworking.NBTDataPlayerMouseX),"Relay whitelist");
   require(PlayerStatus.PlayerGuiState.get(-1)==PlayerStatus.PlayerGuiState.NONE,"Invalid GUI enum");
   var config=ConfigServerSyncHelper.getInstance().getSyncableConfigOnServer();require(config.contains("showPlayerAnimations")&&config.getBooleanOr("showPlayerAnimations",false),"CoroUtil config serialization");
   write("server-checks.json",Map.of("passed",true,"checks",List.of("client_payload_roundtrip","server_payload_roundtrip","relay_authority","invalid_gui_enum","coroutil_configuration")));
  }catch(Throwable t){write("server-checks.json",Map.of("passed",false,"error",t.toString()));throw new RuntimeException(t);}
 }
}
