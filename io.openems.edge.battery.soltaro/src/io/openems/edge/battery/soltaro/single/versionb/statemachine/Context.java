package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.edge.battery.soltaro.CellCharacteristic;
import io.openems.edge.battery.soltaro.single.versionb.Config;
import io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionBImpl;

public class Context {
	protected final SingleRackVersionBImpl component;
	protected final Config config;
	protected final CellCharacteristic cellCharacteristic;

	public Context(SingleRackVersionBImpl component, Config config, CellCharacteristic cellCharacteristic) {
		super();
		this.component = component;
		this.config = config;
		this.cellCharacteristic = cellCharacteristic;
	}
}