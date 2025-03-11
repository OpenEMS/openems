package io.openems.edge.batteryinverter.victron.ro;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.victron.VictronBattery;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.OffGridBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.batteryinverter.victron.statemachine.StateMachine.State;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.victron.enums.ActiveInactive;
import io.openems.edge.victron.enums.ActiveInputSource;
import io.openems.edge.victron.enums.BatteryState;

public interface VictronBatteryInverter extends OffGridBatteryInverter, ManagedSymmetricBatteryInverter,
		SymmetricBatteryInverter, OpenemsComponent, StartStoppable, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY)), //
		CCGX_RELAY1_STATE(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.ON_OFF)), //
		CCGX_RELAY2_STATE(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.ON_OFF)), //
		AC_PV_ON_OUTPUT_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY).unit(Unit.WATT)), //
		AC_PV_ON_OUTPUT_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_PV_ON_OUTPUT_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_PV_ON_INPUT_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_PV_ON_INPUT_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_PV_ON_INPUT_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_CONSUMPTION_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_CONSUMPTION_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_CONSUMPTION_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		GRID_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		GRID_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		GRID_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_GENSET_POWER_L1(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_GENSET_POWER_L2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		AC_GENSET_POWER_L3(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		ACTIVE_INPUT_SOURCE(Doc.of(ActiveInputSource.values()) //
				.accessMode(AccessMode.READ_ONLY)),
		DC_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.VOLT)), //
		DC_BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE)), //
		BATTERY_SOC(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.PERCENT)), //
		BATTERY_STATE(Doc.of(BatteryState.values())//
				.accessMode(AccessMode.READ_ONLY)), //
		BATTERY_CONSUMED_AMPHOURS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE_HOURS)), //
		BATTERY_TIME_TO_GO(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.SECONDS)), //
		DC_PV_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.WATT)), //
		DC_PV_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE)), //
		CHARGER_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		DC_SYSTEM_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		VE_BUS_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.AMPERE)),
		VE_BUS_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)//
				.unit(Unit.WATT)), //
		ESS_CONTROL_LOOP_SETPOINT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)), //
		ESS_MAX_CHARGE_CURRENT_PERCENTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.PERCENT)), //
		ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.PERCENT)), //
		ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)), //
		ESS_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)), //
		SYSTEM_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.AMPERE)), //
		MAX_FEED_IN_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)), //
		FEED_EXCESS_DC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		DONT_FEED_EXCESS_AC(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE)), //
		PV_POWER_LIMITER_ACTIVE(Doc.of(ActiveInactive.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		MAX_CHARGE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.VOLT))

		/*
		, //
		SET_ACTIVE_POWER_L1(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_ACTIVE_POWER_L2(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_ACTIVE_POWER_L3(Doc.of(OpenemsType.SHORT)//
				.unit(Unit.WATT)//
				.accessMode(AccessMode.READ_WRITE)) //
		*/
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
/** separate enum file
	public enum Type {
		Multiplus2GX3kVa("Multiplus II 3kVA Single Phase", -2400, 2400, 2400), //
		Multiplus2GX5kVa("Multiplus II 5kVA Single Phase", -4000, 4000, 4000), //
		Multiplus2GX8kVa("Multiplus II 8kVA Single Phase", -6400, 6400, 6400), //
		Multiplus2GX10kVa("Multiplus II 10kVA Single Phase", -8000, 8000, 8000), //
		Multiplus2GX15kVa("Multiplus II 10kVA Single Phase", -12000, 12000, 12000), //
		Multiplus2GX3kVaL1L2L3("Multiplus II 3kVA Three Phase System", -2400 * 3, 2400 * 3, 2400 * 3), //
		Multiplus2GX5kVaL1L2L3("Multiplus II 5kVA Three Phase System", -4000 * 3, 4000 * 3, 4000 * 3), //
		Multiplus2GX8kVaL1L2L3("Multiplus II 8kVA Three Phase System", -6400 * 3, 6400 * 3, 6400 * 3), //
		Multiplus2GX10kVaL1L2L3("Multiplus II 10kVA Three Phase System", -8000 * 3, 8000 * 3, 8000 * 3),
		Multiplus2GX15kVaL1L2L3("Multiplus II 10kVA Three Phase System", -12000 * 3, 12000 * 3, 12000 * 3)

		;

		private final int acInputLimit;
		private final int acOutputLimit;
		private final String displayName;
		private final int apparentPowerLimit;

		Type(String displayName, int acInputLimit, int acOutputLimit, int apparentPowerLimit) {
			this.displayName = displayName;
			this.acInputLimit = acInputLimit;
			this.acOutputLimit = acOutputLimit;
			this.apparentPowerLimit = apparentPowerLimit;
		}

		public int getAcInputLimit() {
			return this.acInputLimit;
		}

		public int getAcOutputLimit() {
			return this.acOutputLimit;
		}

		public String getDisplayName() {
			return this.displayName;
		}

		public int getApparentPowerLimit() {
			return this.apparentPowerLimit;
		}
	}
*/
	// ###########################
	// AC_PV_ON_OUTPUT_POWER_L1
	public default Value<Integer> getAcPvOnOutputPowerL1() {
	    return this.getAcPvOnOutputPowerL1Channel().value();
	}

	public default IntegerReadChannel getAcPvOnOutputPowerL1Channel() {
	    return this.channel(ChannelId.AC_PV_ON_OUTPUT_POWER_L1);
	}

	// AC_PV_ON_OUTPUT_POWER_L2
	public default Value<Integer> getAcPvOnOutputPowerL2() {
	    return this.getAcPvOnOutputPowerL2Channel().value();
	}

	public default IntegerReadChannel getAcPvOnOutputPowerL2Channel() {
	    return this.channel(ChannelId.AC_PV_ON_OUTPUT_POWER_L2);
	}

	// AC_PV_ON_OUTPUT_POWER_L3
	public default Value<Integer> getAcPvOnOutputPowerL3() {
	    return this.getAcPvOnOutputPowerL3Channel().value();
	}

	public default IntegerReadChannel getAcPvOnOutputPowerL3Channel() {
	    return this.channel(ChannelId.AC_PV_ON_OUTPUT_POWER_L3);
	}

	// AC_PV_ON_INPUT_POWER_L1
	public default Value<Integer> getAcPvOnInputPowerL1() {
	    return this.getAcPvOnInputPowerL1Channel().value();
	}

	public default IntegerReadChannel getAcPvOnInputPowerL1Channel() {
	    return this.channel(ChannelId.AC_PV_ON_INPUT_POWER_L1);
	}

	// AC_PV_ON_INPUT_POWER_L2
	public default Value<Integer> getAcPvOnInputPowerL2() {
	    return this.getAcPvOnInputPowerL2Channel().value();
	}

	public default IntegerReadChannel getAcPvOnInputPowerL2Channel() {
	    return this.channel(ChannelId.AC_PV_ON_INPUT_POWER_L2);
	}

	// AC_PV_ON_INPUT_POWER_L3
	public default Value<Integer> getAcPvOnInputPowerL3() {
	    return this.getAcPvOnInputPowerL3Channel().value();
	}

	public default IntegerReadChannel getAcPvOnInputPowerL3Channel() {
	    return this.channel(ChannelId.AC_PV_ON_INPUT_POWER_L3);
	}

	// AC_CONSUMPTION_POWER_L1
	public default Value<Integer> getAcConsumptionPowerL1() {
	    return this.getAcConsumptionPowerL1Channel().value();
	}

	public default IntegerReadChannel getAcConsumptionPowerL1Channel() {
	    return this.channel(ChannelId.AC_CONSUMPTION_POWER_L1);
	}

	// AC_CONSUMPTION_POWER_L2
	public default Value<Integer> getAcConsumptionPowerL2() {
	    return this.getAcConsumptionPowerL2Channel().value();
	}

	public default IntegerReadChannel getAcConsumptionPowerL2Channel() {
	    return this.channel(ChannelId.AC_CONSUMPTION_POWER_L2);
	}

	// AC_CONSUMPTION_POWER_L3
	public default Value<Integer> getAcConsumptionPowerL3() {
	    return this.getAcConsumptionPowerL3Channel().value();
	}

	public default IntegerReadChannel getAcConsumptionPowerL3Channel() {
	    return this.channel(ChannelId.AC_CONSUMPTION_POWER_L3);
	}

	// GRID_POWER_L1
	public default Value<Integer> getGridPowerL1() {
	    return this.getGridPowerL1Channel().value();
	}

	public default IntegerReadChannel getGridPowerL1Channel() {
	    return this.channel(ChannelId.GRID_POWER_L1);
	}

	// GRID_POWER_L2
	public default Value<Integer> getGridPowerL2() {
	    return this.getGridPowerL2Channel().value();
	}

	public default IntegerReadChannel getGridPowerL2Channel() {
	    return this.channel(ChannelId.GRID_POWER_L2);
	}

	// GRID_POWER_L3
	public default Value<Integer> getGridPowerL3() {
	    return this.getGridPowerL3Channel().value();
	}

	public default IntegerReadChannel getGridPowerL3Channel() {
	    return this.channel(ChannelId.GRID_POWER_L3);
	}

	// AC_GENSET_POWER_L1
	public default Value<Integer> getAcGensetPowerL1() {
	    return this.getAcGensetPowerL1Channel().value();
	}

	public default IntegerReadChannel getAcGensetPowerL1Channel() {
	    return this.channel(ChannelId.AC_GENSET_POWER_L1);
	}

	// AC_GENSET_POWER_L2
	public default Value<Integer> getAcGensetPowerL2() {
	    return this.getAcGensetPowerL2Channel().value();
	}

	public default IntegerReadChannel getAcGensetPowerL2Channel() {
	    return this.channel(ChannelId.AC_GENSET_POWER_L2);
	}

	// AC_GENSET_POWER_L3
	public default Value<Integer> getAcGensetPowerL3() {
	    return this.getAcGensetPowerL3Channel().value();
	}

	public default IntegerReadChannel getAcGensetPowerL3Channel() {
	    return this.channel(ChannelId.AC_GENSET_POWER_L3);
	}

	// DC_BATTERY_VOLTAGE
	public default Value<Integer> getDcBatteryVoltage() {
	    return this.getDcBatteryVoltageChannel().value();
	}

	public default IntegerReadChannel getDcBatteryVoltageChannel() {
	    return this.channel(ChannelId.DC_BATTERY_VOLTAGE);
	}

	// DC_BATTERY_CURRENT
	public default Value<Integer> getDcBatteryCurrent() {
	    return this.getDcBatteryCurrentChannel().value();
	}

	public default IntegerReadChannel getDcBatteryCurrentChannel() {
	    return this.channel(ChannelId.DC_BATTERY_CURRENT);
	}

	// BATTERY_SOC
	public default Value<Integer> getBatterySoc() {
	    return this.getBatterySocChannel().value();
	}

	public default IntegerReadChannel getBatterySocChannel() {
	    return this.channel(ChannelId.BATTERY_SOC);
	}

	// BATTERY_CONSUMED_AMPHOURS
	public default Value<Integer> getBatteryConsumedAmphours() {
	    return this.getBatteryConsumedAmphoursChannel().value();
	}

	public default IntegerReadChannel getBatteryConsumedAmphoursChannel() {
	    return this.channel(ChannelId.BATTERY_CONSUMED_AMPHOURS);
	}

	// BATTERY_TIME_TO_GO
	public default Value<Integer> getBatteryTimeToGo() {
	    return this.getBatteryTimeToGoChannel().value();
	}

	public default IntegerReadChannel getBatteryTimeToGoChannel() {
	    return this.channel(ChannelId.BATTERY_TIME_TO_GO);
	}

	// DC_PV_POWER
	public default Value<Integer> getDcPvPower() {
	    return this.getDcPvPowerChannel().value();
	}

	public default IntegerReadChannel getDcPvPowerChannel() {
	    return this.channel(ChannelId.DC_PV_POWER);
	}

	// DC_PV_CURRENT
	public default Value<Integer> getDcPvCurrent() {
	    return this.getDcPvCurrentChannel().value();
	}

	public default IntegerReadChannel getDcPvCurrentChannel() {
	    return this.channel(ChannelId.DC_PV_CURRENT);
	}

	// CHARGER_POWER
	public default Value<Integer> getChargerPower() {
	    return this.getChargerPowerChannel().value();
	}

	public default IntegerReadChannel getChargerPowerChannel() {
	    return this.channel(ChannelId.CHARGER_POWER);
	}

	// DC_SYSTEM_POWER
	public default Value<Integer> getDcSystemPower() {
	    return this.getDcSystemPowerChannel().value();
	}

	public default IntegerReadChannel getDcSystemPowerChannel() {
	    return this.channel(ChannelId.DC_SYSTEM_POWER);
	}

	// VE_BUS_CHARGE_CURRENT
	public default Value<Integer> getVeBusChargeCurrent() {
	    return this.getVeBusChargeCurrentChannel().value();
	}

	public default IntegerReadChannel getVeBusChargeCurrentChannel() {
	    return this.channel(ChannelId.VE_BUS_CHARGE_CURRENT);
	}

	// VE_BUS_CHARGE_POWER
	public default Value<Integer> getVeBusChargePower() {
	    return this.getVeBusChargePowerChannel().value();
	}

	public default IntegerReadChannel getVeBusChargePowerChannel() {
	    return this.channel(ChannelId.VE_BUS_CHARGE_POWER);
	}

	// ESS_CONTROL_LOOP_SETPOINT
	public default Value<Integer> getEssControlLoopSetpoint() {
	    return this.getEssControlLoopSetpointChannel().value();
	}

	public default IntegerReadChannel getEssControlLoopSetpointChannel() {
	    return this.channel(ChannelId.ESS_CONTROL_LOOP_SETPOINT);
	}

	// ESS_MAX_CHARGE_CURRENT_PERCENTAGE
	public default Value<Integer> getEssMaxChargeCurrentPercentage() {
	    return this.getEssMaxChargeCurrentPercentageChannel().value();
	}

	public default IntegerReadChannel getEssMaxChargeCurrentPercentageChannel() {
	    return this.channel(ChannelId.ESS_MAX_CHARGE_CURRENT_PERCENTAGE);
	}

	// ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE
	public default Value<Integer> getEssMaxDischargeCurrentPercentage() {
	    return this.getEssMaxDischargeCurrentPercentageChannel().value();
	}

	public default IntegerReadChannel getEssMaxDischargeCurrentPercentageChannel() {
	    return this.channel(ChannelId.ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE);
	}

	// ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2
	public default Value<Integer> getEssControlLoopSetpointScaleFactor2() {
	    return this.getEssControlLoopSetpointScaleFactor2Channel().value();
	}

	public default IntegerReadChannel getEssControlLoopSetpointScaleFactor2Channel() {
	    return this.channel(ChannelId.ESS_CONTROL_LOOP_SETPOINT_SCALE_FACTOR_2);
	}

	// ESS_MAX_DISCHARGE_POWER
	public default Value<Integer> getEssMaxDischargePower() {
	    return this.getEssMaxDischargePowerChannel().value();
	}

	public default IntegerReadChannel getEssMaxDischargePowerChannel() {
	    return this.channel(ChannelId.ESS_MAX_DISCHARGE_POWER);
	}

	// SYSTEM_MAX_CHARGE_CURRENT
	public default Value<Integer> getSystemMaxChargeCurrent() {
	    return this.getSystemMaxChargeCurrentChannel().value();
	}

	public default IntegerReadChannel getSystemMaxChargeCurrentChannel() {
	    return this.channel(ChannelId.SYSTEM_MAX_CHARGE_CURRENT);
	}

	// MAX_FEED_IN_POWER
	public default Value<Integer> getMaxFeedInPower() {
	    return this.getMaxFeedInPowerChannel().value();
	}

	public default IntegerReadChannel getMaxFeedInPowerChannel() {
	    return this.channel(ChannelId.MAX_FEED_IN_POWER);
	}

	// MAX_CHARGE_VOLTAGE
	public default Value<Integer> getMaxChargeVoltage() {
	    return this.getMaxChargeVoltageChannel().value();
	}

	public default IntegerReadChannel getMaxChargeVoltageChannel() {
	    return this.channel(ChannelId.MAX_CHARGE_VOLTAGE);
	}

	public void setBattery(VictronBattery battery);

	public void unsetBattery(VictronBattery battery);

    //public VictronBattery getBattery();



    public Integer getMaxChargePower();
    public Integer getMaxDischargePower();

	@Override
	public default ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricBatteryInverter.getModbusSlaveNatureTable(accessMode), //
				this.getModbusSlaveNatureTable(accessMode)
		);
	}

	private ModbusSlaveNatureTable getModbusSlaveNatureTable(AccessMode accessMode) {
		return ModbusSlaveNatureTable.of(VictronBatteryInverter.class, accessMode, 200) //
				.channel(0, ChannelId.ESS_MAX_CHARGE_CURRENT_PERCENTAGE, ModbusType.UINT16) //
				.channel(1, ChannelId.ESS_MAX_DISCHARGE_CURRENT_PERCENTAGE, ModbusType.UINT16) //
				.channel(2, ChannelId.ESS_MAX_DISCHARGE_POWER, ModbusType.UINT16) //
				.build();
	}

	public boolean calculateHardwareLimits();






}
