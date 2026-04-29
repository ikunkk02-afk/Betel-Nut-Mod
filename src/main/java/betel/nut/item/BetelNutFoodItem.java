package betel.nut.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BetelNutFoodItem extends Item {
	public BetelNutFoodItem(Properties properties) {
		super(properties);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		ItemStack result = super.finishUsingItem(stack, level, entity);

		if (level.isClientSide() || !(entity instanceof ServerPlayer player) || player.isCreative()) {
			return result;
		}

		ItemStack residue = new ItemStack(ModItems.BETEL_NUT_RESIDUE);
		if (result.isEmpty()) {
			return residue;
		}

		if (!player.getInventory().add(residue)) {
			player.drop(residue, false);
		}

		return result;
	}
}
