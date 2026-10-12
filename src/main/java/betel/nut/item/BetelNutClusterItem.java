package betel.nut.item;

import betel.nut.block.BetelNutClusterBlock;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.block.Block;

/** Both inventory items place the same compatible block, with their own ripeness. */
public final class BetelNutClusterItem extends BlockItem {
    private final boolean unripe;

    public BetelNutClusterItem(Block block, int age) {
        super(block, new Properties().component(DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(BetelNutClusterBlock.AGE, age)));
        this.unripe = age == 1;
    }

    @Override public String getDescriptionId() {
        return this.unripe ? "item.betel-nut-mod.unripe_betel_nut_cluster" : super.getDescriptionId();
    }
}
