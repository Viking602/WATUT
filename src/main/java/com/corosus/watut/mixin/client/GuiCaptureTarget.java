package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.ScreenParticleRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(GuiRenderer.class)
public abstract class GuiCaptureTarget {
  @ModifyVariable(method = "draw", at = @At("STORE"), ordinal = 0)
  private RenderTarget watut$target(RenderTarget original) {
    return ScreenParticleRenderer.captureTarget != null
        ? ScreenParticleRenderer.captureTarget
        : original;
  }
}
