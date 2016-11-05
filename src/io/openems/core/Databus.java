package io.openems.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelListener;
import io.openems.api.channel.ReadChannel;

public class Databus implements ChannelListener {
	private final static Logger log = LoggerFactory.getLogger(ThingRepository.class);

	private static Databus instance;

	public static synchronized Databus getInstance() {
		if (Databus.instance == null) {
			Databus.instance = new Databus();
		}
		return Databus.instance;
	}

	private Databus() {}

	@Override public void channelEvent(Channel channel) {
		if (channel instanceof ReadChannel<?>) {
			log.debug("Channel [" + channel.address() + "] updated: " + ((ReadChannel<?>) channel).valueOptional());
		}
	}
}
