package me.lyeddie.addon.mixin.impl.accessor;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface AccessorKeyBinding {

    @Accessor("key")
    InputConstants.Key getBoundKey();
}
