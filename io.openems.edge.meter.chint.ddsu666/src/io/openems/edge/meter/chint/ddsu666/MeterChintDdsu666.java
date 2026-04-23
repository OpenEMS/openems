package io.openems.edge.meter.chint.ddsu666;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterChintDdsu666 extends ElectricityMeter, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Total imported active energy (from grid).
		 *
		 * <ul>
		 * <li>Interface: MeterChintDdsu666
		 * <li>Type: INTEGER
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_IMPORT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		/**
		 * Total exported active energy (to grid).
		 *
		 * <ul>
		 * <li>Interface: MeterChintDdsu666
		 * <li>Type: INTEGER
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTIVE_EXPORT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
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
