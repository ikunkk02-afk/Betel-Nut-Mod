package betel.nut.network;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record AddictionSyncPayload(
		int addictionValue,
		int addictionStage,
		int withdrawalSeverity,
		int nextWithdrawalTicks,
		int maxAddictionValue,
		int soberDays, boolean ateToday, int oralCancerStage, int mouthRestrictionLevel) implements CustomPacketPayload {
	public static final Type<AddictionSyncPayload> TYPE = new Type<>(BetelNutMod.id("addiction_sync"));
	public static final StreamCodec<RegistryFriendlyByteBuf, AddictionSyncPayload> CODEC = StreamCodec.of(
			(buf, p) -> {
				buf.writeVarInt(p.addictionValue()); buf.writeVarInt(p.addictionStage());
				buf.writeVarInt(p.withdrawalSeverity()); buf.writeVarInt(p.nextWithdrawalTicks());
				buf.writeVarInt(p.maxAddictionValue()); buf.writeVarInt(p.soberDays());
				buf.writeBoolean(p.ateToday()); buf.writeVarInt(p.oralCancerStage());
				buf.writeVarInt(p.mouthRestrictionLevel());
			}, buf -> new AddictionSyncPayload(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
					buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readBoolean(), buf.readVarInt(), buf.readVarInt()));

	public static void register() {
		PayloadTypeRegistry.playS2C().register(TYPE, CODEC);
	}

	public static void send(ServerPlayer player) {
		if (player.connection == null || !ServerPlayNetworking.canSend(player, TYPE)) {
			return;
		}

		AddictionSyncPayload payload = fromPlayer(player);
		BetelNutMod.LOGGER.debug(
				"[BetelNut Debug] Sync to client: addictionValue={}, stage={}, withdrawalSeverity={}, nextWithdrawalTicks={}",
				payload.addictionValue(), payload.addictionStage(), payload.withdrawalSeverity(),
				payload.nextWithdrawalTicks());
		ServerPlayNetworking.send(player, payload);
	}

	public static AddictionSyncPayload fromPlayer(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem) {
			return new AddictionSyncPayload(0, 0, 0, -1, Math.max(1, config.maxAddictionValue), 0, false, 0, 0);
		}

		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		int maxAddictionValue = Math.max(1, config.maxAddictionValue);
		int addictionValue = addiction.getAddictionValue();
		int addictionStage = addiction.getAddictionStage();
		int withdrawalSeverity = addiction.getWithdrawalSeverity();
		int nextWithdrawalTicks = addiction.getNextWithdrawalTicks(player);
		return new AddictionSyncPayload(addictionValue, addictionStage, withdrawalSeverity, nextWithdrawalTicks,
				maxAddictionValue, addiction.getSoberDays(), addiction.hasEatenToday(), addiction.getOralCancerStage(),
				switch (betel.nut.event.WithdrawalEatingRestrictions.getRestrictionLevel(player, addiction)) {
					case NONE -> 0;
					case SOFT_FOODS -> 1;
					default -> 2;
				});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
