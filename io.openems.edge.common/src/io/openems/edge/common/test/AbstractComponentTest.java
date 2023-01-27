package io.openems.edge.common.test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
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

		/**
		 * Gets the {@link ChannelAddress}.
		 *
		 * @return the {@link ChannelAddress}
		 */
		public ChannelAddress getAddress() {
			return this.address;
		}

		/**
		 * Gets the value {@link Object}.
		 *
		 * @return the {@link Object}
		 */
		public Object getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return this.address.toString() + ":" + this.value;
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
		private final List<ThrowingRunnable<Exception>> onBeforeProcessImageCallbacks = new ArrayList<>();
		private final List<ThrowingRunnable<Exception>> onAfterProcessImageCallbacks = new ArrayList<>();
		private final List<ThrowingRunnable<Exception>> onBeforeControllersCallbacks = new ArrayList<>();
		private final List<ThrowingRunnable<Exception>> onExecuteControllersCallbacks = new ArrayList<>();
		private final List<ThrowingRunnable<Exception>> onAfterControllersCallbacks = new ArrayList<>();
		private final List<ThrowingRunnable<Exception>> onBeforeWriteCallbacks = new ArrayList<>();
		private final List<ThrowingRunnable<Exception>> onExecuteWriteCallbacks = new ArrayList<>();
		private final List<ThrowingRunnable<Exception>> onAfterWriteCallbacks = new ArrayList<>();

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

		/**
		 * Adds an input value for a Channel.
		 *
		 * @param address the {@link ChannelAddress}
		 * @param value   the value {@link Object}
		 * @return myself
		 */
		public TestCase input(ChannelAddress address, Object value) {
			this.inputs.add(new ChannelValue(address, value));
			return this;
		}

		/**
		 * Adds an expected output value for a Channel.
		 *
		 * @param address the {@link ChannelAddress}
		 * @param value   the value {@link Object}
		 * @return myself
		 */
		public TestCase output(ChannelAddress address, Object value) {
			this.outputs.add(new ChannelValue(address, value));
			return this;
		}

		/**
		 * Adds a simulated timeleap, i.e. simulates that a given amount of time passed.
		 *
		 * @param clock       the active {@link TimeLeapClock}, i.e. provided to the
		 *                    system-under-test by a {@link ClockProvider} like
		 *                    {@link ComponentManager}.
		 * @param amountToAdd the amount that should be simulated
		 * @param unit        the {@link TemporalUnit} of the amount, e.g. using the
		 *                    {@link ChronoUnit} enum
		 * @return myself
		 */
		public TestCase timeleap(TimeLeapClock clock, long amountToAdd, TemporalUnit unit) {
			this.timeleap = new TimeLeap(clock, amountToAdd, unit);
			return this;
		}

		/**
		 * Adds a Callback that is called on
		 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_PROCESS_IMAGE} event.
		 *
		 * @param callback the callback
		 * @return myself
		 */
		public TestCase onBeforeProcessImage(ThrowingRunnable<Exception> callback) {
			this.onBeforeProcessImageCallbacks.add(callback);
			return this;
		}

		/**
		 * Adds a Callback that is called on
		 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_PROCESS_IMAGE} event.
		 *
		 * @param callback the callback
		 * @return myself
		 */
		public TestCase onAfterProcessImage(ThrowingRunnable<Exception> callback) {
			this.onAfterProcessImageCallbacks.add(callback);
			return this;
		}

		/**
		 * Adds a Callback that is called on
		 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_CONTROLLERS} event.
		 *
		 * @param callback the callback
		 * @return myself
		 */
		public TestCase onBeforeControllersCallbacks(ThrowingRunnable<Exception> callback) {
			this.onBeforeControllersCallbacks.add(callback);
			return this;
		}

		/**
		 * Adds a Callback that is called after
		 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_CONTROLLERS} and before
		 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_CONTROLLERS}. events.
		 *
		 * @param callback the callback
		 * @return myself
		 */
		public TestCase onExecuteControllersCallbacks(ThrowingRunnable<Exception> callback) {
			this.onExecuteControllersCallbacks.add(callback);
			return this;
		}

		/**
		 * Adds a Callback that is called on
		 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_CONTROLLERS} event.
		 *
		 * @param callback the callback
		 * @return myself
		 */
		public TestCase onAfterControllersCallbacks(ThrowingRunnable<Exception> callback) {
			this.onAfterControllersCallbacks.add(callback);
			return this;
		}

		/**
		 * Adds a Callback that is called on
		 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_WRITE} event.
		 *
		 * @param callback the callback
		 * @return myself
		 */
		public TestCase onBeforeWriteCallbacks(ThrowingRunnable<Exception> callback) {
			this.onBeforeWriteCallbacks.add(callback);
			return this;
		}

		/**
		 * Adds a Callback that is called on
		 * {@link EdgeEventConstants#TOPIC_CYCLE_EXECUTE_WRITE} event.
		 *
		 * @param callback the callback
		 * @return myself
		 */
		public TestCase onExecuteWriteCallbacks(ThrowingRunnable<Exception> callback) {
			this.onExecuteWriteCallbacks.add(callback);
			return this;
		}

		/**
		 * Adds a Callback that is called on
		 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_WRITE} event.
		 *
		 * @param callback the callback
		 * @return myself
		 */
		public TestCase onAfterWriteCallbacks(ThrowingRunnable<Exception> callback) {
			this.onAfterWriteCallbacks.add(callback);
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
				var component = components.get(input.address.getComponentId());
				if (component == null) {
					throw new IllegalArgumentException("On TestCase [" + this.description + "]: " //
							+ "the component [" + input.address.getComponentId() + "] " //
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
		 * @throws Exception on validation failure
		 */
		protected void validateOutputs(Map<String, OpenemsComponent> components) throws Exception {
			for (ChannelValue output : this.outputs) {
				var expected = output.value;
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
					var enumDoc = (EnumDoc) channel.channelDoc();
					var intGot = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, got);
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

	/**
	 * Constructs the Component-Test and validates the implemented Channel-IDs.
	 *
	 * @param sut the 'system-under-test'
	 * @throws OpenemsException on error
	 */
	public AbstractComponentTest(SUT sut) throws OpenemsException {
		this.sut = sut;

		// Of all implemented interfaces...
		for (Class<?> iface : sut.getClass().getInterfaces()) {
			// get the ones which contain a subclass...
			for (Class<?> subclass : iface.getDeclaredClasses()) {
				// that implements 'ChannelId'.
				if (Arrays.asList(subclass.getInterfaces()).contains(io.openems.edge.common.channel.ChannelId.class)) {
					// Then read all the Channel-IDs...
					for (Object enumConstant : subclass.getEnumConstants()) {
						var channelId = (ChannelId) enumConstant;
						// and validate that they were initialized in the constructor.
						try {
							sut.channel(channelId);
						} catch (IllegalArgumentException e) {
							throw new OpenemsException(
									"OpenEMS Nature [" + iface.getSimpleName() + "] was not properly implemented. " //
											+ "Please make sure to initialize the Channel-IDs in the constructor.",
									e);
						}
					}
				}
			}
		}
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

	private boolean addReference(Class<?> clazz, String memberName, Object object)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		try {
			var field = clazz.getDeclaredField(memberName);
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
		return this.addReference(parent, memberName, object);
	}

	/**
	 * Adds an available {@link OpenemsComponent}.
	 *
	 * <p>
	 * If the provided Component is a {@link DummyComponentManager}.
	 *
	 * @param component the {@link OpenemsComponent}s
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
				var cm = (DummyConfigurationAdmin) object;
				cm.addConfig(config);
			}
		}

		var configChangeCount = this.getConfigChangeCount();
		this.callActivate(config);

		if (configChangeCount != this.getConfigChangeCount()) {
			// Config change detected
			this.callModified(config);
		}

		// Now SUT can be added to the list, as it does have an ID now
		this.addComponent(this.sut);
		return this.self();
	}

	private int getConfigChangeCount() throws IOException, InvalidSyntaxException {
		var result = 0;
		for (Object object : this.references) {
			if (object instanceof ConfigurationAdmin) {
				var cm = (ConfigurationAdmin) object;
				var configs = cm.listConfigurations(null);
				for (Configuration config : configs) {
					result += config.getChangeCount();
				}
			}
		}
		return result;
	}

	private void callActivate(AbstractComponentConfig config)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		this.callActivateOrModified("activate", config);
	}

	private boolean callActivateOrModified(String methodName, AbstractComponentConfig config)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		Class<?> clazz = this.sut.getClass();
		var methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (!method.getName().equals(methodName)) {
				continue;
			}
			var args = new Object[method.getParameterCount()];
			for (var i = 0; i < method.getParameterCount(); i++) {
				var parameter = method.getParameters()[i];
				Object arg;

				if (ComponentContext.class.isAssignableFrom(parameter.getType())) {
					// ComponentContext
					arg = DummyComponentContext.from(config);

				} else if (BundleContext.class.isAssignableFrom(parameter.getType())) {
					// BundleContext
					arg = null;

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
			return true;
		}
		return false;
	}

	private void callModified(AbstractComponentConfig config) throws Exception {
		var hasModified = this.callActivateOrModified("modified", config);
		if (hasModified) {
			return;

		} else {
			// Has no modified() method -> Deactivate + recursive activate
			this.callDeactivate();
			this.activate(config);
		}
	}

	private void callDeactivate() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException {
		Class<?> clazz = this.sut.getClass();
		var method = clazz.getDeclaredMethod("deactivate");
		method.setAccessible(true);
		method.invoke(this.sut);
	}

	private boolean invokeSingleArgMethod(Class<?> clazz, String methodName, Object arg)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		var methods = clazz.getDeclaredMethods();
		for (Method method : methods) {
			if (!method.getName().equals(methodName)) {
				continue;
			}
			if (method.getParameterCount() != 1) {
				continue;
			}

			var parameter = method.getParameters()[0];
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
		executeCallbacks(testCase.onBeforeProcessImageCallbacks);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE);
		for (Channel<?> channel : this.getSut().channels()) {
			channel.nextProcessImage();
		}
		testCase.applyInputs(this.components);
		this.onAfterProcessImage();
		executeCallbacks(testCase.onAfterProcessImageCallbacks);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE);
		this.onBeforeControllers();
		executeCallbacks(testCase.onBeforeControllersCallbacks);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS);
		this.onExecuteControllers();
		executeCallbacks(testCase.onExecuteControllersCallbacks);
		this.onAfterControllers();
		executeCallbacks(testCase.onAfterControllersCallbacks);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS);
		this.onBeforeWrite();
		executeCallbacks(testCase.onBeforeWriteCallbacks);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_BEFORE_WRITE);
		this.onExecuteWrite();
		executeCallbacks(testCase.onExecuteWriteCallbacks);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE);
		this.onAfterWrite();
		executeCallbacks(testCase.onAfterWriteCallbacks);
		this.handleEvent(EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE);
		testCase.validateOutputs(this.components);
		return this.self();
	}

	/**
	 * Run a Test-Case multiple times.
	 * 
	 * @param testCase     The TestCase
	 * @param executeCount The execution amount of the test case
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public SELF next(TestCase testCase, int executeCount) throws Exception {
		for (int i = 0; i < executeCount; i++) {
			this.next(testCase);
		}
		return this.self();
	}

	private static void executeCallbacks(List<ThrowingRunnable<Exception>> callbacks) throws Exception {
		for (ThrowingRunnable<Exception> callback : callbacks) {
			callback.run();
		}
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
			var event = new Event(topic, new HashMap<String, Object>());
			((EventHandler) this.sut).handleEvent(event);
		}
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_PROCESS_IMAGE} event.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void onBeforeProcessImage() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_PROCESS_IMAGE} event.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void onAfterProcessImage() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_CONTROLLERS} event.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void onBeforeControllers() throws OpenemsNamedException {
	}

	/**
	 * This method is executed after
	 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_CONTROLLERS} and before
	 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_CONTROLLERS}.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void onExecuteControllers() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_CONTROLLERS} event.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void onAfterControllers() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_BEFORE_WRITE} event.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void onBeforeWrite() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before the
	 * {@link EdgeEventConstants#TOPIC_CYCLE_EXECUTE_WRITE} event.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void onExecuteWrite() throws OpenemsNamedException {
	}

	/**
	 * This method is executed before
	 * {@link EdgeEventConstants#TOPIC_CYCLE_AFTER_WRITE}.
	 *
	 * @throws OpenemsNamedException on error
	 */
	protected void onAfterWrite() throws OpenemsNamedException {

	}

}
