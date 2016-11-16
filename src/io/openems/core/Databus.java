package io.openems.core;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;

public class Databus implements ChannelUpdateListener, ChannelChangeListener {
	private final static Logger log = LoggerFactory.getLogger(Databus.class);

	private static Databus instance;

	public static synchronized Databus getInstance() {
		if (Databus.instance == null) {
			Databus.instance = new Databus();
		}
		return Databus.instance;
	}

	private final ThingRepository thingRepository;

	private Databus() {
		thingRepository = ThingRepository.getInstance();
	}

	@Override public void channelUpdated(Channel channel, Optional<?> newValue) {
		log.debug("Channel [" + channel.address() + "] updated: " + newValue);
		// Call Persistence-Workers
		if (channel instanceof ReadChannel<?> && !(channel instanceof ConfigChannel<?>)) {
			thingRepository.getPersistences().forEach(persistence -> {
				if (persistence instanceof ChannelUpdateListener) {
					((ChannelUpdateListener) persistence).channelUpdated(channel, newValue);
				}
			});
		}
	}

	@Override public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		log.debug("Channel [" + channel.address() + "] changed from " + oldValue + " to " + newValue);
		// Call Persistence-Workers
		if (!(channel instanceof ConfigChannel<?>)) {
			thingRepository.getPersistences().forEach(persistence -> {
				if (persistence instanceof ChannelChangeListener) {
					((ChannelChangeListener) persistence).channelChanged(channel, newValue, oldValue);
				}
			});
		}
	}

	public Optional<?> getValue(String thingId, String channelId) {
		Optional<Channel> channel = thingRepository.getChannel(thingId, channelId);
		if (channel.isPresent() && channel.get() instanceof ReadChannel<?>) {
			return ((ReadChannel<?>) channel.get()).valueOptional();
		} else {
			return Optional.empty();
		}
	}
}
