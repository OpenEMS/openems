package io.openems.edge.common.channel;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.TypeUtils;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public abstract class AbstractReadChannel<T> implements Channel {

	private final ChannelId channelId;
	private final OpenemsComponent component;
	private final OpenemsType type;

	private volatile T nextValue = null; // TODO add timeout for nextValue validity
	private volatile T activeValue = null;

	public AbstractReadChannel(OpenemsType type, OpenemsComponent component, ChannelId channelId) {
		this.type = type;
		this.component = component;
		this.channelId = channelId;
	}

	@Override
	public ChannelId channelId() {
		return this.channelId;
	}

	@Override
	public void nextProcessImage() {
		this.activeValue = this.nextValue;
		System.out.println("nextProcessImage " + this.address() + ": " + this.activeValue);
	}

	@Override
	public ChannelAddress address() {
		return new ChannelAddress(this.component.id(), this.channelId().id());
	}

	@Override
	public OpenemsType getType() {
		return this.type;
	}

	/**
	 * Should be called by actual implementations to set the next value
	 * 
	 * @param value
	 * @throws OpenemsException
	 */
	public final void setNextValue(Object value) throws OpenemsException {
		this.nextValue = TypeUtils.<T>getAsType(type, value);
		System.out.println("Next value for [" + this.address() + "]: " + this.nextValue);
	}
}
