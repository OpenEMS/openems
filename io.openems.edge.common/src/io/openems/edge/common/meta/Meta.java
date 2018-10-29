package io.openems.edge.common.meta;

import io.openems.common.OpenemsConstants;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

public interface Meta extends ModbusSlave {

	public final static String COMPONENT_ID = "_meta";

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * OpenEMS Version
		 * 
		 * <ul>
		 * <li>Interface: Meta
		 * <li>Type: String
		 * </ul>
		 */
		VERSION(new Doc().type(OpenemsType.STRING));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public default ModbusSlaveTable getModbusSlaveTable() {
		return new ModbusSlaveTable( //
				ModbusSlaveNatureTable.of(Meta.class, 199) //
						.uint16(0, OpenemsConstants.VERSION_MAJOR) //
						.uint16(1, OpenemsConstants.VERSION_MINOR) //
						.uint16(2, OpenemsConstants.VERSION_PATCH) //
						.string16(3, OpenemsConstants.MANUFACTURER) //
						.string16(19, OpenemsConstants.MANUFACTURER_MODEL) //
						.string16(35, OpenemsConstants.MANUFACTURER_OPTIONS) //
						.string16(51, OpenemsConstants.MANUFACTURER_VERSION) //
						.string16(67, OpenemsConstants.MANUFACTURER_SERIAL_NUMBER) //
						.string16(83, OpenemsConstants.MANUFACTURER_EMS_SERIAL_NUMBER) //
						.build());
	}

}
