package io.openems.edge.common.test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

/**
 * Provides a test framework for OpenEMS Components.
 */
public abstract class AbstractComponentTest<SELF extends AbstractComponentTest<SELF, SUT>, SUT extends OpenemsComponent> {

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
		private static int instanceCounter = 0;

		private final String description;
		private final List<ChannelValue> inputs = new ArrayList<>();
		private final List<ChannelValue> outputs = new ArrayList<>();

		private TimeLeap timeleap = null;

		public TestCase() {
			this("");
		}

		/**
		 * Create a TestCase with a description.
		 * 
		 * @param description the description
		 */
		public TestCase(String description) {
			this.description = "#" + (++instanceCounter) + (description.isEmpty() ? "" : ": " + description);
		}

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
		protected void applyInputs(Map<String, OpenemsComponent> components) {
			for (ChannelValue input : this.inputs) {
				OpenemsComponent component = components.get(input.address.getComponentId());
				if (component == null) {
					throw new IllegalArgumentException("On TestCase [" + this.description + "]: " + //
							"the component [" + input.address.getComponentId() + "] " //
							+ "was not added to the OpenEMS Component test framework!");
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
		protected void validateOutputs(Map<String, OpenemsComponent> components) throws Exception {
			for (ChannelValue output : this.outputs) {
				Object expected = output.value;
				Channel<?> channel = components.get(output.address.getComponentId())
						.channel(output.address.getChannelId());
				if (expected instanceof OptionsEnum) {
					expected = ((OptionsEnum) expected).getValue();
				}
				Object got;
				String gotText;
				if (channel instanceof WriteChannel) {
					got = ((WriteChannel<?>) channel).getNextWriteValue().orElse(null);
					gotText = Objects.toString(got);
				} else {
					Value<?> value = channel.getNextValue();
					got = value.orElse(null);
					gotText = value.asOptionString();
				}
				if (!Objects.equals(expected, got)) {
					throw new Exception("On TestCase [" + this.description + "]: " //
							+ "expected [" + output.value + "] " //
							+ "got [" + gotText + "] " //
							+ "for Channel [" + output.address.toString() + "] " //
							+ "on Inputs [" + this.inputs + "]");
				}
			}
		}
	}

	/**
	 * References added by {@link #addReference()}.
	 */
	private final Set<Object> references = new HashSet<>();

	/**
	 * Components referenced by the tested Component.
	 */
	private final Map<String, OpenemsComponent> components = new HashMap<>();

	/**
	 * The {@link OpenemsComponent} to be tested. "sut" is for system-under-test.
	 */
	private final SUT sut;

	public AbstractComponentTest(SUT sut) {
		this.sut = sut;
	}

	/**
	 * Gets the 'system-under-test', i.e. the tested {@link OpenemsComponent}.
	 * 
	 * @return the tested Component
	 */
	public SUT getSut() {
		return this.sut;
	}

	/**
	 * Gets an instance of the correct subclass of myself.
	 * 
	 * @return myself
	 */
	protected abstract SELF self();

	/**
	 * Adds a OSGi Declarative Services @Reference via java reflection.
	 * 
	 * <p>
	 * Can also be used to set any other private field via reflection.
	 * 
	 * @param memberName the name of the field or method
	 * @param object     the reference object
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public SELF addReference(String memberName, Object object) throws Exception {
		// Set the reference via reflection
		Class<?> clazz = this.sut.getClass();
		try {
			Field field = clazz.getDeclaredField(memberName);
			field.setAccessible(true);
			field.set(this.sut, object);
		} catch (NoSuchFieldException e) {
			// Ignore. Try method.
			this.invokeSingleArgMethod(memberName, object);
		}

		this.references.add(object);

		// If this is a DummyComponentManager -> fill it with existing Components
		if (object instanceof DummyComponentManager) {
			for (OpenemsComponent component : this.components.values()) {
				((DummyComponentManager) object).addComponent(component);
			}
		}

		// If this is an OpenemsComponent -> store it for later
		if (object instanceof OpenemsComponent) {
			this.addComponent((OpenemsComponent) object);
		}
		return this.self();
	}

	/**
	 * Adds an available {@link OpenemsComponent}.
	 * 
	 * <p>
	 * If the provided Component is a {@link DummyComponentManager}.
	 * 
	 * @param component
	 * @return itself, to use as a builder
	 */
	public SELF addComponent(OpenemsComponent component) {
		this.components.put(component.id(), component);

		// Is a DummyComponentManager present -> add this Component
		for (Object object : this.references) {
			if (object instanceof DummyComponentManager) {
				((DummyComponentManager) object).addComponent(component);
			}
		}
		return this.self();
	}

	/**
	 * Calls the 'activate()' method of the 'system-under-test'.
	 * 
	 * <p>
	 * If 'activate()' changes the configuration, the OSGi behavior is simulated by
	 * calling 'deactivate()' and then again 'activate()'
	 * 
	 * @param config the configuration
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public SELF activate(AbstractComponentConfig config) throws Exception {
		int configChangeCount = this.getConfigChangeCount();
		this.callActivate(config);

		if (configChangeCount != this.getConfigChangeCount()) {
			// deactivate + recursive call
			this.callDeactivate();
			this.activate(config);
		}

		// Now SUT can be added to the list, as it does have an ID now
		this.addComponent(sut);
		return this.self();
	}

	private int getConfigChangeCount() throws IOException, InvalidSyntaxException {
		int result = 0;
		for (Object object : this.references) {
			if (object instanceof ConfigurationAdmin) {
				ConfigurationAdmin cm = (ConfigurationAdmin) object;
				Configuration[] configs = cm.listConfigurations(null);
				for (Configuration config : configs) {
					result += config.getChangeCount();
				}
			}
		}
		return result;
	}

	private void callActivate(AbstractComponentConfig config)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> clazz = this.sut.getClass();
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (!method.getName().equals("activate")) {
				continue;
			}
			Object[] args = new Object[method.getParameterCount()];
			for (int i = 0; i < method.getParameterCount(); i++) {
				Parameter parameter = method.getParameters()[i];
				Object arg;

				if (ComponentContext.class.isAssignableFrom(parameter.getType())) {
					// ComponentContext
					arg = null; // TODO create DummyComponentContext

				} else if (parameter.getType().isInstance(config)) {
					// Config
					arg = config;

				} else {
					throw new IllegalArgumentException("Unknown activate() parameter " + parameter);

				}
				args[i] = arg;
			}
			method.setAccessible(true);
			method.invoke(this.sut, args);
			return;
		}
	}

	private void callDeactivate() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		Class<?> clazz = this.sut.getClass();
		Method method = clazz.getDeclaredMethod("deactivate");
		method.setAccessible(true);
		method.invoke(this.sut);
	}

	private void invokeSingleArgMethod(String methodName, Object arg)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> clazz = this.sut.getClass();
		Method[] methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (!method.getName().equals(methodName)) {
				continue;
			}
			if (method.getParameterCount() != 1) {
				continue;
			}

			Parameter parameter = method.getParameters()[0];
			if (!parameter.getType().isAssignableFrom(arg.getClass())) {
				continue;
			}

			method.setAccessible(true);
			method.invoke(this.sut, arg);
			return;
		}
		throw new IllegalArgumentException(
				"Unable to find matching method for [" + methodName + "] with arg [" + arg + "]");
	}

	/**
	 * Runs a Test-Case.
	 * 
	 * @param testCase The TestCase
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public SELF next(TestCase testCase) throws Exception {
		testCase.applyTimeLeap();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);
		testCase.applyInputs(this.components);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS);
		this.executeLogic();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE);
		testCase.validateOutputs(this.components);
		return this.self();
	}

	/**
	 * TODO: remove: Test-Cases are now executed directly on adding via
	 * {@link #next(TestCase)}.
	 */
	public void run() {

	}

	/**
	 * If the 'system-under-test' is a {@link EventHandler} call the
	 * {@link EventHandler#handleEvent(Event)} method.
	 * 
	 * @param topic the {@link Event} topic
	 */
	protected void handleEvent(String topic) {
		if (this.sut instanceof EventHandler) {
			Event event = new Event(topic, new HashMap<String, Object>());
			((EventHandler) this.sut).handleEvent(event);
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
