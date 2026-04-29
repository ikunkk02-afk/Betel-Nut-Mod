package betel.nut.component;

import betel.nut.BetelNutMod;
import net.minecraft.world.level.Level;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.world.WorldComponentInitializer;

public final class BetelNutWorldComponents implements WorldComponentInitializer {
	public static final ComponentKey<BetelSkyblockWorldComponent> SKYBLOCK_WORLD = ComponentRegistry.getOrCreate(
			BetelNutMod.id("skyblock_world"),
			BetelSkyblockWorldComponent.class);

	@Override
	public void registerWorldComponentFactories(WorldComponentFactoryRegistry registry) {
		registry.registerFor(Level.OVERWORLD, SKYBLOCK_WORLD, BetelSkyblockWorldComponent::new);
		BetelNutMod.LOGGER.info("Betel nut world Cardinal Components data registered successfully");
	}
}
