package io.openems.api.channel;

public class WriteableChannelBuilder<B extends WriteableChannelBuilder<?>> extends ChannelBuilder<B> {
	protected Long maxWriteValue = null;
	protected Channel maxWriteValueChannel = null;
	protected Long minWriteValue = null;
	protected Channel minWriteValueChannel = null;

	@Override
	public WriteableChannel build() {
		return new WriteableChannel(nature, unit, minValue, maxValue, multiplier, delta, labels, minWriteValue,
				minWriteValueChannel, maxWriteValue, maxWriteValueChannel);
	}

	@SuppressWarnings("unchecked")
	public B maxWriteValue(Channel maxWriteValueChannel) {
		this.maxWriteValueChannel = maxWriteValueChannel;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B maxWriteValue(Long maxWriteValue) {
		this.maxWriteValue = maxWriteValue;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B minWriteValue(Channel minWriteValueChannel) {
		this.minWriteValueChannel = minWriteValueChannel;
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B minWriteValue(Long minWriteValue) {
		this.minWriteValue = minWriteValue;
		return (B) this;
	}
}
