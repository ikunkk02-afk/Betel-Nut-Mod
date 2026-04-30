package betel.nut.component;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.advancement.BetelQuestAdvancements;
import betel.nut.addiction.AddictionStageUtil;
import betel.nut.effect.HechengTianxiaEffects;
import betel.nut.message.BetelMessages;
import betel.nut.network.AddictionSyncPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import org.ladysnake.cca.api.v3.component.CopyableComponent;

public class BetelNutAddictionComponent implements CopyableComponent<BetelNutAddictionComponent> {
	private static final String ADDICTION_VALUE_KEY = "addictionValue";
	private static final String WITHDRAWAL_VALUE_KEY = "withdrawalValue";
	private static final String LAST_EAT_TIME_KEY = "lastEatTime";
	private static final String CLEAN_TIME_KEY = "cleanTime";
	private static final String NEXT_WITHDRAWAL_TIME_KEY = "nextWithdrawalTime";
	private static final String NOTIFIED_WITHDRAWAL_STAGE_KEY = "notifiedWithdrawalStage";
	private static final String HECHENG_TIANXIA_POSITIVE_TICKS_KEY = "hechengTianxiaPositiveTicks";
	private static final String HECHENG_TIANXIA_COMA_TICKS_KEY = "hechengTianxiaComaTicks";
	private static final String HECHENG_TIANXIA_NEGATIVE_TICKS_KEY = "hechengTianxiaNegativeTicks";
	private static final int VISIBLE_WITHDRAWAL_THRESHOLD = 25;
	private static final ResourceLocation WITHDRAWAL_MAX_HEALTH_PENALTY_ID = BetelNutMod
			.id("withdrawal_max_health_penalty");

	private int addictionValue;
	private int withdrawalValue;
	private long lastEatTime;
	private long cleanTime;
	private long nextWithdrawalTime = -1;
	private int notifiedWithdrawalStage;
	private long nextFeedbackTime;
	private long nextEatingRestrictionMessageTime;
	private long respawnWithdrawalCheckTime = -1;
	private boolean showRespawnWithdrawalMessage;
	private int hechengTianxiaPositiveTicks;
	private int hechengTianxiaComaTicks;
	private int hechengTianxiaNegativeTicks;
	private long nextHechengTianxiaMessageTime;

	public BetelNutAddictionComponent(Player player) {
	}

	public int getAddictionValue() {
		return this.addictionValue;
	}

	public int getWithdrawalValue() {
		return this.withdrawalValue;
	}

	public long getLastEatTime() {
		return this.lastEatTime;
	}

	public long getCleanTime() {
		return this.cleanTime;
	}

	public int getNotifiedWithdrawalStage() {
		return this.notifiedWithdrawalStage;
	}

	public int getAddictionStage() {
		return AddictionStageUtil.getStage(this.addictionValue, BetelNutConfig.get().maxAddictionValue);
	}

	public int getWithdrawalSeverity() {
		if (this.withdrawalValue < visibleWithdrawalThreshold(BetelNutConfig.get())) {
			return 0;
		}
		return AddictionStageUtil.getWithdrawalSeverity(getAddictionStage());
	}

	public int getWithdrawalStage() {
		return getWithdrawalSeverity();
	}

	public int getNextWithdrawalTicks(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		long gameTime = player.level().getGameTime();
		ensureWithdrawalTimer(config, gameTime);
		return AddictionStageUtil.getNextWithdrawalTicks(this.nextWithdrawalTime, gameTime);
	}

	public double getCurrentMaxHealthPenalty(ServerPlayer player) {
		AttributeModifier modifier = getWithdrawalMaxHealthModifier(player);
		return modifier == null ? 0.0D : Math.max(0.0D, -modifier.amount());
	}

	public boolean hasWithdrawalMaxHealthPenalty(ServerPlayer player) {
		return getWithdrawalMaxHealthModifier(player) != null;
	}

	public void eatBetelNut(ServerPlayer player, int addictionIncrease, long gameTime) {
		eatBetelNut(player, addictionIncrease, gameTime, false);
	}

	public void eatBetelNut(ServerPlayer player, int addictionIncrease, long gameTime,
			boolean clearWithdrawalEffectsOnEat) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem) {
			return;
		}

		int previousAddiction = this.addictionValue;
		int previousStage = getAddictionStage();
		int previousWithdrawal = this.withdrawalValue;
		boolean preserveActiveWithdrawal = !clearWithdrawalEffectsOnEat
				&& previousWithdrawal >= visibleWithdrawalThreshold(config);
		this.addictionValue = clamp(this.addictionValue + addictionIncrease, 0, config.maxAddictionValue);
		if (clearWithdrawalEffectsOnEat) {
			this.withdrawalValue = 0;
			this.notifiedWithdrawalStage = 0;
		}
		this.lastEatTime = gameTime;
		if (preserveActiveWithdrawal) {
			if (this.nextWithdrawalTime < 0) {
				this.nextWithdrawalTime = gameTime;
			}
		} else {
			scheduleNextWithdrawal(config, gameTime);
		}
		boolean removedMaxHealthPenalty = clearWithdrawalEffectsOnEat && clearWithdrawalPenalties(player);

		if (clearWithdrawalEffectsOnEat && previousWithdrawal >= visibleWithdrawalThreshold(config)) {
			sendFeedback(player, gameTime, BetelMessages.WITHDRAWAL_SUPPRESSED);
		} else {
			sendFeedback(player, gameTime,
					BetelMessages.betelNutEatenMessage(previousAddiction, this.addictionValue,
							config.maxAddictionValue));
		}
		if (clearWithdrawalEffectsOnEat) {
			sendRecoveryFeedbackIfNeeded(player, removedMaxHealthPenalty);
		}
		if (previousStage < 1 && getAddictionStage() >= 1) {
			BetelQuestAdvancements.grantAddictionStarted(player);
		}
		AddictionSyncPayload.send(player);
	}

	public boolean hasActiveHechengTianxiaAftermath() {
		return this.hechengTianxiaPositiveTicks > 0
				|| this.hechengTianxiaComaTicks > 0
				|| this.hechengTianxiaNegativeTicks > 0;
	}

	public boolean isHechengTianxiaComatose() {
		return this.hechengTianxiaComaTicks > 0;
	}

	public boolean startHechengTianxiaAftermath(ServerPlayer player, boolean allowRepeat) {
		if (!allowRepeat && hasActiveHechengTianxiaAftermath()) {
			sendHechengTianxiaRepeatBlockedMessage(player);
			return false;
		}

		this.hechengTianxiaPositiveTicks = HechengTianxiaEffects.positiveDurationTicks();
		this.hechengTianxiaComaTicks = 0;
		this.hechengTianxiaNegativeTicks = 0;
		HechengTianxiaEffects.applyPositiveBurst(player);
		BetelNutMod.LOGGER.info(
				"Started synthetic world betel aftereffect for {}: positiveTicks={}, comaTicks={}, negativeTicks={}",
				player.getScoreboardName(), this.hechengTianxiaPositiveTicks,
				HechengTianxiaEffects.comaDurationTicks(), HechengTianxiaEffects.negativeDurationTicks());
		return true;
	}

	public void tickHechengTianxiaAftereffects(ServerPlayer player) {
		if (this.hechengTianxiaPositiveTicks > 0) {
			this.hechengTianxiaPositiveTicks--;
			if (this.hechengTianxiaPositiveTicks == 0) {
				enterHechengTianxiaComa(player);
			}
		}

		if (this.hechengTianxiaComaTicks > 0) {
			HechengTianxiaEffects.maintainComa(player, this.hechengTianxiaComaTicks);
			this.hechengTianxiaComaTicks--;
			if (this.hechengTianxiaComaTicks == 0) {
				finishHechengTianxiaComa(player);
			}
			return;
		}

		if (this.hechengTianxiaNegativeTicks > 0) {
			HechengTianxiaEffects.maintainNegativeAftermath(player, this.hechengTianxiaNegativeTicks);
			this.hechengTianxiaNegativeTicks--;
		}
	}

	public boolean blockHechengTianxiaActionIfComatose(ServerPlayer player, boolean notify) {
		if (!isHechengTianxiaComatose()) {
			return false;
		}

		if (notify) {
			sendHechengTianxiaMessage(player, BetelMessages.HECHENG_TIANXIA_COMA_BLOCKED);
		}
		return true;
	}

	public void sendHechengTianxiaRepeatBlockedMessage(ServerPlayer player) {
		sendHechengTianxiaMessage(player, BetelMessages.HECHENG_TIANXIA_REPEAT_BLOCKED);
	}

	public void applyMilkRelief(ServerPlayer player, long gameTime) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem || !hasAddictionData()) {
			return;
		}

		this.cleanTime = Math.max(this.cleanTime, gameTime + config.milkReliefDurationTicks);
		scheduleNextWithdrawal(config, gameTime);
		boolean removedMaxHealthPenalty = clearWithdrawalPenalties(player);
		sendFeedback(player, gameTime, BetelMessages.MILK_RELIEF);
		sendRecoveryFeedbackIfNeeded(player, removedMaxHealthPenalty);
		AddictionSyncPayload.send(player);
	}

	public void applyGoldenAppleRecovery(ServerPlayer player, long gameTime) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem || !hasAddictionData()) {
			return;
		}

		boolean hadMaxHealthPenalty = hasWithdrawalMaxHealthPenalty(player);
		this.addictionValue = clamp(this.addictionValue - config.goldenAppleAddictionReduction, 0,
				config.maxAddictionValue);
		this.withdrawalValue = clamp(this.withdrawalValue - config.goldenAppleWithdrawalReduction, 0,
				config.maxWithdrawalValue);
		syncNotifiedWithdrawalStageAfterDecrease();

		boolean removedMaxHealthPenalty = false;
		if (this.withdrawalValue < visibleWithdrawalThreshold(config)) {
			removedMaxHealthPenalty = clearWithdrawalPenalties(player);
		} else {
			applyWithdrawalEffects(player, config);
		}
		scheduleNextWithdrawal(config, gameTime);

		sendFeedback(player, gameTime, BetelMessages.GOLDEN_APPLE_RECOVERY);
		sendRecoveryFeedbackIfNeeded(player,
				removedMaxHealthPenalty || (hadMaxHealthPenalty && !hasWithdrawalMaxHealthPenalty(player)));
		AddictionSyncPayload.send(player);
	}

	public void applyEnchantedGoldenAppleRecovery(ServerPlayer player, long gameTime) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem || !hasAddictionData()) {
			return;
		}

		this.addictionValue = clamp(this.addictionValue - config.enchantedGoldenAppleAddictionReduction, 0,
				config.maxAddictionValue);
		this.withdrawalValue = 0;
		this.notifiedWithdrawalStage = 0;
		this.cleanTime = Math.max(this.cleanTime, gameTime + config.enchantedGoldenAppleCleanTimeTicks);
		scheduleNextWithdrawal(config, gameTime);
		boolean removedMaxHealthPenalty = clearWithdrawalPenalties(player);
		sendFeedback(player, gameTime, BetelMessages.ENCHANTED_GOLDEN_APPLE_RECOVERY);
		sendRecoveryFeedbackIfNeeded(player, removedMaxHealthPenalty);
		AddictionSyncPayload.send(player);
	}

	public void serverTick(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem) {
			this.nextWithdrawalTime = -1;
			clearWithdrawalPenalties(player);
			return;
		}

		long gameTime = player.level().getGameTime();

		if (this.addictionValue <= 0) {
			this.withdrawalValue = 0;
			this.notifiedWithdrawalStage = 0;
			this.nextWithdrawalTime = -1;
			clearWithdrawalPenalties(player);
			return;
		}

		if (this.lastEatTime <= 0 || gameTime < this.lastEatTime) {
			this.lastEatTime = gameTime;
			scheduleNextWithdrawal(config, gameTime);
		}

		ensureWithdrawalTimer(config, gameTime);
		int addictionStage = getAddictionStage();
		int withdrawalTicks = AddictionStageUtil.getNextWithdrawalTicks(this.nextWithdrawalTime, gameTime);
		if (player.server.getTickCount() % 20 == 0) {
			BetelNutMod.LOGGER.debug(
					"[BetelNut Debug] Server tick: player={}, addictionValue={}, stage={}, withdrawalTicks={}",
					player.getScoreboardName(), this.addictionValue, addictionStage, withdrawalTicks);
		}

		if (this.cleanTime > gameTime) {
			clearWithdrawalPenalties(player);
			return;
		}

		if (withdrawalTicks < 0) {
			return;
		}

		if (withdrawalTicks > 0) {
			return;
		}

		BetelNutMod.LOGGER.debug(
				"[BetelNut Debug] Withdrawal should trigger: player={}, stage={}, withdrawalTicks={}",
				player.getScoreboardName(), addictionStage, withdrawalTicks);
		triggerWithdrawal(player, config, gameTime);
	}

	public void setAddictionValue(int value) {
		BetelNutConfig config = BetelNutConfig.get();
		this.addictionValue = clamp(value, 0, config.maxAddictionValue);
		if (this.addictionValue <= 0) {
			this.withdrawalValue = 0;
			this.notifiedWithdrawalStage = 0;
			this.nextWithdrawalTime = -1;
		}
	}

	public void setAddictionValueAndResetLastEatTime(ServerPlayer player, int value) {
		setAddictionValue(value);
		resetLastEatTime(player);
	}

	public void addAddictionValue(int value) {
		setAddictionValue(this.addictionValue + value);
	}

	public void resetLastEatTime(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		long gameTime = player.level().getGameTime();
		this.lastEatTime = gameTime;
		this.withdrawalValue = 0;
		this.notifiedWithdrawalStage = 0;
		scheduleNextWithdrawal(config, gameTime);
		clearWithdrawalPenalties(player);
		AddictionSyncPayload.send(player);
	}

	public void setWithdrawalValue(ServerPlayer player, int value) {
		BetelNutConfig config = BetelNutConfig.get();
		this.withdrawalValue = clamp(value, 0, config.maxWithdrawalValue);
		syncNotifiedWithdrawalStageAfterDecrease();
		long gameTime = player.level().getGameTime();

		if (this.withdrawalValue < visibleWithdrawalThreshold(config)) {
			clearWithdrawalPenalties(player);
		} else {
			applyWithdrawalEffects(player, config);
		}
		scheduleNextWithdrawal(config, gameTime);
		AddictionSyncPayload.send(player);
	}

	public void refreshWithdrawalEffects(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		syncNotifiedWithdrawalStageAfterDecrease();

		long gameTime = player.level().getGameTime();
		if (!config.enableAddictionSystem || this.addictionValue <= 0
				|| this.withdrawalValue < visibleWithdrawalThreshold(config)
				|| this.cleanTime > gameTime) {
			clearWithdrawalPenalties(player);
		} else {
			applyWithdrawalEffects(player, config);
		}
		scheduleNextWithdrawal(config, gameTime);
		AddictionSyncPayload.send(player);
	}

	public void scheduleRespawnWithdrawalCheck(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		this.respawnWithdrawalCheckTime = -1;
		this.showRespawnWithdrawalMessage = false;

		if (!config.enableAddictionSystem || !config.reapplyWithdrawalAfterRespawn
				|| this.addictionValue <= 0 || this.withdrawalValue < visibleWithdrawalThreshold(config)) {
			return;
		}

		long gameTime = player.level().getGameTime();
		if (this.cleanTime > gameTime) {
			return;
		}

		this.respawnWithdrawalCheckTime = gameTime + config.respawnWithdrawalDelayTicks;
		this.showRespawnWithdrawalMessage = true;
	}

	public void handleRespawnWithdrawalCheck(ServerPlayer player) {
		if (this.respawnWithdrawalCheckTime < 0) {
			return;
		}

		long gameTime = player.level().getGameTime();
		if (gameTime < this.respawnWithdrawalCheckTime) {
			return;
		}

		boolean shouldShowMessage = this.showRespawnWithdrawalMessage;
		this.respawnWithdrawalCheckTime = -1;
		this.showRespawnWithdrawalMessage = false;

		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem || !config.reapplyWithdrawalAfterRespawn
				|| this.addictionValue <= 0 || this.withdrawalValue < visibleWithdrawalThreshold(config)
				|| this.cleanTime > gameTime) {
			return;
		}

		applyWithdrawalEffects(player, config);
		this.nextWithdrawalTime = gameTime + config.withdrawalCheckIntervalTicks;
		if (shouldShowMessage) {
			BetelMessages.send(player, BetelMessages.WITHDRAWAL_CONTINUES_AFTER_DEATH);
		}
		AddictionSyncPayload.send(player);
	}

	public void triggerWithdrawalTest(ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableAddictionSystem) {
			return;
		}

		long gameTime = player.level().getGameTime();
		int minimumAddiction = Math.max(1, config.minimumAddictionForWithdrawal);
		if (this.addictionValue < minimumAddiction) {
			this.addictionValue = clamp(minimumAddiction, 1, config.maxAddictionValue);
		}

		this.cleanTime = 0;
		this.nextWithdrawalTime = gameTime;
		if (this.lastEatTime <= 0 || gameTime < this.lastEatTime) {
			this.lastEatTime = gameTime;
		}

		int previousWithdrawal = this.withdrawalValue;
		triggerWithdrawal(player, config, gameTime);

		BetelNutMod.LOGGER.info(
				"Triggered betel withdrawal test for {}: addiction={}, withdrawal {} -> {}, lastEatTime={}, gameTime={}, nextWithdrawalTicks={}",
				player.getScoreboardName(), this.addictionValue, previousWithdrawal, this.withdrawalValue,
				this.lastEatTime, gameTime, getNextWithdrawalTicks(player));
	}

	public void clearAddiction(ServerPlayer player) {
		this.addictionValue = 0;
		this.withdrawalValue = 0;
		this.lastEatTime = 0;
		this.cleanTime = 0;
		this.notifiedWithdrawalStage = 0;
		this.nextWithdrawalTime = -1;
		clearWithdrawalPenalties(player);
		AddictionSyncPayload.send(player);
	}

	public void clearActiveWithdrawalPenalties(ServerPlayer player) {
		clearWithdrawalPenalties(player);
	}

	public void clearWithdrawalMaxHealthPenalty(ServerPlayer player) {
		removeWithdrawalMaxHealthPenalty(player);
	}

	public void sendEatingRestrictionMessage(ServerPlayer player, String message) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.showEatingRestrictionMessage) {
			return;
		}

		long gameTime = player.level().getGameTime();
		if (gameTime < this.nextEatingRestrictionMessageTime) {
			return;
		}

		if (BetelMessages.send(player, message)) {
			this.nextEatingRestrictionMessageTime = gameTime + config.eatingRestrictionMessageCooldownTicks;
		}
	}

	private void enterHechengTianxiaComa(ServerPlayer player) {
		this.hechengTianxiaComaTicks = HechengTianxiaEffects.comaDurationTicks();
		HechengTianxiaEffects.maintainComa(player, this.hechengTianxiaComaTicks);
		BetelMessages.send(player, BetelMessages.HECHENG_TIANXIA_COMA_STARTED);
		BetelNutMod.LOGGER.info("Synthetic world betel coma started for {}: comaTicks={}",
				player.getScoreboardName(), this.hechengTianxiaComaTicks);
	}

	private void finishHechengTianxiaComa(ServerPlayer player) {
		this.hechengTianxiaNegativeTicks = HechengTianxiaEffects.negativeDurationTicks();
		HechengTianxiaEffects.applyNegativeAftermath(player, this.hechengTianxiaNegativeTicks);
		BetelMessages.send(player, BetelMessages.HECHENG_TIANXIA_COMA_ENDED);
		BetelNutMod.LOGGER.info("Synthetic world betel aftermath started for {}: negativeTicks={}",
				player.getScoreboardName(), this.hechengTianxiaNegativeTicks);
	}

	private void sendHechengTianxiaMessage(ServerPlayer player, String message) {
		long gameTime = player.level().getGameTime();
		if (gameTime < this.nextHechengTianxiaMessageTime) {
			return;
		}

		if (BetelMessages.send(player, message)) {
			this.nextHechengTianxiaMessageTime = gameTime + BetelNutConfig.get().feedbackCooldownTicks;
		}
	}

	private int withdrawalGainPerInterval(BetelNutConfig config) {
		int addictionStage = getAddictionStage();
		if (addictionStage >= 4) {
			return config.baseWithdrawalIncrease * 4;
		}
		if (addictionStage >= 3) {
			return config.baseWithdrawalIncrease * 2;
		}
		return config.baseWithdrawalIncrease;
	}

	private void triggerWithdrawal(ServerPlayer player, BetelNutConfig config, long gameTime) {
		int previousWithdrawal = this.withdrawalValue;
		increaseWithdrawalForTrigger(config);
		if (this.withdrawalValue > previousWithdrawal) {
			BetelNutMod.LOGGER.debug(
					"AddictionTickHandler increased withdrawal for {}: addiction={}, withdrawal {} -> {}, timeSinceLastEat={} ticks",
					player.getScoreboardName(), this.addictionValue, previousWithdrawal, this.withdrawalValue,
					Math.max(0, gameTime - this.lastEatTime));
		}

		applyWithdrawalEffects(player, config);
		this.nextWithdrawalTime = gameTime + config.withdrawalCheckIntervalTicks;
		AddictionSyncPayload.send(player);
	}

	private void increaseWithdrawalForTrigger(BetelNutConfig config) {
		int gainPerInterval = Math.max(1, withdrawalGainPerInterval(config));
		int visibleThreshold = visibleWithdrawalThreshold(config);
		int targetWithdrawal;
		if (this.withdrawalValue < visibleThreshold) {
			int remainingToVisible = visibleThreshold - this.withdrawalValue;
			int intervalsToVisible = Math.max(1,
					(remainingToVisible + gainPerInterval - 1) / gainPerInterval);
			targetWithdrawal = this.withdrawalValue + intervalsToVisible * gainPerInterval;
		} else {
			targetWithdrawal = this.withdrawalValue + gainPerInterval;
		}

		this.withdrawalValue = clamp(targetWithdrawal, 0, config.maxWithdrawalValue);
	}

	private void applyWithdrawalEffects(ServerPlayer player, BetelNutConfig config) {
		int addictionStage = getAddictionStage();
		int severity = this.withdrawalValue < visibleWithdrawalThreshold(config)
				? 0
				: AddictionStageUtil.getWithdrawalSeverity(addictionStage);
		if (severity <= 0) {
			this.notifiedWithdrawalStage = 0;
			clearWithdrawalPenalties(player);
			return;
		}

		BetelQuestAdvancements.grantWithdrawalStarted(player);
		BetelNutMod.LOGGER.debug(
				"[BetelNut Debug] Withdrawal triggered: player={}, stage={}, severity={}, addictionValue={}, withdrawalValue={}",
				player.getScoreboardName(), addictionStage, severity, this.addictionValue, this.withdrawalValue);
		sendWithdrawalStageFeedback(player);

		switch (severity) {
			case 1 -> applyLightWithdrawalEffects(player, config);
			case 2 -> applyMediumWithdrawalEffects(player, config);
			case 3 -> applyHeavyWithdrawalEffects(player, config);
			case 4 -> applySevereWithdrawalEffects(player, config);
			default -> applyExtremeWithdrawalEffects(player, config);
		}
	}

	private void sendWithdrawalStageFeedback(ServerPlayer player) {
		int currentStage = getWithdrawalSeverity();
		if (currentStage > this.notifiedWithdrawalStage) {
			String message = BetelMessages.withdrawalStageMessage(currentStage);
			if (!message.isEmpty()) {
				BetelMessages.send(player, message);
			}
			this.notifiedWithdrawalStage = currentStage;
		}
	}

	private void sendFeedback(ServerPlayer player, long gameTime, String message) {
		if (gameTime < this.nextFeedbackTime) {
			return;
		}

		if (BetelMessages.send(player, message)) {
			this.nextFeedbackTime = gameTime + BetelNutConfig.get().feedbackCooldownTicks;
		}
	}

	private boolean hasAddictionData() {
		return this.addictionValue > 0 || this.withdrawalValue > 0;
	}

	private void ensureWithdrawalTimer(BetelNutConfig config, long gameTime) {
		if (!canScheduleWithdrawal(config)) {
			this.nextWithdrawalTime = -1;
			return;
		}

		if (this.lastEatTime <= 0 || gameTime < this.lastEatTime) {
			this.lastEatTime = gameTime;
		}

		if (this.nextWithdrawalTime < 0) {
			this.nextWithdrawalTime = initialWithdrawalTime(config);
		}
		if (this.cleanTime > gameTime && this.nextWithdrawalTime < this.cleanTime) {
			this.nextWithdrawalTime = this.cleanTime;
		}
	}

	private void scheduleNextWithdrawal(BetelNutConfig config, long gameTime) {
		if (!canScheduleWithdrawal(config)) {
			this.nextWithdrawalTime = -1;
			return;
		}

		if (this.lastEatTime <= 0 || gameTime < this.lastEatTime) {
			this.lastEatTime = gameTime;
		}

		if (this.withdrawalValue >= visibleWithdrawalThreshold(config)) {
			this.nextWithdrawalTime = gameTime + config.withdrawalCheckIntervalTicks;
		} else {
			this.nextWithdrawalTime = initialWithdrawalTime(config);
		}
		if (this.cleanTime > gameTime && this.nextWithdrawalTime < this.cleanTime) {
			this.nextWithdrawalTime = this.cleanTime;
		}
	}

	private boolean canScheduleWithdrawal(BetelNutConfig config) {
		return config.enableAddictionSystem
				&& this.addictionValue > 0
				&& this.addictionValue >= config.minimumAddictionForWithdrawal
				&& getAddictionStage() > 0;
	}

	private long initialWithdrawalTime(BetelNutConfig config) {
		return this.lastEatTime + firstVisibleWithdrawalDelay(config);
	}

	private long firstVisibleWithdrawalDelay(BetelNutConfig config) {
		int gainPerInterval = Math.max(1, withdrawalGainPerInterval(config));
		int intervalsToFirstEffect = Math.max(1,
				(visibleWithdrawalThreshold(config) + gainPerInterval - 1) / gainPerInterval);
		return config.timeBeforeWithdrawalTicks
				+ (long) (intervalsToFirstEffect - 1) * config.withdrawalCheckIntervalTicks;
	}

	private void syncNotifiedWithdrawalStageAfterDecrease() {
		int currentStage = getWithdrawalSeverity();
		if (currentStage < this.notifiedWithdrawalStage) {
			this.notifiedWithdrawalStage = currentStage;
		}
	}

	private static void applyLightWithdrawalEffects(ServerPlayer player, BetelNutConfig config) {
		removeWithdrawalMaxHealthPenalty(player);
		int stageOneAmplifier = config.stage1EffectAmplifierOffset;
		player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, stageOneAmplifier));
		player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, stageOneAmplifier));
		player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 0));
	}

	private static void applyMediumWithdrawalEffects(ServerPlayer player, BetelNutConfig config) {
		applyWithdrawalMaxHealthPenalty(player, config, config.stage2MaxHealthPenalty);
		player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, 1));
		player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, 1));
		player.addEffect(withdrawalEffect(config, MobEffects.WEAKNESS, 0));
		player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 1));
	}

	private static void applyHeavyWithdrawalEffects(ServerPlayer player, BetelNutConfig config) {
		applyWithdrawalMaxHealthPenalty(player, config, config.stage3MaxHealthPenalty);
		player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, 2));
		player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, 2));
		player.addEffect(withdrawalEffect(config, MobEffects.WEAKNESS, 1));
		player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 1));
		if (player.getRandom().nextFloat() < 0.35F) {
			player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, config.withdrawalNauseaDurationTicks, 0));
		}
	}

	private static void applySevereWithdrawalEffects(ServerPlayer player, BetelNutConfig config) {
		applyWithdrawalMaxHealthPenalty(player, config, config.stage4MaxHealthPenalty);
		player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, 2));
		player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, 2));
		player.addEffect(withdrawalEffect(config, MobEffects.WEAKNESS, 1));
		player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 2));
		player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, config.withdrawalNauseaDurationTicks, 0));
	}

	private static void applyExtremeWithdrawalEffects(ServerPlayer player, BetelNutConfig config) {
		applyWithdrawalMaxHealthPenalty(player, config, config.stage4MaxHealthPenalty);
		player.addEffect(withdrawalEffect(config, MobEffects.MOVEMENT_SLOWDOWN, 3));
		player.addEffect(withdrawalEffect(config, MobEffects.DIG_SLOWDOWN, 3));
		player.addEffect(withdrawalEffect(config, MobEffects.WEAKNESS, 2));
		player.addEffect(withdrawalEffect(config, MobEffects.HUNGER, 2));
		player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, config.withdrawalNauseaDurationTicks, 0));
		if (config.enableStage4BlindnessOrDarkness) {
			player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, config.withdrawalNauseaDurationTicks, 0));
		}
	}

	private static MobEffectInstance withdrawalEffect(BetelNutConfig config,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			int amplifier) {
		return new MobEffectInstance(effect, config.withdrawalEffectDurationTicks, amplifier, false, true, true);
	}

	private static boolean clearWithdrawalPenalties(ServerPlayer player) {
		player.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
		player.removeEffect(MobEffects.DIG_SLOWDOWN);
		player.removeEffect(MobEffects.WEAKNESS);
		player.removeEffect(MobEffects.HUNGER);
		player.removeEffect(MobEffects.CONFUSION);
		player.removeEffect(MobEffects.BLINDNESS);
		player.removeEffect(MobEffects.DARKNESS);
		return removeWithdrawalMaxHealthPenalty(player);
	}

	private static double applyWithdrawalMaxHealthPenalty(ServerPlayer player, BetelNutConfig config,
			double requestedPenalty) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		if (maxHealth == null) {
			return 0.0D;
		}

		maxHealth.removeModifier(WITHDRAWAL_MAX_HEALTH_PENALTY_ID);
		if (!config.enableWithdrawalMaxHealthPenalty || requestedPenalty <= 0.0D) {
			return 0.0D;
		}

		double maxHealthBeforePenalty = maxHealth.getValue();
		double allowedPenalty = Math.max(0.0D, maxHealthBeforePenalty - config.minimumMaxHealthAfterPenalty);
		double appliedPenalty = Math.min(requestedPenalty, allowedPenalty);
		if (appliedPenalty <= 0.0D) {
			return 0.0D;
		}

		maxHealth.addOrUpdateTransientModifier(new AttributeModifier(WITHDRAWAL_MAX_HEALTH_PENALTY_ID,
				-appliedPenalty, AttributeModifier.Operation.ADD_VALUE));
		if (player.getHealth() > player.getMaxHealth()) {
			player.setHealth(player.getMaxHealth());
		}
		return appliedPenalty;
	}

	private static boolean removeWithdrawalMaxHealthPenalty(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		return maxHealth != null && maxHealth.removeModifier(WITHDRAWAL_MAX_HEALTH_PENALTY_ID);
	}

	private static AttributeModifier getWithdrawalMaxHealthModifier(ServerPlayer player) {
		AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
		return maxHealth == null ? null : maxHealth.getModifier(WITHDRAWAL_MAX_HEALTH_PENALTY_ID);
	}

	private static void sendRecoveryFeedbackIfNeeded(ServerPlayer player, boolean removedMaxHealthPenalty) {
		if (removedMaxHealthPenalty) {
			BetelMessages.send(player, BetelMessages.WITHDRAWAL_BODY_RECOVERING);
		}
	}

	@Override
	public void readFromNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		BetelNutConfig config = BetelNutConfig.get();
		this.addictionValue = clamp(tag.getInt(ADDICTION_VALUE_KEY), 0, config.maxAddictionValue);
		this.withdrawalValue = clamp(tag.getInt(WITHDRAWAL_VALUE_KEY), 0, config.maxWithdrawalValue);
		this.lastEatTime = tag.getLong(LAST_EAT_TIME_KEY);
		this.cleanTime = tag.getLong(CLEAN_TIME_KEY);
		this.nextWithdrawalTime = tag.contains(NEXT_WITHDRAWAL_TIME_KEY)
				? tag.getLong(NEXT_WITHDRAWAL_TIME_KEY)
				: -1;
		this.notifiedWithdrawalStage = clamp(tag.getInt(NOTIFIED_WITHDRAWAL_STAGE_KEY), 0, 5);
		this.hechengTianxiaPositiveTicks = Math.max(0, tag.getInt(HECHENG_TIANXIA_POSITIVE_TICKS_KEY));
		this.hechengTianxiaComaTicks = Math.max(0, tag.getInt(HECHENG_TIANXIA_COMA_TICKS_KEY));
		this.hechengTianxiaNegativeTicks = Math.max(0, tag.getInt(HECHENG_TIANXIA_NEGATIVE_TICKS_KEY));
	}

	@Override
	public void writeToNbt(CompoundTag tag, HolderLookup.Provider registryLookup) {
		tag.putInt(ADDICTION_VALUE_KEY, this.addictionValue);
		tag.putInt(WITHDRAWAL_VALUE_KEY, this.withdrawalValue);
		tag.putLong(LAST_EAT_TIME_KEY, this.lastEatTime);
		tag.putLong(CLEAN_TIME_KEY, this.cleanTime);
		tag.putLong(NEXT_WITHDRAWAL_TIME_KEY, this.nextWithdrawalTime);
		tag.putInt(NOTIFIED_WITHDRAWAL_STAGE_KEY, this.notifiedWithdrawalStage);
		tag.putInt(HECHENG_TIANXIA_POSITIVE_TICKS_KEY, this.hechengTianxiaPositiveTicks);
		tag.putInt(HECHENG_TIANXIA_COMA_TICKS_KEY, this.hechengTianxiaComaTicks);
		tag.putInt(HECHENG_TIANXIA_NEGATIVE_TICKS_KEY, this.hechengTianxiaNegativeTicks);
	}

	@Override
	public void copyFrom(BetelNutAddictionComponent other, HolderLookup.Provider registryLookup) {
		copyAllFrom(other);
	}

	public void copyForRespawn(BetelNutAddictionComponent other, boolean lossless) {
		if (lossless) {
			copyAllFrom(other);
			return;
		}

		copyHechengTianxiaFrom(other);

		BetelNutConfig config = BetelNutConfig.get();
		if (!config.keepAddictionAfterDeath) {
			clearStoredData();
			return;
		}

		this.addictionValue = other.addictionValue;
		this.cleanTime = other.cleanTime;

		if (config.keepWithdrawalAfterDeath) {
			this.withdrawalValue = other.withdrawalValue;
			this.lastEatTime = other.lastEatTime;
			this.nextWithdrawalTime = other.nextWithdrawalTime;
			this.notifiedWithdrawalStage = other.notifiedWithdrawalStage;
		} else {
			this.withdrawalValue = 0;
			this.lastEatTime = 0;
			this.nextWithdrawalTime = -1;
			this.notifiedWithdrawalStage = 0;
		}
	}

	private void copyAllFrom(BetelNutAddictionComponent other) {
		this.addictionValue = other.addictionValue;
		this.withdrawalValue = other.withdrawalValue;
		this.lastEatTime = other.lastEatTime;
		this.cleanTime = other.cleanTime;
		this.nextWithdrawalTime = other.nextWithdrawalTime;
		this.notifiedWithdrawalStage = other.notifiedWithdrawalStage;
		copyHechengTianxiaFrom(other);
	}

	private void copyHechengTianxiaFrom(BetelNutAddictionComponent other) {
		this.hechengTianxiaPositiveTicks = other.hechengTianxiaPositiveTicks;
		this.hechengTianxiaComaTicks = other.hechengTianxiaComaTicks;
		this.hechengTianxiaNegativeTicks = other.hechengTianxiaNegativeTicks;
	}

	private void clearStoredData() {
		this.addictionValue = 0;
		this.withdrawalValue = 0;
		this.lastEatTime = 0;
		this.cleanTime = 0;
		this.nextWithdrawalTime = -1;
		this.notifiedWithdrawalStage = 0;
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static int visibleWithdrawalThreshold(BetelNutConfig config) {
		return Math.max(1, Math.min(VISIBLE_WITHDRAWAL_THRESHOLD, config.maxWithdrawalValue));
	}

}
