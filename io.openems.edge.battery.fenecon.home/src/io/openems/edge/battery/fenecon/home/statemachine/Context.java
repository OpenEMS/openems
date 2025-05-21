package io.openems.edge.battery.fenecon.home.statemachine;

import java.time.Clock;

import io.openems.common.function.BooleanConsumer;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHome;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHome.ChannelId;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryFeneconHome> {

	protected final Clock clock;
	/** The Battery-Start-Up-Relay state. */
	protected final Boolean batteryStartUpRelay;
	/** Switches the Battery-Start-Up-Relay ON or OFF. */
	protected final BooleanConsumer setBatteryStartUpRelay;
	/** See {@link ChannelId#BMS_CONTROL}. */
	protected final Boolean bmsControl;
	/** See {@link ModbusComponent.ChannelId#MODBUS_COMMUNICATION_FAILED}. */
	protected final boolean modbusCommunicationFailed;
	protected final Runnable retryModbusCommunication;

	public Context(BatteryFeneconHome parent, Clock clock, Boolean batteryStartUpRelay,
			BooleanConsumer setBatteryStartUpRelay, Boolean bmsControl, boolean modbusCommunicationFailed,
			Runnable retryModbusCommunication) {
		super(parent);
		this.clock = clock;
		this.batteryStartUpRelay = batteryStartUpRelay;
		this.setBatteryStartUpRelay = setBatteryStartUpRelay;
		this.bmsControl = bmsControl;
		this.modbusCommunicationFailed = modbusCommunicationFailed;
		this.retryModbusCommunication = retryModbusCommunication;
	}
}