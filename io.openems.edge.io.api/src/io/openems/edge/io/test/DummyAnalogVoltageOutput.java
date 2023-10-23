package io.openems.edge.io.test;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.io.api.AnalogOutput;
import io.openems.edge.io.api.AnalogVoltageOutput;

/**
 * Provides a simple, simulated Analog Voltage Output component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummyAnalogVoltageOutput extends AbstractOpenemsComponent implements AnalogOutput, AnalogVoltageOutput {

	private Range range;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public DummyAnalogVoltageOutput(String id) {
		this(id, new Range(0, 100, 10000));
	}

	public DummyAnalogVoltageOutput(String id, Range range) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				AnalogOutput.ChannelId.values(), //
				AnalogVoltageOutput.ChannelId.values() //
		);
		super.activate(new DummyComponentContext(), id, "", true);
		this.range = range;
	}

	@Override
	public Range range() {
		return this.range;
	}
}
