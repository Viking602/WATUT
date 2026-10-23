package com.corosus.watut.mixin.client;

import com.corosus.watut.client.screen.RenderHelper;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiRender {
  @Inject(method = "extractRenderState", at = @At("TAIL"))
  private void watut$extract(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
    RenderHelper.guiRender(graphics);
  }
}
