package io.openems.edge.phoenixcontact.plcnext.common.mapper;

import java.util.Objects;

import io.openems.edge.common.channel.ChannelId;

/**
 * Containing @link{ChannelId} to single value assignment 
 */
public class PlcNextGdsDataMappedValue {

	private final ChannelId channelId;
	private final Object value;

	public PlcNextGdsDataMappedValue(ChannelId channelId, Object value) {
		this.channelId = channelId;
		this.value = value;
	}

	public ChannelId getChannelId() {
		return this.channelId;
	}

	public Object getValue() {
		return this.value;
	}

	@Override
	public int hashCode() {
		return Objects.hash(channelId, value);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PlcNextGdsDataMappedValue other = (PlcNextGdsDataMappedValue) obj;
		return Objects.equals(channelId, other.channelId) && Objects.equals(value, other.value);
	}

	@Override
	public String toString() {
		return "PlcNextGdsDataMappedValue [channelId=" + channelId + ", value=" + value + "]";
	}
}
