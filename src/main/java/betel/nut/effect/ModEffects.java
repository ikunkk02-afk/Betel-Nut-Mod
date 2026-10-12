package betel.nut.effect;

import betel.nut.BetelNutMod;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public final class ModEffects {
    public static final Holder<MobEffect> BETEL_DEPENDENCE = register("betel_dependence", new BetelDependenceEffect());
    public static final Holder<MobEffect> ORAL_CANCER = register("oral_cancer",
            new StatusEffect(0xB84A57).addAttributeModifier(Attributes.MAX_HEALTH,
                    BetelNutMod.id("oral_cancer_health"), -2, AttributeModifier.Operation.ADD_VALUE));
    public static final Holder<MobEffect> MOUTH_LOCKED = register("mouth_locked", new StatusEffect(0xD5A679));
    public static final Holder<MobEffect> ECHO_SIGHT = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT,
            BetelNutMod.id("echo_sight"), new EchoSightEffect());

    private static Holder<MobEffect> register(String name, MobEffect effect) {
        return Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, BetelNutMod.id(name), effect);
    }
    private static final class StatusEffect extends MobEffect {
        private StatusEffect(int color) { super(MobEffectCategory.HARMFUL, color); }
    }
    public static void register() { }
    private ModEffects() { }
}
