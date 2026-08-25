package com.vitwo.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LeavesBlock.class)
public class LeavesDecayMixin {

    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void vitwo$cancelLeafDecayInTower(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (world != null && world.getRegistryKey().getValue().getPath().contains("tower")) {
            ci.cancel();
        }
    }

    @Inject(method = "scheduledTick", at = @At("HEAD"), cancellable = true)
    private void vitwo$cancelScheduledLeafDecayInTower(BlockState state, ServerWorld world, BlockPos pos, Random random, CallbackInfo ci) {
        if (world != null && world.getRegistryKey().getValue().getPath().contains("tower")) {
            ci.cancel();
        }
    }
}
