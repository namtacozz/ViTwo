package com.vitwo.mixin;

import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.battles.BattleSide;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BattleSide.class, remap = false)
public interface BattleSideAccessor {
    @Accessor("actors")
    @Mutable
    void vitwo$setActors(BattleActor[] actors);
}
