package io.openems.edge.io.kmtronic.eight;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.io.api.DigitalOutput;

public interface IoKmtronicRelay8Port extends DigitalOutput, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds writes to Relay Output 1 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_1(Doc.of(OpenemsType.BOOLEAN)), //
		/**
		 * Relay Output 1.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_1(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_1)),
		/**
		 * Holds writes to Relay Output 2 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_2(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Relay Output 2.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_2(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_2)),
		/**
		 * Holds writes to Relay Output 3 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_3(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Relay Output 3.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_3(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_3)),
		/**
		 * Holds writes to Relay Output 4 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_4(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Relay Output 4.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_4(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_4)),
		/**
		 * Holds writes to Relay Output 5 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_5(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Relay Output 5.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_5(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_5)),
		/**
		 * Holds writes to Relay Output 6 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_6(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Relay Output 6.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_6(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_6)),
		/**
		 * Holds writes to Relay Output 7 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_7(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Relay Output 7.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_7(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_7)),
		/**
		 * Holds writes to Relay Output 8 for debugging.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_8(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		/**
		 * Relay Output 8.
		 *
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_8(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_8));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
