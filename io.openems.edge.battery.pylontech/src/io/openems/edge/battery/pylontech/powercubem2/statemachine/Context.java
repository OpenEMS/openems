package io.openems.edge.battery.pylontech.powercubem2.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.pylontech.powercubem2.PylontechPowercubeM2Battery;
import io.openems.edge.battery.pylontech.powercubem2.Status;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<PylontechPowercubeM2Battery> {

	protected final IntegerWriteChannel batteryWakeSleepChannel;

	private final Logger log = LoggerFactory.getLogger(Context.class);

	public Context(PylontechPowercubeM2Battery parent, IntegerWriteChannel batteryWakeSleepChannel) {
		super(parent);
		this.batteryWakeSleepChannel = batteryWakeSleepChannel;
	}

	/**
	 * Checks if battery is awake - i.e in charge / discharge modes.
	 * 
	 * @return boolean which says if battery is awake.
	 */
	protected boolean isBatteryAwake() {
		Status status = this.getParent().getSystemStatus();
		if (status == Status.CHARGE || status == Status.DISCHARGE || status == Status.IDLE) {
			return true;
		}
		return false;
	}

	/**
	 * Wake battery up from sleep (analogous to switching it to 'running' state).
	 * setBatteryAwake = true to wake up. setBatteryAwake = false to put to sleep.
	 * 
	 * @param setBatteryAwake (boolean to say if the battery should be set to wake
	 *                        or sleep mode)
	 * @throws OpenemsNamedException on error
	 */
	public void setBatteryWakeSleep(boolean setBatteryAwake) throws OpenemsNamedException {
		if (setBatteryAwake) {
			if (this.batteryWakeSleepChannel == null) {
				this.logInfo(this.log,
						"Battery Wake/Sleep channel not provided to State Machine context. Cannot SWITCH BATTERY ON.");
				return;
			} else {
				this.logInfo(this.log, "Setting Battery Wake/Sleep Channel to WAKE.");
			}
			// Write the WAKE value (0x55) to the Wake/sleep channel
			this.batteryWakeSleepChannel.setNextWriteValue(0x55);
		} else {
			if (this.batteryWakeSleepChannel == null) {
				this.logInfo(this.log,
						"Battery Wake/Sleep channel not provided to State Machine. Cannot SWITCH BATTERY OFF.");
				return;
			} else {
				this.logInfo(this.log, "Setting Battery Wake/Sleep Channel to SLEEP.");
			}
			// Write the SLEEP value (0xAA) to the Wake/sleep channel
			this.batteryWakeSleepChannel.setNextWriteValue(0xAA);

		}

	}

}