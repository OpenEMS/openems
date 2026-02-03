package io.openems.edge.deye.meter;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

public interface DeyeMeterInternal extends ElectricityMeter, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		VOLTAGE_L1_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)),		
		VOLTAGE_L2_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)),
		VOLTAGE_L3_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)),		
		GRID_INNER_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),		
		GRID_INNER_ACTIVE_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		GRID_INNER_ACTIVE_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),	
		GRID_INNER_ACTIVE_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),			
		
		ACTIVE_POWER_L1_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_L2_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),	
		ACTIVE_POWER_L3_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),			
		ACTIVE_POWER_SIDE_TO_SIDE_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),	
		APPARENT_POWER_SIDE_TO_SIDE_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)),
		CURRENT_L1_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),		
		CURRENT_L2_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),	
		CURRENT_L3_TO_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),			
		CURRENT_L1_FROM_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),		
		CURRENT_L2_FROM_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),	
		CURRENT_L3_FROM_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),	
		ACTIVE_POWER_L1_FROM_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),		
		ACTIVE_POWER_L2_FROM_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),
		ACTIVE_POWER_L3_FROM_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),	
		ACTIVE_POWER_FROM_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)),	
		APPARENT_POWER_FROM_GRID(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)),	

		GRID_HIGH_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ) //
				.persistencePriority(PersistencePriority.HIGH)),		

		GRID_LOW_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ) //
				.persistencePriority(PersistencePriority.HIGH)),		
		
		GRID_HIGH_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)),		

		GRID_LOW_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.HIGH)),			
		
		GRID_CONNECTED_POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE)),			
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
