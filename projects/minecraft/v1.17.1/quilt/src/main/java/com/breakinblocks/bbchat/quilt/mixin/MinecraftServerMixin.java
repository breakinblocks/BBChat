package com.breakinblocks.bbchat.quilt.mixin;

import com.breakinblocks.bbchat.quilt.common.BBChatQuiltEvents;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    @Inject(method = "runServer()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;initServer()Z"))
    private void runServer$bbchat$serverStarting(CallbackInfo ci) {
        //noinspection DataFlowIssue
        BBChatQuiltEvents.SERVER_STARTING.serverStarting((MinecraftServer) (Object) this);
    }

    @Inject(method = "runServer()V", at = @At(shift = At.Shift.AFTER, value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;updateStatusIcon(Lnet/minecraft/network/protocol/status/ServerStatus;)V"))
    private static void runServer$bbchat$serverStarted(CallbackInfo ci) {
        BBChatQuiltEvents.SERVER_STARTED.serverStarted();
    }

    @Inject(method = "halt(Z)V", at = @At("HEAD"))
    private static void halt$bbchat$serverStopping(boolean waitForServer, CallbackInfo ci) {
        BBChatQuiltEvents.SERVER_STOPPING.serverStopping();
    }

    @Inject(method = "halt(Z)V", at = @At("TAIL"))
    private static void halt$bbchat$serverStopped(boolean waitForServer, CallbackInfo ci) {
        BBChatQuiltEvents.SERVER_STOPPED.serverStopped();
    }
}
