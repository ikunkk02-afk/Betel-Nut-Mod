package betel.nut.item;

import betel.nut.BetelNutMidnightConfig;
import betel.nut.addiction.AddictionStageUtil;
import betel.nut.effect.ModEffects;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffects;

/** One source for food rewards, tolerance scaling and displayed base effects. */
public enum BetelEffectProfile {
    WRAPPED(effect(MobEffects.MOVEMENT_SPEED, 10, 0)),
    ROASTED(effect(MobEffects.MOVEMENT_SPEED, 20, 0)),
    SWEET(effect(MobEffects.MOVEMENT_SPEED, 40, 0)),
    SPICY(effect(MobEffects.DAMAGE_BOOST, 20, 0), effect(MobEffects.HUNGER, 10, 0)),
    HONEY(effect(MobEffects.REGENERATION, 10, 0)),
    REFRESHING(effect(MobEffects.WATER_BREATHING, 60, 0)),
    NIGHT(effect(MobEffects.NIGHT_VISION, 90, 0)),
    ENERGIZING(effect(MobEffects.MOVEMENT_SPEED, 45, 1), effect(MobEffects.DIG_SPEED, 45, 0),
            effect(MobEffects.HUNGER, 15, 0)),
    GLOW(effect(MobEffects.NIGHT_VISION, 45, 0), effect(MobEffects.GLOWING, 45, 0)),
    PHANTOM(effect(MobEffects.SLOW_FALLING, 60, 0), effect(MobEffects.JUMP, 30, 0)),
    ENDER(),
    LAPIS(effect(MobEffects.LUCK, 120, 0), effect(MobEffects.JUMP, 30, 0)),
    QUARTZ(effect(MobEffects.DIG_SPEED, 60, 0)),
    MAGMA(effect(MobEffects.FIRE_RESISTANCE, 90, 0), effect(MobEffects.DAMAGE_BOOST, 30, 0),
            effect(MobEffects.HUNGER, 15, 0)),
    AMETHYST(effect(MobEffects.DAMAGE_RESISTANCE, 30, 0)),
    ECHO(effect(ModEffects.ECHO_SIGHT, 45, 0)),
    SYNTHETIC(),
    RICH(effect(MobEffects.MOVEMENT_SPEED, 75, 0), effect(MobEffects.NIGHT_VISION, 90, 0),
            effect(MobEffects.DAMAGE_RESISTANCE, 30, 0), effect(MobEffects.GLOWING, 15, 0)),
    UNDERGROUND(effect(MobEffects.NIGHT_VISION, 120, 0), effect(MobEffects.DIG_SPEED, 90, 1),
            effect(MobEffects.DAMAGE_RESISTANCE, 30, 0), effect(ModEffects.ECHO_SIGHT, 30, 0),
            effect(MobEffects.MOVEMENT_SLOWDOWN, 10, 0));

    private final List<EffectSpec> effects;

    BetelEffectProfile(EffectSpec... effects) {
        this.effects = List.of(effects);
    }

    public List<EffectSpec> baseEffects() {
        if (this != SYNTHETIC) return this.effects;
        int seconds = BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds;
        return List.of(effect(MobEffects.MOVEMENT_SPEED, seconds, 1),
                effect(MobEffects.DIG_SPEED, seconds, 1),
                effect(MobEffects.DAMAGE_RESISTANCE, Math.min(seconds, 60), 0),
                effect(MobEffects.ABSORPTION, Math.min(seconds, 30), 0));
    }

    public List<EffectSpec> scaledEffects(int addictionStage) {
        return baseEffects().stream().map(effect -> effect.scaled(addictionStage)).toList();
    }

    public static EffectSpec enderProtection(int addictionStage) {
        return effect(MobEffects.DAMAGE_RESISTANCE, 5, 0).scaled(addictionStage);
    }

    private static EffectSpec effect(Holder<MobEffect> effect, int seconds, int amplifier) {
        return new EffectSpec(effect, Math.max(1, seconds) * 20, amplifier);
    }

    public record EffectSpec(Holder<MobEffect> effect, int durationTicks, int amplifier) {
        public EffectSpec scaled(int addictionStage) {
            if (effect.value().getCategory() != MobEffectCategory.BENEFICIAL) return this;
            return new EffectSpec(effect,
                    Math.max(1, Math.round(durationTicks * AddictionStageUtil.getBetelRewardDurationMultiplier(addictionStage))),
                    Math.max(0, amplifier - AddictionStageUtil.getBetelRewardAmplifierPenalty(addictionStage)));
        }
    }
}
