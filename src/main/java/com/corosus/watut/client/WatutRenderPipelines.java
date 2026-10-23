package com.corosus.watut.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.Identifier;

/** Status cards can be viewed from either side, as in the original renderer. */
public final class WatutRenderPipelines {
  public static final RenderPipeline TRANSLUCENT =
      RenderPipelines.TRANSLUCENT_PARTICLE.toBuilder()
          .withLocation(Identifier.fromNamespaceAndPath("watut", "pipeline/status_particles"))
          .withCull(false)
          .build();
  public static final SingleQuadParticle.Layer STATUS =
      new SingleQuadParticle.Layer(true, TextureAtlas.LOCATION_PARTICLES, TRANSLUCENT);

  private WatutRenderPipelines() {}
}
