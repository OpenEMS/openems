package io.openems.edge.pytes.metergrid;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Unit;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.pytes.enums.AlarmCode;
import io.openems.edge.pytes.enums.CtSelftestResult;
import io.openems.edge.pytes.enums.InverterStatus;
import io.openems.edge.pytes.enums.MeterLocationCode;
import io.openems.edge.pytes.enums.MeterTypeCode;

public interface PytesMeterGrid extends ElectricityMeter, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// -----------------------------------------------------------------------
		// External meter apparent power (reg 33273..33279)
		// Datasheet: 1 VA -> no converter needed.
		// -----------------------------------------------------------------------

		/**
		 * External meter Phase A apparent power (reg 33273, S32).
		 * Datasheet: 1 VA -> no converter needed
		 * Unit: VA
		 */
		APPARENT_POWER_L1(Doc.of(INTEGER)  //
				.accessMode(READ_ONLY) //
				.unit(Unit.VOLT_AMPERE)),

		/**
		 * External meter Phase B apparent power (reg 33275, S32).
		 * Datasheet: 1 VA -> no converter needed
		 * Unit: VA
		 */
		APPARENT_POWER_L2(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(Unit.VOLT_AMPERE)),

		/**
		 * External meter Phase C apparent power (reg 33277, S32).
		 * Datasheet: 1 VA -> no converter needed
		 * Unit: VA
		 */
		APPARENT_POWER_L3(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(Unit.VOLT_AMPERE)),

		/**
		 * External meter total apparent power (reg 33279, S32).
		 * Datasheet: 1 VA -> no converter needed
		 * Unit: VA
		 */
		APPARENT_POWER(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(Unit.VOLT_AMPERE)),

		// -----------------------------------------------------------------------
		// External meter power factor (reg 33281)
		// -----------------------------------------------------------------------

		/**
		 * External meter power factor / cos phi (reg 33281, S16).
		 * Datasheet: 0.01 -> SCALE_FACTOR_MINUS_2 -> stored as a float
		 * Valid range: -1.0 to -0.8 and +0.8 to +1.0
		 */
		METER_PF(Doc.of(FLOAT) //
				.accessMode(READ_ONLY)),

		// -----------------------------------------------------------------------
		// EPM / CT status bits (reg 33248 - input register, FC4)
		// These are the EPM hardware switch states read from the inverter
		// -----------------------------------------------------------------------

		/**
		 * reg 33248 BIT00 - EPM switch state.
		 * Reflects the current ON/OFF state of the EPM hardware switch
		 * Not informing whether it operates or not
		 * false = OFF, true = ON
		 */
		EPM_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33248 BIT01 - Failsafe switch state.
		 * The failsafe switch limits export power when triggered
		 * States whether its active or not but not actually working or not
		 * false = OFF, true = ON
		 */
		FAILSAFE_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		// -----------------------------------------------------------------------
		// EPM / meter / CT status bits (reg 33250 - input register, FC4)
		// These are the EPM and meter system status flags
		// -----------------------------------------------------------------------

		/**
		 * reg 33250 BIT01 - Meter is installed on the grid side.
		 * true = external meter detected in grid position
		 */
		METER_IN_GRID(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)), //

		/**
		 * reg 33250 BIT02 - CT (current transformer) is installed on the grid side.
		 * Used for AC-coupled inverters to avoid uploading meter communication fail alarm
		 */
		CT_IN_GRID(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT04 - EPM switch status (active state).
		 * Indicated whether EPM is currently active and enforcing export limits
		 */
		EPM_SWITCH_STATUS(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT05 - Failsafe switch status (active state).
		 * Indicate whether the failsafe limit is currently active and working or not		 */
		FAILSAFE_SWITCH_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT06 - Power control mode (Appendix 10).
		 * false = 3-phase balanced control, true = 3-phase individual (unbalanced) control
		 */
		POWER_CONTROL_MODE_UNBALANCED(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT07 - EPM current setting switch (Appendix 10).
		 */
		EPM_CURRENT_SETTING_SWITCH_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT08 - External EPM ON/OFF status (Appendix 10, 3PH RHI 5G only).
		 */
		EXTERNAL_EPM_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT09 - External EPM failsafe switch status (Appendix 10, 3PH RHI 5G only).
		 */
		EXTERNAL_EPM_FAILSAFE_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT10 - Parallel EPM power setting switch (Appendix 10).
		 */
		PARALLEL_EPM_POWER_SETTING_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT11 - Parallel EPM current setting switch (Appendix 10).
		 */
		PARALLEL_EPM_CURRENT_SETTING_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		/**
		 * reg 33250 BIT12 - Parallel power control mode (Appendix 10). Valid when BIT10 or BIT11 is on.
		 * false = three-phase unified control, true = three-phase independent control
		 */
		PARALLEL_POWER_CONTROL_MODE_UNBALANCED(Doc.of(BOOLEAN) //
				.accessMode(READ_ONLY)),

		// -----------------------------------------------------------------------
		// Inverter operating status (reg 33287)
		// -----------------------------------------------------------------------

		/**
		 * Inverter operating status (reg 33287, U16)
		 * 0 = Stop, 1 = Open loop, 2 = Soft start, 3 = Grid-connected
		 * 4 = Off-grid/EPS, 5 = Off-grid to on-grid transition, 6 = Backup bypass
		 * 7 = Generator running
		 * See {@link OperatingStatus} enum
		 */
		//OPERATING_STATUS(Doc.of(OperatingStatus.values())),

		// -----------------------------------------------------------------------
		// CT self-test result (reg 33290)
		// -----------------------------------------------------------------------

		/**
		 * CT self-test result (reg 33290, U16).
		 * Reports the outcome of the CT self-test routine
		 * 0 = Not tested, 1 = Not meeting conditions, 2 = Testing,
		 * 3 = Normal, 100 = Abnormal CT connection (direction or phase wrong)
		 * See {@link CtSelftestResult} enum
		 */
		CT_SELFTEST_RESULT(Doc.of(CtSelftestResult.values())),

		// -----------------------------------------------------------------------
		// Equipment fault code (reg 33292)
		// -----------------------------------------------------------------------

		/**
		 * Equipment fault code (reg 33292, U16).
		 * Provides a more detailed fault sub-code used together with reg 33095
		 * to distinguish which specific fault is active
		 * Example: if reg 33095 = 0x1034, reg 33292 = 0x0001 indicates the sub-fault
		 */
		EQUIPMENT_FAULT_CODE(Doc.of(INTEGER) //
				.accessMode(READ_ONLY)),

		// -----------------------------------------------------------------------
		// Meter type and location (reg 33300)
		// The raw U16 word encodes both location (high byte) and type (low byte)
		// Decoded by a listener in the Impl into METER1_LOCATION_CODE and METER1_TYPE_CODE
		// -----------------------------------------------------------------------

		/**
		 * Raw Meter 1 type and location word (reg 33300, U16).
		 * High byte = location code (0x01 = grid side)
		 * Low byte = type code (device brand/model)
		 * Decoded automatically into METER1_LOCATION_CODE and METER1_TYPE_CODE
		 */
		METER1_TYPE_LOCATION_RAW(Doc.of(INTEGER) //
				.accessMode(READ_ONLY)),

		/**
		 * Meter 1 location (decoded from reg 33300 high byte).
		 * Indicates where the external meter is physically installed
		 * See {@link MeterLocationCode} for values
		 */
		METER1_LOCATION_CODE(Doc.of(MeterLocationCode.values())),

		/**
		 * Meter 1 device type (decoded from reg 33300 low byte).
		 * Indicates the brand/protocol of the external meter
		 * See {@link MeterTypeCode} for values
		 */
		METER1_TYPE_CODE(Doc.of(MeterTypeCode.values())),

		// -----------------------------------------------------------------------
		// METER/CT Position - Appendix 12 bits (reg 43073, holding register R/W)
		// This register controls EPM behaviour and meter/CT selection
		// Individual bits decoded below. Raw word kept for diagnostics
		// -----------------------------------------------------------------------

		/**
		 * Raw METER/CT Position word (reg 43073, U16, R/W).
		 * Reconstructed from individual bit channels by a listener in the Impl
		 * kept for diagnostics and potential write-back
		 * See Appendix 12 for full bit definitions
		 */
		METER_CT_POSITION_RAW(Doc.of(INTEGER) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT02 - CT is installed on the grid side.
		 * Used for AC-coupled inverters only, to prevent a meter communication
		 * fail alarm being raised when no external meter is present
		 */
		METER_CT_IN_GRID(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT03 - Parallel PV inverter CT detection switch.
		 * When enabled, the inverter checks reg 33250 BIT03 and reg 33245 to detect
		 * whether a CT is connected for a parallel PV inverter
		 * 1 = detection active, 0 = detection off
		 */
		METER_PARALLEL_PV_CT_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT04 - EPM switch.
		 * Enables or disables the EPM function which limits power export to the grid
		 * For AU 2020 standard this acts as the EPM sof limit ON/OFF
		 * false = OFF, true = ON
		 */
		METER_EPM_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT05 - Failsafe switch.
		 * When ON, the inverter enforces a failsafe export limit even if
		 * communication with the EPM controller is lost
		 * false = OFF, true = ON
		 */
		METER_FAILSAFE_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT06 - Power control mode: unbalanced phase output.
		 * Only effective when EPM (BIT04 or BIT07) is enabled
		 * false = 3-phase balanced control (equal current on all phases, default)
		 * true = 3-phases individual control (unbalanced currents allowed per phase)
		 */
		METER_POWER_CONTROL_MODE_UNBALANCED(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT07 - EPM current-settings switch
		 * When ON, activates current-based EPM limiting. After enabling, set
		 * reg 43326 (balanced) or reg 43327-43329 (unbalanced per phase) accordingly
		 * Note: supported from S6 models onwards
		 * false = OFF (default), true = ON
		 */
		METER_EPM_CURRENT_SETTING_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT08 - External EPM ON/OFF status.
		 * Used only on 3-phase HV hybrid 5G models
		 * Cannot be ON at the same time as BIT04 - set BIT04=0 first, then BIT08=1
		 * false = OFF (default), true = ON
		 */
		METER_EXTERNAL_EPM_STATUS(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT09 - External EPM failsafe switch status.
		 * Used only on 3-phase HV hybrid 5G models
		 * false = OFF, true = ON
		 */
		METER_EXTERNAL_EPM_FAILSAFE_SWITCH(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		/**
		 * reg 43073 BIT013 - Meter/CT selection for grid side.
		 * Currently only used for S6 low-voltage energy storage models
		 * false = external meter (default), true = CT (current transformer)
		 */
		METER_CT_SELECTION(Doc.of(BOOLEAN) //
				.accessMode(READ_WRITE)),

		METER_CT_POSITION(Doc.of(INTEGER)),
		ALARM_CODE(Doc.of(AlarmCode.values())),
		INVERTER_STATUS(Doc.of(InverterStatus.values())), 
		
		BACKFLOW_POWER(Doc.of(INTEGER))

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

	// -----------------------------------------------------------------------
	// Accessor methods - Apparent Power
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#APPARENT_POWER_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getApparentPowerL1Channel() {
		return this.channel(ChannelId.APPARENT_POWER_L1);
	}

	/**
	 * Phase A apparent power [VA]. See {@link ChannelId#APPARENT_POWER_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getApparentPowerL1() {
		return this.getApparentPowerL1Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#APPARENT_POWER_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getApparentPowerL2Channel() {
		return this.channel(ChannelId.APPARENT_POWER_L2);
	}

	/**
	 * Phase B apparent power [VA]. See {@link ChannelId#APPARENT_POWER_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getApparentPowerL2() {
		return this.getApparentPowerL2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#APPARENT_POWER_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getApparentPowerL3Channel() {
		return this.channel(ChannelId.APPARENT_POWER_L3);
	}

	/**
	 * Phase C apparent power [VA]. See {@link ChannelId#APPARENT_POWER_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getApparentPowerL3() {
		return this.getApparentPowerL3Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#APPARENT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getApparentPowerChannel() {
		return this.channel(ChannelId.APPARENT_POWER);
	}

	/**
	 * Total apparent power [VA]. See {@link ChannelId#APPARENT_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getApparentPower() {
		return this.getApparentPowerChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods - Power Factor
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#METER_PF}.
	 *
	 * @return the Channel
	 */
	public default FloatReadChannel getMeterPfChannel() {
		return this.channel(ChannelId.METER_PF);
	}

	/**
	 * Power factor (cos phi). See {@link ChannelId#METER_PF}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Float> getMeterPf() {
		return this.getMeterPfChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods - EPM / CT status bits (reg 33248, 33250)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#EPM_SWITCH}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getEpmSwitchChannel() {
		return this.channel(ChannelId.EPM_SWITCH);
	}

	/**
	 * true if EPM switch is ON. See {@link ChannelId#EPM_SWITCH}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getEpmSwitch() {
		return this.getEpmSwitchChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAILSAFE_SWITCH}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFailsafeSwitchChannel() {
		return this.channel(ChannelId.FAILSAFE_SWITCH);
	}

	/**
	 * true if failsafe switch is ON. See {@link ChannelId#FAILSAFE_SWITCH}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFailsafeSwitch() {
		return this.getFailsafeSwitchChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#METER_IN_GRID}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getMeterInGridChannel() {
		return this.channel(ChannelId.METER_IN_GRID);
	}

	/**
	 * true if meter is in grid position. See {@link ChannelId#METER_IN_GRID}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getMeterInGrid() {
		return this.getMeterInGridChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#CT_IN_GRID}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getCtInGridChannel() {
		return this.channel(ChannelId.CT_IN_GRID);
	}

	/**
	 * true if CT is in grid position. See {@link ChannelId#CT_IN_GRID}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getCtInGrid() {
		return this.getCtInGridChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Operating status and fault codes
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#EQUIPMENT_FAULT_CODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getEquipmentFaultCodeChannel() {
		return this.channel(ChannelId.EQUIPMENT_FAULT_CODE);
	}

	/**
	 * Equipment fault sub-code. See {@link ChannelId#EQUIPMENT_FAULT_CODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getEquipmentFaultCode() {
		return this.getEquipmentFaultCodeChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Meter type and location (reg 33300)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#METER1_TYPE_LOCATION_RAW}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMeter1TypeLocationRawChannel() {
		return this.channel(ChannelId.METER1_TYPE_LOCATION_RAW);
	}

	/**
	 * Raw meter type+location word. See {@link ChannelId#METER1_TYPE_LOCATION_RAW}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMeter1TypeLocationRaw() {
		return this.getMeter1TypeLocationRawChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – METER/CT Position bits (reg 43073)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#METER_CT_POSITION_RAW}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMeterCtPositionRawChannel() {
		return this.channel(ChannelId.METER_CT_POSITION_RAW);
	}

	/**
	 * Raw METER/CT Position word (reg 43073). See {@link ChannelId#METER_CT_POSITION_RAW}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMeterCtPositionRaw() {
		return this.getMeterCtPositionRawChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#METER_CT_IN_GRID}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getMeterCtInGridChannel() {
		return this.channel(ChannelId.METER_CT_IN_GRID);
	}

	/**
	 * true if CT is in grid position (BIT02). See {@link ChannelId#METER_CT_IN_GRID}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getMeterCtInGrid() {
		return this.getMeterCtInGridChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#METER_EPM_SWITCH}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getMeterEpmSwitchChannel() {
		return this.channel(ChannelId.METER_EPM_SWITCH);
	}

	/**
	 * true if EPM is enabled (BIT04). See {@link ChannelId#METER_EPM_SWITCH}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getMeterEpmSwitch() {
		return this.getMeterEpmSwitchChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#METER_FAILSAFE_SWITCH}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getMeterFailsafeSwitchChannel() {
		return this.channel(ChannelId.METER_FAILSAFE_SWITCH);
	}

	/**
	 * true if failsafe is enabled (BIT05). See {@link ChannelId#METER_FAILSAFE_SWITCH}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getMeterFailsafeSwitch() {
		return this.getMeterFailsafeSwitchChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#METER_CT_SELECTION}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getMeterCtSelectionChannel() {
		return this.channel(ChannelId.METER_CT_SELECTION);
	}

	/**
	 * false=meter, true=CT for grid side (BIT13). See {@link ChannelId#METER_CT_SELECTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMeterCtSelection() {
		return this.getMeterCtSelectionChannel().value();
	}
}