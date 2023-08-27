package com.breakinblocks.bbchat.fabric.mixin;

import com.breakinblocks.bbchat.fabric.common.BBChatFabricEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({LivingEntity.class, ServerPlayer.class})
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "die(Lnet/minecraft/world/damagesource/DamageSource;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatTracker;recheckStatus()V"))
    private void die$bbchat$onDeath(DamageSource source, CallbackInfo ci) {
        if (getLevel().isClientSide) return;
        //noinspection DataFlowIssue
        BBChatFabricEvents.LIVING_ENTITY_DEATH.invoker().livingEntityDeath((LivingEntity) (Object) this, source);
    }
}
