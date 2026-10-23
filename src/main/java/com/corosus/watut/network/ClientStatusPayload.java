package com.corosus.watut.network;

import com.corosus.watut.*;
import java.util.List;
import net.minecraft.nbt.CompoundTag;

/** Relay only client status fields; server config and item transfers are server-owned. */
public final class ClientStatusPayload {
  public static CompoundTag sanitize(CompoundTag input) {
    CompoundTag output = new CompoundTag();
    for (String key :
        List.of(
            WatutNetworking.NBTDataPlayerGuiStatus,
            WatutNetworking.NBTDataPlayerChatStatus,
            WatutNetworking.NBTDataPlayerIdleTicks,
            WatutNetworking.NBTDataPlayerScreenWidth,
            WatutNetworking.NBTDataPlayerScreenHeight,
            WatutNetworking.NBTDataPlayerScreenCompressedPixelDataSize,
            WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount,
            WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex))
      input.getInt(key).ifPresent(value -> output.putInt(key, value));
    for (String key :
        List.of(
            WatutNetworking.NBTDataPlayerGuiDontSendDetailedGUIInfo,
            WatutNetworking.NBTDataPlayerGuiDontSendItemInfo,
            WatutNetworking.NBTDataPlayerMousePressed))
      input.getBoolean(key).ifPresent(value -> output.putBoolean(key, value));
    for (String key :
        List.of(
            WatutNetworking.NBTDataPlayerMouseX,
            WatutNetworking.NBTDataPlayerMouseY,
            WatutNetworking.NBTDataPlayerTypingAmp))
      input
          .getFloat(key)
          .filter(Float::isFinite)
          .ifPresent(value -> output.putFloat(key, Math.clamp(value, -8F, 8F)));
    if (input.contains(WatutNetworking.NBTDataPlayerScreenCompressedPixelData)) {
      byte[] bytes =
          input
              .getByteArray(WatutNetworking.NBTDataPlayerScreenCompressedPixelData)
              .orElseGet(() -> new byte[0]);
      int w = output.getIntOr(WatutNetworking.NBTDataPlayerScreenWidth, 0),
          h = output.getIntOr(WatutNetworking.NBTDataPlayerScreenHeight, 0);
      int
          count =
              output.getIntOr(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketCount, 0),
          index =
              output.getIntOr(
                  WatutNetworking.NBTDataPlayerScreenCompressedPixelDataPacketIndex, -1);
      if (w < 1
          || h < 1
          || w > 1024
          || h > 1024
          || output.getIntOr(WatutNetworking.NBTDataPlayerScreenCompressedPixelDataSize, 0)
              != (long) w * h * 4
          || count < 1
          || count > 150
          || index < 0
          || index >= count
          || bytes.length < 1
          || bytes.length > 31000) return new CompoundTag();
      output.putByteArray(WatutNetworking.NBTDataPlayerScreenCompressedPixelData, bytes);
    }
    return output;
  }
}
