package com.corosus.watut.client.screen;

import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.*;
import net.minecraft.client.gui.render.pip.*;
import net.minecraft.client.renderer.fog.FogRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.*;
import net.minecraft.util.ARGB;

/** Independent GUI extraction, modern rendering and asynchronous GPU readback. */
public final class ScreenParticleRenderer {
  public static boolean isRenderingParticleGUI, isRenderingParticleGUI2;
  public static final int defaultWidthScaledDown = 256,
      defaultHeightScaledDown = 256,
      bytesPerPixel = 4;
  public int widthScaledDown = 256, heightScaledDown = 256;
  private static final ScreenParticleRenderer INSTANCE = new ScreenParticleRenderer();
  private static final java.util.concurrent.ExecutorService WORKER =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "watut-image-processing");
            t.setDaemon(true);
            return t;
          });
  private final GuiRenderState state = new GuiRenderState();
  private GuiRenderer renderer;
  private MainTarget target;
  private boolean pending, inFlight;
  private long epoch;
  public static RenderTarget captureTarget;

  public static ScreenParticleRenderer getInstance() {
    return INSTANCE;
  }

  public void checkSetup() {}

  public void resize(int width, int height) {} // Target sizes are checked before every capture.

  public boolean extract(int mouseX, int mouseY, float partialTick) {
    Minecraft mc = Minecraft.getInstance();
    if (pending || inFlight || mc.screen == null) return false;
    state.reset();
    RenderHelper.performingOwnRender = true;
    isRenderingParticleGUI =
        ConfigServerControlledSyncedToClient.dynamicGuiDisableBackgroundRendering;
    try {
      var graphics = new GuiGraphicsExtractor(mc, state, mouseX, mouseY);
      mc.screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, partialTick);
      graphics.blit(RenderHelper.cursor, mouseX, mouseY, mouseX + 6, mouseY + 10, 0, 1, 0, 1);
      pending = true;
    } finally {
      isRenderingParticleGUI = false;
      RenderHelper.performingOwnRender = false;
    }
    return true;
  }

  public void render(FogRenderer fog) {
    if (!pending) return;
    pending = false;
    Minecraft mc = Minecraft.getInstance();
    int width = mc.getWindow().getWidth(), height = mc.getWindow().getHeight();
    if (target == null) target = new MainTarget(width, height);
    else if (target.width != width || target.height != height) target.resize(width, height);
    if (renderer == null) {
      var atlas = mc.getAtlasManager();
      renderer =
          new GuiRenderer(
              state,
              mc.renderBuffers().bufferSource(),
              mc.gameRenderer.getSubmitNodeStorage(),
              mc.gameRenderer.getFeatureRenderDispatcher(),
              net.neoforged.neoforge.client.ClientHooks.gatherPictureInPictureRenderers(
                  List.of(
                      new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(
                          GuiEntityRenderState.class,
                          b -> new GuiEntityRenderer(b, mc.getEntityRenderDispatcher())),
                      new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(
                          GuiSkinRenderState.class, GuiSkinRenderer::new),
                      new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(
                          GuiBookModelRenderState.class, GuiBookModelRenderer::new),
                      new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(
                          GuiBannerResultRenderState.class,
                          b -> new GuiBannerResultRenderer(b, atlas)),
                      new net.neoforged.neoforge.client.gui.PictureInPictureRendererRegistration<>(
                          GuiSignRenderState.class, b -> new GuiSignRenderer(b, atlas)))));
    }
    RenderSystem.backupProjectionMatrix();
    captureTarget = target;
    try {
      RenderSystem.getDevice()
          .createCommandEncoder()
          .clearColorAndDepthTextures(target.getColorTexture(), 0, target.getDepthTexture(), 1);
      renderer.render(fog.getBuffer(FogRenderer.FogMode.NONE));
      renderer.endFrame();
      mc.gameRenderer.getSubmitNodeStorage().endFrame();
      mc.gameRenderer.getFeatureRenderDispatcher().endFrame();
    } finally {
      captureTarget = null;
      RenderSystem.restoreProjectionMatrix();
    }
    double scale = mc.getWindow().getGuiScale();
    boolean entire = ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen;
    int blur = Math.clamp(ConfigServerControlledSyncedToClient.dynamicGuiBlurLevel, 0, 2);
    double radius = ConfigServerControlledSyncedToClient.dynamicGuiSizeRadiusInPixelsToShow;
    int size = entire ? 1024 : 256;
    widthScaledDown = heightScaledDown = size;
    inFlight = true;
    long generation = epoch;
    var level = mc.level;
    var texture = target.getColorTexture();
    GpuBuffer buffer =
        RenderSystem.getDevice()
            .createBuffer(
                () -> "WATUT GUI readback",
                GpuBuffer.USAGE_MAP_READ | GpuBuffer.USAGE_COPY_DST,
                (long) width * height * 4);
    var encoder = RenderSystem.getDevice().createCommandEncoder();
    encoder.copyTextureToBuffer(
        texture,
        buffer,
        0,
        () -> {
          NativeImage image = new NativeImage(width, height, false);
          try (var mapped = encoder.mapBuffer(buffer, true, false)) {
            for (int y = 0; y < height; y++)
              for (int x = 0; x < width; x++)
                image.setPixel(
                    x, height - y - 1, ARGB.fromABGR(mapped.data().getInt((x + y * width) * 4)));
          } finally {
            buffer.close();
          }
          WORKER.execute(
              () -> {
                try (image) {
                  var compressed =
                      RenderHelper.processImage(image, size, scale, entire, blur, radius);
                  mc.execute(
                      () -> {
                        if (generation == epoch
                            && mc.level == level
                            && RenderHelper.useDynamicGUISystem())
                          RenderHelper.enqueue(compressed, size);
                      });
                } finally {
                  mc.execute(
                      () -> {
                        if (generation == epoch) inFlight = false;
                      });
                }
              });
        },
        0);
  }

  public void close() {
    epoch++;
    pending = false;
    inFlight = false;
    RenderHelper.clearPending();
    state.reset();
    if (renderer != null) {
      renderer.close();
      renderer = null;
    }
    if (target != null) {
      target.destroyBuffers();
      target = null;
    }
  }
}
