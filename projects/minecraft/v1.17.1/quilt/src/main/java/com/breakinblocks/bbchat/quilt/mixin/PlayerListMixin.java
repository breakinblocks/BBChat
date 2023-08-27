package com.breakinblocks.bbchat.quilt.mixin;

import com.breakinblocks.bbchat.quilt.common.BBChatQuiltEvents;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "placeNewPlayer(Lnet/minecraft/network/Connection;Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("TAIL"))
    private static void placeNewPlayer$bbchat$playerLogin(Connection netManager, ServerPlayer player, CallbackInfo ci) {
        BBChatQuiltEvents.PLAYER_LOGIN.playerLogin(player);
    }

    @Inject(method = "remove(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"))
    private static void remove$bbchat$playerLogout(ServerPlayer player, CallbackInfo ci) {
        BBChatQuiltEvents.PLAYER_LOGOUT.playerLogout(player);
    }
}
