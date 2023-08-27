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
     * This a temporary backport of QSL's "init" entrypoint from Minecraft 1.18.2.
     * If QSL is ever backported, we can use that instead.
     * There is a slight difference, as it seems that there is no "freezing" of registries in this older version.
     * So, we'll just init after wrappedStreams() is called.
     */
    @Inject(method = "bootStrap()V", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/server/Bootstrap;wrapStreams()V"))
    private static void bootStrap$bbchat$onInitialize(CallbackInfo ci) {
        for (var initializer : QuiltLoader.getEntrypointContainers(BBChatInitializer.ENTRYPOINT_KEY, BBChatInitializer.class)) {
            initializer.getEntrypoint().onInitialize(initializer.getProvider());
        }
    }
}
