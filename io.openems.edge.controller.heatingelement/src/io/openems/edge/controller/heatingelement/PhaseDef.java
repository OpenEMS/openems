package io.openems.edge.controller.heatingelement;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;

public class PhaseDef {

	private final ControllerHeatingElement parent;
	LocalDateTime phaseTimeOn = null;
	LocalDateTime phaseTimeOff = null;
	LocalDateTime lastRunningTimeCheck = null;
	long totalPhaseTime = 0; // milliseconds
	Stopwatch timeStopwatch = Stopwatch.createUnstarted();
	double totalPhasePower = 0;
	ChannelAddress outputChannelAddress;

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
			phaseTimeOff = null;
			phaseTimeOn = null;
			if (this.timeStopwatch.isRunning()) {
				this.timeStopwatch.stop();
			}
			this.off(outputChannelAddress);
		} else {
			// phase one is running
			if (phaseTimeOn == null) {
				phaseTimeOn = LocalDateTime.now();
				this.timeStopwatch.start();
				// do not take the current time
			} else {
				totalPhaseTime = this.timeStopwatch.elapsed(TimeUnit.SECONDS);
				totalPhasePower = calculatePower(totalPhaseTime);
			}
			this.on(outputChannelAddress);
		}
	}

//	private void displayObject() {
//		this.logInfo(this.LOGGER, " phaseTimeOn : "+ phaseTimeOn +
//		 ", phaseTimeOff : " + phaseTimeOff +
//		", totalPhaseTime : " + totalPhaseTime +
//		", totalPhasePower : " + totalPhasePower );
//	}

	/**
	 * function to calculates the Kilowatthour, using the power of each phase
	 * 
	 * @param time Long values of time in seconds
	 * 
	 */
	private double calculatePower(double time) {
		double kiloWattHour = ((time) / 3600.0) * parent.powerOfPhase;
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
