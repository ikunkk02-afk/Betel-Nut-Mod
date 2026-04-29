package betel.nut.component;

import betel.nut.BetelNutMod;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentFactoryRegistry;
import org.ladysnake.cca.api.v3.entity.EntityComponentInitializer;

public final class BetelNutEntityComponents implements EntityComponentInitializer {
	public static final ComponentKey<BetelNutAddictionComponent> ADDICTION = ComponentRegistry.getOrCreate(
			BetelNutMod.id("addiction"),
			BetelNutAddictionComponent.class);
	public static final ComponentKey<BetelSkyblockPlayerComponent> SKYBLOCK_PLAYER = ComponentRegistry.getOrCreate(
			BetelNutMod.id("skyblock_player"),
			BetelSkyblockPlayerComponent.class);

	@Override
	public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
		registry.registerForPlayers(ADDICTION, BetelNutAddictionComponent::new,
				(from, to, registryLookup, lossless, keepInventory, sameCharacter) -> to.copyForRespawn(from,
						lossless));
		registry.registerForPlayers(SKYBLOCK_PLAYER, BetelSkyblockPlayerComponent::new,
				(from, to, registryLookup, lossless, keepInventory, sameCharacter) -> to.copyFrom(from,
						registryLookup));
		BetelNutMod.LOGGER.info("Betel nut Cardinal Components data registered successfully");
	}
}
