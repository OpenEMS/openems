package io.openems.edge.core.host;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;

public interface Host extends OpenemsComponent, JsonApi {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		DISK_IS_FULL(Doc.of(Level.WARNING) //
				.text("Disk is full")); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#DISK_IS_FULL}.
	 *
	 * @return the Channel
	 */
	public default StateChannel getDiskIsFullChannel() {
		return this.channel(ChannelId.DISK_IS_FULL);
	}

	/**
	 * Gets the Disk is Full Warning State. See {@link ChannelId#DISK_IS_FULL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getDiskIsFull() {
		return this.getDiskIsFullChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#DISK_IS_FULL}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDiskIsFull(boolean value) {
		this.getDiskIsFullChannel().setNextValue(value);
	}

}
