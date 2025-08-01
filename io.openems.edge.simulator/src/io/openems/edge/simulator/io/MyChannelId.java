package io.openems.edge.simulator.io;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.AbstractDoc;

public class MyChannelId implements io.openems.edge.common.channel.ChannelId {

	private final String name;
	private final AbstractDoc<Boolean> doc;

	public MyChannelId(String name, AbstractDoc<Boolean> doc) {
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