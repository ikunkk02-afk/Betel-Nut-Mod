package betel.nut.advancement;

import betel.nut.BetelNutMod;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class BetelQuestAdvancements {
	public static final ResourceLocation ENTER_BETEL_SKYBLOCK = BetelNutMod
			.id("skyblock/enter_betel_skyblock");
	public static final ResourceLocation EAT_FIRST_BETEL_NUT = BetelNutMod
			.id("skyblock/eat_first_betel_nut");
	public static final ResourceLocation GET_FIRST_RESIDUE = BetelNutMod
			.id("skyblock/get_first_residue");
	public static final ResourceLocation ADDICTION_STARTED = BetelNutMod
			.id("skyblock/addiction_started");
	public static final ResourceLocation WITHDRAWAL_STARTED = BetelNutMod
			.id("skyblock/withdrawal_started");
	public static final ResourceLocation EAT_HECHENG_TIANXIA = BetelNutMod
			.id("skyblock/eat_hecheng_tianxia");
	public static final ResourceLocation END_FRAME_RITUAL = BetelNutMod
			.id("skyblock/end_frame_ritual");

	public static void grantEnterBetelSkyblock(ServerPlayer player) {
		grant(player, ENTER_BETEL_SKYBLOCK);
	}

	public static void grantEatFirstBetelNut(ServerPlayer player) {
		grant(player, EAT_FIRST_BETEL_NUT);
	}

	public static void grantGetFirstResidue(ServerPlayer player) {
		grant(player, GET_FIRST_RESIDUE);
	}

	public static void grantAddictionStarted(ServerPlayer player) {
		grant(player, ADDICTION_STARTED);
	}

	public static void grantWithdrawalStarted(ServerPlayer player) {
		grant(player, WITHDRAWAL_STARTED);
	}

	public static void grantEatHechengTianxia(ServerPlayer player) {
		grant(player, EAT_HECHENG_TIANXIA);
	}

	public static void grantEndFrameRitual(ServerPlayer player) {
		grant(player, END_FRAME_RITUAL);
	}

	private static void grant(ServerPlayer player, ResourceLocation id) {
		AdvancementHolder advancement = player.server.getAdvancements().get(id);
		if (advancement == null) {
			BetelNutMod.LOGGER.warn("FTB Quests helper advancement {} is not loaded; cannot grant it to {}.",
					id, player.getScoreboardName());
			return;
		}

		AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
		if (progress.isDone()) {
			return;
		}

		for (String criterion : progress.getRemainingCriteria()) {
			player.getAdvancements().award(advancement, criterion);
		}
	}

	private BetelQuestAdvancements() {
	}
}
