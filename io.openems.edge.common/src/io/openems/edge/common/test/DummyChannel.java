package io.openems.edge.common.test;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyChannel extends IntegerReadChannel {

	/**
	 * Creates a {@link DummyChannel} with the given name.
	 * 
	 * @param name the channel name
	 * @return a {@link DummyChannel}
	 */
	public static DummyChannel of(String name) {
		var doc = new IntegerDoc();
		var channelId = new ChannelIdImpl(name, doc);
		return new DummyChannel(null, channelId, doc);
	}

	protected DummyChannel(OpenemsComponent component, ChannelId channelId, IntegerDoc channelDoc) {
		super(component, channelId, channelDoc);
	}

}
