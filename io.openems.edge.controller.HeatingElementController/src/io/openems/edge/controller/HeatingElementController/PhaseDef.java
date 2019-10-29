package io.openems.edge.controller.HeatingElementController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;

public class PhaseDef {

	private final ControllerHeatingElement parent;
	LocalDateTime phaseTimeOn = null;
	LocalDateTime phaseTimeOff = null;
	long totalPhaseTime = 0;
	long totalPhasePower = 0;
	ChannelAddress outputChannelAddress;
	int powerOfPhase = 0;

	ComponentManager componentManager;
	boolean isSwitchOn = false;

	PhaseDef(ControllerHeatingElement parent) {
		this.parent = parent;
	}

	private final Logger LOGGER = LoggerFactory.getLogger(PhaseDef.class);

	protected void logInfo(Logger log, String message) {
		log.info(message);
	}

	protected void logError(Logger log, String message) {
		log.error(message);
	}

	public void computeTime() throws IllegalArgumentException, OpenemsNamedException {
		if (!isSwitchOn) {
			// If the Phase one is not switched-On do not record the PhasetimeOff
			if (phaseTimeOn == null) {
				phaseTimeOff = null;
			} else {
				phaseTimeOff = LocalDateTime.now();
			}

			this.off(outputChannelAddress);
		} else {
			// phase one is running
			if (phaseTimeOn != null) {
				// do not take the current time
			} else {
				phaseTimeOn = LocalDateTime.now();
			}
			this.on(outputChannelAddress);
		}
		if (phaseTimeOn != null && phaseTimeOff != null) {
			// cycle of turning phase one On and off is complete
			totalPhaseTime += ChronoUnit.SECONDS.between(phaseTimeOn, phaseTimeOff);
			totalPhasePower += calculatePower(this.totalPhaseTime);
			// Once the totalPhaseTime is calculated, reset the phasetimeOn to null to
			// calculate the time for the next cycle of switch On and Off
			phaseTimeOn = null;
		} else if (totalPhaseTime != 0) {
			// reserve the calculated totalPhaseTime
		} else {
			// phase one is not started, or still running
			totalPhaseTime = 0;
		}
	}

	/**
	 * function to calculates the Kilowatthour, using the power of each phase
	 * 
	 * @param time Long values of time in seconds
	 * 
	 */
	private float calculatePower(long time) {
		float kiloWattHour = ((float) (time) / 3600) * parent.powerOfPhase;
		return kiloWattHour;
	}

	/**
	 * Switch the output ON.
	 * 
	 * @param outputChannelAddress address of the channel which must set to ON
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void on(ChannelAddress outputChannelAddress) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(true, outputChannelAddress);
	}

	/**
	 * Switch the output OFF.
	 * 
	 * @param outputChannelAddress address of the channel which must set to OFF
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void off(ChannelAddress outputChannelAddress) throws IllegalArgumentException, OpenemsNamedException {
		this.setOutput(false, outputChannelAddress);
	}

	/**
	 * Helper function to switch an output if it was not switched before.
	 *
	 * @param value                The boolean value which must set on the output
	 *                             channel address
	 * @param outputChannelAddress The address of the channel
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	private void setOutput(boolean value, ChannelAddress outputChannelAddress)
			throws IllegalArgumentException, OpenemsNamedException {
		try {
			WriteChannel<Boolean> outputChannel = parent.componentManager.getChannel(outputChannelAddress);
			Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
				this.logInfo(this.LOGGER, "Set output [" + outputChannel.address() + "] " + (value) + ".");
				outputChannel.setNextWriteValue(value);
			}
		} catch (OpenemsException e) {
			this.logError(this.LOGGER, "Unable to set output: [" + outputChannelAddress + "] " + e.getMessage());
		}
	}

}
