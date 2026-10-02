package me.bloodred.dss.client.mixin;

import me.bloodred.dss.client.screen.ScreenshotGalleryScreen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin extends Screen {

    protected PauseScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void addScreenshotGalleryButton(CallbackInfo info) {
        int buttonWidth = Math.min(100, Math.max(1, this.width - 12));
        addRenderableWidget(Button.builder(
                Component.literal("Screenshots"),
                button -> minecraft.gui.setScreen(new ScreenshotGalleryScreen(this))
        ).pos(this.width - buttonWidth - 6, this.height - 28).size(buttonWidth, 20).build());
    }
}
