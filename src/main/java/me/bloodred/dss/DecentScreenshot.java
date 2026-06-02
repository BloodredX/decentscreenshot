package me.bloodred.dss;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DecentScreenshot implements ModInitializer {

    public static final String MOD_ID = "decentscreenshot";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[DecentScreenshot] Initialized.");
    }
}
