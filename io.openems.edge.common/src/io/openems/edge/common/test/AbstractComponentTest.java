package io.openems.edge.common.test;

import static io.openems.common.utils.ReflectionUtils.invokeMethodViaReflection;
import static io.openems.common.utils.ReflectionUtils.invokeMethodWithoutArgumentsViaReflection;
import static io.openems.common.utils.ReflectionUtils.setAttributeViaReflection;

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
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Debounce;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingRunnable;
import io.openems.common.test.AbstractComponentConfig;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.common.utils.ReflectionUtils.ReflectionException;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.test.AbstractComponentTest.ChannelValue.ChannelAddressValue;
import io.openems.edge.common.test.AbstractComponentTest.ChannelValue.ChannelIdValue;
import io.openems.edge.common.test.AbstractComponentTest.ChannelValue.ChannelNameValue;
import io.openems.edge.common.test.AbstractComponentTest.ChannelValue.ComponentChannelIdValue;
import io.openems.edge.common.type.TypeUtils;

/**
 * Provides a test framework for OpenEMS Components.
 */
public abstract class AbstractComponentTest<SELF extends AbstractComponentTest<SELF, SUT>, SUT extends OpenemsComponent> {

	public sealed interface ChannelValue {

		/**
		 * Gets the value.
		 * 
		 * @return the value
		 */
		public Object value();

		/**
		 * Is the value enforced?.
		 * 
		 * @return true for force
		 */
		public boolean force();

		public record ChannelAddressValue(ChannelAddress address, Object value, boolean force) implements ChannelValue {
			@Override
			public String toString() {
				return this.address.toString() + ":" + this.value;
			}
		}

		public record ChannelIdValue(ChannelId channelId, Object value, boolean force) implements ChannelValue {
			@Override
			public String toString() {
				return this.channelId.id() + ":" + this.value;
			}
		}

		public record ChannelNameValue(String channelName, Object value, boolean force) implements ChannelValue {
			@Override
			public String toString() {
				return this.channelName + ":" + this.value;
			}
		}

		public record ComponentChannelIdValue(String componentId, ChannelId channelId, Object value, boolean force)
				implements ChannelValue {
			@Override
			public String toString() {
				return this.componentId + "/" + this.channelId.id() + ":" + this.value;
			}
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
		 * Adds an input value for a {@link ChannelAddress}.
		 *
		 * @param address the {@link ChannelAddress}
		 * @param value   the value {@link Object}
		 * @return myself
		 */
		public TestCase input(ChannelAddress address, Object value) {
			this.inputs.add(new ChannelAddressValue(address, value, false));
			return this;
		}

		/**
		 * Adds an input value for a ChannelId of the given Component.
		 *
		 * @param componentId the Component-ID
		 * @param channelId   the Channel-ID in CamelCase
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase input(String componentId, String channelId, Object value) {
			return this.input(new ChannelAddress(componentId, channelId), value);
		}

		/**
		 * Adds an input value for a {@link ChannelId} of the given Component.
		 *
		 * @param componentId the Component-ID
		 * @param channelId   the {@link ChannelId}
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase input(String componentId, ChannelId channelId, Object value) {
			this.inputs.add(new ComponentChannelIdValue(componentId, channelId, value, false));
			return this;
		}

		/**
		 * Adds an input value for a {@link ChannelId} of the system-under-test.
		 *
		 * @param channelId the {@link ChannelId}
		 * @param value     the value {@link Object}
		 * @return myself
		 */
		public TestCase input(ChannelId channelId, Object value) {
			if (channelId instanceof Sum.ChannelId) {
				return this.input("_sum", channelId, value);
			}
			this.inputs.add(new ChannelIdValue(channelId, value, false));
			return this;
		}

		/**
		 * Adds an input value for a ChannelId of the system-under-test.
		 *
		 * @param channelName the Channel
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase input(String channelName, Object value) {
			this.inputs.add(new ChannelNameValue(channelName, value, false));
			return this;
		}

		/**
		 * Enforces an input value for a {@link ChannelAddress}.
		 * 
		 * <p>
		 * Use this method if you want to be sure, that the Channel actually applies the
		 * value, e.g. to override a {@link Debounce} setting.
		 *
		 * @param address the {@link ChannelAddress}
		 * @param value   the value {@link Object}
		 * @return myself
		 */
		public TestCase inputForce(ChannelAddress address, Object value) {
			this.inputs.add(new ChannelAddressValue(address, value, true));
			return this;
		}

		/**
		 * Enforces an input value for a {@link ChannelAddress}.
		 * 
		 * <p>
		 * Use this method if you want to be sure, that the Channel actually applies the
		 * value, e.g. to override a {@link Debounce} setting.
		 *
		 * @param componentId the Component-ID
		 * @param channelId   the Channel-ID
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase inputForce(String componentId, String channelId, Object value) {
			return this.inputForce(new ChannelAddress(componentId, channelId), value);
		}

		/**
		 * Enforces an input value for a {@link ChannelId} of the given Component.
		 *
		 * @param componentId the Component-ID
		 * @param channelId   the {@link ChannelId}
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase inputForce(String componentId, ChannelId channelId, Object value) {
			this.inputs.add(new ComponentChannelIdValue(componentId, channelId, value, true));
			return this;
		}

		/**
		 * Enforces an input value for a {@link ChannelId} of the system-under-test.
		 *
		 * <p>
		 * Use this method if you want to be sure, that the Channel actually applies the
		 * value, e.g. to override a {@link Debounce} setting.
		 *
		 * @param channelId the {@link ChannelId}
		 * @param value     the value {@link Object}
		 * @return myself
		 */
		public TestCase inputForce(ChannelId channelId, Object value) {
			this.inputs.add(new ChannelIdValue(channelId, value, true));
			return this;
		}

		/**
		 * Enforces an input value for a Channel of the system-under-test.
		 *
		 * <p>
		 * Use this method if you want to be sure, that the Channel actually applies the
		 * value, e.g. to override a {@link Debounce} setting.
		 *
		 * @param channelName the Channel
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase inputForce(String channelName, Object value) {
			this.inputs.add(new ChannelNameValue(channelName, value, true));
			return this;
		}

		/**
		 * Adds an expected output value for a {@link ChannelAddress}.
		 *
		 * @param address the {@link ChannelAddress}
		 * @param value   the value {@link Object}
		 * @return myself
		 */
		public TestCase output(ChannelAddress address, Object value) {
			this.outputs.add(new ChannelAddressValue(address, value, false));
			return this;
		}

		/**
		 * Adds an expected output value for a {@link ChannelAddress}.
		 *
		 * @param componentId the Component-ID
		 * @param channelId   the Channel-ID in CamelCase
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase output(String componentId, String channelId, Object value) {
			return this.output(new ChannelAddress(componentId, channelId), value);
		}

		/**
		 * Adds an expected output value for a {@link ChannelId} of the given Component.
		 *
		 * @param componentId the Component-ID
		 * @param channelId   the {@link ChannelId}
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase output(String componentId, ChannelId channelId, Object value) {
			this.outputs.add(new ComponentChannelIdValue(componentId, channelId, value, true));
			return this;
		}

		/**
		 * Adds an expected output value for a {@link ChannelId} of the
		 * system-under-test.
		 *
		 * @param channelId the {@link ChannelId}
		 * @param value     the value {@link Object}
		 * @return myself
		 */
		public TestCase output(ChannelId channelId, Object value) {
			this.outputs.add(new ChannelIdValue(channelId, value, false));
			return this;
		}

		/**
		 * Adds an expected output value for a Channel of the system-under-test.
		 *
		 * @param channelName the Channel
		 * @param value       the value {@link Object}
		 * @return myself
		 */
		public TestCase output(String channelName, Object value) {
			this.outputs.add(new ChannelNameValue(channelName, value, false));
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
		 * Helper method to scope variables or logic specifically for this
		 * {@link TestCase}.
		 * 
		 * @param consumer the {@link Consumer} which gets immediately executed with the
		 *                 current {@link TestCase}
		 * @return myself
		 */
		public TestCase also(Consumer<TestCase> consumer) {
			consumer.accept(this);
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
		 * @param act the {@link AbstractComponentTest}
		 * @throws OpenemsNamedException    on error
		 * @throws IllegalArgumentException on error
		 */
		protected void applyInputs(AbstractComponentTest<?, ?> act)
				throws IllegalArgumentException, OpenemsNamedException {
			for (var input : this.inputs) {
				final Channel<?> channel = this.getChannel(act, input);

				// (Force) set the Read-Value
				do {
					channel.setNextValue(input.value());
					channel.nextProcessImage();
				} while (input.force() && !Objects.equals(channel.value().get(), input.value()));

				// Set the Write-Value
				if (channel instanceof WriteChannel<?> c) {
					c.setNextWriteValueFromObject(input.value());
				}
			}
		}

		/**
		 * Validates the output values.
		 *
		 * @param act the {@link AbstractComponentTest}
		 * @throws Exception on validation failure
		 */
		@SuppressWarnings("unchecked")
		protected void validateOutputs(AbstractComponentTest<?, ?> act) throws Exception {
			for (var output : this.outputs) {
				final Channel<?> channel = this.getChannel(act, output);

				Object got;
				final String readWriteInfo;
				if (channel instanceof WriteChannel wc) {
					got = wc.getNextWriteValueAndReset().orElse(null);
					readWriteInfo = "WriteValue";
				} else {
					var value = channel.getNextValue();
					got = value.orElse(null);
					readWriteInfo = "ReadValue";
				}
				// Try to parse an Enum
				if (channel.channelDoc() instanceof EnumDoc) {
					var enumDoc = (EnumDoc) channel.channelDoc();
					var intGot = TypeUtils.<Integer>getAsType(OpenemsType.INTEGER, got);
					got = enumDoc.getOption(intGot);
				}
				if (!Objects.equals(output.value(), got)) {
					throw new Exception("On TestCase [" + this.description + "]: " //
							+ "expected " + readWriteInfo + " [" + output.value() + "] " //
							+ "got [" + got + "] " //
							+ "for Channel [" + output.toString() + "] " //
							+ "on Inputs [" + this.inputs + "]");
				}
			}
		}

		private OpenemsComponent getComponent(Map<String, OpenemsComponent> components, String componentId) {
			var component = components.get(componentId);
			if (component != null) {
				return component;
			}
			throw new IllegalArgumentException("On TestCase [" + this.description + "]: " //
					+ "the component [" + componentId + "] " //
					+ "was not added to the OpenEMS Component test framework!");
		}

		private Channel<?> getChannel(AbstractComponentTest<?, ?> act, ChannelValue cv)
				throws IllegalArgumentException {
			if (cv instanceof ChannelAddressValue cav) {
				var component = this.getComponent(act.components, cav.address.getComponentId());
				return component.channel(cav.address.getChannelId());
			}

			if (cv instanceof ChannelIdValue civ) {
				return act.sut.channel(civ.channelId);
			}

			if (cv instanceof ChannelNameValue civ2) {
				return act.sut.channel(civ2.channelName);
			}

			if (cv instanceof ComponentChannelIdValue cciv) {
				var component = this.getComponent(act.components, cciv.componentId());
				return component.channel(cciv.channelId());
			}

			throw new IllegalArgumentException("Unhandled subtype of ChannelValue");
		}
	}

	/**
	 * The {@link OpenemsComponent} to be tested. "sut" is for system-under-test.
	 */
	public final SUT sut;

	/**
	 * References added by {@link #addReference()}.
	 */
	private final Set<Object> references = new HashSet<>();

	/**
	 * Components referenced by the tested Component.
	 */
	private final Map<String, OpenemsComponent> components = new HashMap<>();

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
						final Channel<?> channel;
						try {
							channel = sut.channel(channelId);
						} catch (IllegalArgumentException e) {
							throw new OpenemsException(
									"OpenEMS Nature [" + iface.getSimpleName() + "] was not properly implemented. " //
											+ "Please make sure to initialize the Channel-IDs in the constructor.",
									e);
						}

						// Test if all values of OptionsEnum types are unique
						if (channel.channelDoc() instanceof EnumDoc e) {
							if (e.getOptions().length > Stream.of(e.getOptions()) //
									.mapToInt(OptionsEnum::getValue) //
									.distinct() //
									.count()) {
								throw new OpenemsException(
										"OptionsEnum [" + e.getOptions()[0].getClass().getSimpleName() + "] in " //
												+ "[" + sut.getClass().getSimpleName() + "] has non-unique values!");
							}
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
			setAttributeViaReflection(this.sut, memberName, object);
			return true;
		} catch (ReflectionException e) {
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

	/**
	 * Calls the 'deactivate()' method of the 'system-under-test'.
	 *
	 * @return itself, to use as a builder
	 * @throws Exception on error
	 */
	public SELF deactivate() throws Exception {
		this.callDeactivate();
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
			invokeMethodViaReflection(this.sut, method, args);
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
		invokeMethodWithoutArgumentsViaReflection(this.sut, "deactivate");
	}

	private boolean invokeSingleArgMethod(Class<?> clazz, String methodName, Object arg) throws ReflectionException {
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

			invokeMethodViaReflection(this.sut, method, arg);
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
		testCase.applyInputs(this);
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
		testCase.validateOutputs(this);
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
		var event = new Event(topic, new HashMap<String, Object>());
		for (var component : this.components.values()) {
			if (component instanceof EventHandler eh) {
				eh.handleEvent(event);
			}
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
