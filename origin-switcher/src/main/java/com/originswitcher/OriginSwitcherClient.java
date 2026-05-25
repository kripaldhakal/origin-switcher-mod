package com.originswitcher;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.originswitcher.command.OriginCommand;
import com.originswitcher.network.OriginPacketHandler;

@Environment(EnvType.CLIENT)
public class OriginSwitcherClient implements ClientModInitializer {

    public static final String MOD_ID = "originswitcher";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[OriginSwitcher] Initializing client-side origin switcher...");

        // Register our custom client-side commands
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            OriginCommand.register(dispatcher, registryAccess);
        });

        // Register custom packet channel for communicating with server
        // (only used if the server also has this mod, as a bonus feature)
        OriginPacketHandler.registerReceiver();

        LOGGER.info("[OriginSwitcher] Ready! Use /os setorigin <origin> to switch your origin.");
    }
}
