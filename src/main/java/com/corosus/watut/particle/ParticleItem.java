package com.corosus.watut.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ItemPickupParticle;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Keep full item models by submitting the vanilla item pickup render state. */
public class ParticleItem extends ItemPickupParticle {
  public ParticleItem(
      ClientLevel level,
      float brightness,
      ItemStack stack,
      RenderBuffers buffers,
      EntityRenderDispatcher dispatcher,
      float xFrom,
      float yFrom,
      float zFrom,
      float xTo,
      float yTo,
      float zTo) {
    super(
        level,
        state(level, stack, dispatcher, xFrom, yFrom, zFrom),
        new ItemEntity(level, xTo, yTo, zTo, stack),
        Vec3.ZERO);
    targetX = targetXOld = xTo;
    targetY = targetYOld = yTo;
    targetZ = targetZOld = zTo;
  }

  private static EntityRenderState state(
      ClientLevel level,
      ItemStack stack,
      EntityRenderDispatcher dispatcher,
      float x,
      float y,
      float z) {
    ItemEntity entity = new ItemEntity(level, x, y, z, stack.copy());
    return dispatcher.extractEntity(entity, 0);
  }

  @Override
  public void tick() {
    if (++life >= 3) remove();
  }
}
