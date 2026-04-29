package betel.nut.component;

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

	private boolean hasGeneratedBetelSkyIsland;
	private int islandX;
	private int islandY = 128;
	private int islandZ;
	private boolean hasSetSkyblockSpawn;

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

	public void markGenerated(BlockPos islandCenter) {
		this.hasGeneratedBetelSkyIsland = true;
		this.islandX = islandCenter.getX();
		this.islandY = islandCenter.getY();
		this.islandZ = islandCenter.getZ();
	}

	public void markSkyblockSpawnSet() {
		this.hasSetSkyblockSpawn = true;
	}

	public void resetBetelSkyIslandGeneration() {
		this.hasGeneratedBetelSkyIsland = false;
		this.islandX = 0;
		this.islandY = 128;
		this.islandZ = 0;
		this.hasSetSkyblockSpawn = false;
	}

	@Override
	public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		this.hasGeneratedBetelSkyIsland = tag.getBoolean(HAS_GENERATED_BETEL_SKY_ISLAND_KEY);
		this.islandX = tag.getInt(ISLAND_X_KEY);
		this.islandY = tag.contains(ISLAND_Y_KEY) ? tag.getInt(ISLAND_Y_KEY) : 128;
		this.islandZ = tag.getInt(ISLAND_Z_KEY);
		this.hasSetSkyblockSpawn = tag.getBoolean(HAS_SET_SKYBLOCK_SPAWN_KEY);
	}

	@Override
	public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		tag.putBoolean(HAS_GENERATED_BETEL_SKY_ISLAND_KEY, this.hasGeneratedBetelSkyIsland);
		tag.putInt(ISLAND_X_KEY, this.islandX);
		tag.putInt(ISLAND_Y_KEY, this.islandY);
		tag.putInt(ISLAND_Z_KEY, this.islandZ);
		tag.putBoolean(HAS_SET_SKYBLOCK_SPAWN_KEY, this.hasSetSkyblockSpawn);
	}
}
