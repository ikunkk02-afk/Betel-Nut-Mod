package betel.nut.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public final class EchoSightEffect extends MobEffect {
    public EchoSightEffect() { super(MobEffectCategory.BENEFICIAL, 0x38C7BC); }
    @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 20 == 0; }
    @Override public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (!entity.level().isClientSide()) {
            entity.removeEffect(MobEffects.DARKNESS);
            if (entity instanceof Player) {
                for (LivingEntity target : entity.level().getEntitiesOfClass(LivingEntity.class,
                        entity.getBoundingBox().inflate(16), e -> e instanceof Enemy && e.isAlive())) {
                    target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 40, 0, false, false));
                }
            }
        }
        return true;
    }
}
