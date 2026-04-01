package me.lyeddie.addon.mixin.meteor;

import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Helpers;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen implements Helpers {

    @Unique
    private boolean showd = false;

    public MixinTitleScreen(Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Ljava/lang/String;III)I", ordinal = 0))
    private void pastedMixinLol(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!showd) {
            showd = true;
            Managers.INST().info((TitleScreen) (Object) this);
        }
    }
}
