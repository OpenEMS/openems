package io.openems.edge.common.component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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

	private final static String DEFAULT_ID = "UNDEFINED";

	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsComponent.class);
	private final Map<io.openems.edge.common.channel.doc.ChannelId, Channel<?>> channels = Collections
			.synchronizedMap(new HashMap<>());

	private String id = DEFAULT_ID;
	private boolean isActive = false;
	private boolean isEnabled = false;

	protected void activate(String id, boolean isEnabled) {
		this.id = id;
		this.isActive = true;
		this.isEnabled = isEnabled;
		this.logMessage("Activate");
	}

	protected void deactivate() {
		this.logMessage("Deactivate");
		this.isActive = false;
		this.id = DEFAULT_ID;
	}

	private void logMessage(String reason) {
		String packageName = this.getClass().getPackage().getName();
		if (packageName.startsWith("io.openems.")) {
			packageName = packageName.substring(11);
		}
		log.debug(reason + " [" + this.id + "]: " + this.getClass().getSimpleName() + " [" + packageName + "]");
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public boolean isActive() {
		return this.isActive;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(boolean isEnabled) {
		this.isEnabled = isEnabled;
	}

	@Override
	public Channel<?> channel(io.openems.edge.common.channel.doc.ChannelId channelId) {
		Channel<?> channel = this.channels.get(channelId);
		if (channel == null) {
			throw new IllegalArgumentException("ID [" + this.id() + "] has no Channel [" + channelId + "]");
		}
		return channel;
	}

	protected void addChannel(Channel<?> channel) {
		if (channel == null) {
			throw new NullPointerException(
					"Trying to add 'null' Channel. Hint: Check for missing handling of Enum value.");
		}
		this.channels.put(channel.channelId(), channel);
	}

	@Override
	public Collection<Channel<?>> channels() {
		return this.channels.values();
	}
}
