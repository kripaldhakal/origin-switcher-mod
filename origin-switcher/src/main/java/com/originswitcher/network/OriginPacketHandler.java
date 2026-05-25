package com.originswitcher.network;

import com.originswitcher.OriginSwitcherClient;
import com.originswitcher.util.OriginManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class OriginPacketHandler {

    public static final Identifier SET_ORIGIN_CHANNEL =
            new Identifier(OriginSwitcherClient.MOD_ID, "set_origin");

    public static final Identifier SYNC_ORIGIN_CHANNEL =
            new Identifier(OriginSwitcherClient.MOD_ID, "sync_origin");

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(SYNC_ORIGIN_CHANNEL,
            (client, handler, buf, responseSender) -> {
                String layerIdStr  = buf.readString(256);
                String originIdStr = buf.readString(256);

                client.execute(() -> {
                    if (client.player == null) return;
                    Identifier layerId  = new Identifier(layerIdStr);
                    Identifier originId = new Identifier(originIdStr);
                    boolean applied = OriginManager.applyOriginLocally(client.player, layerId, originId);
                    if (applied) {
                        OriginSwitcherClient.LOGGER.info(
                            "[OriginSwitcher] Server synced origin: {} on layer {}", originIdStr, layerIdStr);
                    }
                });
            });
    }

    public static void sendChangeRequest(Identifier layerId, Identifier originId) {
        if (!ClientPlayNetworking.canSend(SET_ORIGIN_CHANNEL)) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(layerId.toString());
        buf.writeString(originId.toString());
        ClientPlayNetworking.send(SET_ORIGIN_CHANNEL, buf);
    }
}
