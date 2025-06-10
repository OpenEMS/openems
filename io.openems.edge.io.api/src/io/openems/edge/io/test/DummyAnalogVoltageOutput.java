package io.openems.edge.io.test;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.io.api.AnalogOutput;
import io.openems.edge.io.api.AnalogVoltageOutput;

/**
 * Provides a simple, simulated {@link AnalogVoltageOutput} component that can
 * be used together with the OpenEMS Component test framework.
 */
public class DummyAnalogVoltageOutput extends AbstractDummyOpenemsComponent<DummyAnalogVoltageOutput>
		implements AnalogOutput, AnalogVoltageOutput {

	private Range range = new Range(0, 100, 10000);

	public DummyAnalogVoltageOutput(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				AnalogOutput.ChannelId.values(), //
				AnalogVoltageOutput.ChannelId.values() //
		);
	}

	@Override
	protected DummyAnalogVoltageOutput self() {
		return this;
	}

	public DummyAnalogVoltageOutput setRange(Range range) {
		this.range = range;
		return this;
	}

	@Override
	public Range range() {
		return this.range;
	}

}
