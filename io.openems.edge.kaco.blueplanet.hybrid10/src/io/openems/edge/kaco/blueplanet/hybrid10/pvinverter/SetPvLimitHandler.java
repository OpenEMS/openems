package io.openems.edge.kaco.blueplanet.hybrid10.pvinverter;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ed.data.Settings;

import io.openems.common.exceptions.CheckedRunnable;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.kaco.blueplanet.hybrid10.BpConstants;

public class SetPvLimitHandler implements CheckedRunnable {

	private final Logger log = LoggerFactory.getLogger(SetPvLimitHandler.class);
	private final BpPvInverterImpl parent;
	private final ChannelId channelId;

	private Float lastEPLimit = null;
	private LocalDateTime lastWMaxLimPctTime = LocalDateTime.MIN;

	public SetPvLimitHandler(BpPvInverterImpl parent, ChannelId channelId) {
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
			ePLimit = (int) ((double) power / BpConstants.MAX_APPARENT_POWER * 100.0);

			// keep percentage in range [0, 100]
			if (ePLimit > 100) {
				ePLimit = 100f;
			}
			if (ePLimit < 0) {
				ePLimit = 0f;
			}
		} else {
			// Reset limit
			power = BpConstants.MAX_APPARENT_POWER;
			ePLimit = 100;
		}

		if (!Objects.equals(this.lastEPLimit, ePLimit) || this.lastWMaxLimPctTime
				.isBefore(LocalDateTime.now().minusSeconds(60 /* TODO: how often should it be written? */))) {
			// Value needs to be set
			Settings settings = this.parent.core.getSettings();
			if (settings != null) {
				this.parent.logInfo(this.log, "Apply new limit: " + power + " W (" + ePLimit + " %)");
				settings.setEPLimit(ePLimit);

				this.lastEPLimit = ePLimit;
				this.lastWMaxLimPctTime = LocalDateTime.now();

			} else {
				this.parent.logWarn(this.log, "Unable to apply limit: no Settings available.");
			}
		}
	}

}
