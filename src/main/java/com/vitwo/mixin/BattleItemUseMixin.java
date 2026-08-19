package com.vitwo.mixin;

import com.vitwo.battle.TowerBattleManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class BattleItemUseMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void vitwo$cancelBagItemsInTower(World world, PlayerEntity user, Hand hand, CallbackInfoReturnable<TypedActionResult<ItemStack>> cir) {
        if (!world.isClient() && TowerBattleManager.getInstance().isInTowerBattle(user.getUuid())) {
            ItemStack stack = (ItemStack) (Object) this;
            // Block direct potion / healing item consumption
            if (stack.getItem() instanceof PotionItem || stack.getItem().getTranslationKey().contains("potion") || stack.getItem().getTranslationKey().contains("revive") || stack.getItem().getTranslationKey().contains("heal")) {
                user.sendMessage(Text.translatable("vitwo.tower.bag_item_banned"), true);
                cir.setReturnValue(TypedActionResult.fail(stack));
            }
        }
    }
}
