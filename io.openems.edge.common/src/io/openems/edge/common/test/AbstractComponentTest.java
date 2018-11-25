package io.openems.edge.common.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
	 * Defines a Test-Case consisting of given inputs and expected outputs.
	 */
	public static class TestCase {
		private final List<ChannelValue> inputs = new ArrayList<>();
		private final List<ChannelValue> outputs = new ArrayList<>();

		public TestCase input(ChannelAddress address, Object value) {
			this.inputs.add(new ChannelValue(address, value));
			return this;
		}

		public TestCase output(ChannelAddress address, Object value) {
			this.outputs.add(new ChannelValue(address, value));
			return this;
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
		 * @throws Exception on validation failure
		 */
		public void validateOutputs(Map<String, OpenemsComponent> components) throws Exception {
			for (ChannelValue output : this.outputs) {
				WriteChannel<?> channel = components.get(output.address.getComponentId())
						.channel(output.address.getChannelId());
				Object expected = output.value;
				Object got = channel._getNextWriteValue().orElse(null);
				if (!Objects.equals(expected, got)) {
					throw new Exception("Expected [" + expected + "], Got [" + got + "] for Channel ["
							+ output.address.toString() + "] on Inputs [" + this.inputs + "]");
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
		for (OpenemsComponent component : components) {
			this.components.put(component.id(), component);
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
		for (TestCase testCase : this.testCases) {
			testCase.applyInputs(this.components);
			this.executeLogic();
			testCase.validateOutputs(this.components);
		}
	}

	/**
	 * Executes the tested component logic. This method is executed after the inputs
	 * are applied. After finishing the expected outputs are validated.
	 */
	protected abstract void executeLogic();

}
