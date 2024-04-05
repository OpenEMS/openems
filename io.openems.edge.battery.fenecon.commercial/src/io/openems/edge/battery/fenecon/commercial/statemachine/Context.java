package io.openems.edge.battery.fenecon.commercial.statemachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.battery.fenecon.commercial.BatteryFeneconCommercial;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryFeneconCommercial> {

	/**
	 * The Battery-Master-Bms-Start-Up-Relay Channel used to start the battery;
	 * possibly null.
	 */
	protected final BooleanWriteChannel batteryStartStopRelayChannel;

	private final Logger log = LoggerFactory.getLogger(Context.class);

	public Context(BatteryFeneconCommercial parent, BooleanWriteChannel batteryMasterBmsStartUpRelayChannel) {
		super(parent);
		this.batteryStartStopRelayChannel = batteryMasterBmsStartUpRelayChannel;
	}

	/**
	 * Is battery Started ?.
	 *
	 * @return true if battery started.
	 */
	public boolean isBatteryStarted() {
		var isStarted = this.getParent().getMasterStarted();
		if (!isStarted.isDefined()) {
			return false;
		}
		return isStarted.get();
	}

	/**
	 * Set Relay Open or Relay Close.
	 *
	 * @param value true to open the relay; <br/>
	 *              false to close the relay; <br/>
	 * @throws OpenemsNamedException on error
	 */
	protected void setBatteryStartUpRelays(boolean value) throws OpenemsNamedException {
		if (value) {
			if (this.batteryStartStopRelayChannel == null || this.batteryStartStopRelayChannel == null) {
				this.log.info(
						"Because of the wrong/missed configured Battery Start Stop Relay Channel Address, relay CAN NOT OPEN.");
				return;
			}
			this.log.info("Set output [" + this.batteryStartStopRelayChannel.address() + "] OPEN.");

		} else if (this.batteryStartStopRelayChannel == null || this.batteryStartStopRelayChannel == null) {
			this.log.info(
					"Because of the wrong/missed configured Battery Start Stop Relay Channel Address, relay CAN NOT CLOSE.");
			return;

		} else {
			this.log.info("Set output [" + this.batteryStartStopRelayChannel.address() + "] CLOSE.");
		}
		this.batteryStartStopRelayChannel.setNextWriteValue(value);
	}

	/**
	 * Get Start Stop relay current status.
	 *
	 * @return true stands for the relay opened; <br/>
	 *         false stands for the relay closed; <br/>
	 *         possibly null
	 */
	protected Boolean getBatteryStartStopRelay() {
		if (this.batteryStartStopRelayChannel != null) {
			return this.batteryStartStopRelayChannel.value().get();
		}
		return false;
	}
}