package io.openems.edge.bridge.modbus.tester;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface BridgeModbusTester extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Raw register payload as hex (e.g. "003F" for a single register, "003F 00A1"
		 * for two).
		 */
		RESPONSE(Doc.of(OpenemsType.STRING)),

		/**
		 * Full Modbus frame reconstruction (protocol-dependent). E.g. for ASCII:
		 * {@code :010304003F00A1xx\r\n}. Allows comparison with bridge-level logs.
		 */
		MESSAGE(Doc.of(OpenemsType.STRING)),

		/** Configured register address (mirrors config for visibility). */
		REGISTER_ADDRESS(Doc.of(OpenemsType.INTEGER)),

		/** Configured register count. */
		REGISTER_COUNT(Doc.of(OpenemsType.INTEGER)),

		/** Configured Modbus unit ID. */
		UNIT_ID(Doc.of(OpenemsType.INTEGER)),

		/** Configured protocol type as string (TCP/RTU/ASCII). */
		MODBUS_PROTOCOL(Doc.of(OpenemsType.STRING)),

		/** True after a successful read, false after an error. */
		COMMUNICATION_OK(Doc.of(OpenemsType.BOOLEAN)),

		/** Running counter of communication errors. */
		COMMUNICATION_ERRORS(Doc.of(OpenemsType.INTEGER)),

		/** Last error message text. */
		LAST_ERROR(Doc.of(OpenemsType.STRING)),

		;

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
