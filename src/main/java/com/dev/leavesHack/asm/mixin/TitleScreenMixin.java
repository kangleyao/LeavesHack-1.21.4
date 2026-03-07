package com.dev.leavesHack.asm.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public class TitleScreenMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        var textRenderer = MinecraftClient.getInstance().textRenderer;
        String text = "Leaveshack by Leaves_aws | 1.21.4 Updated by kangleyao";
        int textWidth = textRenderer.getWidth(text);
        context.drawTextWithShadow(textRenderer, text, context.getScaledWindowWidth() - textWidth - 2, 32, 0xFFFF55);
    }
}
