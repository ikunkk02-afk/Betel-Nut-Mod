package betel.nut.component;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.CopyableComponent;

public class BetelSkyblockPlayerComponent implements CopyableComponent<BetelSkyblockPlayerComponent> {
	private static final String HAS_JOINED_BETEL_SKYBLOCK_BEFORE_KEY = "hasJoinedBetelSkyblockBefore";
	private static final String HAS_EATEN_FIRST_BETEL_NUT_IN_SKYBLOCK_KEY = "hasEatenFirstBetelNutInSkyblock";
	private static final String HAS_RECEIVED_BETEL_ONE_BLOCK_STARTER_ITEMS_KEY =
			"hasReceivedBetelOneBlockStarterItems";
	private static final String LEGACY_HAS_EATEN_FIRST_SKYBLOCK_BETEL_KEY = "hasEatenFirstSkyblockBetel";
	private static final String LEGACY_HAS_COMPLETED_OPENING_CHALLENGE_KEY = "hasCompletedSkyblockOpeningChallenge";

	private boolean hasJoinedBetelSkyblockBefore;
	private boolean hasEatenFirstBetelNutInSkyblock;
	private boolean hasReceivedBetelOneBlockStarterItems;

	public BetelSkyblockPlayerComponent(Player player) {
	}

	public boolean hasJoinedBetelSkyblockBefore() {
		return this.hasJoinedBetelSkyblockBefore;
	}

	public void markJoinedBetelSkyblockBefore() {
		this.hasJoinedBetelSkyblockBefore = true;
	}

	public boolean hasEatenFirstBetelNutInSkyblock() {
		return this.hasEatenFirstBetelNutInSkyblock;
	}

	public void markEatenFirstBetelNutInSkyblock() {
		this.hasEatenFirstBetelNutInSkyblock = true;
	}

	public boolean hasReceivedBetelOneBlockStarterItems() {
		return this.hasReceivedBetelOneBlockStarterItems;
	}

	public void markReceivedBetelOneBlockStarterItems() {
		this.hasReceivedBetelOneBlockStarterItems = true;
	}

	@Override
	public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		this.hasJoinedBetelSkyblockBefore = tag.getBoolean(HAS_JOINED_BETEL_SKYBLOCK_BEFORE_KEY);
		this.hasEatenFirstBetelNutInSkyblock = tag.getBoolean(HAS_EATEN_FIRST_BETEL_NUT_IN_SKYBLOCK_KEY)
				|| tag.getBoolean(LEGACY_HAS_EATEN_FIRST_SKYBLOCK_BETEL_KEY)
				|| tag.getBoolean(LEGACY_HAS_COMPLETED_OPENING_CHALLENGE_KEY);
		this.hasReceivedBetelOneBlockStarterItems = tag.getBoolean(HAS_RECEIVED_BETEL_ONE_BLOCK_STARTER_ITEMS_KEY);
	}

	@Override
	public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		tag.putBoolean(HAS_JOINED_BETEL_SKYBLOCK_BEFORE_KEY, this.hasJoinedBetelSkyblockBefore);
		tag.putBoolean(HAS_EATEN_FIRST_BETEL_NUT_IN_SKYBLOCK_KEY, this.hasEatenFirstBetelNutInSkyblock);
		tag.putBoolean(HAS_RECEIVED_BETEL_ONE_BLOCK_STARTER_ITEMS_KEY,
				this.hasReceivedBetelOneBlockStarterItems);
	}

	@Override
	public void copyFrom(BetelSkyblockPlayerComponent other, HolderLookup.Provider registryLookup) {
		this.hasJoinedBetelSkyblockBefore = other.hasJoinedBetelSkyblockBefore;
		this.hasEatenFirstBetelNutInSkyblock = other.hasEatenFirstBetelNutInSkyblock;
		this.hasReceivedBetelOneBlockStarterItems = other.hasReceivedBetelOneBlockStarterItems;
	}
}
