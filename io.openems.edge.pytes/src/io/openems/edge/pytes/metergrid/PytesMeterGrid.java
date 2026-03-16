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

		// Register 43073 – METER/CT Position (Appendix 12 bitmask, R/W)
		// Raw U16 word – kept for diagnostics / write-back
		METER_CT_POSITION_RAW(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)),
 
		// BIT02: CT in grid (AC Couple inverter only)
		METER_CT_IN_GRID(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
 
		/*
		BIT03: Parallel PV inverter CT detection switch
		1?If 33250 BIT03=0 and 33245 has value over 500W, it means there is CT connected 
			for parallel PV inverter and power flow exists.
		2?If 33250=0, 33245 value needs to be displayed.
		*/ 
		METER_PARALLEL_PV_CT_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
		
 		/*
		BIT04: EPM Switch (EPM ON/OFF, or AU-2020 soft-limit ON/OFF)
		*/
		METER_EPM_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
 
		// BIT05: Failsafe Switch  0=OFF  1=ON
		METER_FAILSAFE_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
 
		/*
		BIT06: Power Control Mode  0=3-phase balanced  1=3-phase individual (unbalanced)
		Effective when EPM function (BIT04 or BIT07) Enabled
		*/
		METER_POWER_CONTROL_MODE_UNBALANCED(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
 
		/* 
		BIT07: EPM current-setting switch  0=off(default)  1=on
		After opening, set 43326 or 43327-43329 according to the mode corresponding to 
		BIT06; (Note: temporarily supported from S6 models)
		*/ 
		METER_EPM_CURRENT_SETTING_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
 
		/*
		BIT08: External EPM ON/OFF Status  0=OFF  1=ON  (3ph HV hybrid 5G only)
		Not support BIT 04 to be ON at the same time. If need to turn on BIT08, need to set BIT04=0 and BIT08=1
		*/ 
		METER_EXTERNAL_EPM_STATUS(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
 
		// BIT09: External EPM Failsafe Switch Status  0=OFF  1=ON  (3ph HV hybrid 5G only)
		METER_EXTERNAL_EPM_FAILSAFE_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
 
		// BIT13: Meter/CT selection for grid side  0=meter(default)  1=CT  (S6 LV storage only)
		METER_CT_SELECTION(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)),
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
