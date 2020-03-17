package io.openems.edge.controller.heatingelement;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;

public class LevelDef {
	
	private final ControllerHeatingElement parent;
	private long totalLevelTime = 0; // milliseconds
	private Stopwatch timeStopwatch = Stopwatch.createUnstarted();
	private double totalLevelPower = 0;
	

	private ChannelAddress[] outputChannelAddress;
	
	/**
	 * This boolean variable specifies the phase is one or off
	 */
	private boolean isSwitchOn;
	
	public void computeTime() throws IllegalArgumentException, OpenemsNamedException {
		if(!isSwitchOn()) {
			if(this.timeStopwatch.isRunning()) {
				this.timeStopwatch.stop();
			}			
			totalLevelTime = this.timeStopwatch.elapsed(TimeUnit.SECONDS);
			totalLevelPower = calculatePower(totalLevelTime);
			this.off(getOutputChannelAddress());
		}else {
			if(!this.timeStopwatch.isRunning()) {
				this.timeStopwatch.start();
			}			
			totalLevelTime = this.timeStopwatch.elapsed(TimeUnit.SECONDS);
			totalLevelPower = calculatePower(totalLevelTime);
			this.on(getOutputChannelAddress());
		}
	}
	
	/**
	 * Switch the output ON.
	 * 
	 * @param outputChannelAddress[] address of the channel which must set to ON
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void on(ChannelAddress[] outputChannelAddress) throws IllegalArgumentException, OpenemsNamedException {
		for(ChannelAddress channelAddress: outputChannelAddress ) {
			this.setOutput(true, channelAddress);
		}
		
	}

	/**
	 * Switch the output OFF.
	 * 
	 * @param outputChannelAddress address of the channel which must set to OFF
	 * 
	 * @throws OpenemsNamedException
	 * @throws IllegalArgumentException
	 */
	private void off(ChannelAddress[] outputChannelAddress) throws IllegalArgumentException, OpenemsNamedException {
		for(ChannelAddress channelAddress: outputChannelAddress ) {
			this.setOutput(false, channelAddress);
		}
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
	
	/**
	 * function to calculates the Kilowatthour, using the power of each phase
	 * 
	 * @param time Long values of time in seconds
	 * 
	 */
	private double calculatePower(double time) {
		double kiloWattHour = ((time) / 3600000.0) * parent.config.powerOfPhase();
		return kiloWattHour;
	}
	
	public LevelDef(ControllerHeatingElement parent) {
		this.parent = parent;
		setSwitchOn(false);
	}
	
	public ChannelAddress[] getOutputChannelAddress() {
		return outputChannelAddress;
	}

	public void setOutputChannelAddress(ChannelAddress[] outputChannelAddress) {
		this.outputChannelAddress = outputChannelAddress;
	}

	public boolean isSwitchOn() {
		return isSwitchOn;
	}

	public void setSwitchOn(boolean isSwitchOn) {
		this.isSwitchOn = isSwitchOn;
	}
	
	public ControllerHeatingElement getParent() {
		return parent;
	}
	
	private final Logger LOGGER = LoggerFactory.getLogger(PhaseDef.class);

	protected void logInfo(Logger log, String message) {
		log.info(message);
	}

	protected void logError(Logger log, String message) {
		log.error(message);
	}
	
	public long getTotalLevelTime() {
		return totalLevelTime;
	}

	public void setTotalLevelTime(long totalLevelTime) {
		this.totalLevelTime = totalLevelTime;
	}

	public Stopwatch getTimeStopwatch() {
		return timeStopwatch;
	}

	public void setTimeStopwatch(Stopwatch timeStopwatch) {
		this.timeStopwatch = timeStopwatch;
	}

	public double getTotalLevelPower() {
		return totalLevelPower;
	}

	public void setTotalLevelPower(double totalLevelPower) {
		this.totalLevelPower = totalLevelPower;
	}

	public Logger getLOGGER() {
		return LOGGER;
	}
	
}
