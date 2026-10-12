package betel.nut.effect;

import betel.nut.BetelNutMod;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.core.Holder;
import java.util.function.BiConsumer;
import net.minecraft.world.entity.player.Player;

/** Persistent eating-day symptoms. Withdrawal relief cannot suppress these modifiers. */
public final class BetelDependenceEffect extends MobEffect {
    public BetelDependenceEffect() {
        super(MobEffectCategory.HARMFUL, 0xB46B35);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, BetelNutMod.id("dependence_movement"),
                -0.05, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.BLOCK_BREAK_SPEED, BetelNutMod.id("dependence_mining"),
                -0.10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.ATTACK_DAMAGE, BetelNutMod.id("dependence_attack"),
                -0.5, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public void createModifiers(int amplifier, BiConsumer<Holder<Attribute>, AttributeModifier> consumer) {
        super.createModifiers(amplifier, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ATTACK_DAMAGE)) {
                if (amplifier > 0) consumer.accept(attribute, new AttributeModifier(modifier.id(), -0.5 * amplifier, modifier.operation()));
            } else consumer.accept(attribute, modifier);
        });
    }

    @Override
    public void addAttributeModifiers(AttributeMap attributes, int amplifier) {
        removeAttributeModifiers(attributes);
        createModifiers(amplifier, (attribute, modifier) -> {
            var instance = attributes.getInstance(attribute);
            if (instance != null) instance.addPermanentModifier(modifier);
        });
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide() && entity instanceof Player player && !player.isCreative() && !player.isSpectator())
            player.causeFoodExhaustion(0.04F * (amplifier + 1));
        return true;
    }
}
