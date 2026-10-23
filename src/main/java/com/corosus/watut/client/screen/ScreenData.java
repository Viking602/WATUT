package com.corosus.watut.client.screen;

import com.corosus.watut.PlayerStatusManagerClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gui.screens.Screen;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenData {

    private volatile ByteBuffer texturePixelData = null;
    private volatile ByteBuffer decompressionBuffer = null;
    private final AtomicBoolean isBufferReady = new AtomicBoolean(false);
    private byte[] texturePixelDataPartial = null;

    //private byte[] texturePixelDataDiff = null;

    private long gameTicksSinceFirstPacket = 0;
    private int lastIndexReceived = 0;
    private long gameTicksSinceLastScreenSend = 0;
    private long gameTicksSinceLastScreenReceiveAndRender = 0;

    private net.minecraft.client.particle.SingleQuadParticle.Layer particleRenderType;

    private boolean needsNewRenderFromPixelData = false;

    //used for communicating from outside screen render hook to inside it
    private boolean needsNewRenderToPixelData = false;

    private DynamicTexture image = null;
    private int width = ScreenParticleRenderer.defaultWidthScaledDown;
    private int height = ScreenParticleRenderer.defaultHeightScaledDown;

    //since gui states are kinda old system and require specifically adding support for a screen, we use this instead to track true differences now
    private Screen lastScreen;

    private static final java.util.concurrent.atomic.AtomicInteger NEXT_TEXTURE = new java.util.concurrent.atomic.AtomicInteger();
    private final net.minecraft.resources.Identifier textureId = net.minecraft.resources.Identifier.fromNamespaceAndPath("watut", "screen/" + NEXT_TEXTURE.getAndIncrement());
    public net.minecraft.resources.Identifier getTextureId() { return textureId; }
    public void initClient() {
        particleRenderType = new net.minecraft.client.particle.SingleQuadParticle.Layer(true, textureId, com.corosus.watut.client.WatutRenderPipelines.TRANSLUCENT);
    }
    public void close() {
        closeImage();
        if (decompressionBuffer != null) { MemoryUtil.memFree(decompressionBuffer); decompressionBuffer = null; }
        texturePixelData = null; texturePixelDataPartial = null;
        isBufferReady.set(false);
    }
    public ByteBuffer getTexturePixelData() {
        return texturePixelData;
    }

    public void freeTexturePixelData() {
        if (texturePixelData != null) {
            texturePixelData = null;
        }
    }

    public void setTexturePixelData(ByteBuffer texturePixelData) {
        this.texturePixelData = texturePixelData;
    }

    public net.minecraft.client.particle.SingleQuadParticle.Layer getParticleRenderType() {
        return particleRenderType;
    }

    public void setParticleRenderType(net.minecraft.client.particle.SingleQuadParticle.Layer particleRenderType) {
        this.particleRenderType = particleRenderType;
    }

    public synchronized boolean needsNewRenderFromPixelData() {
        return needsNewRenderFromPixelData;
    }

    public synchronized void markNeedsNewRenderFromPixelData(boolean needsNewRender) {
        this.needsNewRenderFromPixelData = needsNewRender;
    }

    public boolean isNeedsNewRenderToPixelData() {
        return needsNewRenderToPixelData;
    }

    public void setNeedsNewRenderToPixelData(boolean needsNewRenderToPixelData) {
        this.needsNewRenderToPixelData = needsNewRenderToPixelData;
    }

    public byte[] getTexturePixelDataPartial() {
        return texturePixelDataPartial;
    }

    public void setTexturePixelDataPartial(byte[] texturePixelDataPartial) {
        this.texturePixelDataPartial = texturePixelDataPartial;
    }

    public long getGameTicksSinceFirstPacket() {
        return gameTicksSinceFirstPacket;
    }

    public void setGameTicksSinceFirstPacket(long gameTicksSinceFirstPacket) {
        this.gameTicksSinceFirstPacket = gameTicksSinceFirstPacket;
    }

    public int getLastIndexReceived() {
        return lastIndexReceived;
    }

    public void setLastIndexReceived(int lastIndexReceived) {
        this.lastIndexReceived = lastIndexReceived;
    }

    public long getGameTicksSinceLastScreenSend() {
        return gameTicksSinceLastScreenSend;
    }

    public void setGameTicksSinceLastScreenSend(long gameTicksSinceLastScreenSend) {
        this.gameTicksSinceLastScreenSend = gameTicksSinceLastScreenSend;
    }

    public long getGameTicksSinceLastScreenReceiveAndRender() {
        return gameTicksSinceLastScreenReceiveAndRender;
    }

    public void setGameTicksSinceLastScreenReceiveAndRender(long gameTicksSinceLastScreenReceiveAndRender) {
        this.gameTicksSinceLastScreenReceiveAndRender = gameTicksSinceLastScreenReceiveAndRender;
    }

    public AtomicBoolean getIsBufferReady() {
        return isBufferReady;
    }

    public ByteBuffer getDecompressionBuffer() {
        return decompressionBuffer;
    }

    public void setDecompressionBuffer(ByteBuffer decompressionBuffer) {
        this.decompressionBuffer = decompressionBuffer;
    }

    public DynamicTexture getImage() {
        return image;
    }

    public void setImage(DynamicTexture image) {
        this.image = image;
        net.minecraft.client.Minecraft.getInstance().getTextureManager().register(textureId, image);
    }

    public void closeImage() {
        if (this.image != null) {
            net.minecraft.client.Minecraft.getInstance().getTextureManager().release(textureId);
            this.image = null;
        }
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Screen getLastScreen() {
        return lastScreen;
    }

    public void setLastScreen(Screen lastScreen) {
        this.lastScreen = lastScreen;
    }

    /*public byte[] getTexturePixelDataDiff() {
        return texturePixelDataDiff;
    }

    public void setTexturePixelDataDiff(byte[] texturePixelDataDiff) {
        this.texturePixelDataDiff = texturePixelDataDiff;
    }*/
}
