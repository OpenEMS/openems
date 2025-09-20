package io.openems.edge.goodwe.batteryinverter.statemachine;

import java.time.Clock;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.goodwe.batteryinverter.GoodWeBatteryInverterImpl;

public class Context extends AbstractContext<GoodWeBatteryInverterImpl> {

	protected Clock clock;

	public Context(GoodWeBatteryInverterImpl parent, Clock clock) {
		super(parent);
		this.clock = clock;
	}
}