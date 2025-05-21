package io.openems.edge.kaco.blueplanet.hybrid10.pvinverter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerWriteChannel;

public class SetPvLimitHandler implements ThrowingRunnable<OpenemsNamedException> {

	private final Logger log = LoggerFactory.getLogger(SetPvLimitHandler.class);
	private final KacoBlueplanetHybrid10PvInverterImpl parent;
	private final ChannelId channelId;

	private Float lastEpLimit = null;
	private LocalDateTime lastWMaxLimPctTime = LocalDateTime.MIN;

	public SetPvLimitHandler(KacoBlueplanetHybrid10PvInverterImpl parent, ChannelId channelId) {
		this.parent = parent;
		this.channelId = channelId;
	}

	@Override
	public void run() throws OpenemsNamedException {
		IntegerWriteChannel channel = this.parent.channel(this.channelId);
		Optional<Integer> powerOpt = channel.getNextWriteValueAndReset();

		float ePLimit;
		int power;
		if (powerOpt.isPresent()) {
			power = powerOpt.get();
			ePLimit = (int) ((double) power / KacoBlueplanetHybrid10PvInverter.MAX_APPARENT_POWER * 100.0);

			// keep percentage in range [0, 100]
			if (ePLimit > 100) {
				ePLimit = 100f;
			}
			if (ePLimit < 0) {
				ePLimit = 0f;
			}
		} else {
			// Reset limit
			power = KacoBlueplanetHybrid10PvInverter.MAX_APPARENT_POWER;
			ePLimit = 100;
		}

		if (!Objects.equals(this.lastEpLimit, ePLimit) || this.lastWMaxLimPctTime
				.isBefore(LocalDateTime.now().minusSeconds(60 /* TODO: how often should it be written? */))) {
			// Value needs to be set
			var bpData = this.parent.core.getBpData();
			if (bpData != null) {
				this.parent.logInfo(this.log, "Apply new limit: " + power + " W (" + ePLimit + " %)");
				bpData.settings.setEPLimit(ePLimit);

				this.lastEpLimit = ePLimit;
				this.lastWMaxLimPctTime = LocalDateTime.now();
			}
		}
	}

}
