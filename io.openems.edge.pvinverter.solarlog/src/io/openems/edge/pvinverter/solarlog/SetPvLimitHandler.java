package io.openems.edge.pvinverter.solarlog;

import java.time.LocalDateTime;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.pvinverter.solarlog.PvInverterSolarlog.ChannelId;

public class SetPvLimitHandler implements ThrowingRunnable<OpenemsNamedException> {

	private final Logger log = LoggerFactory.getLogger(SetPvLimitHandler.class);
	private final PvInverterSolarlogImpl parent;
	private final ManagedSymmetricPvInverter.ChannelId channelId;

	private Integer lastPLimitPerc = null;
	private LocalDateTime lastPLimitPercTime = LocalDateTime.MIN;

	public SetPvLimitHandler(PvInverterSolarlogImpl parent, ManagedSymmetricPvInverter.ChannelId activePowerLimit) {
		this.parent = parent;
		this.channelId = activePowerLimit;
	}

	@Override
	public void run() throws OpenemsNamedException {
		IntegerWriteChannel channel = this.parent.channel(this.channelId);
		var powerOpt = channel.getNextWriteValueAndReset();

		int pLimitPerc;
		int power;
		if (powerOpt.isPresent()) {
			power = powerOpt.get();
			pLimitPerc = (int) ((double) power / (double) this.parent.config.maxActivePower() * 100.0);

			// keep percentage in range [0, 100]
			if (pLimitPerc > 100) {
				pLimitPerc = 100;
			}
			if (pLimitPerc < 0) {
				pLimitPerc = 0;
			}
		} else {
			// Reset limit
			power = this.parent.config.maxActivePower();
			pLimitPerc = 100;
		}

		if (!Objects.equals(this.lastPLimitPerc, pLimitPerc) || this.lastPLimitPercTime
				.isBefore(LocalDateTime.now().minusSeconds(150 /* watchdog timeout is 300 */))) {
			// Value needs to be set
			this.parent.logInfo(this.log, "Apply new limit: " + power + " W (" + pLimitPerc + " %)");
			IntegerWriteChannel pLimitPercCh = this.parent.channel(ChannelId.P_LIMIT_PERC);
			pLimitPercCh.setNextWriteValue(pLimitPerc);

			EnumWriteChannel pLimitTypeCh = this.parent.channel(ChannelId.P_LIMIT_TYPE);
			pLimitTypeCh.setNextWriteValue(PLimitType.FIXED_LIMIT);

			IntegerWriteChannel watchDogTagCh = this.parent.channel(ChannelId.WATCH_DOG_TAG);
			watchDogTagCh.setNextWriteValue((int) System.currentTimeMillis());

			this.lastPLimitPerc = pLimitPerc;
			this.lastPLimitPercTime = LocalDateTime.now();
		}
	}

}
