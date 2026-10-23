package dev.viking602.watut.probe.mixin;
import dev.viking602.watut.probe.ClientProbe;
import com.corosus.watut.*;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import net.minecraft.client.model.player.PlayerModel;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value=PlayerStatusManagerClient.class,remap=false)
public abstract class ModelProbeMixin {
 @Inject(method="receiveItemMove",at=@At("TAIL"),remap=false)
 private void probe$item(net.minecraft.nbt.CompoundTag data,CallbackInfo ci){if(data.contains(WatutNetworking.NBTDataItemTransferItemStack))ClientProbe.itemTransfers++;}
 @Inject(method="renderPingIconHook",at=@At("RETURN"),remap=false)
 private void probe$tab(CallbackInfoReturnable<Boolean> ci){if(ci.getReturnValue())ClientProbe.tabIdleFrames++;}
 @Unique private float probe$arm,probe$head;
 @Inject(method="setupRotationsHook",at=@At("HEAD"),remap=false)
 private void probe$before(EntityModel model,Entity entity,float a,float b,float age,float yaw,float pitch,CallbackInfo ci){
  if(model instanceof PlayerModel player){probe$arm=player.rightArm.xRot;probe$head=player.head.xRot;}
 }

 @Inject(method="setupRotationsHook",at=@At("TAIL"),remap=false)
 private void probe$model(EntityModel model,Entity entity,float a,float b,float age,float yaw,float pitch,CallbackInfo ci){
  if(entity==Minecraft.getInstance().player||!(model instanceof PlayerModel player))return;var status=WatutMod.getPlayerStatusManagerClient().getStatus(entity.getUUID());
  if(status.getPlayerChatState()==PlayerStatus.PlayerChatState.CHAT_TYPING&&Math.abs(player.rightArm.xRot-probe$arm)>0.05F)ClientProbe.typingModels++;
  if(status.isIdle()&&Math.abs(player.head.xRot-probe$head)>0.01F)ClientProbe.idleModels++;
 }
}
