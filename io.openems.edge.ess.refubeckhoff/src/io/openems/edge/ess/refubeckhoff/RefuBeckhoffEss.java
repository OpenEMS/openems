package io.openems.edge.ess.refubeckhoff;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.refubeckhoff.enums.BatteryMode;
import io.openems.edge.ess.refubeckhoff.enums.BatteryState;
import io.openems.edge.ess.refubeckhoff.enums.DcdcStatus;
import io.openems.edge.ess.refubeckhoff.enums.SetOperationMode;
import io.openems.edge.ess.refubeckhoff.enums.StopStart;
import io.openems.edge.ess.refubeckhoff.enums.SystemState;

public interface RefuBeckhoffEss
		extends SymmetricEss, ManagedSymmetricEss, EventHandler, OpenemsComponent, ModbusSlave {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// EnumWriteChannel
		SYSTEM_ERROR_RESET(Doc.of(StopStart.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		OPERATION_MODE(Doc.of(SetOperationMode.values()) //
				.accessMode(AccessMode.READ_ONLY)), //
		WORK_STATE(Doc.of(StopStart.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		SET_SYSTEM_ERROR_RESET(Doc.of(StopStart.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_OPERATION_MODE(Doc.of(SetOperationMode.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_WORK_STATE(Doc.of(StopStart.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// EnumReadChannel
		BATTERY_STATE(Doc.of(BatteryState.values())), //
		BATTERY_MODE(Doc.of(BatteryMode.values())), //
		SYSTEM_STATE(Doc.of(SystemState.values())), //
		DCDC_STATUS(Doc.of(DcdcStatus.values())), //

		// IntegerReadChannel
		ERROR_HANDLER_STATE(Doc.of(OpenemsType.INTEGER)), //
		INVERTER_ERROR_CODE(Doc.of(OpenemsType.INTEGER)), //
		DCDC_ERROR_CODE(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BATTERY_VOLTAGE_PCS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT_PCS(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		PCS_ALLOWED_CHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		PCS_ALLOWED_DISCHARGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		BATTERY_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		BATTERY_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS)), //
		BATTERY_HIGHEST_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_LOWEST_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_HIGHEST_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_LOWEST_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_STOP_REQUEST(Doc.of(OpenemsType.INTEGER)), //
		CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //

		COS_PHI_3P(Doc.of(OpenemsType.INTEGER)), //
		COS_PHI_L1(Doc.of(OpenemsType.INTEGER)), //
		COS_PHI_L2(Doc.of(OpenemsType.INTEGER)), //
		COS_PHI_L3(Doc.of(OpenemsType.INTEGER)), //

		STATE_BMS_ERROR(Doc.of(Level.FAULT) //
				.text("BMS In Error")), //
		STATE_BMS_OVERVOLTAGE(Doc.of(Level.FAULT) //
				.text("BMS Overvoltage")), //
		STATE_BMS_UNDERVOLTAGE(Doc.of(Level.FAULT) //
				.text("BMS Undervoltage")), //
		STATE_BMS_OVERCURRENT(Doc.of(Level.FAULT) //
				.text("BMS Overcurrent")), //
		STATE_BMS_LIMIT_NOT_INITIALIZED(Doc.of(Level.FAULT) //
				.text("Error BMS Limits Not Initialized")), //
		STATE_CONNECT_ERROR(Doc.of(Level.FAULT) //
				.text("Connect Error")), //
		STATE_OVERVOLTAGE_WARNING(Doc.of(Level.INFO) //
				.text("Overvoltage Warning")), //
		STATE_UNDERVOLTAGE_WARNING(Doc.of(Level.INFO) //
				.text("Undervoltage Warning")), //
		STATE_OVERCURRENT_WARNING(Doc.of(Level.INFO) //
				.text("Overcurrent Warning")), //
		STATE_BMS_READY(Doc.of(Level.INFO) //
				.text("BMS Ready")), //
		STATE_TREX_READY(Doc.of(Level.INFO) //
				.text("TREX Ready")), //

		STATE_GATEWAY_INITIALIZED(Doc.of(Level.WARNING) //
				.text("Gateway Initialized")), //
		STATE_MODBUS_SLAVE_STATUS(Doc.of(Level.WARNING) //
				.text("Modbus Slave Status")), //
		STATE_MODBUS_MASTER_STATUS(Doc.of(Level.WARNING) //
				.text("Modbus Master Status")), //
		STATE_CAN_TIMEOUT(Doc.of(Level.WARNING) //
				.text("CAN Timeout")), //
		STATE_FIRST_COMMNUICATION_OK(Doc.of(Level.WARNING) //
				.text("First Communication Ok")), //

		INVERTER_STATE_READYTOPOWERON(Doc.of(OpenemsType.BOOLEAN) //
				.text("Ready to Power on")), //
		INVERTER_STATE_READYFOROPERATING(Doc.of(OpenemsType.BOOLEAN) //
				.text("Ready for Operating")), //
		INVERTER_STATE_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Enabled")), //
		INVERTER_STATE_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.text("Inverter Fault")), //
		INVERTER_STATE_WARNING(Doc.of(Level.INFO) //
				.text("Inverter Warning")), //
		INVERTER_STATE_VOLTAGE_CURRENT_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Voltage/Current mode")), //
		INVERTER_STATE_POWER_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Power mode")), //
		INVERTER_STATE_STATUS_AC_RELAYS(Doc.of(OpenemsType.BOOLEAN) //
				.text("Status AC relays (1:Close, 0:Open)")), //
		INVERTER_STATE_STATUS_DC_1_RELAYS(Doc.of(OpenemsType.BOOLEAN) //
				.text("Status DC relay 1 (1:Close, 0:Open)")), //
		INVERTER_STATE__STATUS_DC_2_RELAYS(Doc.of(OpenemsType.BOOLEAN) //
				.text("Status DC relay 2 (1:Close, 0:Open)")), //
		INVERTER_STATE_MAINS_OK(Doc.of(OpenemsType.BOOLEAN) //
				.text("Mains OK")), //

		DCDC_STATE_READYTOPOWERON(Doc.of(OpenemsType.BOOLEAN) //
				.text("Ready to Power on")), //
		DCDC_STATE_READYFOROPERATING(Doc.of(OpenemsType.BOOLEAN) //
				.text("Ready for Operating")), //
		DCDC_STATE_ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.text("Enabled")), //
		DCDC_STATE_DCDC_FAULT(Doc.of(OpenemsType.BOOLEAN) //
				.text("DCDC Fault")), //
		DCDC_STATE_DCDC_WARNING(Doc.of(Level.INFO) //
				.text("DCDC Warning")), //
		DCDC_STATE_VOLTAGE_CURRENT_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Voltage/Current mode")), //
		DCDC_STATE_POWER_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.text("Power mode")), //

		BATTERY_ON_GRID_STATE_0(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 0")), //
		BATTERY_ON_GRID_STATE_1(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 1")), //
		BATTERY_ON_GRID_STATE_2(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 2")), //
		BATTERY_ON_GRID_STATE_3(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 3")), //
		BATTERY_ON_GRID_STATE_4(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 4")), //
		BATTERY_ON_GRID_STATE_5(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 5")), //
		BATTERY_ON_GRID_STATE_6(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 6")), //
		BATTERY_ON_GRID_STATE_7(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 7")), //
		BATTERY_ON_GRID_STATE_8(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 8")), //
		BATTERY_ON_GRID_STATE_9(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 9")), //
		BATTERY_ON_GRID_STATE_10(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 10")), //
		BATTERY_ON_GRID_STATE_11(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 11")), //
		BATTERY_ON_GRID_STATE_12(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 12")), //
		BATTERY_ON_GRID_STATE_13(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 13")), //
		BATTERY_ON_GRID_STATE_14(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 14")), //
		BATTERY_ON_GRID_STATE_15(Doc.of(OpenemsType.BOOLEAN) //
				.text("On-grid status of battery group 15")), //

		STATE_16(Doc.of(Level.INFO) //
				.text("Normal Charging Over Current")), //
		STATE_17(Doc.of(Level.INFO) //
				.text("Charging Current Over Limit")), //
		STATE_18(Doc.of(Level.INFO) //
				.text("Discharging Current Over Limit")), //
		STATE_19(Doc.of(Level.INFO) //
				.text("Normal High Voltage")), //
		STATE_20(Doc.of(Level.INFO) //
				.text("Normal Low Voltage")), //
		STATE_21(Doc.of(Level.INFO) //
				.text("Abnormal Voltage Variation")), //
		STATE_22(Doc.of(Level.INFO) //
				.text("Normal High Temperature")), //
		STATE_23(Doc.of(Level.INFO) //
				.text("Normal Low Temperature")), //
		STATE_24(Doc.of(Level.INFO) //
				.text("Abnormal Temperature Variation")), //
		STATE_25(Doc.of(Level.INFO) //
				.text("Serious High Voltage")), //
		STATE_26(Doc.of(Level.INFO) //
				.text("Serious Low Voltage")), //
		STATE_27(Doc.of(Level.INFO) //
				.text("Serious Low Temperature")), //
		STATE_28(Doc.of(Level.INFO) //
				.text("Charging Serious Over Current")), //
		STATE_29(Doc.of(Level.INFO) //
				.text("Discharging Serious Over Current")), //
		STATE_30(Doc.of(Level.INFO) //
				.text("Abnormal Capacity Alarm")), //

		STATE_31(Doc.of(Level.INFO) //
				.text("EEPROM Parameter Failure")), //
		STATE_32(Doc.of(Level.INFO) //
				.text("Switch Of Inside Combined Cabinet")), //
		STATE_33(Doc.of(Level.INFO) //
				.text("Should Not Be Connected To Grid Due To The DC Side Condition")), //
		STATE_34(Doc.of(Level.INFO) //
				.text("Emergency Stop Require From System Controller")), //

		STATE_35(Doc.of(Level.INFO) //
				.text("Battery Group 1 Enable And Not Connected To Grid")), //
		STATE_36(Doc.of(Level.INFO) //
				.text("Battery Group 2 Enable And Not Connected To Grid")), //
		STATE_37(Doc.of(Level.INFO) //
				.text("Battery Group 3 Enable And Not Connected To Grid")), //
		STATE_38(Doc.of(Level.INFO) //
				.text("Battery Group 4 Enable And Not Connected To Grid")), //

		STATE_39(Doc.of(Level.INFO) //
				.text("TheIsolationSwitchOfBatteryGroup1Open")), //
		STATE_40(Doc.of(Level.INFO) //
				.text("TheIsolationSwitchOfBatteryGroup2Open")), //
		STATE_41(Doc.of(Level.INFO) //
				.text("TheIsolationSwitchOfBatteryGroup3Open")), //
		STATE_42(Doc.of(Level.INFO) //
				.text("TheIsolationSwitchOfBatteryGroup4Open")), //

		STATE_43(Doc.of(Level.INFO) //
				.text("BalancingSamplingFailureOfBatteryGroup1")), //
		STATE_44(Doc.of(Level.INFO) //
				.text("BalancingSamplingFailureOfBatteryGroup2")), //
		STATE_45(Doc.of(Level.INFO) //
				.text("BalancingSamplingFailureOfBatteryGroup3")), //
		STATE_46(Doc.of(Level.INFO) //
				.text("BalancingSamplingFailureOfBatteryGroup4")), //

		STATE_47(Doc.of(Level.INFO) //
				.text("BalancingControlFailureOfBatteryGroup1")), //
		STATE_48(Doc.of(Level.INFO) //
				.text("BalancingControlFailureOfBatteryGroup2")), //
		STATE_49(Doc.of(Level.INFO) //
				.text("BalancingControlFailureOfBatteryGroup3")), //
		STATE_50(Doc.of(Level.INFO) //
				.text("BalancingControlFailureOfBatteryGroup4")), //

		STATE_51(Doc.of(Level.WARNING) //
				.text("NoEnableBateryGroupOrUsableBatteryGroup")), //
		STATE_52(Doc.of(Level.WARNING) //
				.text("NormalLeakageOfBatteryGroup")), //
		STATE_53(Doc.of(Level.WARNING) //
				.text("SeriousLeakageOfBatteryGroup")), //
		STATE_54(Doc.of(Level.WARNING) //
				.text("BatteryStartFailure")), //
		STATE_55(Doc.of(Level.WARNING) //
				.text("BatteryStopFailure")), //
		STATE_56(Doc.of(Level.WARNING) //
				.text("InterruptionOfCANCommunicationBetweenBatteryGroupAndController")), //
		STATE_57(Doc.of(Level.WARNING) //
				.text("EmergencyStopAbnormalOfAuxiliaryCollector")), //
		STATE_58(Doc.of(Level.WARNING) //
				.text("LeakageSelfDetectionOnNegative")), //
		STATE_59(Doc.of(Level.WARNING) //
				.text("LeakageSelfDetectionOnPositive")), //
		STATE_60(Doc.of(Level.WARNING) //
				.text("SelfDetectionFailureOnBattery")), //

		STATE_61(Doc.of(Level.WARNING) //
				.text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup1")), //
		STATE_62(Doc.of(Level.WARNING) //
				.text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup2")), //
		STATE_63(Doc.of(Level.WARNING) //
				.text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup3")), //
		STATE_64(Doc.of(Level.WARNING) //
				.text("CANCommunicationInterruptionBetweenBatteryGroupAndGroup4")), //

		STATE_65(Doc.of(Level.WARNING) //
				.text("MainContractorAbnormalInBatterySelfDetectGroup1")), //
		STATE_66(Doc.of(Level.WARNING) //
				.text("MainContractorAbnormalInBatterySelfDetectGroup2")), //
		STATE_67(Doc.of(Level.WARNING) //
				.text("MainContractorAbnormalInBatterySelfDetectGroup3")), //
		STATE_68(Doc.of(Level.WARNING) //
				.text("MainContractorAbnormalInBatterySelfDetectGroup4")), //

		STATE_69(Doc.of(Level.WARNING) //
				.text("PreChargeContractorAbnormalOnBatterySelfDetectGroup1")), //
		STATE_70(Doc.of(Level.WARNING) //
				.text("PreChargeContractorAbnormalOnBatterySelfDetectGroup2")), //
		STATE_71(Doc.of(Level.WARNING) //
				.text("PreChargeContractorAbnormalOnBatterySelfDetectGroup3")), //
		STATE_72(Doc.of(Level.WARNING) //
				.text("PreChargeContractorAbnormalOnBatterySelfDetectGroup4")), //

		STATE_73(Doc.of(Level.WARNING) //
				.text("MainContactFailureOnBatteryControlGroup1")), //
		STATE_74(Doc.of(Level.WARNING) //
				.text("MainContactFailureOnBatteryControlGroup2")), //
		STATE_75(Doc.of(Level.WARNING) //
				.text("MainContactFailureOnBatteryControlGroup3")), //
		STATE_76(Doc.of(Level.WARNING) //
				.text("MainContactFailureOnBatteryControlGroup4")), //

		STATE_77(Doc.of(Level.WARNING) //
				.text("PreChargeFailureOnBatteryControlGroup1")), //
		STATE_78(Doc.of(Level.WARNING) //
				.text("PreChargeFailureOnBatteryControlGroup2")), //
		STATE_79(Doc.of(Level.WARNING) //
				.text("PreChargeFailureOnBatteryControlGroup3")), //
		STATE_80(Doc.of(Level.WARNING) //
				.text("PreChargeFailureOnBatteryControlGroup4")), //

		STATE_81(Doc.of(Level.WARNING) //
				.text("SamplingCircuitAbnormalForBMU")), //
		STATE_82(Doc.of(Level.WARNING) //
				.text("PowerCableDisconnectFailure")), //
		STATE_83(Doc.of(Level.WARNING) //
				.text("SamplingCircuitDisconnectFailure")), //
		STATE_84(Doc.of(Level.WARNING) //
				.text("CANDisconnectForMasterAndSlave")), //
		STATE_85(Doc.of(Level.WARNING) //
				.text("SammplingCircuitFailure")), //
		STATE_86(Doc.of(Level.FAULT) //
				.text("SingleBatteryFailure")), //
		STATE_87(Doc.of(Level.WARNING) //
				.text("CircuitDetectionAbnormalForMainContactor")), //
		STATE_88(Doc.of(Level.WARNING) //
				.text("CircuitDetectionAbnormalForMainContactorSecond")), //
		STATE_89(Doc.of(Level.WARNING) //
				.text("CircuitDetectionAbnormalForFancontactor")), //
		STATE_90(Doc.of(Level.WARNING) //
				.text("BMUPowerContactorCircuitDetectionAbnormal")), //
		STATE_91(Doc.of(Level.WARNING) //
				.text("CentralContactorCircuitDetectionAbnormal")), // 3

		STATE_92(Doc.of(Level.FAULT) //
				.text("SeriousTemperatureFault")), //
		STATE_93(Doc.of(Level.WARNING) //
				.text("CommunicationFaultForSystemController")), //
		STATE_94(Doc.of(Level.WARNING) //
				.text("FrogAlarm")), //
		STATE_95(Doc.of(Level.FAULT) //
				.text("FuseFault")), //
		STATE_96(Doc.of(Level.WARNING) //
				.text("NormalLeakage")), //
		STATE_97(Doc.of(Level.FAULT) //
				.text("SeriousLeakage")), //
		STATE_98(Doc.of(Level.WARNING) //
				.text("CANDisconnectionBetweenBatteryGroupAndBatteryStack")), //
		STATE_99(Doc.of(Level.WARNING) //
				.text("CentralContactorCircuitOpen")), //
		STATE_100(Doc.of(Level.FAULT) //
				.text("BMUPowerContactorOpen")), //

		BATTERY_CONTROL_STATE_0(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 0")), //
		BATTERY_CONTROL_STATE_1(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 1")), //
		BATTERY_CONTROL_STATE_2(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 2")), //
		BATTERY_CONTROL_STATE_3(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 3")), //
		BATTERY_CONTROL_STATE_4(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 4")), //
		BATTERY_CONTROL_STATE_5(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 5")), //
		BATTERY_CONTROL_STATE_6(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 6")), //
		BATTERY_CONTROL_STATE_7(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 7")), //
		BATTERY_CONTROL_STATE_8(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 8")), //
		BATTERY_CONTROL_STATE_9(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 9")), //
		BATTERY_CONTROL_STATE_10(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 10")), //
		BATTERY_CONTROL_STATE_11(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 11")), //
		BATTERY_CONTROL_STATE_12(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 12")), //
		BATTERY_CONTROL_STATE_13(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 13")), //
		BATTERY_CONTROL_STATE_14(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 14")), //
		BATTERY_CONTROL_STATE_15(Doc.of(OpenemsType.BOOLEAN) //
				.text("Control Status Battery Group 15")), //

		ERROR_LOG_0(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_1(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_2(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_3(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_4(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_5(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_6(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_7(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_8(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_9(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_10(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_11(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_12(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_13(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_14(Doc.of(OpenemsType.INTEGER)), //
		ERROR_LOG_15(Doc.of(OpenemsType.INTEGER));

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
