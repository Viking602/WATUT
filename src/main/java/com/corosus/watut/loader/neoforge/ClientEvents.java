package com.corosus.watut.loader.neoforge;

import com.corosus.watut.ParticleRegistry;
import com.corosus.watut.WatutMod;
import com.corosus.watut.command.CommandWatutReloadJSON;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

public class ClientEvents {
    public void registerPipelines(net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent event) {
        event.registerPipeline(com.corosus.watut.client.WatutRenderPipelines.TRANSLUCENT);
    }

    public void getRegisteredParticles(TextureAtlasStitchedEvent event) {
        if (event.getAtlas().location().equals(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_PARTICLES))
            com.corosus.watut.client.screen.ScreenParticleRenderer.getInstance().close();
        ParticleRegistry.textureAtlasUpload(event.getAtlas());
    }

    public void onGameTick(ClientTickEvent.Post event) {
        WatutMod.getPlayerStatusManagerClient().tickGame();
    }

    public void onRegisterCommandsClient(RegisterClientCommandsEvent event) {
        CommandWatutReloadJSON.register(event.getDispatcher());
    }

    public void onMouse(InputEvent.MouseButton.Post event) {
        WatutMod.getPlayerStatusManagerClient().onMouse(event.getAction() != 0);
    }

    public void onKey(InputEvent.Key event) {
        WatutMod.getPlayerStatusManagerClient().onKey();
    }

}
