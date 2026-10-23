package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererCapture {
  @Shadow @Final private FogRenderer fogRenderer;

  @Inject(method = "render", at = @At("TAIL"))
  private void watut$capture(CallbackInfo ci) {
    ScreenParticleRenderer.getInstance().render(fogRenderer);
  }

  @Inject(
      method = {"close", "resetData"},
      at = @At("HEAD"))
  private void watut$close(CallbackInfo ci) {
    ScreenParticleRenderer.getInstance().close();
  }
}
