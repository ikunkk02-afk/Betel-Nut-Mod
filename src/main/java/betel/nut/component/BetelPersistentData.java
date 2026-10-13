package betel.nut.component;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;
/** Native attachment serialization around the existing field-level NBT contract. */
public interface BetelPersistentData extends INBTSerializable<CompoundTag> {
 void readFromNbt(CompoundTag tag, HolderLookup.Provider lookup);
 void writeToNbt(CompoundTag tag, HolderLookup.Provider lookup);
 @Override default CompoundTag serializeNBT(HolderLookup.Provider lookup) {
  CompoundTag tag = new CompoundTag(); writeToNbt(tag, lookup); return tag;
 }
 @Override default void deserializeNBT(HolderLookup.Provider lookup, CompoundTag tag) { readFromNbt(tag, lookup); }
}
