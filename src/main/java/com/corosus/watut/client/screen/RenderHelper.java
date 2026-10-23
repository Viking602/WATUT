package com.corosus.watut.client.screen;

import com.corosus.watut.*;
import com.corosus.watut.config.*;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.ByteArrayOutputStream;
import java.nio.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class RenderHelper {
  public static boolean performingOwnRender;
  public static final Identifier cursor =
      Identifier.fromNamespaceAndPath("watut", "textures/misc/mouse.png");
  public static final int MAX_IMAGE_BYTES = 1024 * 1024 * 4;

  private record Frame(ByteBuffer bytes, int size) {}

  private static final ConcurrentLinkedQueue<Frame> COMPLETE = new ConcurrentLinkedQueue<>();

  public static void enqueue(ByteBuffer bytes, int size) {
    COMPLETE.clear();
    COMPLETE.add(new Frame(bytes, size));
  }

  public static void clearPending() {
    COMPLETE.clear();
  }

  public static boolean useDynamicGUISystem() {
    return !ConfigServerControlledSyncedToClient.dynamicGuiUseOldSimpleGUIVisual
        && !ConfigClient.dontSendDetailedGUIInfo;
  }

  public static void guiRender(GuiGraphicsExtractor graphics) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level == null) return;
    long time = mc.level.getGameTime();
    for (PlayerStatus status :
        WatutMod.getPlayerStatusManagerClient().lookupPlayerToStatus.values()) {
      ScreenData data = status.getScreenData();
      if (!data.getIsBufferReady().get()
          || !data.needsNewRenderFromPixelData()
          || data.getGameTicksSinceLastScreenReceiveAndRender()
                  + ConfigClient.tickReceiveAndRenderRateOfGUIUpdates
              >= time) continue;
      ByteBuffer bytes = data.getDecompressionBuffer();
      if (!validatePixelByteBuffer(bytes, data.getWidth() * data.getHeight() * 4, 4)) continue;
      if (data.getParticleRenderType() == null) data.initClient();
      DynamicTexture texture = data.getImage();
      if (texture == null
          || texture.getPixels().getWidth() != data.getWidth()
          || texture.getPixels().getHeight() != data.getHeight()) {
        data.closeImage();
        data.setImage(
            new DynamicTexture("WATUT player GUI", data.getWidth(), data.getHeight(), true));
        texture = data.getImage();
      }
      var pixels = bytes.duplicate().order(ByteOrder.LITTLE_ENDIAN);
      for (int y = 0; y < data.getHeight(); y++)
        for (int x = 0; x < data.getWidth(); x++)
          texture.getPixels().setPixel(x, y, net.minecraft.util.ARGB.fromABGR(pixels.getInt()));
      texture.upload();
      data.markNeedsNewRenderFromPixelData(false);
      data.setGameTicksSinceLastScreenReceiveAndRender(time);
      data.getIsBufferReady().set(false);
    }
    Frame completed;
    while ((completed = COMPLETE.poll()) != null) {
      if (!useDynamicGUISystem()) continue;
      var local = WatutMod.getPlayerStatusManagerClient().getStatusLocal();
      local.getScreenData().setWidth(completed.size());
      local.getScreenData().setHeight(completed.size());
      local.getScreenData().setTexturePixelData(completed.bytes());
      WatutMod.getPlayerStatusManagerClient().sendScreenRenderData(local);
    }
  }

  public static void renderWithTooltipEnd(GuiGraphicsExtractor graphics, int x, int y, float a) {
    Minecraft mc = Minecraft.getInstance();
    if (!useDynamicGUISystem() || mc.level == null || mc.player == null) return;
    var data = WatutMod.getPlayerStatusManagerClient().getStatusLocal().getScreenData();
    if (data.isNeedsNewRenderToPixelData()) {
      if (ScreenParticleRenderer.getInstance().extract(x, y, a))
        data.setNeedsNewRenderToPixelData(false);
    }
  }

  public static ByteBuffer processImage(
      NativeImage image, int size, double guiScale, boolean entire, int blur, double radius) {
    int[] pixels = new int[size * size];
    double step = entire ? (double) Math.max(image.getWidth(), image.getHeight()) / size : guiScale;
    for (int y = 0; y < size; y++)
      for (int x = 0; x < size; x++) {
        int sx = (int) (image.getWidth() / 2.0 + (x - size / 2.0) * step),
            sy = (int) (image.getHeight() / 2.0 + (y - size / 2.0) * step);
        if (sx >= 0 && sy >= 0 && sx < image.getWidth() && sy < image.getHeight())
          pixels[x + y * size] = image.getPixel(sx, sy);
      }
    // CPU blur and radial alpha replace the old two-pass GL shader without changing shared graphics
    // state.
    for (int pass = 0; pass < blur; pass++) {
      int[] out = new int[pixels.length];
      for (int y = 0; y < size; y++)
        for (int x = 0; x < size; x++) {
          int aa = 0, rr = 0, gg = 0, bb = 0;
          for (int dy = -1; dy <= 1; dy++)
            for (int dx = -1; dx <= 1; dx++) {
              int c =
                  pixels[Math.clamp(x + dx, 0, size - 1) + Math.clamp(y + dy, 0, size - 1) * size];
              aa += c >>> 24;
              rr += (c >>> 16) & 255;
              gg += (c >>> 8) & 255;
              bb += c & 255;
            }
          out[x + y * size] = (aa / 9 << 24) | (rr / 9 << 16) | (gg / 9 << 8) | bb / 9;
        }
      pixels = out;
    }
    ByteBuffer raw = ByteBuffer.allocate(size * size * 4).order(ByteOrder.LITTLE_ENDIAN);
    for (int y = 0; y < size; y++)
      for (int x = 0; x < size; x++) {
        int color = pixels[x + y * size];
        if (radius >= 0 && !entire) {
          double distance = Math.hypot(x - size / 2.0, y - size / 2.0),
              fade = Math.clamp((radius - distance) / 8.0, 0, 1);
          color = (int) ((color >>> 24) * fade) << 24 | color & 0xffffff;
        }
        raw.putInt(net.minecraft.util.ARGB.toABGR(color));
      }
    raw.flip();
    return compress(raw);
  }

  public static ByteBuffer compress(ByteBuffer input) {
    byte[] bytes = new byte[input.remaining()];
    input.duplicate().get(bytes);
    Deflater deflater = new Deflater(Deflater.BEST_SPEED);
    try {
      deflater.setInput(bytes);
      deflater.finish();
      var output = new ByteArrayOutputStream();
      byte[] temp = new byte[8192];
      while (!deflater.finished()) output.write(temp, 0, deflater.deflate(temp));
      return ByteBuffer.wrap(output.toByteArray());
    } finally {
      deflater.end();
    }
  }

  public static ByteBuffer decompress(ScreenData data, ByteBuffer compressed, int expectedSize)
      throws DataFormatException {
    if (expectedSize <= 0
        || expectedSize > MAX_IMAGE_BYTES
        || compressed.remaining() > MAX_IMAGE_BYTES + 65536)
      throw new DataFormatException("Invalid WATUT image size");
    byte[] input = new byte[compressed.remaining()];
    compressed.duplicate().get(input);
    byte[] output = new byte[expectedSize];
    Inflater inflater = new Inflater();
    try {
      inflater.setInput(input);
      int offset = 0;
      while (!inflater.finished() && offset < output.length) {
        int count = inflater.inflate(output, offset, output.length - offset);
        if (count == 0) throw new DataFormatException("Truncated WATUT image");
        offset += count;
      }
      if (offset != expectedSize || !inflater.finished() || inflater.getRemaining() != 0)
        throw new DataFormatException("WATUT image length mismatch");
    } finally {
      inflater.end();
    }
    var old = data.getDecompressionBuffer();
    if (old != null) MemoryUtil.memFree(old);
    ByteBuffer result = MemoryUtil.memAlloc(expectedSize);
    result.put(output).flip();
    data.setDecompressionBuffer(result);
    return result;
  }

  public static boolean validatePixelByteBuffer(ByteBuffer buffer, int size, int alignment) {
    return buffer != null && size > 0 && size <= MAX_IMAGE_BYTES && buffer.remaining() == size;
  }
}
