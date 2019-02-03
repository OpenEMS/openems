package io.openems.edge.common.component;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;

/**
 * This is the base interface for and should be implemented by every service
 * component in OpenEMS Edge.
 * 
 * Every OpenEMS service has:
 * <ul>
 * <li>a unique ID (see {@link #id()})
 * <li>an enabled/disabled state (see {@link #isEnabled()})
 * <li>an OSGi service PID (see {@link #servicePid()}
 * <li>Channels (see {@link Channel}), identified by {@link ChannelId} or
 * String-ID and provided via {@link #channel(String)},
 * {@link #channel(io.openems.edge.common.channel.doc.ChannelId)} and
 * {@link #channels()}
 * <li>a kind of 'toString' method which provides the most important info about
 * the component. (see {@link #debugLog()})
 * </ul>
 * 
 * The recommended implementation of an OpenEMS component is via
 * {@link AbstractOpenemsComponent}.
 */
public interface OpenemsComponent {

	/**
	 * Returns a unique ID for this OpenEMS component
	 * 
	 * @return
	 */
	String id();

	/**
	 * Returns whether this component is enabled
	 * 
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Returns the Service PID.
	 * 
	 * @return
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
	 * Returns the ComponentContext
	 */
	ComponentContext getComponentContext();

	/**
	 * Returns an undefined Channel defined by its ChannelId string representation.
	 * 
	 * Note: It is preferred to use the typed channel()-method, that's why it is
	 * marked as @Deprecated.
	 * 
	 * @param channelName
	 * @return channel or null
	 */
	@Deprecated
	public Channel<?> _channel(String channelName);

	/**
	 * Returns a Channel defined by its ChannelId string representation.
	 * 
	 * @param channelName
	 * @throws IllegalArgumentException on error
	 * @return
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
	 * @param           <T> the Type of the Channel. See {@link Doc#getType()}
	 * @param channelId the Channel-ID
	 * @return the Channel
	 */
	default <T extends Channel<?>> T channel(io.openems.edge.common.channel.doc.ChannelId channelId) {
		T channel = this.<T>channel(channelId.id());
		return channel;
	}

	/**
	 * Returns all Channels
	 * 
	 * @return
	 */
	Collection<Channel<?>> channels();

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		// Running State of the component. Keep values in sync with 'Level' enum!
		STATE(new Doc().unit(Unit.NONE).options(Level.values()));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public static ModbusSlaveNatureTable getModbusSlaveNatureTable() {
		return ModbusSlaveNatureTable.of(OpenemsComponent.class, 80) //
				.channel(0, ChannelId.STATE, ModbusType.UINT16) //
				.build();
	}

	default StateCollectorChannel getState() {
		return this._getChannelAs(ChannelId.STATE, StateCollectorChannel.class);
	}

	@SuppressWarnings("unchecked")
	default <T extends Channel<?>> T _getChannelAs(ChannelId channelId, Class<T> type) {
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
	 * @return
	 */
	public default String debugLog() {
		return null;
	}

	/**
	 * Sets a target filter for a Declarative Service @Reference member.
	 * 
	 * Usage:
	 * 
	 * <pre>
	 * updateReferenceFilter(config.service_pid(), "Controllers", controllersIds);
	 * </pre>
	 * 
	 * Generates a 'target' filter on the 'Controllers' member so, that the target
	 * component 'id' is in 'controllerIds'.
	 * 
	 * @param cm     a ConfigurationAdmin instance. Get one using
	 * 
	 *               <pre>
	 *               &#64;Reference
	 *               ConfigurationAdmin cm;
	 *               </pre>
	 * 
	 * @param pid    PID of the calling component (use 'config.service_pid()' or
	 *               '(String)prop.get(Constants.SERVICE_PID)'
	 * @param member Name of the Method or Field with the Reference annotation, e.g.
	 *               'Controllers' for 'addControllers()' method
	 * @param ids    Component IDs to be filtered for
	 * 
	 * @return true if the filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	public static boolean updateReferenceFilter(ConfigurationAdmin cm, String pid, String member, String... ids) {
		final String targetProperty = member + ".target";
		/*
		 * generate required target filter
		 */
		// target component must be enabled
		StringBuilder targetBuilder = new StringBuilder("(&(enabled=true)");
		// target component must not be the same as the calling component
		targetBuilder.append("(!(service.pid=" + pid + "))");
		// add filter for given Component-IDs
		targetBuilder.append("(|");
		for (String id : ids) {
			targetBuilder.append("(id=" + id + ")");
		}
		targetBuilder.append("))");
		String requiredTarget = targetBuilder.toString();
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
	 * Usage:
	 * 
	 * <pre>
	 * updateConfigurationProperty(cm, servicePid, "propertyName", "propertyValue");
	 * </pre>
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
	public static void updateConfigurationProperty(ConfigurationAdmin cm, String pid, String property, int value) {
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
