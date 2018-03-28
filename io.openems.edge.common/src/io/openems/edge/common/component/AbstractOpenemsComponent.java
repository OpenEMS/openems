package io.openems.edge.common.component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.ChannelDoc;

/**
 * This is the default implementation of the {@link OpenemsComponent} interface.
 * 
 * 'activate()' and 'deactivate()' methods should be called by the corresponding
 * methods in the OSGi component.
 * 
 * @author stefan.feilmeier
 */
public class AbstractOpenemsComponent implements OpenemsComponent {
	private final static String DEFAULT_ID = "UNDEFINED";
	private final Logger log = LoggerFactory.getLogger(AbstractOpenemsComponent.class);
	private final Map<ChannelDoc, Channel> channels = Collections.synchronizedMap(new HashMap<>());

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
	public Channel channel(ChannelDoc channelId) {
		Channel channel = this.channels.get(channelId);
		if (channel == null) {
			log.error("ID [" + this.id() + "] has no Channel [" + channelId + "]");
		}
		return this.channels.get(channelId);
	}

	protected void addChannel(Channel channel) {
		this.channels.put(channel.channelDoc(), channel);
	}

	protected final void addChannels(Channel... channels) {
		for (Channel channel : channels) {
			this.addChannel(channel);
		}
	}

	@Override
	public Collection<Channel> channels() {
		return this.channels.values();
	}
}
