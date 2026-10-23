package com.corosus.watut.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureManager;

/** Modern particles use vanilla groups, including their extraction and resource lifecycle. */
public final class CustomParticleEngine {
  public CustomParticleEngine(ClientLevel level, TextureManager textures) {}

  public void add(Particle particle) {
    Minecraft.getInstance().particleEngine.add(particle);
  }

  public void setLevel(ClientLevel level) {}
}
