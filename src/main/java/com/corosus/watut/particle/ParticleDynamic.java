package com.corosus.watut.particle;

import com.corosus.watut.config.ConfigClient;
import com.corosus.watut.config.ConfigServerControlledSyncedToClient;
import net.minecraft.client.multiplayer.ClientLevel;

public class ParticleDynamic extends ParticleRotating {
  private final Layer layer;

  public ParticleDynamic(ClientLevel level, double x, double y, double z, Layer layer) {
    this(level, x, y, z, layer, 1F);
  }

  public ParticleDynamic(
      ClientLevel level, double x, double y, double z, Layer layer, float brightness) {
    super(level, x, y, z);
    this.layer = layer;
    lifetime = Integer.MAX_VALUE;
    gravity = 0;
    setSize(.2F, .2F);
    quadSize = .5F;
    xd = yd = zd = 0;
    setColor(brightness, brightness, brightness);
  }

  @Override
  public void tick() {
    super.tick();
    xo = x;
    yo = y;
    zo = z;
    age++;
  }

  @Override
  public float getQuadSize(float a) {
    return (float)
        (quadSize
            * ConfigClient.particleSizeScale
            * (ConfigServerControlledSyncedToClient.dynamicGuiShowClientsEntireScreen ? 4 : 2));
  }

  @Override
  protected Layer getLayer() {
    return layer;
  }

  @Override
  protected float getU0() {
    return 0;
  }

  @Override
  protected float getU1() {
    return 1;
  }

  @Override
  protected float getV0() {
    return 0;
  }

  @Override
  protected float getV1() {
    return 1;
  }
}
