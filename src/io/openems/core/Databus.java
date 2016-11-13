package io.openems.core;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;

public class Databus implements ChannelUpdateListener {
	private final static Logger log = LoggerFactory.getLogger(ThingRepository.class);

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
		if (channel instanceof ReadChannel<?>) {
			log.debug("Channel [" + channel.address() + "] updated: " + newValue);
		}
		// Call Persistence-Workers
		if (channel instanceof ReadChannel<?> && !(channel instanceof ConfigChannel<?>)) {
			thingRepository.getPersistences().forEach(persistence -> {
				persistence.channelUpdated(channel, newValue);
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
