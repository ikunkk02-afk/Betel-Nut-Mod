package betel.nut.item;

import java.util.List;
import java.util.function.IntSupplier;

import betel.nut.BetelNutConfig;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.skyblock.BetelSkyblockManager;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BetelNutItem extends BetelNutFoodItem {
	private final IntSupplier addictionIncreaseSupplier;
	private final List<EffectSpec> effects;

	public BetelNutItem(Properties properties, int addictionIncrease, List<EffectSpec> effects) {
		super(properties);
		this.addictionIncreaseSupplier = () -> addictionIncrease;
		this.effects = effects;
	}

	public BetelNutItem(Properties properties, IntSupplier addictionIncreaseSupplier, List<EffectSpec> effects) {
		super(properties);
		this.addictionIncreaseSupplier = addictionIncreaseSupplier;
		this.effects = effects;
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		boolean isEnderBetelNut = stack.is(ModItems.ENDER_BETEL_NUT);
		ItemStack result = super.finishUsingItem(stack, level, entity);

		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			for (EffectSpec effect : this.effects) {
				player.addEffect(new MobEffectInstance(effect.effect(), effect.durationTicks(), effect.amplifier()));
			}

			if (BetelNutConfig.get().enableAddictionSystem) {
				BetelNutEntityComponents.ADDICTION.get(player).eatBetelNut(player,
						Math.max(0, this.addictionIncreaseSupplier.getAsInt()),
						level.getGameTime());
			}
			BetelSkyblockManager.onBetelNutEaten(player);

			if (isEnderBetelNut) {
				EnderBetelTeleportHandler.tryTeleport(player);
			}
		}

		return result;
	}

	public int getAddictionIncrease() {
		return Math.max(0, this.addictionIncreaseSupplier.getAsInt());
	}

	public record EffectSpec(Holder<MobEffect> effect, int durationTicks, int amplifier) {
	}
}
