package io.openems.edge.battery.fenecon.home.statemachine;

import io.openems.edge.battery.fenecon.home.BatteryFeneconHome;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryFeneconHome> {

	/**
	 * The Battery-Start-Up-Relay Channel used to start the battery; possibly null.
	 */
	protected final BooleanWriteChannel batteryStartUpRelayChannel;

	public Context(BatteryFeneconHome parent, BooleanWriteChannel batteryStartUpRelayChannel) {
		super(parent);
		this.batteryStartUpRelayChannel = batteryStartUpRelayChannel;
	}

	protected boolean isBatteryStarted() {
		var isNotStarted = this.getParent().getBmsControl();
		if (!isNotStarted.isDefined()) {
			return false;
		}
		return !isNotStarted.get();
	}

	protected void retryModbusCommunication() {
		this.getParent().retryModbusCommunication();
	}
}