package io.openems.edge.common.component;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
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
		ComponentContext context = this.getComponentContext();
		if (context != null) {
			Dictionary<String, Object> properties = context.getProperties();
			Object servicePid = properties.get(Constants.SERVICE_PID);
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
		ComponentContext context = this.getComponentContext();
		if (context != null) {
			Dictionary<String, Object> properties = context.getProperties();

			Object servicePid = properties.get("service.factoryPid");
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
	 * @throws IllegalArgumentException on error
	 * @return the Channel or throw Exception
	 */
	@SuppressWarnings("unchecked")
	default <T extends Channel<?>> T channel(String channelName) throws IllegalArgumentException {
		Channel<?> channel = this._channel(channelName);
		// check for null
		if (channel == null) {
			throw new IllegalArgumentException(
					"Channel [" + channelName + "] is not defined for ID [" + this.id() + "].");
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
		T channel = this.<T>channel(channelId.id());
		return channel;
	}

	/**
	 * Returns all Channels.
	 * 
	 * @return a Collection of Channels
	 */
	public Collection<Channel<?>> channels();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// Running State of the component. Keep values in sync with 'Level' enum!
		STATE(new StateCollectorChannelDoc());

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
	 * Used for Modbus/TCP Api Controller. Provides a modbus table for the Channels
	 * of this Component.
	 * 
	 * @param accessMode the {@link AccessMode} of the Controller
	 * @return a {@link ModbusSlaveNatureTable}
	 */
	public static ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(OpenemsComponent.class, accessMode, 80) //
				.channel(0, ChannelId.STATE, ModbusType.UINT16) //
				.build();
	}

	/**
	 * Gets the Component State-Channel.
	 * 
	 * @return the StateCollectorChannel
	 */
	public default StateCollectorChannel getStateChannel() {
		return this._getChannelAs(ChannelId.STATE, StateCollectorChannel.class);
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
		Level level = this.getState();
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
	 * @param member Name of the Method or Field with the Reference annotation, e.g.
	 * 
	 * @param ids    Component IDs to be filtered for; for empty list, no ids are
	 *               added to the target filter
	 * 
	 * @return true if the filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	public static boolean updateReferenceFilter(ConfigurationAdmin cm, String pid, String member, String... ids) {
		final String targetProperty = member + ".target";
		final String requiredTarget = ConfigUtils.generateReferenceTargetFilter(pid, ids);
		/*
		 * read existing target filter
		 */
		Configuration c;
		try {
			c = cm.getConfiguration(pid, "?");
			Dictionary<String, Object> properties = c.getProperties();
			String existingTarget = (String) properties.get(targetProperty);
			/*
			 * update target filter if required
			 */
			if (!requiredTarget.equals(existingTarget)) {
				properties.put(targetProperty, requiredTarget);
				c.update(properties);
				return true;
			}
		} catch (IOException | SecurityException e) {
			System.out.println("ERROR: " + e.getMessage());
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
			Dictionary<String, Object> properties = c.getProperties();
			properties.put(property, value);
			c.update(properties);
		} catch (IOException | SecurityException e) {
			System.out.println("ERROR: " + e.getMessage());
		}
	}
}
