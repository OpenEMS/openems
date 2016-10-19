package io.openems.api.channel;

import java.math.BigInteger;

public class WriteableChannelBuilder<B extends WriteableChannelBuilder<?>> extends ChannelBuilder<B> {
	protected BigInteger maxWriteValue = null;
	protected Channel maxWriteValueChannel = null;
	protected BigInteger minWriteValue = null;
	protected Channel minWriteValueChannel = null;

	@Override
	public WriteableChannel build() {
		return new WriteableChannel(nature, unit, minValue, maxValue, multiplier, delta, labels, minWriteValue,
				minWriteValueChannel, maxWriteValue, maxWriteValueChannel);
	}

	@SuppressWarnings("unchecked")
	public B maxWriteValue(BigInteger maxWriteValue) {
		this.maxWriteValue = maxWriteValue;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B maxWriteValue(Channel maxWriteValueChannel) {
		this.maxWriteValueChannel = maxWriteValueChannel;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B minWriteValue(BigInteger minWriteValue) {
		this.minWriteValue = minWriteValue;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B minWriteValue(Channel minWriteValueChannel) {
		this.minWriteValueChannel = minWriteValueChannel;
		return (B) this;
	}
}
