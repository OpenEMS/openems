package io.openems.edge.io.siemenslogo;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.MEDIUM;
import static io.openems.common.types.OpenemsType.BOOLEAN;

import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.io.api.DigitalOutput;

public interface SiemensLogoRelay extends DigitalOutput, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Input 1.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayInput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		INPUT_1(new BooleanDoc() //
				.accessMode(READ_ONLY) //
				.persistencePriority(HIGH)),
		/**
		 * Input 2.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayInput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		INPUT_2(new BooleanDoc() //
				.accessMode(READ_ONLY) //
				.persistencePriority(HIGH)),
		/**
		 * Input 3.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayInput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		INPUT_3(new BooleanDoc() //
				.accessMode(READ_ONLY) //
				.persistencePriority(HIGH)),
		/**
		 * Input 4.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayInput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		INPUT_4(new BooleanDoc() //
				.accessMode(READ_ONLY) //
				.persistencePriority(HIGH)),
		/**
		 * Holds writes to Relay Output 1 for debugging.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_1(Doc.of(BOOLEAN)), //
		/**
		 * Relay 1.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_1(new BooleanDoc() //
				.accessMode(READ_WRITE) //
				.persistencePriority(MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_1)),
		/**
		 * Holds writes to Relay Output 2 for debugging.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_2(Doc.of(BOOLEAN) //
				.persistencePriority(MEDIUM)), //
		/**
		 * Relay 2.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_2(new BooleanDoc() //
				.accessMode(READ_WRITE) //
				.persistencePriority(MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_2)),

		/**
		 * Holds writes to Relay Output 3 for debugging.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_3(Doc.of(BOOLEAN) //
				.persistencePriority(MEDIUM)), //
		/**
		 * Relay 3.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_3(new BooleanDoc() //
				.accessMode(READ_WRITE) //
				.persistencePriority(MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_3)),

		/**
		 * Holds writes to Relay Output 4 for debugging.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_4(Doc.of(BOOLEAN) //
				.persistencePriority(MEDIUM)), //
		/**
		 * Relay 4.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_4(new BooleanDoc() //
				.accessMode(READ_WRITE) //
				.persistencePriority(MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_4)),

		/**
		 * Holds writes to Relay Output 5 for debugging.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_5(Doc.of(BOOLEAN) //
				.persistencePriority(MEDIUM)), //
		/**
		 * Relay 5.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_5(new BooleanDoc() //
				.accessMode(READ_WRITE) //
				.persistencePriority(MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_5)),

		/**
		 * Holds writes to Relay Output 6 for debugging.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_6(Doc.of(BOOLEAN) //
				.persistencePriority(MEDIUM)), //
		/**
		 * Relay 6.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_6(new BooleanDoc() //
				.accessMode(READ_WRITE) //
				.persistencePriority(MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_6)),

		/**
		 * Holds writes to Relay Output 7 for debugging.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_7(Doc.of(BOOLEAN) //
				.persistencePriority(MEDIUM)), //
		/**
		 * Relay 7.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_7(new BooleanDoc() //
				.accessMode(READ_WRITE) //
				.persistencePriority(MEDIUM) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_7)),

		/**
		 * Holds writes to Relay Output 8 for debugging.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_8(Doc.of(BOOLEAN) //
				.persistencePriority(MEDIUM)), //
		/**
		 * Relay 8.
		 * 
		 * <ul>
		 * <li>Interface: SiemensLogoRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		RELAY_8(new BooleanDoc() //
				.accessMode(READ_WRITE) //
				.persistencePriority(MEDIUM) //
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
