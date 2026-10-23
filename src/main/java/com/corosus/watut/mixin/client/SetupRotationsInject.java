package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
public abstract class SetupRotationsInject {
  @Inject(
      method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",
      at = @At("TAIL"))
  private void watut$animate(AvatarRenderState state, CallbackInfo ci) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    Entity entity = mc.level.getEntity(state.id);
    if (entity != null)
      WatutMod.getPlayerStatusManagerClient()
          .setupRotationsHook((PlayerModel) (Object) this, entity, 0, 0, state.ageInTicks, 0, 0);
  }
}
