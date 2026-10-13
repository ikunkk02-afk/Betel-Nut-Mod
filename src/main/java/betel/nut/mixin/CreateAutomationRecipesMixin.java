package betel.nut.mixin;

import betel.nut.compat.CreateRecipePolicy;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Optional target: no Create classes are linked when Create is absent. */
@Pseudo
@Mixin(targets = "com.simibubi.create.AllRecipeTypes", remap = false)
public abstract class CreateAutomationRecipesMixin {
    @Inject(method = "shouldIgnoreInAutomation", at = @At("HEAD"), cancellable = true, remap = false)
    private static void betelNut$useDedicatedProductionRoutes(RecipeHolder<?> recipe,
            CallbackInfoReturnable<Boolean> cir) {
        if (CreateRecipePolicy.requiresDedicatedRoute(recipe)) {
            cir.setReturnValue(true);
        }
    }
}
