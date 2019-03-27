package io.openems.edge.ess.streetscooter;

import java.time.LocalDateTime;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;

public class PowerHandler implements BiConsumer<Integer, Integer> {

	private static final int MINUTES_TO_WAIT_FOR_2ND_TRY = 30;
	private AbstractEssStreetscooter parent;
	private final Logger log = LoggerFactory.getLogger(PowerHandler.class);
	private LocalDateTime lastRestartAfterFault;
	private int attempsToRestart;

	public PowerHandler(AbstractEssStreetscooter parent) {
		this.parent = parent;
	}

	@Override
	// TODO should throws OpenemsException
	public void accept(Integer activePower, Integer reactivePower) {

		checkForResettingFault();

		if (isInverterInFaultMode()) {
			doErrorHandling();
		}

		if (isErrorHandling()) {
			return;
		}

		if (!isRunning()) {
			setRunning(true);
		}
		if (isRunning() && !isEnabled()) {
			setEnabled(true);
		}

		if (isRunning() && isEnabled() && isInverterInNormalMode()) {
			writeActivePower(activePower);
		}
	}

	private boolean isErrorHandling() {
		return lastRestartAfterFault != null;
	}

	private void checkForResettingFault() {
		if (isInverterInNormalMode() && isWaitingPeriodAfterFaultRestartPassed()) {
			lastRestartAfterFault = null;
			attempsToRestart = 0;
		}
	}

	private void doErrorHandling() {
		if (lastRestartAfterFault == null && attempsToRestart == 0) {
			lastRestartAfterFault = LocalDateTime.now();
			attempsToRestart = 1;
			setRunning(true);
			setEnabled(true);
			this.parent.logInfo(this.log, "Try to restart the system for the first time after detecting fault");
		} else {
			if (isWaitingPeriodAfterFaultRestartPassed() && attempsToRestart == 1) {
				attempsToRestart++;
				setRunning(true);
				setEnabled(true);
				this.parent.logInfo(this.log, "Try to restart the system for the second time after detecting fault");
			} else if (isWaitingPeriodAfterFaultRestartPassed() && attempsToRestart > 1) {
				// Do nothing, let system in fault mode
				StringWriteChannel errorChannel = parent.channel(StrtsctrChannelId.SYSTEM_STATE_INFORMATION);
				errorChannel.setNextValue("System could not be started after waiting period and two start attempts");
			}
		}
	}

	private void writeActivePower(Integer activePower) {
		try {
			IntegerWriteChannel setActivePowerChannel = parent.channel(StrtsctrChannelId.INVERTER_SET_ACTIVE_POWER);
			setActivePowerChannel.setNextWriteValue(activePower);
		} catch (OpenemsNamedException e) {
			this.parent.logError(this.log, "Unable to set ActivePower: " + e.getMessage());
		}
	}

	private boolean isInverterInNormalMode() {
		EnumReadChannel inverterModeChannel = parent.channel(StrtsctrChannelId.INVERTER_MODE);
		return inverterModeChannel.value().orElse(InverterMode.UNDEFINED.getValue())
				.equals(InverterMode.NORMAL.getValue());
	}

	private boolean isInverterInFaultMode() {
		EnumReadChannel inverterModeChannel = parent.channel(StrtsctrChannelId.INVERTER_MODE);
		return inverterModeChannel.value().orElse(InverterMode.UNDEFINED.getValue())
				.equals(InverterMode.FAULT.getValue());
	}

	private void setEnabled(boolean value) {
		try {
			BooleanWriteChannel channel = parent.channel(StrtsctrChannelId.ICU_ENABLED);
			channel.setNextWriteValue(value);
		} catch (Exception e) {
			this.parent.logError(this.log, "Unable to set icu enabled: " + e.getMessage());
		}
	}

	private void setRunning(boolean value) {
		try {
			BooleanWriteChannel channel = parent.channel(StrtsctrChannelId.ICU_RUN);
			channel.setNextWriteValue(value);
		} catch (Exception e) {
			this.parent.logError(this.log, "Unable to set icu run: " + e.getMessage());
		}
	}

	private boolean isEnabled() {
		BooleanReadChannel icuEnabled = parent.channel(StrtsctrChannelId.ICU_ENABLED);
		boolean value = icuEnabled.value().orElse(false);
		return value;
	}

	private boolean isRunning() {
		BooleanReadChannel icuRunChannel = parent.channel(StrtsctrChannelId.ICU_RUN);
		boolean value = icuRunChannel.value().orElse(false);
		return value;
	}

	private boolean isWaitingPeriodAfterFaultRestartPassed() {
		return lastRestartAfterFault != null
				&& lastRestartAfterFault.plusMinutes(MINUTES_TO_WAIT_FOR_2ND_TRY).isAfter(LocalDateTime.now());
	}
}
