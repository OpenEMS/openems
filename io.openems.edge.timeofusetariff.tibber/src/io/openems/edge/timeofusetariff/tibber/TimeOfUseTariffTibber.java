package io.openems.edge.timeofusetariff.tibber;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

public interface TimeOfUseTariffTibber extends TimeOfUseTariff, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		HTTP_STATUS_CODE(Doc.of(OpenemsType.INTEGER) //
				.text("The HTTP status code")), //
		STATUS_TIMEOUT(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Tibber: timout while reading from server")), //
		STATUS_AUTHENTICATION_FAILED(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Tibber: access token authentication failed")), //
		STATUS_SERVER_ERROR(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Tibber: unexpected server error")), //
		FILTER_IS_REQUIRED(Doc.of(Level.WARNING) //
				.text("Found multiple 'Homes'. Please configure either an ID (format UUID) "
						+ "or 'appNickname' for unambiguous identification")),
		UNABLE_TO_UPDATE_PRICES(Doc.of(Level.WARNING) //
				.text("Unable to update prices from Tibber API")
				.persistencePriority(PersistencePriority.HIGH)) //
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

	/**
	 * Used for Modbus/TCP Api Controller. Provides a Modbus table for the Channels
	 * of this Component.
	 *
	 * @param accessMode filters the Modbus-Records that should be shown
	 * @return the {@link ModbusSlaveNatureTable}
	*/
	public default ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(TimeOfUseTariffTibber.class, accessMode, 100) //
				.channel(0, ChannelId.HTTP_STATUS_CODE, ModbusType.UINT16) //
				.channel(1, ChannelId.STATUS_TIMEOUT, ModbusType.UINT16) //
				.channel(2, ChannelId.STATUS_SERVER_ERROR, ModbusType.UINT16) //
				.channel(3, ChannelId.FILTER_IS_REQUIRED, ModbusType.UINT16) //
				.channel(4, ChannelId.UNABLE_TO_UPDATE_PRICES, ModbusType.UINT16) //
				.build();
	}
		 
}
