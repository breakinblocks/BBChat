package com.breakinblocks.bbchat.quilt.mixin;

import com.breakinblocks.bbchat.quilt.common.BBChatQuiltEvents;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin {
    @Shadow
    private ServerPlayer player;

    @Inject(method = "award(Lnet/minecraft/advancements/Advancement;Ljava/lang/String;)Z",
            at = @At(value = "INVOKE", shift = At.Shift.AFTER, target = "Lnet/minecraft/advancements/AdvancementRewards;grant(Lnet/minecraft/server/level/ServerPlayer;)V"))
    private void award$bbchat$advancementGranted(Advancement advancement, String criterionKey, CallbackInfoReturnable<Boolean> cir) {
        BBChatQuiltEvents.ADVANCEMENT_GRANTED.invoker().advancementGranted(player, advancement);
    }
}
