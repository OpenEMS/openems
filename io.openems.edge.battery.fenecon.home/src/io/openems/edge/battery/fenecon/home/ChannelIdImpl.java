package io.openems.edge.battery.fenecon.home;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

/**
 * This class is used to create Cell Voltage and Temperature Dynamic Channels.
 */
public class ChannelIdImpl implements ChannelId {
	;

	private final String name;
	private final Doc doc;

	public ChannelIdImpl(String name, Doc doc) {
		this.name = name;
		this.doc = doc;
	}

	@Override
	public String name() {
		return this.name;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}
