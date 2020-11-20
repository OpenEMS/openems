package io.openems.edge.common.test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Collection;
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
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.TypeUtils;

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
		 * @throws OpenemsNamedException    on error
		 * @throws IllegalArgumentException on error
		 */
		protected void applyInputs(Map<String, OpenemsComponent> components)
				throws IllegalArgumentException, OpenemsNamedException {
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
				if (channel instanceof WriteChannel<?>) {
					((WriteChannel<?>) channel).setNextWriteValueFromObject(input.getValue());
				}
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
				Object got;
				if (channel instanceof WriteChannel) {
					got = ((WriteChannel<?>) channel).getNextWriteValueAndReset().orElse(null);
				} else {
					Value<?> value = channel.getNextValue();
					got = value.orElse(null);
				}

				// Try to parse an Enum
				if (channel.channelDoc() instanceof EnumDoc) {
					EnumDoc enumDoc = (EnumDoc) channel.channelDoc();
					Integer intGot = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, got);
					got = enumDoc.getOption(intGot);
				}
				if (!Objects.equals(expected, got)) {
					throw new Exception("On TestCase [" + this.description + "]: " //
							+ "expected [" + output.value + "] " //
							+ "got [" + got + "] " //
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
		// Set the reference recursively via reflection
		if (!this.addReference(this.sut.getClass(), memberName, object)) {
			throw new Exception("Unable to add reference on field or method [" + memberName + "]");
		}

		// Store reference
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
		if (object instanceof Collection<?>) {
			for (Object o : (Collection<?>) object) {
				if (o instanceof OpenemsComponent) {
					this.addComponent((OpenemsComponent) o);
				}
			}
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
		// Add the configuration to ConfigurationAdmin
		for (Object object : this.references) {
			if (object instanceof DummyConfigurationAdmin) {
				DummyConfigurationAdmin cm = (DummyConfigurationAdmin) object;
				cm.addConfig(config);
			}
		}

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
					arg = DummyComponentContext.from(config);

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

	private boolean addReference(Class<?> clazz, String memberName, Object object)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		try {
			Field field = clazz.getDeclaredField(memberName);
			field.setAccessible(true);
			field.set(this.sut, object);
			return true;
		} catch (NoSuchFieldException e) {
			// Ignore. Try method.
			if (this.invokeSingleArgMethod(clazz, memberName, object)) {
				return true;
			}
		}
		// If we are here, no matching field or method was found. Search in parent
		// classes.
		Class<?> parent = clazz.getSuperclass();
		if (parent == null) {
			return false; // reached 'java.lang.Object'
		}
		return addReference(parent, memberName, object);
	}

	private boolean invokeSingleArgMethod(Class<?> clazz, String methodName, Object arg)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
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
			return true;
		}

		// Unable to find matching method
		return false;
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
		this.onBeforeProcessImage();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);
		for (Channel<?> channel : this.getSut().channels()) {
			channel.nextProcessImage();
		}
		testCase.applyInputs(this.components);
		this.onAfterProcessImage();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE);
		this.onBeforeControllers();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS);
		this.onExecuteControllers();
		this.onAfterControllers();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS);
		this.onBeforeWrite();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE);
		this.onExecuteWrite();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE);
		this.onAfterWrite();
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE);
		testCase.validateOutputs(this.components);
		return this.self();
	}

	/**
	 * If the 'system-under-test' is a {@link EventHandler} call the
	 * {@link EventHandler#handleEvent(Event)} method.
	 * 
	 * @param topic the {@link Event} topic
	 * @throws Exception on error
	 * 
	 */
	protected void handleEvent(String topic) throws Exception {
		if (this.sut instanceof EventHandler) {
			Event event = new Event(topic, new HashMap<String, Object>());
			((EventHandler) this.sut).handleEvent(event);
		}
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_PROCESS_IMAGE event.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void onBeforeProcessImage() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_PROCESS_IMAGE event.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void onAfterProcessImage() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_CONTROLLERS event.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void onBeforeControllers() throws OpenemsNamedException {
	}

	/**
	 * This method is executed after TOPIC_CYCLE_BEFORE_CONTROLLERS and before
	 * TOPIC_CYCLE_AFTER_CONTROLLERS.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void onExecuteControllers() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_CONTROLLERS event.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void onAfterControllers() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_WRITE event.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void onBeforeWrite() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_EXECUTE_WRITE event.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void onExecuteWrite() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before
	 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_WRITE.
	 * 
	 * @throws OpenemsNamedException on error
	 */
	protected void onAfterWrite() {

	}

}
