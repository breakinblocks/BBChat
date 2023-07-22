package com.breakinblocks.bbchat.quilt.mixin;

import com.breakinblocks.bbchat.quilt.common.BBChatQuiltEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.TextFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public class ServerGamePacketListenerImplMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleChat(Lnet/minecraft/server/network/TextFilter$FilteredText;)V",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/server/players/PlayerList;broadcastMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Lnet/minecraft/network/chat/ChatType;Ljava/util/UUID;)V"))
    private void handleChat$bbchat$chatMessageSent(TextFilter.FilteredText filteredMessage, CallbackInfo ci) {
        Component rawComponent = new TranslatableComponent("chat.type.text", player.getDisplayName(), filteredMessage.getRaw());
        BBChatQuiltEvents.CHAT_MESSAGE_SENT.invoker().chatMessageSent(player, rawComponent);
    }
}
