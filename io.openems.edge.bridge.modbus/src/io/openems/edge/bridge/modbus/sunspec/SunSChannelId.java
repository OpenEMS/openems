package io.openems.edge.bridge.modbus.sunspec;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.AbstractDoc;

public class SunSChannelId<T> implements io.openems.edge.common.channel.ChannelId {

	private final String name;
	private final AbstractDoc<T> doc;

	public SunSChannelId(String name, AbstractDoc<T> doc) {
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

	@Override
	public String toString() {
		return this.name;
	}
}