package com.breakinblocks.bbchat.quilt.mixin;

import com.breakinblocks.bbchat.quilt.common.BBChatInitializer;
import net.minecraft.server.Bootstrap;
import org.quiltmc.loader.api.QuiltLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Bootstrap.class)
public abstract class BootstrapMixin {
    /**
     * This a essentially QSL's "init" entrypoint without needing to depend on QSL,
     * since Quilt doesn't ship with their own entrypoint.
     */
    @Inject(method = "bootStrap()V", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/core/registries/BuiltInRegistries;bootStrap()V"))
    private static void bootStrap$bbchat$onInitialize(CallbackInfo ci) {
        for (var initializer : QuiltLoader.getEntrypointContainers(BBChatInitializer.ENTRYPOINT_KEY, BBChatInitializer.class)) {
            initializer.getEntrypoint().onInitialize(initializer.getProvider());
        }
    }
}
