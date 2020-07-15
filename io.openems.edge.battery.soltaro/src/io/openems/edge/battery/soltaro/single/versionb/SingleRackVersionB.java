package io.openems.edge.battery.soltaro.single.versionb;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.battery.soltaro.single.versionb.enums.ContactorControl;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface SingleRackVersionB extends SoltaroBattery, Battery, OpenemsComponent, StartStoppable {
	
	static final Integer SYSTEM_RESET = 0x1;
	static final Integer SLEEP = 0x1;
	public static int RETRY_COMMAND_SECONDS = 30;
	public static int RETRY_COMMAND_MAX_ATTEMPTS = 30;

	default void startSystem() {
		// To avoid hardware damages do not send start command if system has already
		// started
		if (this.getContactorControl() == ContactorControl.ON_GRID || this.getContactorControl() == ContactorControl.CONNECTION_INITIATING) {
			return;
		}

		try {
//			log.debug("write value to contactor control channel: value: " + ContactorControl.CONNECTION_INITIATING);
			this.setContactorControl(ContactorControl.CONNECTION_INITIATING);
		} catch (OpenemsNamedException e) {
//			log.error("Error while trying to start system\n" + e.getMessage());
			System.out.println(("Error while trying to start system\n" + e.getMessage()));
		}
	}

	default void stopSystem() {
		// To avoid hardware damages do not send stop command if system has already
		// stopped
		if (this.getContactorControl() == ContactorControl.CUT_OFF) {
			return;
		}

		try {
//			log.debug("write value to contactor control channel: value: " + ContactorControl.CUT_OFF);
			this.setContactorControl(ContactorControl.CUT_OFF);
		} catch (OpenemsNamedException e) {
//			log.error("Error while trying to stop system\n" + e.getMessage());
			System.out.println(("Error while trying to stop system\n" + e.getMessage()));
		}
	}
	
	default void resetSystem() {
		try {
			this.setSystemReset(SYSTEM_RESET);
		} catch (OpenemsNamedException e) {
			System.out.println("Error while trying to reset the system!");
		}
	}

	default void sleepSystem() {
		try {
			this.setSleep(SLEEP);
		} catch (OpenemsNamedException e) {
			System.out.println("Error while trying to sleep the system!");
		}

	}
	
	default boolean isSystemRunning() {		
		return  this.getContactorControl() == ContactorControl.ON_GRID;
	}

	default boolean isSystemStopped() {
		return  this.getContactorControl() == ContactorControl.CUT_OFF;
	}

	
	public default WriteChannel<ContactorControl> getContactorControlChannel() {
		return this.channel(io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.BMS_CONTACTOR_CONTROL);
	}
	
	public default ContactorControl getContactorControl() {
		return this.getContactorControlChannel().value().asEnum();
	}
	
	public default void _setContactorControl(ContactorControl value) {
		this.getContactorControlChannel().setNextValue(value);
	}
	
	public default void setContactorControl(ContactorControl value) throws OpenemsNamedException {
		this.getContactorControlChannel().setNextWriteValue(value);
	}
	
	public default IntegerWriteChannel getSystemResetChannel() {
		return this.channel(io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.SYSTEM_RESET);
	}
	
	public default Value<Integer> getSystemReset() {
		return this.getSystemResetChannel().value();
	}
	
	public default void _setSystemReset(Integer value) {
		this.getSystemResetChannel().setNextValue(value);
	}
	
	public default void setSystemReset(Integer value) throws OpenemsNamedException {
		this.getSystemResetChannel().setNextWriteValue(value);
	}
	
	public default IntegerWriteChannel getSleepChannel() {
		return this.channel(io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.SLEEP);
	}
	
	public default Value<Integer> getSleep() {
		return this.getSleepChannel().value();
	}
	
	public default void _setSleep(Integer value) {
		this.getSleepChannel().setNextValue(value);
	}
	
	public default void setSleep(Integer value) throws OpenemsNamedException {
		this.getSleepChannel().setNextWriteValue(value);
	}
	
	public default IntegerWriteChannel getSocLowProtectionChannel() {
		return this.channel(io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION);
	}
	
	public default Value<Integer> getSocLowProtection() {
		return this.getSocLowProtectionChannel().value();
	}
	
	public default void _setSocLowProtection(Integer value) {
		this.getSocLowProtectionChannel().setNextValue(value);
	}
	
	public default void setSocLowProtection(Integer value) throws OpenemsNamedException {
		this.getSocLowProtectionChannel().setNextWriteValue(value);
	}
	
	public default IntegerWriteChannel getSocLowProtectionRecoverChannel() {
		return this.channel(io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.STOP_PARAMETER_SOC_LOW_PROTECTION_RECOVER);
	}
	
	public default Value<Integer> getSocLowProtectionRecover() {
		return this.getSocLowProtectionRecoverChannel().value();
	}
	
	public default void _setSocLowProtectionRecover(Integer value) {
		this.getSocLowProtectionRecoverChannel().setNextValue(value);
	}
	
	public default void setSocLowProtectionRecover(Integer value) throws OpenemsNamedException {
		this.getSocLowProtectionRecoverChannel().setNextWriteValue(value);
	}
	
	public default IntegerWriteChannel getWatchdogChannel() {
		return this.channel(io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.EMS_COMMUNICATION_TIMEOUT);
	}
	
	public default Value<Integer> getWatchdog() {
		return this.getWatchdogChannel().value();
	}
	
	public default void _setWatchdog(Integer value) {
		this.getWatchdogChannel().setNextValue(value);
	}
	
	public default void setWatchdog(Integer value) throws OpenemsNamedException {
		this.getWatchdogChannel().setNextWriteValue(value);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStartAttemptsChannel() {
		return this.channel(io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.MAX_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartAttempts() {
		return this.getMaxStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_START_ATTEMPTS} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStartAttempts(Boolean value) {
		this.getMaxStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStopAttemptsChannel() {
		return this.channel(io.openems.edge.battery.soltaro.single.versionb.SingleRackChannelId.MAX_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopAttempts() {
		return this.getMaxStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_ATTEMPTS}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStopAttempts(Boolean value) {
		this.getMaxStopAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

}
