package io.openems.api.persistence;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelUpdateListener;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.AbstractWorker;

public abstract class Persistence extends AbstractWorker implements Thing, ChannelUpdateListener {

	public final static String THINGID_PREFIX = "_persistence";
	private static int instanceCounter = 0;

	public Persistence() {
		super(THINGID_PREFIX + instanceCounter++);
	}

	@Override public abstract void channelUpdated(Channel channel, Optional<?> newValue);
}
