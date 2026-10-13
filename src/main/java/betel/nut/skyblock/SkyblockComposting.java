package betel.nut.skyblock;

import betel.nut.platform.BetelPlatform;
import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;

/** Extra manual compost inputs only in the two enabled skyblock presets. */
public final class SkyblockComposting {
    public static void register() { BetelPlatform.onUseBlock(SkyblockComposting::use); }

    public static InteractionResult use(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
        if (!(world instanceof ServerLevel level) || player.isSpectator() || !player.mayBuild()
                || (!BetelSkyblockManager.isEnabled(level) && !BetelSkyblockManager.isOneBlockEnabled(level)))
            return InteractionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        ItemStack stack = player.getItemInHand(hand);
        float chance = chance(stack);
        if (!state.is(Blocks.COMPOSTER) || chance == 0) return InteractionResult.PASS;
        int fill = state.getValue(ComposterBlock.LEVEL);
        if (fill >= 7) return InteractionResult.PASS; // Vanilla handles ready bone meal and its delay.
        boolean added = fill == 0 || level.getRandom().nextDouble() < chance;
        if (added) {
            var next = state.setValue(ComposterBlock.LEVEL, fill + 1);
            level.setBlock(pos, next, Block.UPDATE_ALL);
            level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, next));
            if (fill + 1 == 7) level.scheduleTick(pos, Blocks.COMPOSTER, 20);
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        level.levelEvent(1500, pos, added ? 1 : 0);
        return InteractionResult.SUCCESS;
    }

    private static float chance(ItemStack stack) {
        if (stack.is(ModItems.RAW_BETEL_NUT)) return .65F;
        if (stack.is(ModItems.BETEL_LEAF) || stack.is(ModBlocks.BETEL_PALM_LEAVES.asItem())) return .3F;
        return 0;
    }

    private SkyblockComposting() {}
}
