package io.openems.edge.pvinverter.kaco.blueplanet;

import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.CheckedConsumer;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.pvinverter.kaco.blueplanet.KacoBlueplanet.ChannelId;

public class SetPvLimitHandler implements CheckedConsumer<Integer> {

	private final Logger log = LoggerFactory.getLogger(SetPvLimitHandler.class);
	private final KacoBlueplanet parent;

	private Integer lastWMaxLimPct = null;
	private LocalDateTime lastWMaxLimPctTime = LocalDateTime.MIN;

	public SetPvLimitHandler(KacoBlueplanet parent) {
		this.parent = parent;
	}

	@Override
	public void accept(Integer power) throws OpenemsNamedException {
		if (power == null) {
			return;
		}

		int wMaxLimPct = (int) (power / 15_000.0 * 100.0 /* percent */ * 10.0 /* scale factor */);

		// keep percentage in range [0, 100]
		if (wMaxLimPct > 1000) {
			wMaxLimPct = 1000;
		}
		if (wMaxLimPct < 1) {
			wMaxLimPct = 1;
		}

		if (!Objects.equals(this.lastWMaxLimPct, wMaxLimPct) || this.lastWMaxLimPctTime
				.isBefore(LocalDateTime.now().minusSeconds(150 /* TODO: read from SunSpec register */))) {
			// Value needs to be set
			IntegerWriteChannel wMaxLimPctChannel = this.parent.channel(ChannelId.W_MAX_LIM_PCT);
			this.parent.logInfo(this.log, "Apply new limit: " + wMaxLimPct / 10. + " %");
			wMaxLimPctChannel.setNextWriteValue(wMaxLimPct);

			this.lastWMaxLimPct = wMaxLimPct;
			this.lastWMaxLimPctTime = LocalDateTime.now();
		}

		// Is limitation enabled?
		IntegerWriteChannel wMaxLimEnaChannel = this.parent.channel(ChannelId.W_MAX_LIM_ENA);
		if (wMaxLimEnaChannel.value().orElse(0) == 0) {
			this.parent.logInfo(this.log, "Enabling W MAX LIM");
			wMaxLimEnaChannel.setNextWriteValue(1);
		}
	};

}
