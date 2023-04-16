package io.openems.edge.pvinverter.solarlog;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public interface SolarLog
		extends ManagedSymmetricPvInverter, ElectricityMeter, OpenemsComponent, EventHandler, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		LAST_UPDATE_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.SECONDS)), //
		PDC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		UDC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),
		YESTERDAY_YIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		MONTHLY_YIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		YEARLY_YIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		TOTAL_YIELD(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		PAC_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		YESTERDAY_YIELD_CONS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		MONTHLY_YIELD_CONS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		YEARLY_YIELD_CONS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		TOTAL_YIELD_CONS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		TOTAL_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS_BY_WATT_PEAK)),

		// PV
		P_LIMIT_TYPE(Doc.of(PLimitType.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		P_LIMIT_PERC(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.PERCENT)),
		P_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT)),
		WATCH_DOG_TAG(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)), //
		STATUS(Doc.of(Status.values())),

		PV_LIMIT_FAILED(Doc.of(Level.FAULT) //
				.text("PV-Limit failed"));

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
