package io.openems.edge.controller.HeatingElementController;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;

public class OldPhase {

	private ChannelAddress outputChannelAddress;
	LocalDateTime switchedOnTime = null;
	LocalDateTime switchedOffTime = null;
	long totalRunningTime = 0;
	
	ComponentManager componentManager;
	
	private final Logger log = LoggerFactory.getLogger(OldPhase.class);
	
	OldPhase(ChannelAddress outputChannelAddress, ComponentManager componentManager){
		this.outputChannelAddress = outputChannelAddress;
		this.componentManager = componentManager;
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
	public void activate(boolean value)
			throws IllegalArgumentException, OpenemsNamedException {
		try {
			WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(this.outputChannelAddress);
			Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
				this.log.info("Set output [" + outputChannel.address() + "] " + (value) + ".");				
				outputChannel.setNextWriteValue(value);
			}
			switchedOnTime = LocalDateTime.now();			
		} catch (OpenemsException e) {
			this.log.error("Unable to set output: [" +this.outputChannelAddress + "] " + e.getMessage());
		}
	}
	
	/**
	 * Helper function to switch an output off if it was not switched before.
	 *
	 * @param value                The boolean value which must set on the output
	 *                             channel address
	 * @param outputChannelAddress The address of the channel
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 * @return totalRunningTime    Total running time of the phase
	 */
	protected long deActivate(boolean value)
			throws IllegalArgumentException, OpenemsNamedException {
		try {
			WriteChannel<Boolean> outputChannel = this.componentManager.getChannel(this.outputChannelAddress);
			Optional<Boolean> currentValueOpt = outputChannel.value().asOptional();
			if (!currentValueOpt.isPresent() || currentValueOpt.get() != value) {
				this.log.info("Set output [" + outputChannel.address() + "] " + (value) + ".");				
				outputChannel.setNextWriteValue(value);
			}
			switchedOffTime = LocalDateTime.now();
			if (switchedOnTime != null) {
				totalRunningTime += ChronoUnit.MILLIS.between(switchedOffTime, switchedOnTime);
			}else {
				totalRunningTime = 0;
			}			
			return totalRunningTime;
			
		} catch (OpenemsException e) {
			totalRunningTime = 0;
			this.log.error("Unable to set output: [" + this.outputChannelAddress + "] " + e.getMessage());
			return totalRunningTime;
		}
	}




}