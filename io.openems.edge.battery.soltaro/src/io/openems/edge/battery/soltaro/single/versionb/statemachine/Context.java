package io.openems.edge.battery.soltaro.single.versionb.statemachine;

import io.openems.edge.battery.soltaro.CellCharacteristic;
import io.openems.edge.battery.soltaro.single.versionb.Config;
import io.openems.edge.battery.soltaro.single.versionb.SingleRackVersionBImpl;
import io.openems.edge.common.statemachine.AbstractContext;

public class Context extends AbstractContext<SingleRackVersionBImpl> {

	protected final Config config;
	protected final CellCharacteristic cellCharacteristic;

	public Context(SingleRackVersionBImpl parent, Config config, CellCharacteristic cellCharacteristic) {
		super(parent);
		this.config = config;
		this.cellCharacteristic = cellCharacteristic;
	}

}