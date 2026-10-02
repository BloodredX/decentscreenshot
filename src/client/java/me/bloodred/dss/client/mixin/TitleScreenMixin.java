package me.bloodred.dss.client.mixin;

import me.bloodred.dss.client.screen.ScreenshotGalleryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addScreenshotGalleryButton(CallbackInfo info) {
        addRenderableWidget(Button.builder(
                Component.literal("Screenshots"),
                button -> minecraft.gui.setScreen(new ScreenshotGalleryScreen(this))
        ).pos(this.width - 106, this.height - 52).size(100, 20).build());
    }
}
