package io.openems.edge.common.component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StateChannel;

/**
 * This is the default implementation of the {@link OpenemsComponent} interface.
 * 
 * {@link #activate(ComponentContext, String, String, boolean)} and
 * {@link #deactivate()} methods should be called by the corresponding methods
 * in the OSGi component.
 */
public abstract class AbstractOpenemsComponent implements OpenemsComponent {

	private final static AtomicInteger NEXT_GENERATED_COMPONENT_ID = new AtomicInteger(-1);

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsComponent.class);

	/**
	 * Holds all Channels by their Channel-ID String representation (in
	 * CaseFormat.UPPER_CAMEL)
	 */
	private final Map<String, Channel<?>> channels = Collections.synchronizedMap(new HashMap<>());

	private String id = null;
	private ComponentContext componentContext = null;
	private boolean enabled = true;

	/**
	 * Handles @Activate of implementations. Prints log output.
	 * 
	 * @param context
	 * @param properties
	 * @param id
	 * @param enabled
	 */
	protected void activate(ComponentContext context, String id, boolean enabled) {
		if (id == null || id.trim().equals("")) {
			this.id = "_component" + AbstractOpenemsComponent.NEXT_GENERATED_COMPONENT_ID.incrementAndGet();
		} else {
			this.id = id;
		}

		this.enabled = enabled;
		this.componentContext = context;
		if (isEnabled()) {
			this.logMessage("Activate");
		} else {
			this.logMessage("Activate DISABLED");
		}
	}

	/**
	 * Handles @Deactivate of implementations. Prints log output.
	 */
	protected void deactivate() {
		this.logMessage("Deactivate");
		// deactivate all Channels
		for (Channel<?> channel : this.channels.values()) {
			channel.deactivate();
		}
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public ComponentContext getComponentContext() {
		return this.componentContext;
	}

	private void logMessage(String reason) {
		String packageName = this.getClass().getPackage().getName();
		if (packageName.startsWith("io.openems.")) {
			packageName = packageName.substring(11);
		}
		this.logInfo(this.log, reason + " " + this.getClass().getSimpleName() + " [" + packageName + "]");
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public Channel<?> _channel(String channelName) {
		Channel<?> channel = this.channels.get(channelName);
		return channel;
	}

	/**
	 * Adds a Channel to this Component.
	 * 
	 * @param channel the Channel
	 * @throws NullPointerException if the Channel was not initialized.
	 */
	protected void addChannel(Channel<?> channel) throws NullPointerException {
		if (channel == null) {
			throw new NullPointerException(
					"Trying to add 'null' Channel. Hint: Check for missing handling of Enum value.");
		}
		// Add Channel to channels list
		this.channels.put(channel.channelId().id(), channel);
		// Handle StateChannels
		if (channel instanceof StateChannel) {
			this.getState().addChannel((StateChannel) channel);
		}
	}

	/**
	 * Removes a Channel from this Component.
	 * 
	 * @param channel the Channel
	 */
	protected void removeChannel(Channel<?> channel) {
		// Add Channel to channels list
		this.channels.remove(channel.channelId().id(), channel);
		// Handle StateChannels
		if (channel instanceof StateChannel) {
			this.getState().removeChannel((StateChannel) channel);
		}
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
	protected void logInfo(Logger log, String message) {
		log.info("[" + this.id() + "] " + message);
	}

	/**
	 * Log a warn message including the Component ID.
	 * 
	 * @param log
	 * @param message
	 */
	protected void logWarn(Logger log, String message) {
		log.warn("[" + this.id() + "] " + message);
	}

	/**
	 * Log an error message including the Component ID.
	 * 
	 * @param log
	 * @param message
	 */
	protected void logError(Logger log, String message) {
		log.error("[" + this.id() + "] " + message);
	}
}
