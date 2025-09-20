package io.openems.edge.pvinverter.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

/**
 * Provides a simple, simulated {@link ManagedSymmetricPvInverter} Component
 * that can be used together with the OpenEMS Component test framework.
 */
public class DummyManagedSymmetricPvInverter extends AbstractDummyOpenemsComponent<DummyManagedSymmetricPvInverter>
		implements ElectricityMeter, OpenemsComponent, ManagedSymmetricPvInverter {

	public DummyManagedSymmetricPvInverter(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				ElectricityMeter.ChannelId.values());
	}

	@Override
	protected DummyManagedSymmetricPvInverter self() {
		return this;
	}
}
