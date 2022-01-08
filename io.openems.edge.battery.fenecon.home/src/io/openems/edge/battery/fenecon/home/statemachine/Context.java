package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.FeneconHomeBattery;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<FeneconHomeBattery> {

	/**
	 * The Battery-Start-Up-Relay Channel used to start the battery; possibly null.
	 */
	protected final BooleanWriteChannel batteryStartUpRelayChannel;

	public Context(FeneconHomeBattery parent, BooleanWriteChannel batteryStartUpRelayChannel) {
		super(parent);
		this.batteryStartUpRelayChannel = batteryStartUpRelayChannel;
	}

	protected boolean isBatteryStarted() {
		switch (this.getParent().getBmsControl()) {
		case SWITCHED_ON:
		case IGNORED:
			return true;

		case SWITCHED_OFF:
		case UNDEFINED:
			return false;
		}
		return false;
	}

}