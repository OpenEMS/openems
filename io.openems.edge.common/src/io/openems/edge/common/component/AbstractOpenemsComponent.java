package io.openems.edge.common.component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;

/**
 * This is the default implementation of the {@link OpenemsComponent} interface.
 * 
 * 'activate()' and 'deactivate()' methods should be called by the corresponding
 * methods in the OSGi component.
 * 
 * @author stefan.feilmeier
 */
public abstract class AbstractOpenemsComponent implements OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsComponent.class);

	/**
	 * Holds all Channels by their Channel-ID String representation (in
	 * CaseFormat.UPPER_CAMEL)
	 */
	private final Map<String, Channel<?>> channels = Collections.synchronizedMap(new HashMap<>());

	private String id = null;
	private boolean enabled = true;

	/**
	 * Handles @Activate of implementations. Prints log output.
	 * 
	 * @param id
	 */
	protected void activate(ComponentContext context, String service_pid, String id, boolean enabled) {
		this.id = id;
		this.enabled = enabled;
		this.logMessage("Activate");
	}

	/**
	 * Handles @Deactivate of implementations. Prints log output.
	 * 
	 * @param id
	 */
	protected void deactivate() {
		this.logMessage("Deactivate");
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	private void logMessage(String reason) {
		String packageName = this.getClass().getPackage().getName();
		if (packageName.startsWith("io.openems.")) {
			packageName = packageName.substring(11);
		}
		log.info(reason + " [" + this.id + "]: " + this.getClass().getSimpleName() + " [" + packageName + "]");
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public Channel<?> channel(String channelName) {
		Channel<?> channel = this.channels.get(channelName);
		if (channel == null) {
			throw new IllegalArgumentException(
					"Channel [" + channelName + "] is not defined for ID [" + this.id() + "].");
		}
		return channel;
	}

	protected void addChannel(Channel<?> channel) {
		if (channel == null) {
			throw new NullPointerException(
					"Trying to add 'null' Channel. Hint: Check for missing handling of Enum value.");
		}
		this.channels.put(channel.channelId().id(), channel);
	}

	@Override
	public Collection<Channel<?>> channels() {
		return this.channels.values();
	}

	/**
	 * Log an info message including the Component ID.
	 * 
	 * @param log
	 * @param message
	 */
	protected final void logInfo(Logger log, String message) {
		log.info("[" + this.id() + "] " + message);
	}
}
