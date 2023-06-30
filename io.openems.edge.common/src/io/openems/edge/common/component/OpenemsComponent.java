package io.openems.edge.common.component;

import java.io.IOException;
import java.util.Collection;
import java.util.Hashtable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import com.google.common.base.Objects;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.utils.ConfigUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannelDoc;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * This is the base interface for and should be implemented by every service
 * component in OpenEMS Edge.
 *
 * <p>
 * Every OpenEMS service has:
 * <ul>
 * <li>a unique ID (see {@link #id()})
 * <li>an enabled/disabled state (see {@link #isEnabled()})
 * <li>an OSGi service PID (see {@link #servicePid()}
 * <li>Channels (see {@link Channel}), identified by {@link ChannelId} or
 * String-ID and provided via {@link #channel(String)},
 * {@link #channel(io.openems.edge.common.channel.ChannelId)} and
 * {@link #channels()}
 * <li>a kind of 'toString' method which provides the most important info about
 * the component. (see {@link #debugLog()})
 * </ul>
 *
 * <p>
 * The recommended implementation of an OpenEMS component is via
 * {@link AbstractOpenemsComponent}.
 */
public interface OpenemsComponent {

	/**
	 * Returns a unique ID for this OpenEMS component.
	 *
	 * @return the unique ID
	 */
	public String id();

	/**
	 * Returns a human-readable name of this Component..
	 *
	 * @return the human-readable name
	 */
	public String alias();

	/**
	 * Returns whether this component is enabled.
	 *
	 * @return true if the component is enabled
	 */
	public boolean isEnabled();

	/**
	 * Returns the Service PID.
	 *
	 * @return the OSGi Service PID
	 */
	default String servicePid() {
		var context = this.getComponentContext();
		if (context != null) {
			var properties = context.getProperties();
			var servicePid = properties.get(Constants.SERVICE_PID);
			if (servicePid != null) {
				return servicePid.toString();
			}
		}
		return "";
	}

	/**
	 * Returns the Service Factory-PID.
	 *
	 * @return the OSGi Service Factory-PID
	 */
	default String serviceFactoryPid() {
		var context = this.getComponentContext();
		if (context != null) {
			var properties = context.getProperties();

			var servicePid = properties.get("service.factoryPid");
			if (servicePid != null) {
				return servicePid.toString();
			}

			// Singleton?
			servicePid = properties.get("component.name");
			if (servicePid != null) {
				return servicePid.toString();
			}
		}
		return "";
	}

	/**
	 * Returns the ComponentContext.
	 *
	 * @return the OSGi ComponentContext
	 */
	public ComponentContext getComponentContext();

	/**
	 * Returns an undefined Channel defined by its ChannelId string representation.
	 *
	 * <p>
	 * Note: It is preferred to use the typed channel()-method, that's why it is
	 * marked as @Deprecated.
	 *
	 * @param channelName the Channel-ID as a string
	 * @return the Channel or null
	 */
	@Deprecated()
	public Channel<?> _channel(String channelName);

	/**
	 * Returns a Channel defined by its ChannelId string representation.
	 *
	 * @param channelName the Channel-ID as a string
	 * @param <T>         the expected typed Channel
	 * @return the Channel or throw Exception
	 * @throws IllegalArgumentException on error
	 */
	@SuppressWarnings("unchecked")
	default <T extends Channel<?>> T channel(String channelName) throws IllegalArgumentException {
		Channel<?> channel = this._channel(channelName);
		// check for null
		if (channel == null) {
			if (this.id() == null) {
				throw new IllegalArgumentException("Channel [" + channelName + "] is not defined for implementation ["
						+ this.getClass().getCanonicalName() + "].");
			}
			throw new IllegalArgumentException("Channel [" + channelName + "] is not defined for ID [" + this.id()
					+ "]. Implementation [" + this.getClass().getCanonicalName() + "]");
		}
		// check correct type
		T typedChannel;
		try {
			typedChannel = (T) channel;
		} catch (ClassCastException e) {
			throw new IllegalArgumentException(
					"Channel [" + this.id() + "/" + channelName + "] is not of expected type.");
		}
		return typedChannel;
	}

	/**
	 * Returns a Channel defined by its ChannelId.
	 *
	 * @param <T>       the Type of the Channel. See {@link Doc#getType()}
	 * @param channelId the Channel-ID
	 * @return the Channel
	 * @throws IllegalArgumentException on error
	 */
	default <T extends Channel<?>> T channel(io.openems.edge.common.channel.ChannelId channelId)
			throws IllegalArgumentException {
		return this.<T>channel(channelId.id());
	}

	/**
	 * Returns all Channels.
	 *
	 * @return a Collection of Channels
	 */
	public Collection<Channel<?>> channels();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// Running State of the component. Keep values in sync with 'Level' enum!
		STATE(new StateCollectorChannelDoc() //
				// Set Text to "0:Ok, 1:Info, 2:Warning, 3:Fault"
				.text(Stream.of(Level.values()) //
						.map(option -> (option.getValue() + ":" + option.getName())) //
						.collect(Collectors.joining(", "))) //
				.persistencePriority(PersistencePriority.VERY_HIGH));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(OpenemsComponent.class, accessMode, 80) //
				.channel(0, ChannelId.STATE, ModbusType.ENUM16) //
				.build();
	}

	/**
	 * Gets the Component State-Channel.
	 *
	 * @return the StateCollectorChannel
	 */
	public default StateCollectorChannel getStateChannel() {
		try {
			return this._getChannelAs(ChannelId.STATE, StateCollectorChannel.class);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(//
					"Class [" + this.getClass().getCanonicalName() + "] does not have a Channel 'State'. " //
							+ "\nMake sure to pass the 'OpenemsComponent.ChannelId.values()' as first parameter " //
							+ "in the AbstractOpenemsComponent constructor.",
					e);
		}
	}

	/**
	 * Gets the Component State {@link Level}.
	 *
	 * @return the StateCollectorChannel
	 */
	public default Level getState() {
		return this.getStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel as the given Type.
	 *
	 * @param <T>       the expected Channel type
	 * @param channelId the Channel-ID
	 * @param type      the expected Type
	 * @return the Channel
	 */
	@SuppressWarnings("unchecked")
	public default <T extends Channel<?>> T _getChannelAs(ChannelId channelId, Class<T> type) {
		Channel<?> channel = this.channel(channelId);
		if (channel == null) {
			throw new IllegalArgumentException("Channel [" + channelId + "] is not defined.");
		}
		if (!type.isAssignableFrom(channel.getClass())) {
			throw new IllegalArgumentException(
					"Channel [" + channelId + "] must be of type '" + type.getSimpleName() + "'.");
		}
		return (T) channel;
	}

	/**
	 * Gets some output that is suitable for a continuous Debug log. Returns 'null'
	 * by default which causes no output.
	 *
	 * @return the debug log output
	 */
	public default String debugLog() {
		return null;
	}

	/**
	 * Does this OpenEMS Component report any Faults?
	 *
	 * <p>
	 * Evaluates all {@link StateChannel}s and returns true if any Channel with
	 * {@link Level#FAULT} is set.
	 *
	 * @return true if there is a Fault.
	 */
	public default boolean hasFaults() {
		var level = this.getState();
		return level.isAtLeast(Level.FAULT);
	}

	/**
	 * Sets a target filter for a Declarative Service @Reference member.
	 *
	 * <p>
	 * Usage:
	 *
	 * <pre>
	 * updateReferenceFilter(config.service_pid(), "Controllers", controllersIds);
	 * </pre>
	 *
	 * <p>
	 * Generates a 'target' filter on the 'Controllers' member so, that the the
	 * expected service to be injected needs to fulfill:
	 * <ul>
	 * <li>the service must be enabled
	 * <li>the service must not have the same PID as the calling component
	 * <li>the service "id" must be one of the provided "controllersIds"
	 * </ul>
	 *
	 *
	 * @param cm     a ConfigurationAdmin instance. Get one using
	 *
	 *               <pre>
	 *               &#64;Reference
	 *               ConfigurationAdmin cm;
	 *               </pre>
	 *
	 * @param pid    PID of the calling component (use 'config.service_pid()' or
	 *               '(String)prop.get(Constants.SERVICE_PID)'; if null, PID filter
	 *               is not added to the resulting target filter
	 * @param member Name of the Method or Field with the Reference annotation
	 * @param ids    Component IDs to be filtered for; for empty list, no ids are
	 *               added to the target filter
	 *
	 * @return true if the filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	public static boolean updateReferenceFilter(ConfigurationAdmin cm, String pid, String member, String... ids) {
		final var filter = ConfigUtils.generateReferenceTargetFilter(pid, ids);
		return updateReferenceFilterRaw(cm, pid, member, filter);
	}

	/**
	 * Sets a target filter for a Declarative Service @Reference member.
	 *
	 * <p>
	 * Use this method only if you know what you are doing. Usually you will want to
	 * use the
	 * {@link #updateReferenceFilter(ConfigurationAdmin, String, String, String...)}
	 * method instead.
	 *
	 * <p>
	 * Usage:
	 *
	 * <pre>
	 * updateReferenceFilterRaw(config.service_pid(), "Controllers", "(enabled=true)");
	 * </pre>
	 *
	 * @param cm     a ConfigurationAdmin instance. Get one using
	 *
	 *               <pre>
	 *               &#64;Reference
	 *               ConfigurationAdmin cm;
	 *               </pre>
	 *
	 * @param pid    PID of the calling component (use 'config.service_pid()' or
	 *               '(String)prop.get(Constants.SERVICE_PID)'; if null, PID filter
	 *               is not added to the resulting target filter
	 * @param member Name of the Method or Field with the Reference annotation, e.g.
	 *
	 * @param filter The filter attribute
	 *
	 * @return true if the filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	public static boolean updateReferenceFilterRaw(ConfigurationAdmin cm, String pid, String member, String filter) {
		if (cm == null) {
			throw new IllegalArgumentException("ConfigurationAdmin is null for updateReferenceFilterRaw" //
					+ "(pid=\"" + pid + "\",member=\"" + member + "\",filter=\"" + filter + "\")");
		}

		final var targetProperty = member + ".target";
		/*
		 * read existing target filter
		 */
		Configuration c;
		try {
			c = cm.getConfiguration(pid, "?");
			var properties = c.getProperties();
			var existingFilter = (String) properties.get(targetProperty);
			/*
			 * update target filter if required
			 */
			if (!filter.equals(existingFilter)) {
				properties.put(targetProperty, filter);
				c.update(properties);
				return true;
			}
		} catch (IOException | SecurityException e) {
			System.err.println("updateReferenceFilter ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Validates and possibly fixes the Component-ID of a Singleton.
	 *
	 * <p>
	 * Singleton Components are allowed to live only exactly once in an OpenEMS
	 * instance. These Components are marked with an Annotation:
	 *
	 * <pre>
	 * &#64;Designate(factory = false)
	 * </pre>
	 *
	 * <p>
	 * By design it is required for these Singleton Components to have a predefined
	 * Component-ID, like '_cycle', '_sum', etc. This method makes sure the
	 * Component-ID matches this predefined ID - and if not automatically adjusts
	 * it.
	 *
	 * <p>
	 * Sidenote: ideally it would be possible to use the Component Annotation
	 *
	 * <pre>
	 * &#64;Component(property = { "id=_cycle" })
	 * </pre>
	 *
	 * <p>
	 * for this purpose. Unfortunately this is not sufficient to have the 'id'
	 * property listed in EdgeConfig, ConfigurationAdmin, etc. This is why this
	 * workaround is required.
	 *
	 * <p>
	 * Usage:
	 *
	 * <pre>
	 * if (OpenemsComponent.validateSingletonComponentId(this.cm, this.serviceFactoryPid(), SINGLETON_COMPONENT_ID)) {
	 * 	return;
	 * }
	 * </pre>
	 *
	 * @param cm         a ConfigurationAdmin instance. Get one using
	 *
	 *                   <pre>
	 *                   &#64;Reference
	 *                   ConfigurationAdmin cm;
	 *                   </pre>
	 *
	 * @param pid        PID of the calling component (use 'config.service_pid()' or
	 *                   '(String)prop.get(Constants.SERVICE_PID)'; if null,
	 *                   Component-ID can not be updated.
	 * @param expectedId The expected predefined Component-ID
	 *
	 * @return true if the ID was updated. You may use it to abort the activate()
	 *         method.
	 */
	public static boolean validateSingleton(ConfigurationAdmin cm, String pid, String expectedId) {
		Configuration c;
		try {
			c = cm.getConfiguration(pid, "?");
			var properties = c.getProperties();

			final String actualId;
			final String actualAlias;
			if (properties == null) {
				// trigger creation of new configuration
				properties = new Hashtable<>();
				actualId = null;
				actualAlias = null;
			} else {
				actualId = (String) properties.get("id");
				actualAlias = (String) properties.get("alias");
			}
			// Fix Component-ID if required
			if (!Objects.equal(expectedId, actualId) || !Objects.equal(pid, actualAlias)) {
				properties.put("id", expectedId);
				properties.put("alias", pid);
				c.update(properties);
				return true;
			}
		} catch (IOException | SecurityException e) {
			System.err.println(
					"validateSingletonComponentId ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Update a configuration property.
	 *
	 * <p>
	 * Usage:
	 *
	 * <pre>
	 * updateConfigurationProperty(cm, servicePid, "propertyName", "propertyValue");
	 * </pre>
	 *
	 * <p>
	 *
	 * @param cm       a ConfigurationAdmin instance. Get one using
	 *
	 *                 <pre>
	 *                 &#64;Reference
	 *                 ConfigurationAdmin cm;
	 *                 </pre>
	 *
	 * @param pid      PID of the calling component (use 'config.service_pid()' or
	 *                 '(String)prop.get(Constants.SERVICE_PID)'
	 * @param property Name of the configuration property
	 * @param value    New configuration value
	 */
	public static void updateConfigurationProperty(ConfigurationAdmin cm, String pid, String property, Object value) {
		Configuration c;
		try {
			c = cm.getConfiguration(pid, "?");
			var properties = c.getProperties();
			properties.put(property, value);
			c.update(properties);
		} catch (IOException | SecurityException e) {
			System.out.println("ERROR: " + e.getMessage());
		}
	}

	/**
	 * Log a debug message including the Component ID.
	 *
	 * @param component the {@link OpenemsComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logDebug(OpenemsComponent component, Logger log, String message) {
		// TODO use log.debug(String, Object...) to improve speed
		var id = getComponentIdentifier(component);
		if (id != null) {
			log.debug("[" + id + "] " + message);
		} else {
			log.debug(message);
		}
	}

	/**
	 * Log a info message including the Component ID.
	 *
	 * @param component the {@link OpenemsComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logInfo(OpenemsComponent component, Logger log, String message) {
		var id = getComponentIdentifier(component);
		if (id != null) {
			log.info("[" + id + "] " + message);
		} else {
			log.info(message);
		}
	}

	/**
	 * Log a warn message including the Component ID.
	 *
	 * @param component the {@link OpenemsComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logWarn(OpenemsComponent component, Logger log, String message) {
		var id = getComponentIdentifier(component);
		if (id != null) {
			log.warn("[" + id + "] " + message);
		} else {
			log.warn(message);
		}
	}

	/**
	 * Log a error message including the Component ID.
	 *
	 * @param component the {@link OpenemsComponent}
	 * @param log       the {@link Logger} instance
	 * @param message   the message
	 */
	public static void logError(OpenemsComponent component, Logger log, String message) {
		var id = getComponentIdentifier(component);
		if (id != null) {
			log.error("[" + id + "] " + message);
		} else {
			log.error(message);
		}
	}

	private static String getComponentIdentifier(OpenemsComponent component) {
		if (component == null) {
			return null;
		}
		var id = component.id();
		if (id != null && !id.isBlank()) {
			return id;
		}
		return component.getClass().getSimpleName();
	}

}
