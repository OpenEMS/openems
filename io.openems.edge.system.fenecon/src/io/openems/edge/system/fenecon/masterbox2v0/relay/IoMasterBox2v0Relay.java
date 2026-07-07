package io.openems.edge.system.fenecon.masterbox2v0.relay;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalOutput;

public interface IoMasterBox2v0Relay extends DigitalOutput, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		DEBUG_RELAY_1(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_1)),

		DEBUG_RELAY_2(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_2)),

		DEBUG_RELAY_3(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_3)),

		DEBUG_RELAY_4(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_4)),

		DEBUG_RELAY_5(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_5)),

		DEBUG_RELAY_6(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_6));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_1}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay1Channel() {
		return this.channel(ChannelId.RELAY_1);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_2}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay2Channel() {
		return this.channel(ChannelId.RELAY_2);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_3}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay3Channel() {
		return this.channel(ChannelId.RELAY_3);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_4}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay4Channel() {
		return this.channel(ChannelId.RELAY_4);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_5}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay5Channel() {
		return this.channel(ChannelId.RELAY_5);
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_3}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay6Channel() {
		return this.channel(ChannelId.RELAY_6);
	}

}
