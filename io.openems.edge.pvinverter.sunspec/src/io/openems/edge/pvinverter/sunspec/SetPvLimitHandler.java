package io.openems.edge.pvinverter.sunspec;

import java.time.Duration;
import java.time.Instant;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel.S123_WMaxLim_Ena;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public class SetPvLimitHandler implements ThrowingRunnable<OpenemsNamedException> {

	private static final float COMPARE_THRESHOLD = 0.0001F;

	private final AbstractSunSpecPvInverter parent;

	public SetPvLimitHandler(AbstractSunSpecPvInverter parent) {
		this.parent = parent;
	}

	private Instant lastWMaxLimPctTime = Instant.MIN;

	@Override
	public void run() throws OpenemsNamedException {
		// Get ActivePowerLimit that should be applied
		IntegerWriteChannel activePowerLimitChannel = this.parent
				.channel(ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);
		var activePowerLimitOpt = activePowerLimitChannel.getNextWriteValueAndReset();

		FloatWriteChannel wMaxLimPctChannel;
		FloatReadChannel wRtgChannel;
		EnumWriteChannel wMaxLimEnaChannel;

		try {
			// Get Power Limitation WriteChannel
			wMaxLimPctChannel = this.parent.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_PCT);

			// Get Continuous power output capability of the inverter (WRtg)
			wRtgChannel = this.parent.getSunSpecChannelOrError(DefaultSunSpecModel.S120.W_RTG);

			// Get Power Limitation Enabled WriteChannel
			wMaxLimEnaChannel = this.parent.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_ENA);

		} catch (OpenemsNamedException e) {
			// Unable to get required Channels,...
			if (activePowerLimitOpt.isPresent()) {
				// and power should be limited -> forward error
				throw e;
			}
			// and no power limitation is required -> ignore error and exit
			return;
		}

		float wMaxLimPct;
		if (activePowerLimitOpt.isPresent()) {
			/*
			 * A ActivePowerLimit is set
			 */
			int activePowerLimit = activePowerLimitOpt.get();

			// calculate limitation in percent
			float wRtg = wRtgChannel.value().getOrError();
			wMaxLimPct = activePowerLimit * 100 / wRtg;

		} else {
			/*
			 * No ActivePowerLimit is set -> reset to 100 %
			 */
			wMaxLimPct = 100F;
		}

		// Just to be sure: keep percentage in range [1, 100]. Do never set "0" as it
		// causes some inverters to go to standby mode, which is
		// generally not what we want.
		if (wMaxLimPct > 100F) {
			wMaxLimPct = 100F;
		} else if (wMaxLimPct < 1F) {
			wMaxLimPct = 1F;
		}

		// Get Power Limitation Timeout Channel
		IntegerReadChannel wMaxLimPctRvrtTmsChannel = this.parent
				.getSunSpecChannelOrError(DefaultSunSpecModel.S123.W_MAX_LIM_PCT_RVRT_TMS);
		int wMaxLimPctRvrtTms = wMaxLimPctRvrtTmsChannel.value().orElse(0);

		if (
		// Value is not available
		!wMaxLimPctChannel.value().isDefined()
				// Value changed
				|| Math.abs(wMaxLimPct - wMaxLimPctChannel.value().get()) > COMPARE_THRESHOLD
				// Value needs to be set again to avoid timeout
				|| Duration.between(this.lastWMaxLimPctTime, Instant.now()).getSeconds() > wMaxLimPctRvrtTms / 2.) {

			// Set Power Limitation
			wMaxLimPctChannel.setNextWriteValue(wMaxLimPct);

			// Apply Power Limitation Enabled
			if (wMaxLimEnaChannel.value().asEnum() != S123_WMaxLim_Ena.ENABLED) {
				wMaxLimEnaChannel.setNextWriteValue(S123_WMaxLim_Ena.ENABLED);
			}

			// Remember last Set-Time
			this.lastWMaxLimPctTime = Instant.now();
		}

	}

}
