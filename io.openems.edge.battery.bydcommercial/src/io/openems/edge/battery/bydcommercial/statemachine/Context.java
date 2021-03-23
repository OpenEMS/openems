package io.openems.edge.battery.bydcommercial.statemachine;

import io.openems.edge.battery.bydcommercial.BatteryBoxC130;
import io.openems.edge.battery.bydcommercial.Config;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<BatteryBoxC130> {

	protected final Config config;

	public Context(BatteryBoxC130 parent, Config config) {
		super(parent);
		this.config = config;
	}
}