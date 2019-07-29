package io.openems.edge.pvinverter.sunspec;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.CheckedRunnable;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public class SetPvLimitHandler implements CheckedRunnable {

	private final Logger log = LoggerFactory.getLogger(SetPvLimitHandler.class);
	private final SunSpecPvInverter parent;

	private Integer lastWMaxLimPct = null;
	private LocalDateTime lastWMaxLimPctTime = LocalDateTime.MIN;

	public SetPvLimitHandler(SunSpecPvInverter parent) {
		this.parent = parent;
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Get ActivePowerLimit that should be applied
		IntegerWriteChannel activePowerLimitChannel = this.parent
				.channel(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);
		Optional<Integer> activePowerLimitOpt = activePowerLimitChannel.getNextWriteValueAndReset();

		if (activePowerLimitOpt.isPresent()) {
			/*
			 * A ActivePowerLimit is set
			 */
			// Get Continuous power output capability of the inverter (WRtg)
			Optional<IntegerReadChannel> wRtgChannelOpt = this.parent.getSunSpecChannel(SunSpecModel.S120.W_RTG);
			if (!wRtgChannelOpt.isPresent() || !wRtgChannelOpt.get().value().isDefined()) {
				throw new OpenemsException(
						"Continuous power output capability of the inverter (WRtg) is not available");
			}
			int wRtg = wRtgChannelOpt.get().value().get();

		} else {
			/*
			 * No ActivePowerLimit is set
			 */

		}
//
//		int wMaxLimPct;
//		int power;
//		if (activePowerLimitOpt.isPresent()) {
//			power = activePowerLimitOpt.get();
//			wMaxLimPct = (int) (power / 15_000.0 * 100.0 /* percent */ * 10.0 /* scale factor */);
//
//			// keep percentage in range [0, 100]
//			if (wMaxLimPct > 1000) {
//				wMaxLimPct = 1000;
//			}
//			if (wMaxLimPct < 1) {
//				wMaxLimPct = 1;
//			}
//		} else {
//			// Reset limit
//			power = 15_000; // TODO read from modbus
//			wMaxLimPct = 1000;
//		}
//
//		if (!Objects.equals(this.lastWMaxLimPct, wMaxLimPct) || this.lastWMaxLimPctTime
//				.isBefore(LocalDateTime.now().minusSeconds(150 /* TODO: read from SunSpec register */))) {
//			// Value needs to be set
//			IntegerWriteChannel wMaxLimPctChannel = this.parent.channel(KacoBlueplanet.PvChannelId.W_MAX_LIM_PCT);
//			this.parent.logInfo(this.log, "Apply new limit: " + power + " W (" + wMaxLimPct / 10. + " %)");
//			wMaxLimPctChannel.setNextWriteValue(wMaxLimPct);
//
//			this.lastWMaxLimPct = wMaxLimPct;
//			this.lastWMaxLimPctTime = LocalDateTime.now();
//		}
//
//		// Is limitation enabled?
//		IntegerWriteChannel wMaxLimEnaChannel = this.parent.channel(KacoBlueplanet.PvChannelId.W_MAX_LIM_ENA);
//		if (wMaxLimEnaChannel.value().orElse(0) == 0) {
//			this.parent.logInfo(this.log, "Enabling W MAX LIM");
//			wMaxLimEnaChannel.setNextWriteValue(1);
//		}
	};

}
