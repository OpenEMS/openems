package io.openems.edge.common.component;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;

public interface OpenemsComponent {

	/**
	 * Returns a unique ID for this Thing (i.e. the OSGi service.pid)
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
	 * Returns the Service PID. E.g. 'Ess.Fenecon.Commercial40'
	 * 
	 * @return
	 */
	String servicePid();

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
	 * @param channelId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default <T extends Channel<?>> T channel(String channelName) {
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
	 * Returns a Channel defined by its ChannelId
	 * 
	 * @param channelId
	 * @return
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
		// Running State of the component
		STATE(new Doc().unit(Unit.NONE) //
				.option(0, "Ok") //
				.option(1, "Warning") //
				.option(2, "Fault"));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	default StateChannel getState() {
		return this._getChannelAs(ChannelId.STATE, StateChannel.class);
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
	 * Outputs all channels and their values to stdout. Useful for debugging.
	 */
	public default void listAllChannels() {
		this.channels().forEach(channel -> {
			System.out.println(String.format("%-20s : %10s", channel.channelId(), channel.format()));
		});
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
	 * @param cm
	 *            a ConfigurationAdmin instance. Get one using
	 * 
	 *            <pre>
	 *            &#64;Reference
	 *            ConfigurationAdmin cm;
	 *            </pre>
	 * 
	 * @param pid
	 *            PID of the calling component (use 'config.service_pid()' or
	 *            '(String)prop.get(Constants.SERVICE_PID)'
	 * @param member
	 *            Name of the Method or Field with the Reference annotation, e.g.
	 *            'Controllers' for 'addControllers()' method
	 * @param ids
	 *            Component IDs to be filtered for
	 * 
	 * @return true if the filter was updated. You may use it to abort the
	 *         activate() method.
	 */
	public static boolean updateReferenceFilter(ConfigurationAdmin cm, String pid, String member, String... ids) {
		final String targetProperty = member + ".target";
		/*
		 * generate required target filter
		 */
		StringBuilder targetBuilder = new StringBuilder("(&(enabled=true)(|");
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
}
