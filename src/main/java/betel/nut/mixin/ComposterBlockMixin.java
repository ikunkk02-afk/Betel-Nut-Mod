package betel.nut.mixin;

import betel.nut.advancement.BetelQuestAdvancements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ComposterBlock.class)
public abstract class ComposterBlockMixin {
    @Inject(method = "extractProduce", at = @At("RETURN"))
    private static void betelNutMod$awardActualCompost(Entity entity, BlockState state, Level level, BlockPos pos,
            CallbackInfoReturnable<BlockState> cir) {
        // A pickup or starter-kit bone meal is not evidence of composting. Require
        // vanilla's successful ready-to-empty transition with a real player.
        if (!level.isClientSide && entity instanceof ServerPlayer player
                && state.getValue(ComposterBlock.LEVEL) == 8 && cir.getReturnValue().getValue(ComposterBlock.LEVEL) == 0) {
            BetelQuestAdvancements.grantCompost(player);
        }
    }
}
