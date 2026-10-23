package com.corosus.watut.particle;

import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.state.level.QuadParticleRenderState;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

public abstract class ParticleRotating extends SingleQuadParticle {
  public boolean useCustomRotation = true;
  public float prevRotationYaw,
      rotationYaw,
      prevRotationPitch,
      rotationPitch,
      prevRotationRoll,
      rotationRoll;
  public float brightness = 1F;
  public int despawnCountdown = 40;

  public ParticleRotating(ClientLevel level, double x, double y, double z) {
    super(level, x, y, z, null);
  }

  @Override
  public void tick() {
    if (--despawnCountdown <= 0) remove();
  }

  public float getColorRed() {
    return rCol;
  }

  public float getColorGreen() {
    return gCol;
  }

  public float getColorBlue() {
    return bCol;
  }

  public void keepAlive() {
    despawnCountdown = 40;
  }

  public void setQuadSize(float size) {
    quadSize = size;
  }

  @Override
  public void setAlpha(float value) {
    alpha = value;
  }

  @Override
  protected Layer getLayer() {
    return com.corosus.watut.client.WatutRenderPipelines.STATUS;
  }

  @Override
  public void extract(QuadParticleRenderState state, Camera camera, float partialTick) {
    Quaternionf rotation = new Quaternionf(camera.rotation());
    if (useCustomRotation)
      rotation
          .identity()
          .mul(Axis.YP.rotationDegrees(Mth.lerp(partialTick, prevRotationYaw, rotationYaw)))
          .mul(Axis.XP.rotationDegrees(Mth.lerp(partialTick, prevRotationPitch, rotationPitch)))
          .mul(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, prevRotationRoll, rotationRoll)));
    else rotation.rotateZ(Mth.lerp(partialTick, oRoll, roll));
    extractRotatedQuad(state, camera, rotation, partialTick);
  }

  public void setPosPrev(double x, double y, double z) {
    xo = x;
    yo = y;
    zo = z;
  }

  public float getBrightness() {
    return brightness;
  }

  public void setBrightness(float value) {
    brightness = value;
  }
}
