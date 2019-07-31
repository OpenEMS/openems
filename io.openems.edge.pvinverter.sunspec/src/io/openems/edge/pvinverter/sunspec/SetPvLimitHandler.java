package io.openems.edge.pvinverter.sunspec;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

import io.openems.common.exceptions.CheckedRunnable;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel.S123_WMaxLim_Ena;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public class SetPvLimitHandler implements CheckedRunnable {

	private final SunSpecPvInverter parent;

	public SetPvLimitHandler(SunSpecPvInverter parent) {
		this.parent = parent;
	}

	private LocalDateTime lastWMaxLimPctTime = LocalDateTime.MIN;

	@Override
	public void run() throws OpenemsNamedException {
		// Get ActivePowerLimit that should be applied
		IntegerWriteChannel activePowerLimitChannel = this.parent
				.channel(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);
		Optional<Integer> activePowerLimitOpt = activePowerLimitChannel.getNextWriteValueAndReset();

		IntegerWriteChannel wMaxLimPctChannel;
		IntegerReadChannel wRtgChannel;
		EnumWriteChannel wMaxLimEnaChannel;

		try {
			// Get Power Limitation WriteChannel
			wMaxLimPctChannel = this.parent.getSunSpecChannelOrError(SunSpecModel.S123.W_MAX_LIM_PCT);

			// Get Continuous power output capability of the inverter (WRtg)
			wRtgChannel = this.parent.getSunSpecChannelOrError(SunSpecModel.S120.W_RTG);

			// Get Power Limitation Enabled WriteChannel
			wMaxLimEnaChannel = this.parent.getSunSpecChannelOrError(SunSpecModel.S123.W_MAX_LIM_ENA);

		} catch (OpenemsNamedException e) {
			// Unable to get required Channels,...
			if (activePowerLimitOpt.isPresent()) {
				// and power should be limited -> forward error
				throw e;
			} else {
				// and no power limitation is required -> ignore error and exit
				return;
			}
		}

		if (activePowerLimitOpt.isPresent()) {
			/*
			 * A ActivePowerLimit is set
			 */
			int activePowerLimit = activePowerLimitOpt.get();

			// calculate limitation in percent
			int wRtg = wRtgChannel.value().getOrError();
			Integer wMaxLimPct = (int) (activePowerLimit * 100 / (float) wRtg);

			// Just to be sure: keep percentage in range [1, 100]. Do never set "0" as it
			// causes some inverters to go to standby mode, which is
			// generally not what we want.
			if (wMaxLimPct > 100) {
				wMaxLimPct = 100;
			} else if (wMaxLimPct < 1) {
				wMaxLimPct = 1;
			}

			// Get Power Limitation Timeout Channel
			IntegerReadChannel wMaxLimPctRvrtTmsChannel = this.parent
					.getSunSpecChannelOrError(SunSpecModel.S123.W_MAX_LIM_PCT_RVRT_TMS);
			int wMaxLimPctRvrtTms = wMaxLimPctRvrtTmsChannel.value().orElse(0);

			if (
			// Value changed
			!Objects.equals(wMaxLimPct, wMaxLimPctChannel.value().get()) //
					// Value needs to be set again to avoid timeout
					|| this.lastWMaxLimPctTime.isBefore(LocalDateTime.now().minusSeconds(wMaxLimPctRvrtTms / 2))) {

				// Set Power Limitation
				wMaxLimPctChannel.setNextWriteValue(wMaxLimPct);

				// Apply Power Limitation Enabled
				if (wMaxLimEnaChannel.value().asEnum() != S123_WMaxLim_Ena.ENABLED) {
					wMaxLimEnaChannel.setNextWriteValue(S123_WMaxLim_Ena.ENABLED);
				}

				// Remember last Set-Time
				this.lastWMaxLimPctTime = LocalDateTime.now();
			}

		} else {
			/*
			 * No ActivePowerLimit is set
			 */
			// Reset Power Limitation to 100 %
			if (wMaxLimPctChannel.value().orElse(0) != 100) {
				wMaxLimPctChannel.setNextWriteValue(100);
			}

			// Apply Power Limitation Enabled
			if (wMaxLimEnaChannel.value().asEnum() != S123_WMaxLim_Ena.ENABLED) {
				wMaxLimEnaChannel.setNextWriteValue(S123_WMaxLim_Ena.ENABLED);
			}
		}

	}

}
