package io.openems.edge.meter.siemens.pac1600;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.meter.api.ElectricityMeter;

public interface MeterSiemensPac1600 extends ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		VOLTAGE_L1L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),
		
		VOLTAGE_L2L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),
		
		VOLTAGE_L3L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),
		
		VOLTAGE_LL(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT)),		
		
		REACTIVE_CONSUMPTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		
		REACTIVE_PRODUCTION_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		
		REACTIVE_CONSUMPTION_ENERGY_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		
		REACTIVE_PRODUCTION_ENERGY_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		
		REACTIVE_CONSUMPTION_ENERGY_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		
		REACTIVE_PRODUCTION_ENERGY_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		
		REACTIVE_CONSUMPTION_ENERGY_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		
		REACTIVE_PRODUCTION_ENERGY_L3(Doc.of(OpenemsType.INTEGER) //
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