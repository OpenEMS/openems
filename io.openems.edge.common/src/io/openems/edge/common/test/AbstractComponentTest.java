package io.openems.edge.common.test;

import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a test framework for OpenEMS Components.
 */
public abstract class AbstractComponentTest {

	/**
	 * Stores a tuple of ChannelAddress and Object value.
	 */
	public static class ChannelValue {
		private final ChannelAddress address;
		private final Object value;

		public ChannelValue(ChannelAddress address, Object value) {
			this.address = address;
			this.value = value;
		}

		public ChannelAddress getAddress() {
			return address;
		}

		public Object getValue() {
			return value;
		}

		@Override
		public String toString() {
			return address.toString() + ":" + value;
		}
	}

	/**
	 * Wraps a Time-leap.
	 */
	private static class TimeLeap {
		private final TimeLeapClock clock;
		private final long amountToAdd;
		private final TemporalUnit unit;

		public TimeLeap(TimeLeapClock clock, long amountToAdd, TemporalUnit unit) {
			this.clock = clock;
			this.amountToAdd = amountToAdd;
			this.unit = unit;
		}

		public void apply() {
			this.clock.leap(this.amountToAdd, this.unit);
		}
	}

	/**
	 * Defines a Test-Case consisting of given inputs and expected outputs.
	 */
	public static class TestCase {
		private final List<ChannelValue> inputs = new ArrayList<>();
		private final List<ChannelValue> outputs = new ArrayList<>();

		private TimeLeap timeleap = null;

		public TestCase input(ChannelAddress address, Object value) {
			this.inputs.add(new ChannelValue(address, value));
			return this;
		}

		public TestCase output(ChannelAddress address, Object value) {
			this.outputs.add(new ChannelValue(address, value));
			return this;
		}

		public TestCase timeleap(TimeLeapClock clock, long amountToAdd, TemporalUnit unit) {
			this.timeleap = new TimeLeap(clock, amountToAdd, unit);
			return this;
		}

		/**
		 * Applies the time leap to the clock.
		 */
		public void applyTimeLeap() {
			if (this.timeleap != null) {
				this.timeleap.apply();
			}
		}

		/**
		 * Applies the values for input channels.
		 * 
		 * @param components Referenced components
		 */
		public void applyInputs(Map<String, OpenemsComponent> components) {
			for (ChannelValue input : this.inputs) {
				OpenemsComponent component = components.get(input.address.getComponentId());
				if (component == null) {
					throw new IllegalArgumentException("The component [" + input.address.getComponentId()
							+ "] was not added to the OpenEMS Component test framework!");
				}
				Channel<?> channel = component.channel(input.address.getChannelId());
				channel.setNextValue(input.getValue());
				channel.nextProcessImage();
			}
		}

		/**
		 * Validates the output values.
		 * 
		 * @param components Referenced components
		 * @param index
		 * @throws Exception on validation failure
		 */
		public void validateOutputs(Map<String, OpenemsComponent> components, int index) throws Exception {
			for (ChannelValue output : this.outputs) {
				Object expected = output.value;
				Channel<?> channel = components.get(output.address.getComponentId())
						.channel(output.address.getChannelId());
				Object got;
				if (channel instanceof WriteChannel) {
					got = ((WriteChannel<?>) channel).getNextWriteValue().orElse(null);
				} else {
					got = channel.getNextValue().orElse(null);
				}
				if (!Objects.equals(expected, got)) {
					throw new Exception("On TestCase #" + index + ": expected [" + expected + "] got [" + got
							+ "] for Channel [" + output.address.toString() + "] on Inputs [" + this.inputs + "]");
				}
			}
		}
	}

	/**
	 * Components referenced by the tested Component.
	 */
	private final Map<String, OpenemsComponent> components = new HashMap<>();

	private final List<TestCase> testCases = new ArrayList<>();

	public AbstractComponentTest(OpenemsComponent... components) {
		// store Components
		for (OpenemsComponent component : components) {
			this.components.put(component.id(), component);
		}
	}

	public AbstractComponentTest(OpenemsComponent[] components, DummyComponentManager componentManager) {
		this(components);
		// forward Components to ComponentManager
		for (OpenemsComponent c : components) {
			componentManager.addComponent(c);
		}
	}

	/**
	 * Adds a Test-Case.
	 * 
	 * @param testCase The TestCase
	 * @return itself, to use as a builder
	 */
	public AbstractComponentTest next(TestCase testCase) {
		this.testCases.add(testCase);
		return this;
	}

	/**
	 * Runs all Test-Cases.
	 * 
	 * @throws Exception on validation failure
	 */
	public void run() throws Exception {
		int index = 0;
		for (TestCase testCase : this.testCases) {
			testCase.applyTimeLeap();
			testCase.applyInputs(this.components);
			this.executeLogic();
			testCase.validateOutputs(this.components, ++index);
		}
	}

	/**
	 * Executes the tested component logic. This method is executed after the inputs
	 * are applied. After finishing the expected outputs are validated.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected abstract void executeLogic() throws OpenemsNamedException;

}
