package me.bloodred.dss.client;

import com.mojang.blaze3d.platform.InputConstants;
import me.bloodred.dss.DecentScreenshot;
import me.bloodred.dss.client.screen.ScreenshotGalleryScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class DecentScreenshotClient implements ClientModInitializer {

    public static KeyMapping openGalleryKey;

    @Override
    public void onInitializeClient() {
        // Register the keybind: default F10 (F12 is reserved by Minecraft for screenshots)
        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(DecentScreenshot.MOD_ID, "main")
        );

        openGalleryKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key." + DecentScreenshot.MOD_ID + ".open_gallery",
                InputConstants.KEY_F10,
                category
        ));

        // Poll keybind each tick on the client
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openGalleryKey.consumeClick()) {
                if (client.gui.screen() == null) {
                    client.gui.setScreen(new ScreenshotGalleryScreen(null));
                }
            }
        });

        // Release all cached textures when the client shuts down
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // No-op placeholder; cache cleanup handled by GalleryScreen.onClose()
        });

        DecentScreenshot.LOGGER.info("[DecentScreenshot] Client initialized. Press F10 to open the screenshot gallery.");
    }
}
