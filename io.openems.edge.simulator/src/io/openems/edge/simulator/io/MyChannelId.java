package io.openems.edge.simulator.io;

import io.openems.edge.common.channel.Doc;

public class MyChannelId implements io.openems.edge.common.channel.ChannelId {

	private final String name;
	private final Doc doc = new Doc();

	public MyChannelId(String name) {
		this.name = name;
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