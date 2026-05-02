package betel.nut.component;

import betel.nut.BetelNutMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import org.ladysnake.cca.api.v3.component.Component;

public class BetelSkyblockWorldComponent implements Component {
	private static final String HAS_GENERATED_BETEL_SKY_ISLAND_KEY = "hasGeneratedBetelSkyIsland";
	private static final String ISLAND_X_KEY = "islandX";
	private static final String ISLAND_Y_KEY = "islandY";
	private static final String ISLAND_Z_KEY = "islandZ";
	private static final String HAS_SET_SKYBLOCK_SPAWN_KEY = "hasSetSkyblockSpawn";
	private static final String HAS_GENERATED_BETEL_ONE_BLOCK_ISLAND_KEY = "hasGeneratedBetelOneBlockIsland";
	private static final String ONE_BLOCK_X_KEY = "oneBlockX";
	private static final String ONE_BLOCK_Y_KEY = "oneBlockY";
	private static final String ONE_BLOCK_Z_KEY = "oneBlockZ";
	private static final String HAS_SET_BETEL_ONE_BLOCK_SPAWN_KEY = "hasSetBetelOneBlockSpawn";
	private static final String BETEL_SKY_ISLAND_RESET_PENDING_KEY = "betelSkyIslandResetPending";
	private static final String BETEL_ONE_BLOCK_ISLAND_RESET_PENDING_KEY = "betelOneBlockIslandResetPending";

	private boolean hasGeneratedBetelSkyIsland;
	private int islandX;
	private int islandY = 128;
	private int islandZ;
	private boolean hasSetSkyblockSpawn;
	private boolean hasGeneratedBetelOneBlockIsland;
	private int oneBlockX;
	private int oneBlockY = 128;
	private int oneBlockZ;
	private boolean hasSetBetelOneBlockSpawn;
	private boolean betelSkyIslandResetPending;
	private boolean betelOneBlockIslandResetPending;

	public BetelSkyblockWorldComponent(Level level) {
	}

	public boolean hasGeneratedBetelSkyIsland() {
		return this.hasGeneratedBetelSkyIsland;
	}

	public BlockPos getIslandCenter() {
		return new BlockPos(this.islandX, this.islandY, this.islandZ);
	}

	public boolean hasSetSkyblockSpawn() {
		return this.hasSetSkyblockSpawn;
	}

	public boolean hasGeneratedBetelOneBlockIsland() {
		return this.hasGeneratedBetelOneBlockIsland;
	}

	public BlockPos getOneBlockCenter() {
		return new BlockPos(this.oneBlockX, this.oneBlockY, this.oneBlockZ);
	}

	public boolean hasSetBetelOneBlockSpawn() {
		return this.hasSetBetelOneBlockSpawn;
	}

	public boolean isBetelSkyIslandResetPending() {
		return this.betelSkyIslandResetPending;
	}

	public boolean isBetelOneBlockIslandResetPending() {
		return this.betelOneBlockIslandResetPending;
	}

	public void markGenerated(BlockPos islandCenter) {
		this.hasGeneratedBetelSkyIsland = true;
		this.islandX = islandCenter.getX();
		this.islandY = islandCenter.getY();
		this.islandZ = islandCenter.getZ();
		this.betelSkyIslandResetPending = false;
	}

	public void markSkyblockSpawnSet() {
		this.hasSetSkyblockSpawn = true;
	}

	public void markOneBlockGenerated(BlockPos oneBlockCenter) {
		this.hasGeneratedBetelOneBlockIsland = true;
		this.oneBlockX = oneBlockCenter.getX();
		this.oneBlockY = oneBlockCenter.getY();
		this.oneBlockZ = oneBlockCenter.getZ();
		this.betelOneBlockIslandResetPending = false;
	}

	public void markBetelOneBlockSpawnSet() {
		this.hasSetBetelOneBlockSpawn = true;
	}

	public void resetBetelSkyIslandGeneration() {
		this.hasGeneratedBetelSkyIsland = false;
		this.islandX = 0;
		this.islandY = 128;
		this.islandZ = 0;
		this.hasSetSkyblockSpawn = false;
		this.betelSkyIslandResetPending = true;
	}

	public void resetBetelOneBlockIslandGeneration() {
		this.hasGeneratedBetelOneBlockIsland = false;
		this.oneBlockX = 0;
		this.oneBlockY = 128;
		this.oneBlockZ = 0;
		this.hasSetBetelOneBlockSpawn = false;
		this.betelOneBlockIslandResetPending = true;
	}

	@Override
	public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Loading skyblock world component...");
		this.hasGeneratedBetelSkyIsland = tag.getBoolean(HAS_GENERATED_BETEL_SKY_ISLAND_KEY);
		this.islandX = tag.getInt(ISLAND_X_KEY);
		this.islandY = tag.contains(ISLAND_Y_KEY) ? tag.getInt(ISLAND_Y_KEY) : 128;
		this.islandZ = tag.getInt(ISLAND_Z_KEY);
		this.hasSetSkyblockSpawn = tag.getBoolean(HAS_SET_SKYBLOCK_SPAWN_KEY);
		this.hasGeneratedBetelOneBlockIsland = tag.getBoolean(HAS_GENERATED_BETEL_ONE_BLOCK_ISLAND_KEY);
		this.oneBlockX = tag.getInt(ONE_BLOCK_X_KEY);
		this.oneBlockY = tag.contains(ONE_BLOCK_Y_KEY) ? tag.getInt(ONE_BLOCK_Y_KEY) : 128;
		this.oneBlockZ = tag.getInt(ONE_BLOCK_Z_KEY);
		this.hasSetBetelOneBlockSpawn = tag.getBoolean(HAS_SET_BETEL_ONE_BLOCK_SPAWN_KEY);
		this.betelSkyIslandResetPending = tag.getBoolean(BETEL_SKY_ISLAND_RESET_PENDING_KEY);
		this.betelOneBlockIslandResetPending = tag.getBoolean(BETEL_ONE_BLOCK_ISLAND_RESET_PENDING_KEY);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] hasGeneratedBetelSkyIsland = {}",
				this.hasGeneratedBetelSkyIsland);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] hasGeneratedBetelOneBlockIsland = {}",
				this.hasGeneratedBetelOneBlockIsland);
	}

	@Override
	public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Saving skyblock world component...");
		tag.putBoolean(HAS_GENERATED_BETEL_SKY_ISLAND_KEY, this.hasGeneratedBetelSkyIsland);
		tag.putInt(ISLAND_X_KEY, this.islandX);
		tag.putInt(ISLAND_Y_KEY, this.islandY);
		tag.putInt(ISLAND_Z_KEY, this.islandZ);
		tag.putBoolean(HAS_SET_SKYBLOCK_SPAWN_KEY, this.hasSetSkyblockSpawn);
		tag.putBoolean(HAS_GENERATED_BETEL_ONE_BLOCK_ISLAND_KEY, this.hasGeneratedBetelOneBlockIsland);
		tag.putInt(ONE_BLOCK_X_KEY, this.oneBlockX);
		tag.putInt(ONE_BLOCK_Y_KEY, this.oneBlockY);
		tag.putInt(ONE_BLOCK_Z_KEY, this.oneBlockZ);
		tag.putBoolean(HAS_SET_BETEL_ONE_BLOCK_SPAWN_KEY, this.hasSetBetelOneBlockSpawn);
		tag.putBoolean(BETEL_SKY_ISLAND_RESET_PENDING_KEY, this.betelSkyIslandResetPending);
		tag.putBoolean(BETEL_ONE_BLOCK_ISLAND_RESET_PENDING_KEY, this.betelOneBlockIslandResetPending);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Saved hasGeneratedBetelSkyIsland = {}",
				this.hasGeneratedBetelSkyIsland);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Saved hasGeneratedBetelOneBlockIsland = {}",
				this.hasGeneratedBetelOneBlockIsland);
	}
}
