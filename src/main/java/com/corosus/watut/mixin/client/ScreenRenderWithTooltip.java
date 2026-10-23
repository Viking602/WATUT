package com.corosus.watut.mixin.client;

import com.corosus.watut.WatutMod;
import com.corosus.watut.client.screen.RenderHelper;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class ScreenRenderWithTooltip {
  @Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
  private void watut$extract(
      GuiGraphicsExtractor graphics, int x, int y, float a, CallbackInfo ci) {
    if (!RenderHelper.performingOwnRender) {
      WatutMod.getPlayerStatusManagerClient().onGuiRender(graphics);
      RenderHelper.renderWithTooltipEnd(graphics, x, y, a);
    }
  }
}
