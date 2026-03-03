package io.openems.edge.pytes.metergrid;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;


import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pytes.enums.AlarmCode;
import io.openems.edge.pytes.enums.CtSelftestResult;
import io.openems.edge.pytes.enums.InverterStatus;
import io.openems.edge.pytes.enums.MeterLocationCode;
import io.openems.edge.pytes.enums.MeterTypeCode;
import io.openems.edge.pytes.enums.OperatingStatus;

public interface PytesMeterGrid extends ElectricityMeter, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)),	
		APPARENT_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)),
		APPARENT_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)),
		APPARENT_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), 
		METER_PF(Doc.of(OpenemsType.FLOAT)),// cos phi
		METER_CT_POSITION(Doc.of(OpenemsType.INTEGER)),
		ALARM_CODE(Doc.of(AlarmCode.values())),
		INVERTER_STATUS(Doc.of(InverterStatus.values())),
		OPERATING_STATUS(Doc.of(OperatingStatus.values())),
		CT_SELFTEST_RESULT(Doc.of(CtSelftestResult.values())),
		EQUIPMENT_FAULT_CODE(Doc.of(OpenemsType.INTEGER)),
		
		EPM_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //

		FAILSAFE_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		EPM_SWITCH_STATUS(Doc.of(OpenemsType.BOOLEAN) //ToDo: Double?
				.accessMode(AccessMode.READ_ONLY)), //

		FAILSAFE_SWITCH_STATUS(Doc.of(OpenemsType.BOOLEAN) // ToDo: Double?
				.accessMode(AccessMode.READ_ONLY)), //
		
		METER_IN_GRID(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //

		CT_IN_GRID(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		METER_FAULT_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //

		CT_FAULT_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //

		EPM_FAULT_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		
		METER_REVERSE_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //

		CT_REVERSE_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), //
		
		POWER_CONTROL_MODE_UNBALANCED_ALLOWED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)), 
		
		METER1_TYPE_LOCATION_RAW(Doc.of(OpenemsType.INTEGER)), 
		
		METER1_LOCATION_CODE(Doc.of(MeterLocationCode.values())), // internal, external
		
		METER1_TYPE_CODE(Doc.of(MeterTypeCode.values())), // e.g. Eastron, generic, etc
			
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
	 * Gets the Channel for {@link ChannelId#METER1_TYPE_LOCATION_RAW}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMeter1TypeLocationRawChannel() {
	    return this.channel(ChannelId.METER1_TYPE_LOCATION_RAW);
	}

	/**
	 * Returns the value from the Meter1 Type & Location raw Channel.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMeter1TypeLocationRaw() {
	    return this.getMeter1TypeLocationRawChannel().value();
	}		
}
