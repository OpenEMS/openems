package io.openems.edge.pytes.ess;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.LOW;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.enums.Appendix2;
import io.openems.edge.pytes.enums.Appendix8;
import io.openems.edge.pytes.enums.EnableDisable;
import io.openems.edge.pytes.enums.RemoteDispatchRealtimeControlSwitch;
import io.openems.edge.pytes.enums.RemoteDispatchSystemLimitSwitch;
import io.openems.edge.pytes.enums.StandardWorkingMode;
import io.openems.common.channel.Level;

public interface PytesJs3 extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		STARTER_BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.VOLT).persistencePriority(LOW)),

		INVERTED_RATED_APPARENT_POWER(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.VOLT_AMPERE)),

		// Add details and comments beyond this line
		SAFETY_VERSION(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
		),

		// Add details and comments beyond this line

		HMI_SUB_VERSION(Doc.of(INTEGER) // Version number of the HMI
		// .accessMode(READ_ONLY)
		),

		/*
		 * Add Alarm code data to distinguishing displayed Alarm code For external fan
		 * failure, each bit indicates the status of one fan; In conjunction with the
		 * 33095 register address, it is used for subdivided fault information display.
		 * Example: 33095 register read information is 0x1020,
		 */

		ALARM_CODE_DATA(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		DC_BUS_VOLTAGE(Doc.of(INTEGER).accessMode(READ_ONLY).unit(Unit.MILLIVOLT)),

		DC_BUS_HALF_VOLTAGE(Doc.of(INTEGER).accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT).persistencePriority(LOW)),

		APPARENT_POWER(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		INVERTER_CURRENT_STATUS(Doc.of(Appendix2.values()).accessMode(AccessMode.READ_ONLY)),

		LEAD_ACID_BATTERY_TEMP(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FUNCTION_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		CURRENT_DRM_CODE_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		INVERTER_CABINET_TEMP(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		LIMITED_POWER_ACTUAL_VALUE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		PF_ADJUSTMENT_ACTUAL_VALUE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		LIMITED_REACTIVE_POWER(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		INVERTER_MODULE_TEMP2(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		VOLT_VAR_VREF_RT_VALUES(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		BMS_CHARGING_VOLTAGE_LIMIT(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		BATTERY_BMS_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		INVERTER_INITIAL_SETTING_STATE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		BATCH_UPGRADE_BOWL(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		FCAS_MODE_RUNNING_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),
		
		SET_PV_SHUTDOWN_SWITCH(Doc.of(OpenemsType.BOOLEAN).accessMode(WRITE_ONLY).text("PV shutdown mode")),
		SET_GRID_CHARGE_ALLOWED(Doc.of(OpenemsType.BOOLEAN).accessMode(WRITE_ONLY).text("Battery grid charge allowed")),
		SET_DO_CONTROL(Doc.of(OpenemsType.BOOLEAN).accessMode(WRITE_ONLY).text("DO Control enabled")),
		SET_OFF_GRID_BATTERY_STANDBY(Doc.of(OpenemsType.BOOLEAN).accessMode(WRITE_ONLY).text("Off-grid battery standby")),
		
		PV_SHUTDOWN_SWITCH(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("PV shutdown mode")),
		GRID_CHARGE_ALLOWED(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery grid charge allowed")),
		DO_CONTROL(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("DO Control enabled")),
		OFF_GRID_BATTERY_STANDBY(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Off-grid battery standby")),		

		SETTING_FLAG_BIT(Doc.of(INTEGER).accessMode(READ_ONLY)),

		// ── Appendix 7 ── Register 33115 decoded bits ──
		SETTING_FLAG_FLASH_TIMEOUT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("FLASH read/write timeout")),
		SETTING_FLAG_CLEAR_ENERGY(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Clear energy flag")),
		SETTING_FLAG_RESERVED_02(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT02)")),
		SETTING_FLAG_RESERVED_03(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT03)")),
		SETTING_FLAG_RESERVED_04(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT04)")),
		SETTING_FLAG_RESERVED_05(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT05)")),
		SETTING_FLAG_RESERVED_06(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT06)")),
		SETTING_FLAG_RESERVED_07(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT07)")),
		SETTING_FLAG_RESET_DATALOGGER(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reset datalogger flag")),
		SETTING_FLAG_FACTORY_RECOVER(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Return factory setting of datalogger")),
		SETTING_FLAG_RESERVED_10(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT10)")),
		SETTING_FLAG_RESERVED_11(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT11)")),
		SETTING_FLAG_RESERVED_12(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT12)")),
		SETTING_FLAG_RESERVED_13(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT13)")),
		SETTING_FLAG_RESERVED_14(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT14)")),
		SETTING_FLAG_RESERVED_15(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (SETFLAG BIT15)")),

		// ── Appendix 4 ── raw bitmask registers (read as INTEGER, decoded in
		// handleEvent) ──
		FAULT_CODE_01(Doc.of(INTEGER).accessMode(READ_ONLY)), // Register 33116
		FAULT_CODE_02(Doc.of(INTEGER).accessMode(READ_ONLY)), // Register 33117
		FAULT_CODE_03(Doc.of(INTEGER).accessMode(READ_ONLY)), // Register 33118
		FAULT_CODE_04(Doc.of(INTEGER).accessMode(READ_ONLY)), // Register 33119
		FAULT_CODE_05(Doc.of(INTEGER).accessMode(READ_ONLY)), // Register 33120
		FAULT_CODE_06(Doc.of(INTEGER).accessMode(READ_ONLY)), // Register 33124
		FAULT_CODE_07(Doc.of(INTEGER).accessMode(READ_ONLY)), // Register 33125

		// ── Appendix 4 ── Register 33116 decoded bits ──
		FAULT_REG1_NO_GRID(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("No grid")),
		FAULT_REG1_GRID_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid overvoltage")),
		FAULT_REG1_GRID_UNDERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid undervoltage")),
		FAULT_REG1_GRID_OVERFREQ(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid overfrequency")),
		FAULT_REG1_GRID_UNDERFREQ(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid underfrequency")),
		FAULT_REG1_UNBALANCED_GRID(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Unbalanced grid")),
		FAULT_REG1_GRID_FREQ_FLUCTUATION(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid frequency fluctuation")),
		FAULT_REG1_GRID_REVERSE_CURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid reverse current")),
		FAULT_REG1_GRID_CURRENT_TRACKING_ERROR(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid current tracking error")),
		FAULT_REG1_METER_COM_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("METER COM fail")),
		FAULT_REG1_FAILSAFE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("FailSafe")),
		FAULT_REG1_METER_SELECT_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Meter Select Fail")),
		FAULT_REG1_EPM_HARD_LIMIT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("EPM Hard Limit Protection")),
		FAULT_REG1_G100_CURRENT_OVER_LIMIT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("G100 Current Over Limit")),
		FAULT_REG1_RESERVED_14(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG1 BIT14)")),
		FAULT_REG1_ABNORMAL_GRID_PHASE_POLARITY(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Abnormal grid phase polarity")),

		// ── Appendix 4 ── Register 33117 decoded bits ──
		FAULT_REG2_BACKUP_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Backup overvoltage fault")),
		FAULT_REG2_BACKUP_OVERLOAD(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Backup overload fault")),
		FAULT_REG2_GRID_BACKUP_OVERLOAD(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid Backup overload")),
		FAULT_REG2_OFFGRID_BACKUP_UNDERVOLTAGE(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Off-grid Backup undervoltage")),
		FAULT_REG2_HUB_PANEL_OV_CURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Hub Panel Over-Current")),
		FAULT_REG2_RESERVED_05(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT05)")),
		FAULT_REG2_RESERVED_06(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT06)")),
		FAULT_REG2_RESERVED_07(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT07)")),
		FAULT_REG2_RESERVED_08(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT08)")),
		FAULT_REG2_RESERVED_09(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT09)")),
		FAULT_REG2_RESERVED_10(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT10)")),
		FAULT_REG2_RESERVED_11(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT11)")),
		FAULT_REG2_RESERVED_12(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT12)")),
		FAULT_REG2_RESERVED_13(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT13)")),
		FAULT_REG2_RESERVED_14(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT14)")),
		FAULT_REG2_RESERVED_15(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG2 BIT15)")),

		// ── Appendix 4 ── Register 33118 decoded bits ──
		FAULT_REG3_BATTERY_NOT_CONNECTED(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery not connected")),
		FAULT_REG3_BATTERY_OVERVOLTAGE_CHECK(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery overvoltage check")),
		FAULT_REG3_BATTERY_UNDERVOLTAGE_CHECK(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery undervoltage check")),
		FAULT_REG3_BATTERY_BMS_ALARM(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery BMS Alarm")),
		FAULT_REG3_INCONSISTENT_BATTERY_SELECTION(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Inconsistent battery selection")),
		FAULT_REG3_LEAD_ACID_TEMP_TOO_LOW(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Lead-acid battery temperature too low")),
		FAULT_REG3_LEAD_ACID_TEMP_TOO_HIGH(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Lead-acid battery temperature too high")),
		FAULT_REG3_SECOND_BATTERY_NOT_CONNECTED(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Second battery not connected")),
		FAULT_REG3_SECOND_BATTERY_SW_OVERVOLTAGE(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Second battery software overvoltage")),
		FAULT_REG3_SECOND_BATTERY_SW_UNDERVOLTAGE(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Second battery software undervoltage")),
		FAULT_REG3_PARALLEL_BATTERY_COM_ABNORMAL(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Parallel battery communication abnormal")),
		FAULT_REG3_LOW_BATTERY_OFFGRID(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Low battery (off-grid)")),
		FAULT_REG3_RESERVED_12(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG3 BIT12)")),
		FAULT_REG3_RESERVED_13(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG3 BIT13)")),
		FAULT_REG3_RESERVED_14(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG3 BIT14)")),
		FAULT_REG3_RESERVED_15(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG3 BIT15)")),

		// ── Appendix 4 ── Register 33119 decoded bits ──
		FAULT_REG4_DC_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC overvoltage")),
		FAULT_REG4_DC_BUS_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC Bus overvoltage")),
		FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC Bus unbalanced voltage")),
		FAULT_REG4_DC_BUS_UNDERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC Bus undervoltage")),
		FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE_2(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC Bus unbalanced voltage 2")),
		FAULT_REG4_DC_OVERCURRENT_A(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC overcurrent on A circuit")),
		FAULT_REG4_DC_OVERCURRENT_B(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC overcurrent on B circuit")),
		FAULT_REG4_DC_INPUT_INTERFERENCE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC input interference")),
		FAULT_REG4_GRID_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid overcurrent")),
		FAULT_REG4_IGBT_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("IGBT overcurrent")),
		FAULT_REG4_GRID_INTERFERENCE_02(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid interference 02")),
		FAULT_REG4_AFCI_SELF_CHECK(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("AFCI self-check")),
		FAULT_REG4_ARC_FAULT_RESERVED(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Arc fault reserved")),
		FAULT_REG4_GRID_CURRENT_SAMPLING_FAULT(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid current sampling fault")),
		FAULT_REG4_DSP_SELF_CHECK_ERROR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DSP self-check error")),
		FAULT_REG4_BATTERY_DISCHARGE_OVERCURRENT(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery Discharge Overcurrent")),

		// ── Appendix 4 ── Register 33120 decoded bits ──
		FAULT_REG5_GRID_INTERFERENCE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid interference")),
		FAULT_REG5_OVER_DC_COMPONENTS(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Over DC components")),
		FAULT_REG5_OVER_TEMPERATURE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Over temperature protection")),
		FAULT_REG5_RELAY_CHECK(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Relay check protection")),
		FAULT_REG5_UNDER_TEMPERATURE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Under temperature protection")),
		FAULT_REG5_PV_INSULATION_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("PV insulation fault")),
		FAULT_REG5_12V_UNDERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("12V undervoltage protection")),
		FAULT_REG5_LEAK_CURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Leak current protection")),
		FAULT_REG5_LEAK_CURRENT_SELF_CHECK(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Leak current self-check protection")),
		FAULT_REG5_DSP_INITIAL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DSP initial protection")),
		FAULT_REG5_DSP_B(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DSP B protection")),
		FAULT_REG5_BATTERY_OVERVOLTAGE_HW(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery overvoltage hardware fault")),
		FAULT_REG5_LLC_HW_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("LLC hardware overcurrent")),
		FAULT_REG5_GRID_TRANSIENT_OVERCURRENT(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid transient overcurrent")),
		FAULT_REG5_BATTERY_COM_FAILURE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery communication failure")),
		FAULT_REG5_DSP_COM_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DSP COM FAIL")),

		// ── Appendix 4 ── Register 33124 decoded bits ──
		FAULT_REG6_SLAVE_LOSE_ERR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("SlaveLoseErr")),
		FAULT_REG6_MASTER_LOSE_ERR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("MasterLoseErr")),
		FAULT_REG6_SLAVE_PRD_ERR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("SlavePrd-Err")),
		FAULT_REG6_MASTER_PRD_ERR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("MasterPrd-Err")),
		FAULT_REG6_ADDR_CONFLICT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Addr-Conflict")),
		FAULT_REG6_HEARTBEAT_LOSE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("HeartbeatLose")),
		FAULT_REG6_DCAN_ERR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DCanErr")),
		FAULT_REG6_MUL_MASTER_ERR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("MulMasterErr")),
		FAULT_REG6_MODE_CONFLICT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("ModeConflict")),
		FAULT_REG6_S_PLUG_VOLT_ERR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("S-PlugVoltErr")),
		FAULT_REG6_OTHERS_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Others' Fault")),
		FAULT_REG6_CAN_BUS_LOSE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("CAN-BUS-LOSE")),
		FAULT_REG6_MODEL_MISMATCH(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("ModelMismatch")),
		FAULT_REG6_3P_CREATE_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("3P_CreateFail")),
		FAULT_REG6_ACBK_OPEN(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("ACBK-Open")),
		FAULT_REG6_RESERVED_15(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG6 BIT15)")),

		// ── Appendix 4 ── Register 33125 decoded bits ──
		FAULT_REG7_REVE_DC(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reve-DC")),
		FAULT_REG7_BATTERY_HW_OVERVOLTAGE_02(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery hardware overvoltage 02")),
		FAULT_REG7_BATTERY_HW_OVERCURRENT(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery hardware overcurrent")),
		FAULT_REG7_BUS_MIDPOINT_HW_OVERCURRENT(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Bus midpoint hardware overcurrent")),
		FAULT_REG7_BATTERY_STARTUP_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery startup fail")),
		FAULT_REG7_DC3_AVG_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC 3 average overcurrent")),
		FAULT_REG7_DC4_AVG_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC 4 average overcurrent")),
		FAULT_REG7_SOFTRUN_TIMEOUT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Softrun timeout")),
		FAULT_REG7_OFFGRID_TO_GRID_TIMEOUT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Off-grid to Grid timeout")),
		FAULT_REG7_DRM_NOT_CONNECT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DRM Not Connect")),
		FAULT_REG7_RESERVED_10(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG7 BIT10)")),
		FAULT_REG7_RESERVED_11(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG7 BIT11)")),
		FAULT_REG7_RESERVED_12(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG7 BIT12)")),
		FAULT_REG7_RESERVED_13(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG7 BIT13)")),
		FAULT_REG7_RESERVED_14(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG7 BIT14)")),
		FAULT_REG7_RESERVED_15(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG7 BIT15)")),

		OPERATING_STATUS(Doc.of(INTEGER).accessMode(READ_ONLY)),

		// ── Appendix 5 ── Register 33121 / 36026 decoded bits ──
		OPERATING_STAT_NORMAL_OPERATION(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Normal Operation")),
		OPERATING_STAT_INITIALIZING(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Initializing")),
		OPERATING_STAT_CONTROLLED_OFF(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Controlled turning OFF")),
		OPERATING_STAT_FAULT_OFF(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Fault leads to turning OFF")),
		OPERATING_STAT_STANDBY(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Stand-by")),
		OPERATING_STAT_LIMITED_TEMP_FREQ(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Limited Operation (temperature/frequency)")),
		OPERATING_STAT_LIMITED_EXTERNAL(
				Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Limited Operation (external reason)")),
		OPERATING_STAT_BACKUP_OVERLOAD(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Backup overload")),
		OPERATING_STAT_LOAD_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Load fault")),
		OPERATING_STAT_GRID_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid fault")),
		OPERATING_STAT_BATTERY_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery fault")),
		OPERATING_STAT_RESERVED_11(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (OPSTAT BIT11)")),
		OPERATING_STAT_GRID_SURGE_WARN(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid Surge (Warn)")),
		OPERATING_STAT_FAN_FAULT_WARN(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Fan fault (Warn)")),
		OPERATING_STAT_EXTERNAL_FAN_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("External fan failure")),
		OPERATING_STAT_RESERVED_15(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (OPSTAT BIT15)")),

		OPERATING_MODE(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		WORKING_MODE_RUNNING_STATUS(Doc.of(INTEGER)
		// .accessMode(READ_ONLY)
		),

		STORAGE_CONTROL_SWITCHING_VALUE(Doc.of(INTEGER).accessMode(READ_ONLY)),

		// ── Appendix 6 ── Register 33132 decoded bits ──
		STORAGE_CTRL_SELF_USE_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Self-Use Mode")),
		STORAGE_CTRL_TIME_OF_USE_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Time of Use mode")),
		STORAGE_CTRL_OFFGRID_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("OFF-grid mode")),
		STORAGE_CTRL_BATT_WAKEUP(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery wakeup switch")),
		STORAGE_CTRL_RESERVE_BATT_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserve battery mode")),
		STORAGE_CTRL_ALLOW_GRID_CHARGE(
				Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Allow grid to charge battery")),
		STORAGE_CTRL_FEED_IN_PRIORITY(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Feed in priority mode")),
		STORAGE_CTRL_BATT_OVC(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Batt OVC Function")),
		STORAGE_CTRL_FORCE_CHARGE_PEAKSHAVING(
				Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery force charge / peak shaving")),
		STORAGE_CTRL_BATT_CURRENT_CORRECTION(
				Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery current correction enable")),
		STORAGE_CTRL_BATT_HEALING_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery healing mode")),
		STORAGE_CTRL_PEAK_SHAVING_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Peak-shaving mode")),
		STORAGE_CTRL_RESERVED_12(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (STORAGE BIT12)")),
		STORAGE_CTRL_RESERVED_13(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (STORAGE BIT13)")),
		STORAGE_CTRL_RESERVED_14(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (STORAGE BIT14)")),
		STORAGE_CTRL_RESERVED_15(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (STORAGE BIT15)")),

		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		CURRENT_L1(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		CURRENT_L2(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		CURRENT_L3(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIHERTZ)),

		SET_REMOTE_CONTROL_AC_GRID_PORT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.WRITE_ONLY)), //

		SET_REMOTE_CONTROL_MODE(Doc.of(OpenemsType.INTEGER) // 0 OFF， 1 ON with 'system grid connection point'， 2 ON
															// with 'Inverter AC grid port'
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_REMOTE_DISPATCH_SWITCH(Doc.of(EnableDisable.values()) // 0 OFF, 1 ON
		        .text("Remote dispatch switch. 0 = OFF, 1 = ON")
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_SWITCH(Doc.of(EnableDisable.values()) // 0 OFF, 1 ON
		        .text("Remote dispatch switch. 0 = OFF, 1 = ON")
				.accessMode(AccessMode.READ_ONLY)),

		SET_REMOTE_DISPATCH_TIMEOUT(Doc.of(OpenemsType.INTEGER) // Timeout in minutes for remote dispatch (failsafe
																// timer)
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_TIMEOUT(Doc.of(OpenemsType.INTEGER) // Timeout in minutes for remote dispatch (failsafe
				// timer)
				.accessMode(AccessMode.READ_ONLY)),

		SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH(Doc.of(RemoteDispatchSystemLimitSwitch.values()) // 0 Disable system import/export limit, 1
																			// Enable
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH(Doc.of(RemoteDispatchSystemLimitSwitch.values()) // 0 Disable system import/export limit, 1
				// Enable
				.accessMode(AccessMode.READ_ONLY)),

		SET_REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT(Doc.of(OpenemsType.INTEGER) // System import limit; 0xFFFF = default /
																			// ignore
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT(Doc.of(OpenemsType.INTEGER) // System import limit; 0xFFFF = default /
				// ignore
				.accessMode(AccessMode.READ_ONLY)),

		SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT(Doc.of(OpenemsType.INTEGER) // System export limit; 0xFFFF = default /
																			// ignore
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT(Doc.of(OpenemsType.INTEGER) // System export limit; 0xFFFF = default /
				// ignore
				.accessMode(AccessMode.READ_ONLY)),

		SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH(Doc.of(OpenemsType.INTEGER) // 1 Ignore register value, 2 Battery
																				// charge/discharge control, 3 Grid
																				// import/export control, 4 Grid power
																				// control
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH(Doc.of(OpenemsType.INTEGER) // 1 Ignore register value, 2 Battery
				// charge/discharge control, 3 Grid
				// import/export control, 4 Grid power
				// control
				.accessMode(AccessMode.READ_ONLY)),

		SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER(Doc.of(OpenemsType.INTEGER) // S32 value (two registers), unit 10 W;
																				// positive = charge/import, negative =
																				// discharge/export
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_REALTIME_CONTROL_POWER(Doc.of(OpenemsType.INTEGER) // S32 value (two registers), unit 10 W;
				// positive = charge/import, negative =
				// discharge/export
				.accessMode(AccessMode.READ_ONLY)),

		SET_REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH(Doc.of(RemoteDispatchRealtimeControlSwitch.values()) // Bitfield: PV shutdown, DO
																							// control, allow grid
																							// charge, off-grid battery
																							// standby
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH(Doc.of(RemoteDispatchRealtimeControlSwitch.values()) // Bitfield: PV shutdown, DO
				// control, allow grid
				// charge, off-grid battery
				// standby
				.accessMode(AccessMode.READ_ONLY)),

		STANDARD_WORKING_MODE(Doc.of(StandardWorkingMode.values()).accessMode(AccessMode.READ_ONLY)),

		OPERATING_MODE_DECODE(Doc.of(Appendix8.values()).accessMode(AccessMode.READ_ONLY)),;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	// Set remote dispatch switch
	/**
	 * Sets the remote dispatch switch.
	 *
	 * @param value the {@link EnableDisable} value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRemoteDispatchSwitch(EnableDisable value) throws OpenemsNamedException {
	    this.setRemoteDispatchSwitchChannel().setNextWriteValue(value);
	}

	public default EnableDisable getRemoteDispatchSwitch() {
	    return this.getRemoteDispatchSwitchChannel().value().asEnum();
	}

	public default Channel<EnableDisable> getRemoteDispatchSwitchChannel() {
	    return this.channel(ChannelId.REMOTE_DISPATCH_SWITCH);
	}

	public default WriteChannel<EnableDisable> setRemoteDispatchSwitchChannel() {
	    return this.channel(ChannelId.SET_REMOTE_DISPATCH_SWITCH);
	}

	// Set remote dispatch timeout
	public default void setRemoteDispatchTimeout(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchTimeoutChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchTimeoutChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_TIMEOUT);
	}
	
	
	/**
	 * Sets the remote dispatch system limit switch.
	 *
	 * <p>
	 * BIT00: System Import Limit Switch (0 = Disable, 1 = Enable)  
	 * BIT01: System Export Limit Switch (0 = Disable, 1 = Enable)  
	 * BIT02-BIT15: Reserved
	 *
	 * @param value the {@link SystemLimitSwitch} value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRemoteDispatchSystemLimitSwitch(RemoteDispatchSystemLimitSwitch value) throws OpenemsNamedException {
	    this.setRemoteDispatchSystemLimitSwitchChannel().setNextWriteValue(value);
	}

	public default RemoteDispatchSystemLimitSwitch getRemoteDispatchSystemLimitSwitch() {
	    return this.getRemoteDispatchSystemLimitSwitchChannel().value().asEnum();
	}

	public default Channel<RemoteDispatchSystemLimitSwitch> getRemoteDispatchSystemLimitSwitchChannel() {
	    return this.channel(ChannelId.REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH);
	}

	public default WriteChannel<RemoteDispatchSystemLimitSwitch> setRemoteDispatchSystemLimitSwitchChannel() {
	    return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH);
	}

	// Set remote dispatch system import limit
	public default void setRemoteDispatchSystemImportLimit(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchSystemImportLimitChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchSystemImportLimitChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT);
	}

	// Set remote dispatch system export limit
	public default void setRemoteDispatchSystemExportLimit(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchSystemExportLimitChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchSystemExportLimitChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT);
	}

	// Set realtime control power (S32 value)
	public default void setRemoteDispatchRealtimeControlPower(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchRealtimeControlPowerChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteDispatchRealtimeControlPowerChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER);
	}
/*
	// Get realtime control function switch
	public default Integer getRemoteDispatchRealtimeControlFunctionSwitch() {
		return this.getRemoteDispatchRealtimeControlFunctionSwitchChannel().value().get();
	}

	public default Channel<Integer> getRemoteDispatchRealtimeControlFunctionSwitchChannel() {
		return this.channel(ChannelId.REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH);
	}
*/
	
	/**
	 * Sets the remote dispatch realtime control function switch.
	 *
	 * @param pvShutdown true = PV shutdown enabled; false = PV shutdown disabled
	 * @param doControl true = DO control enabled; false = DO control disabled
	 * @param gridChargeAllowed true = grid charge allowed; false = grid charge not allowed
	 * @param offGridBatteryStandby true = off-grid battery standby enabled; false = disabled
	 * @throws OpenemsNamedException on error
	 */
	public default void setRemoteDispatchRealtimeControlFunctionSwitch(boolean pvShutdown, boolean doControl,
			boolean gridChargeAllowed, boolean offGridBatteryStandby) throws OpenemsNamedException {

		int value = 0;

		// BIT00-01: PV shutdown switch
		// 1 = Disable, 2 = Enable
		value |= pvShutdown ? 2 : 1;

		// BIT02-03: DO control
		// 1 = Disable, 2 = Enable
		value |= (doControl ? 2 : 1) << 2;

		// BIT04-05: Allow grid charge
		// 1 = Allow, 2 = Not allow
		value |= (gridChargeAllowed ? 1 : 2) << 4;

		// BIT06-07: Off-grid battery standby
		// 1 = Disable, 2 = Enable
		value |= (offGridBatteryStandby ? 2 : 1) << 6;

		this.getSetRemoteDispatchRealtimeControlFunctionSwitchChannel().setNextWriteValue(value);
	}
	
	public default IntegerWriteChannel getSetRemoteDispatchRealtimeControlFunctionSwitchChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH);
	}	
	
	/**
	 * Sets the remote dispatch realtime control switch.
	 *
	 * @param value the {@link RemoteDispatchRealtimeControlSwitch} value
	 * @throws OpenemsNamedException on error
	 */
	public default void setRemoteDispatchRealtimeControlSwitch(RemoteDispatchRealtimeControlSwitch value)
	        throws OpenemsNamedException {
	    this.setRemoteDispatchRealtimeControlSwitchChannel().setNextWriteValue(value);
	}

	public default RemoteDispatchRealtimeControlSwitch getRemoteDispatchRealtimeControlSwitch() {
	    return this.getRemoteDispatchRealtimeControlSwitchChannel().value().asEnum();
	}

	public default Channel<RemoteDispatchRealtimeControlSwitch> getRemoteDispatchRealtimeControlSwitchChannel() {
	    return this.channel(ChannelId.REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH);
	}

	public default WriteChannel<RemoteDispatchRealtimeControlSwitch> setRemoteDispatchRealtimeControlSwitchChannel() {
	    return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH);
	}
	

	// Set remote control mode
	public default void setRemoteControlMode(int value) throws OpenemsNamedException {
		this.getSetRemoteControlModeChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteControlModeChannel() {
		return this.channel(ChannelId.SET_REMOTE_CONTROL_MODE);
	}

	// Set power setpoint
	public default void setRemoteControlPower(int value) throws OpenemsNamedException {
		this.getSetRemoteControlPowerChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetRemoteControlPowerChannel() {
		return this.channel(ChannelId.SET_REMOTE_CONTROL_AC_GRID_PORT_POWER);
	}

	/**
	 * Adds Battery to ESS hybrid system.
	 * 
	 * @param battery link to Pytes battery
	 */
	public void addBattery(PytesBattery battery);

	/**
	 * Removes link to battery.
	 * 
	 * @param PytesBattery battery
	 */
	public void removeBattery(PytesBattery battery);

	/**
	 * Adds DC-charger to ESS hybrid system. Represents PV production
	 * 
	 * @param charger link to DC charger(s)
	 */
	public void addCharger(PytesDcCharger charger);

	/**
	 * Removes link to pv DC charger.
	 * 
	 * @param charger charger
	 */
	public void removeCharger(PytesDcCharger charger);

	/**
	 * returns ModbusBrdigeId from config.
	 * 
	 * @return ModbusBrdigeId from config
	 */
	public String getModbusBridgeId();

	public Integer getUnitId();
}
