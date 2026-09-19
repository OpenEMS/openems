package io.openems.edge.pytes.ess;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.AccessMode.WRITE_ONLY;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.LOW;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.BOOLEAN;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.enums.Appendix2;
import io.openems.edge.pytes.enums.Appendix8;
import io.openems.edge.pytes.enums.EnableDisable;
import io.openems.edge.pytes.enums.InverterOperatingStatus;
import io.openems.edge.pytes.enums.RemoteDispatchRealtimeControlSwitch;
import io.openems.edge.pytes.enums.RemoteDispatchSystemLimitSwitch;
import io.openems.edge.pytes.enums.StandardWorkingMode;
import io.openems.edge.pytes.enums.WorkState;
import io.openems.edge.pytes.enums.BatteryBmsStatus;

public interface PytesJs3 extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// -----------------------------------------------------------------------
		// Internal work state machine — not a Modbus register
		// -----------------------------------------------------------------------

		/**
		 * Internal OpenEMS work state (not a Modbus register).
		 * Managed by defineWorkState() in PytesJs3Impl.
		 * Transitions: UNDEFINED → INITIALIZING → NORMAL | WARNING | ERROR | STANDBY.
		 * NORMAL and WARNING both apply set-points (a WARNING-level channel is
		 * informational, the inverter keeps running); only ERROR (a FAULT-level
		 * channel) stops the EMS control.
		 */
		/**
		 * Grid feed-in limit currently applied to the inverter (regs 44102/44104),
		 * derived from Core.Meta; null when no limitation is active.
		 */
		GRID_FEED_IN_LIMIT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.WATT)//
				.persistencePriority(HIGH)),

		/**
		 * The feed-in limit read back from the inverter (regs 44102/44104) does not
		 * match the applied one for more than 30 s, i.e. the inverter did not
		 * accept it or lost it (e.g. after a power cycle).
		 */
		GRID_FEED_IN_LIMIT_MISMATCH(Doc.of(Level.WARNING)//
				.text("Grid feed-in limit in the inverter differs from the applied one (regs 44102/44104)")),

		/**
		 * Dynamic feed-in limitation is active: the AC output is capped via reg
		 * 43052 so that the export stays below the Core.Meta limit; PV is curtailed.
		 */
		PV_LIMIT_ACTIVE(Doc.of(Level.INFO)//
				.text("Dynamic feed-in limitation active: AC output capped, PV curtailed")),

		/** The AC output cap applied by the dynamic feed-in limitation; null when inactive. */
		AC_OUTPUT_LIMIT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.WATT)//
				.persistencePriority(HIGH)),

		/**
		 * AC floor of the surplus search in AC output control (see
		 * {@code PvSurplusProbe}); 0 while the search is off.
		 */
		SURPLUS_FLOOR(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.WATT)//
				.persistencePriority(HIGH)),

		WORK_STATE(Doc.of(WorkState.values())
				.accessMode(READ_WRITE)
				.persistencePriority(HIGH)),

		// -----------------------------------------------------------------------
		// Holding registers — SoC limits (reg 43010, 43011, 43018)
		// FC16 write / FC3 read. Priority LOW — configuration, survives power cycle.
		// -----------------------------------------------------------------------

		/**
		 * Max Charge SOC — write channel (reg 43010, U16, FC16).
		 * Datasheet: "Max Charge SOC. 1% resolution. Range: 80–100%. Default: 100%.
		 * The inverter stops charging when battery SoC reaches this level."
		 * Unit: %
		 */
		SET_MAX_CHARGE_SOC(Doc.of(INTEGER)
				.accessMode(WRITE_ONLY)
				.unit(Unit.PERCENT)),

		/**
		 * Max Charge SOC — read-back channel (reg 43010, U16, FC3).
		 * Confirms the value currently stored in the inverter's flash.
		 * Unit: %
		 */
		MAX_CHARGE_SOC(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.PERCENT)
				.persistencePriority(LOW)),

		/**
		 * Overdischarge SOC — write channel (reg 43011, U16, FC16).
		 * Datasheet: "Overdischarge SOC. 1% resolution. Range: 5–40%. Default: 20%.
		 * The inverter stops discharging when battery SoC reaches this level.
		 * Must always be >= Force Charge SOC (reg 43018)."
		 * Unit: %
		 */
		SET_OVERDISCHARGE_SOC(Doc.of(INTEGER)
				.accessMode(WRITE_ONLY)
				.unit(Unit.PERCENT)),

		/**
		 * Overdischarge SOC — read-back channel (reg 43011, U16, FC3).
		 * Used by setDefaultValues() to confirm the written value was accepted.
		 * Unit: %
		 */
		OVERDISCHARGE_SOC(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.PERCENT)
				.persistencePriority(LOW)),

		/**
		 * Force Charge SOC — write channel (reg 43018, U16, FC16).
		 * Datasheet: "Force Charge SOC. 1% resolution. Range: 4% up to reg 43011. Default: 10%.
		 * Emergency floor — the inverter forces a grid charge if SoC falls to this level.
		 * Must always be <= Overdischarge SOC (reg 43011)."
		 * setDefaultValues() writes config.minSoc()-1 here.
		 * Unit: %
		 */
		SET_FORCE_CHARGE_SOC(Doc.of(INTEGER)
				.accessMode(WRITE_ONLY)
				.unit(Unit.PERCENT)),

		/**
		 * Force Charge SOC — read-back channel (reg 43018, U16, FC3).
		 * Unit: %
		 */
		/**
		 * Inverter battery-model setting: max charge current (reg 43012, U16, 0.1 A,
		 * read-only here). LV hybrid: 50.0-100.0 A, default 62.5 A. Unit: mA.
		 */
		INVERTER_MAX_CHARGE_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/**
		 * Inverter battery-model setting: max discharge current (reg 43013, U16,
		 * 0.1 A, read-only here). LV hybrid: 50.0-100.0 A, default 62.5 A. Unit: mA.
		 */
		INVERTER_MAX_DISCHARGE_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/**
		 * Inverter storage-control setting: battery max charge current (reg 43117,
		 * U16, 0.1 A, read-only here). S6: 0 .. max battery charge current. Unit: mA.
		 */
		STORAGE_CTRL_MAX_CHARGE_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/**
		 * Inverter storage-control setting: battery max discharge current (reg
		 * 43118, U16, 0.1 A, read-only here). Unit: mA.
		 */
		STORAGE_CTRL_MAX_DISCHARGE_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/**
		 * Inverter status: battery max charge current (reg 33206, U16, 0.1 A). Max
		 * 70 A, default 70 A. Unit: mA.
		 */
		BATTERY_MAX_CHARGE_CURRENT_STATUS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/**
		 * Inverter status: battery max discharge current (reg 33207, U16, 0.1 A).
		 * Max 70 A, default 70 A. Unit: mA.
		 */
		BATTERY_MAX_DISCHARGE_CURRENT_STATUS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		FORCE_CHARGE_SOC(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.PERCENT)
				.persistencePriority(LOW)),

		// -----------------------------------------------------------------------
		// Holding register — Backup circuit setting (reg 43111)
		// FC16 write / FC3 read. Priority LOW — survives power cycle.
		// -----------------------------------------------------------------------

		/**
		 * Backup circuit setting — write channel (reg 43111, U16, FC16).
		 * Datasheet: "Backup circuit setting. 0x0000 = disable, 0x0001 = enable. Default: enable."
		 * Controls the physical relay on the backup AC port.
		 * Written during INITIALIZING based on config.enableBackupPort().
		 */
		SET_BACKUP_CIRCUIT_SETTING(Doc.of(EnableDisable.values())
				.accessMode(WRITE_ONLY)
				.text("Backup circuit setting — write to change")),

		/**
		 * Backup circuit setting — read-back channel (reg 43111, U16, FC3).
		 * Confirms the current hardware state of the backup port relay.
		 * To change: write to SET_BACKUP_CIRCUIT_SETTING.
		 */
		BACKUP_CIRCUIT_SETTING(Doc.of(EnableDisable.values())
				.accessMode(READ_ONLY)
				.text("Backup circuit setting — current hardware state")),

		// -----------------------------------------------------------------------
		// Remote Dispatch Registers
		// -----------------------------------------------------------------------

		/** Remote Dispatch Switch — write channel (reg 44100, U16, FC16).
		 * 0 = OFF (disable remote dispatch), 1 = ON. Not saved after power cycle. */
		SET_REMOTE_DISPATCH_SWITCH(Doc.of(EnableDisable.values())
				.accessMode(AccessMode.WRITE_ONLY)
		        .text("Remote dispatch switch. 0 = OFF, 1 = ON")),

		/** Remote Dispatch Switch — read-back channel (reg 44100, U16, FC3). */
		REMOTE_DISPATCH_SWITCH(Doc.of(EnableDisable.values())
				.accessMode(AccessMode.READ_ONLY)
		        .text("Remote dispatch switch. 0 = OFF, 1 = ON")),

		/** Remote Dispatch Failsafe — write channel (reg 44101, U16, FC16).
		 * Timeout in minutes (1–1440). Default 5. If the remote dispatch heartbeat stops,
		 * the inverter reverts to local control after this timeout.
		 * Write 0xFFFF to reset to default. Not saved after power cycle. */
		SET_REMOTE_DISPATCH_FAILSAFE_SETTING(Doc.of(INTEGER)
				.accessMode(AccessMode.WRITE_ONLY)
				.unit(Unit.MINUTE)),

		/** Remote Dispatch Failsafe — read-back channel (reg 44101, U16, FC3). */
		REMOTE_DISPATCH_FAILSAFE_SETTING(Doc.of(INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.MINUTE)),

		/** System Limit Switch — write channel (reg 44102, U16, FC16).
		 * BIT00=import limit enabled, BIT01=export limit enabled. Not saved after power cycle. */
		SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH(Doc.of(RemoteDispatchSystemLimitSwitch.values())
				.accessMode(AccessMode.WRITE_ONLY)),

		/** System Limit Switch — read-back channel (reg 44102, U16, FC3). */
		REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH(Doc.of(RemoteDispatchSystemLimitSwitch.values())
				.accessMode(AccessMode.READ_ONLY)),

		/** System Import Limit — write channel (reg 44103, U16, FC16).
		 * Datasheet: 1 unit = 100 W. Write 0xFFFF to reset to rated power.
		 * Active only when REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH BIT00 = 1.
		 * Not saved after power cycle. */
		SET_REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT(Doc.of(INTEGER)
				.accessMode(AccessMode.WRITE_ONLY)
				.unit(Unit.WATT)),

		/** System Import Limit — read-back channel (reg 44103, U16, FC3). 1 unit = 100 W. */
		REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT(Doc.of(INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.WATT)),

		/** System Export Limit — write channel (reg 44104, U16, FC16).
		 * Datasheet: 1 unit = 100 W. Write 0xFFFF to reset to rated power.
		 * Active only when REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH BIT01 = 1.
		 * Not saved after power cycle. */
		SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT(Doc.of(INTEGER)
				.accessMode(AccessMode.WRITE_ONLY)
				.unit(Unit.WATT)),

		/** System Export Limit — read-back channel (reg 44104, U16, FC3). 1 unit = 100 W. */
		REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT(Doc.of(INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.WATT)),
		
		/**
		 * Remote Dispatch Real-Time Control Switch — write channel (reg 44105, U16, FC16).
		 * Datasheet: 1=Battery Standby, 2=Battery charge/discharge control,
		 * 3=Grid connection point control, 4=AC grid port control. Default: 1.
		 * Value 0 is invalid per datasheet; UNDEFINED(0) is the sentinel only.
		 */
		SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH(
				Doc.of(RemoteDispatchRealtimeControlSwitch.values())
				.accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Remote Dispatch Real-Time Control Switch — read-back channel (reg 44105, U16, FC3).
		 */
		REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH(
				Doc.of(RemoteDispatchRealtimeControlSwitch.values())
				.accessMode(AccessMode.READ_ONLY)),
		
		/** Real-Time Control Function Switch — write channel (reg 44108, U16, FC16).
		 * Four 2-bit groups: BIT00–01=PV shutdown, BIT02–03=DO control,
		 * BIT04–05=grid charge allowed, BIT06–07=off-grid battery standby.
		 * Encoding per group: 1=Disable/NotAllow, 2=Enable/Allow.
		 * Use setRemoteDispatchRealtimeControlFunctionSwitch() for a boolean-friendly API.
		 * Not saved after power cycle. */
		SET_REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH(Doc.of(INTEGER)
				.accessMode(AccessMode.WRITE_ONLY)),

		/** Real-Time Control Function Switch — read-back channel (reg 44108, U16, FC3). */
		REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH(Doc.of(INTEGER) 
				.accessMode(AccessMode.READ_ONLY)),

		// -----------------------------------------------------------------------
		// Main input registers - inverter status, AC measurements, faults
		// -----------------------------------------------------------------------
		/** Safety (grid code) version number (reg 33068, U16, FC4).
		 * Raw integer identifying the installed grid-code firmware version. No unit. */
		SAFETY_VERSION(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		/** HMI sub-version number (reg 33069, U16, FC4).
		 * Combine with reg 33002 (firmware major version) to form the complete HMI version string. */
		HMI_SUB_VERSION(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		/*
		 * Add Alarm code data to distinguishing displayed Alarm code For external fan
		 * failure, each bit indicates the status of one fan; In conjunction with the
		 * 33095 register address, it is used for subdivided fault information display.
		 * Example: 33095 register read information is 0x1020,
		 */
		ALARM_CODE_DATA(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),
		
		/**
		 * DC bus total voltage (reg 33071, U16, FC4).
		 * Datasheet: 0.1 V → SCALE_FACTOR_2 → stored as mV.
		 * persistencePriority(HIGH) — logged every cycle.
		 * Unit: mV
		 */
		DC_BUS_VOLTAGE(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIVOLT)
				.persistencePriority(HIGH)),
		
		/**
		 * DC bus split-rail half voltage (reg 33072, U16, FC4).
		 * Datasheet: 0.1 V → SCALE_FACTOR_2 → stored as mV.
		 * Should equal DC_BUS_VOLTAGE / 2 under normal balance.
		 * persistencePriority(LOW).
		 * Unit: mV
		 */
		DC_BUS_HALF_VOLTAGE(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIVOLT)
				.persistencePriority(LOW)),

		/**
		 * Phase L1 (or AB line) voltage (reg 33073, U16, FC4).
		 * 3-phase models: AB line voltage. Single/split-phase: L1 phase voltage.
		 * Datasheet: 0.1 V → SCALE_FACTOR_2 → stored as mV.
		 * persistencePriority(HIGH).
		 * Unit: mV
		 */
		VOLTAGE_L1(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIVOLT)
				.persistencePriority(HIGH)),
		
		/**
		 * Phase L2 (or BC line) voltage (reg 33074, U16, FC4).
		 * 3-phase models: BC line voltage. Split-phase: L2 phase voltage.
		 * Datasheet: 0.1 V → SCALE_FACTOR_2 → stored as mV.
		 * persistencePriority(HIGH).
		 * Unit: mV
		 */
		VOLTAGE_L2(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIVOLT)
				.persistencePriority(HIGH)),

		/**
		 * Phase L3 (or CA line) voltage (reg 33075, U16, FC4).
		 * 3-phase models: CA line voltage. 0 on single-phase models.
		 * Datasheet: 0.1 V → SCALE_FACTOR_2 → stored as mV.
		 * persistencePriority(HIGH).
		 * Unit: mV
		 */
		VOLTAGE_L3(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIVOLT)
				.persistencePriority(HIGH)),

		/**
		 * Phase L1 output current (reg 33076, U16, FC4).
		 * Datasheet: 0.1 A → SCALE_FACTOR_2 → stored as mA.
		 * persistencePriority(HIGH).
		 * Unit: mA
		 */
		CURRENT_L1(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIAMPERE)
				.persistencePriority(HIGH)),

		/**
		 * Phase L2 output current (reg 33077, U16, FC4).
		 * Datasheet: 0.1 A → SCALE_FACTOR_2 → stored as mA.
		 * persistencePriority(HIGH).
		 * Unit: mA
		 */
		CURRENT_L2(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIAMPERE)
				.persistencePriority(HIGH)),
		
		/**
		 * Phase L3 output current (reg 33078, U16, FC4).
		 * 0 on single-phase models.
		 * Datasheet: 0.1 A → SCALE_FACTOR_2 → stored as mA.
		 * persistencePriority(HIGH).
		 * Unit: mA
		 */
		CURRENT_L3(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIAMPERE)
				.persistencePriority(HIGH)),
		
		/**
		 * Total apparent power (reg 33083–33084, S32, FC4).
		 * Datasheet: 1 VA resolution, no converter needed.
		 * persistencePriority(HIGH).
		 * Unit: VA
		 */
		APPARENT_POWER(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.VOLT_AMPERE)
				.persistencePriority(HIGH)),

		/**
		 * Standard working mode (reg 33091, U16, FC4).
		 * See {@link StandardWorkingMode} for values (e.g. Volt-watt, Volt-var, Fixed PF).
		 * Used on models with Hawaii / AS/NZS 4777 grid standard.
		 */
		STANDARD_WORKING_MODE(Doc.of(StandardWorkingMode.values())
				.accessMode(AccessMode.READ_ONLY)),
		
		/**
		 * Grid frequency (reg 33094, U16, FC4).
		 * Datasheet: 0.01 Hz → SCALE_FACTOR_1 → stored as mHz.
		 * persistencePriority(HIGH).
		 * Unit: mHz
		 */
		FREQUENCY(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIHERTZ)
				.persistencePriority(HIGH)),

		/**
		 * Inverter current status code (reg 33095, U16, FC4).
		 * See {@link Appendix2} enum.
		 * Used with ALARM_CODE_DATA (reg 33070) for subdivided fault information display.
		 */
		INVERTER_CURRENT_STATUS(Doc.of(Appendix2.values())
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Lead-acid battery temperature (reg 33096, S16, FC4).
		 * Datasheet: 0.1 °C resolution.
		 * Only valid when a lead-acid battery type is configured; always 0 otherwise.
		 * Unit: °C (raw × 0.1)
		 */
		LEAD_ACID_BATTERY_TEMP(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.DEZIDEGREE_CELSIUS)),

		/**
		 * Function Status (reg 33097, U16, read-only).
		 *
		 * <p>Datasheet: "Function status bitmask." Each bit is independent.
		 * The raw word is stored here; individual bits are decoded by BitsWordElement
		 * into FUNCTION_STAT_* boolean channels below.
		 *
		 * <pre>
		 *   BIT00 DRM function               0=OFF, 1=ON
		 *   BIT01 Parallel running status    0=stopped, 1=running
		 *   BIT02 Master/slave               0=slave, 1=master
		 *   BIT03 3PH unbalance status       0=balanced, 1=unbalanced
		 *   BIT04 Generator start conditions 0=not met, 1=met
		 *   BIT05 Generator started          0=no, 1=yes
		 *   BIT06 Battery mode               0=parallel, 1=independent
		 *   BIT07 AFCI board present         0=no, 1=yes
		 *   BIT08 AFCI self-test done        0=no, 1=finished
		 *   BIT09 Grid connection            0=off-grid, 1=grid-connected
		 *   BIT10 Double backup enabled      0=disabled, 1=enabled
		 *   BIT11 RSD switch (S6 HV only)    0=open, 1=closed
		 *   BIT12 Emergency switch (S6 HV)   0=open, 1=closed
		 *   BIT13 AC coupling running        0=not running, 1=running
		 *   BIT14-15 Reserved
		 * </pre>
		 */

		// decoded boolean sub-channels (filled by BitsWordElement in defineModbusProtocol):
		
		/** reg 33097 BIT00 – DRM function enabled. */
		FUNCTION_STAT_DRM(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("DRM function (reg 33097 BIT00)")),
		
		/** reg 33097 BIT01 – Parallel running. */
		FUNCTION_STAT_PARALLEL_RUNNING(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Parallel running (reg 33097 BIT01)")),
		
		/** reg 33097 BIT02 – Master (1) or slave (0). */
		FUNCTION_STAT_MASTER(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Master/slave: 1=master (reg 33097 BIT02)")),
		
		/** reg 33097 BIT03 – 3-phase unbalanced operation. */
		FUNCTION_STAT_3PH_UNBALANCED(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("3PH unbalanced operation (reg 33097 BIT03)")),
		
		/** reg 33097 BIT04 – Generator start conditions met. */
		FUNCTION_STAT_GEN_START_CONDITIONS(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Generator start conditions met (reg 33097 BIT04)")),
		
		/** reg 33097 BIT05 – Generator started successfully. */
		FUNCTION_STAT_GEN_STARTED(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Generator started (reg 33097 BIT05)")),
		
		/** reg 33097 BIT06 – Battery independent (1) or parallel (0). */
		FUNCTION_STAT_BATT_INDEPENDENT(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Battery independent mode (reg 33097 BIT06)")),
		
		/** reg 33097 BIT07 – AFCI board present. */
		FUNCTION_STAT_AFCI_PRESENT(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("AFCI board present (reg 33097 BIT07)")),
		
		/** reg 33097 BIT08 – AFCI self-test finished. */
		FUNCTION_STAT_AFCI_SELFTEST_DONE(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("AFCI self-test finished (reg 33097 BIT08)")),
		
		/** reg 33097 BIT09 – Grid connected (1) or off-grid (0). */
		FUNCTION_STAT_GRID_CONNECTED(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Grid connected (reg 33097 BIT09)")),
		
		/** reg 33097 BIT10 – Double backup enabled. */
		FUNCTION_STAT_DOUBLE_BACKUP(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Double backup enabled (reg 33097 BIT10)")),
		
		/** reg 33097 BIT11 – RSD switch closed (S6 HV hybrid only). */
		FUNCTION_STAT_RSD_SWITCH(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("RSD switch closed (reg 33097 BIT11, S6 HV only)")),
		
		/** reg 33097 BIT12 – Emergency switch closed (S6 HV hybrid only). */
		FUNCTION_STAT_EMERGENCY_SWITCH(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Emergency switch closed (reg 33097 BIT12, S6 HV only)")),
		
		/** reg 33097 BIT13 – AC coupling running. */
		FUNCTION_STAT_AC_COUPLING(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("AC coupling running (reg 33097 BIT13)")),
		
		/** reg 33097 BIT14 – Reserved. */
		FUNCTION_STAT_RESERVED_14(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Reserved (reg 33097 BIT14)")),
		
		/** reg 33097 BIT15 – Reserved. */
		FUNCTION_STAT_RESERVED_15(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Reserved (reg 33097 BIT15)")),

		/** Current DRM Code Status (reg 33098, U16, FC4).
		 * Bitmask of active Demand Response Mode conditions per AS/NZS 4755.3. Raw — no scale. */
		CURRENT_DRM_CODE_STATUS(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),
		
		/**
		 * Inverter cabinet temperature (reg 33099, S16, FC4).
		 * Datasheet: 0.1 °C resolution → SCALE_FACTOR_MINUS_1 → stored as °C integer.
		 * persistencePriority(LOW).
		 * Unit: °C
		 */
		INVERTER_CABINET_TEMP(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(LOW)),

		/** Limited Power Actual Value (reg 33104, U16, FC4).
		 * Datasheet: 0.01% resolution. Current active power limit as a percentage of rated power. */
		LIMITED_POWER_ACTUAL_VALUE(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.PERCENT)),

		/**
		 * Limited power setting - write channel (reg 43052, U16, FC6). Active power
		 * limit of the inverter's AC output in % of rated power (0-110 %). Driven
		 * by the dynamic feed-in limitation (PvLimitHandler); written only when the
		 * value changes.
		 */
		SET_LIMITED_POWER(Doc.of(INTEGER)
				.accessMode(WRITE_ONLY)
				.unit(Unit.PERCENT)),

		/** Limited power setting - read-back (reg 43052, U16, FC3). Unit: %. */
		LIMITED_POWER_SETTING(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.PERCENT)),

		/** PF Adjustment Actual Value (reg 33105, S16, FC4).
		 * Datasheet: 0.001 resolution. E.g. 1000 = 1.000 (unity PF), 800 = 0.800.
		 * Range: –1.000 to +1.000. No unit declared. */
		PF_ADJUSTMENT_ACTUAL_VALUE(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		/** Limited Reactive Power (reg 33106, S16, FC4).
		 * Datasheet: 0.01% resolution. Range: –6000 to +6000 (–60.00% to +60.00%).
		 * Effective only in Working Mode 4 (Fix reactive power). */
		LIMITED_REACTIVE_POWER(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.PERCENT)),

		/**
		 * Inverter module temperature 2 (reg 33107, S16, FC4).
		 * Second NTC sensor — only present on models with dual temperature sensing.
		 * Datasheet: 0.1 °C resolution → SCALE_FACTOR_MINUS_1 → stored as °C integer.
		 * persistencePriority(LOW).
		 * Unit: °C
		 */
		INVERTER_MODULE_TEMP2(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.DEZIDEGREE_CELSIUS)
				.persistencePriority(LOW)),
		
		/**
		 * Volt-var real-time Vref value (reg 33108, U16, FC4).
		 * Datasheet: 0.1 V → SCALE_FACTOR_2 → stored as mV.
		 * Active voltage reference for IEEE 1547-2018 Volt-var mode.
		 * persistencePriority(LOW).
		 * Unit: mV
		 */
		VOLT_VAR_VREF_RT_VALUES(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIVOLT)
				.persistencePriority(LOW)),
		
		/**
		 * BMS maximum charging voltage limit (reg 33110, U16, FC4).
		 * Reported by the battery BMS to the inverter.
		 * Datasheet: 0.1 V → SCALE_FACTOR_2 → stored as mV.
		 * persistencePriority(LOW).
		 * Unit: mV
		 */
		BMS_CHARGING_VOLTAGE_LIMIT(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.MILLIVOLT)
				.persistencePriority(LOW)),

		/** Battery BMS Communication Status (reg 33111, U16, FC4).
		 * Datasheet: 0=normal, 1=communication failure, 2=BMS warning. */
		BATTERY_BMS_STATUS(Doc.of(BatteryBmsStatus.values())
				.accessMode(READ_ONLY)),

		/**
		 * Inverter Initial Setting State (reg 33112, U16, read-only).
		 *
		 * <p>Datasheet: "Initial setting state returned by DSP. See Appendix 1."
		 * Indicates which hardware and firmware initialisation steps have completed.
		 * Read this register during startup to confirm the inverter is fully configured
		 * before issuing control commands.
		 *
		 * <pre>
		 *   BIT00 Model setting complete
		 *   BIT01 National standard (grid code) setting complete
		 *   BIT02 Power curve setting complete
		 *   BIT03 Module ID (0=Onsemi, 1=Infineon — 110kW models only)
		 *   BIT04 Fan detection hardware support (5–20kW: 1=yes)
		 *   BIT05 FCAS function running (0=not running, 1=running)
		 *   BIT06 AFCI self-test ended
		 *   BIT07 AFCI self-test found arc
		 *   BIT08-09 Main DSP chip type (00=F28062, 01=F28374S)
		 *   BIT10 IGBT screening complete
		 *   BIT11-13 Reserved
		 *   BIT14 DSP waveform data ready (0=no waveform, 1=ready)
		 * </pre>
		 */
			
		// Decoded sub-channels:
		/** reg 33112 BIT00 – Model setting complete. */
		INIT_STATE_MODEL_SET(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Model setting complete (reg 33112 BIT00)")),
		
		/** reg 33112 BIT01 – National standard (grid code) setting complete. */
		INIT_STATE_GRID_CODE_SET(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Grid code setting complete (reg 33112 BIT01)")),
		
		/** reg 33112 BIT02 – Power curve setting complete. */
		INIT_STATE_POWER_CURVE_SET(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Power curve setting complete (reg 33112 BIT02)")),
			
		/** reg 33112 BIT03 – Module ID (Infineon=1, Onsemi=0). */
		INIT_STATE_MODULE_TYPE_INFINEON(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Module type Infineon (reg 33112 BIT03)")),
	
		/** reg 33112 BIT04 – Fan detection hardware support. */
		INIT_STATE_FAN_DETECTION_SUPPORTED(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Fan detection supported (reg 33112 BIT04)")),
			
		/** reg 33112 BIT05 – FCAS function currently running. */
		INIT_STATE_FCAS_RUNNING(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("FCAS function running (reg 33112 BIT05)")),
			
		/** reg 33112 BIT06 – AFCI self-test ended. */
		INIT_STATE_AFCI_TEST_ENDED(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("AFCI self-test ended (reg 33112 BIT06)")),
		
		/** reg 33112 BIT07 – AFCI self-test found arc. */
		INIT_STATE_AFCI_ARC_FOUND(Doc.of(Level.FAULT)
				.accessMode(READ_ONLY)
				.text("AFCI self-test found arc (reg 33112 BIT07)")),
			
		/** reg 33112 BIT08 – DSP chip type bit 1. */
		INIT_STATE_DSP_CHIP_TYPE_1(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("DSP chip type bit 1 (reg 33112 BIT08)")),
		
		/** reg 33112 BIT09 – DSP chip type bit 2. */
		INIT_STATE_DSP_CHIP_TYPE_2(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("DSP chip type bit 2 (reg 33112 BIT09)")),
			
		/** reg 33112 BIT10 – IGBT screening complete. */
		INIT_STATE_IGBT_SCREENING_COMPLETED(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("IGBT screening complete (reg 33112 BIT10)")),
			
		/** reg 33112 BIT11 – Reserved. */
		INIT_STATE_RESERVED_11(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Reserved (reg 33112 BIT11)")),
		
		/** reg 33112 BIT12 – Reserved. */
		INIT_STATE_RESERVED_12(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Reserved (reg 33112 BIT12)")),
		
		/** reg 33112 BIT13 – Reserved. */
		INIT_STATE_RESERVED_13(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Reserved (reg 33112 BIT13)")),
			
		/** reg 33112 BIT14 – DSP waveform data ready. */
		INIT_STATE_WAVEFORM_READY(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("DSP waveform data ready (reg 33112 BIT14)")),

		/**
		 * Batch Upgrade Support (reg 33113, U16, read-only).
		 *
		 * <p>Datasheet: "If it supports batch upgrade."
		 * Read before attempting a batch firmware upgrade to confirm both processors support it.
		 *
		 * <pre>
		 *   BIT00 DSP supports batch upgrade  0=no, 1=yes
		 *   BIT04 ARM supports batch upgrade  0=no, 1=yes
		 *   All other bits: unused / always 0
		 * </pre>
		 */
			
		/** reg 33113 BIT00 – DSP processor supports batch upgrade. */
		BATCH_UPGRADE_DSP(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("DSP supports batch upgrade (reg 33113 BIT00)")),
		
		/** reg 33113 BIT04 – ARM processor supports batch upgrade. */
		BATCH_UPGRADE_ARM(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("ARM supports batch upgrade (reg 33113 BIT04)")),

		/** FCAS Mode Running Status (reg 33114, U16, FC4).
		 * Datasheet: 0=not running (default), 1=running.
		 * FCAS = Frequency Control Ancillary Service (Australian grid services). */
		FCAS_MODE_RUNNING_STATUS(Doc.of(EnableDisable.values())
				.accessMode(READ_ONLY)),

		// ── Appendix 7 ── Register 33115 decoded bits ──
		SETTING_FLAG_FLASH_TIMEOUT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("FLASH read/write timeout")),
		SETTING_FLAG_CLEAR_ENERGY(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Clear energy flag completed (reg 33115 BIT01)")),
		SETTING_FLAG_RESERVED_02(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT02)")),
		SETTING_FLAG_RESERVED_03(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT03)")),
		SETTING_FLAG_RESERVED_04(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT04)")),
		SETTING_FLAG_RESERVED_05(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT05)")),
		SETTING_FLAG_RESERVED_06(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT06)")),
		SETTING_FLAG_RESERVED_07(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT07)")),
		SETTING_FLAG_RESET_DATALOGGER(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reset datalogger completed (reg 33115 BIT08)")),
		SETTING_FLAG_FACTORY_RECOVER(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Return factory setting completed (reg 33115 BIT09)")),
		SETTING_FLAG_RESERVED_10(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT10)")),
		SETTING_FLAG_RESERVED_11(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT11)")),
		SETTING_FLAG_RESERVED_12(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT12)")),
		SETTING_FLAG_RESERVED_13(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT13)")),
		SETTING_FLAG_RESERVED_14(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT14)")),
		SETTING_FLAG_RESERVED_15(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Reserved (reg 33115 BIT15)")),

		// ── Appendix 4 ── Register 33116 decoded bits ──
		FAULT_REG1_NO_GRID(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("No grid")),
		FAULT_REG1_GRID_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid overvoltage")),
		FAULT_REG1_GRID_UNDERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid undervoltage")),
		FAULT_REG1_GRID_OVERFREQ(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid overfrequency")),
		FAULT_REG1_GRID_UNDERFREQ(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid underfrequency")),
		FAULT_REG1_UNBALANCED_GRID(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Unbalanced grid")),
		FAULT_REG1_GRID_FREQ_FLUCTUATION(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid frequency fluctuation")),
		FAULT_REG1_GRID_REVERSE_CURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid reverse current")),
		FAULT_REG1_GRID_CURRENT_TRACKING_ERROR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid current tracking error")),
		FAULT_REG1_METER_COM_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("METER COM fail")),
		FAULT_REG1_FAILSAFE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("FailSafe")),
		FAULT_REG1_METER_SELECT_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Meter Select Fail")),
		FAULT_REG1_EPM_HARD_LIMIT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("EPM Hard Limit Protection")),
		FAULT_REG1_G100_CURRENT_OVER_LIMIT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("G100 Current Over Limit")),
		FAULT_REG1_RESERVED_14(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG1 BIT14)")),
		FAULT_REG1_ABNORMAL_GRID_PHASE_POLARITY(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Abnormal grid phase polarity")),

		// ── Appendix 4 ── Register 33117 decoded bits ──
		FAULT_REG2_BACKUP_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Backup overvoltage fault")),
		FAULT_REG2_BACKUP_OVERLOAD(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Backup overload fault")),
		FAULT_REG2_GRID_BACKUP_OVERLOAD(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid Backup overload")),
		FAULT_REG2_OFFGRID_BACKUP_UNDERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Off-grid Backup undervoltage")),
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
		FAULT_REG3_BATTERY_OVERVOLTAGE_CHECK(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery overvoltage check")),
		FAULT_REG3_BATTERY_UNDERVOLTAGE_CHECK(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery undervoltage check")),
		FAULT_REG3_BATTERY_BMS_ALARM(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery BMS Alarm")),
		FAULT_REG3_INCONSISTENT_BATTERY_SELECTION(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Inconsistent battery selection")),
		FAULT_REG3_LEAD_ACID_TEMP_TOO_LOW(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Lead-acid battery temperature too low")),
		FAULT_REG3_LEAD_ACID_TEMP_TOO_HIGH(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Lead-acid battery temperature too high")),
		FAULT_REG3_SECOND_BATTERY_NOT_CONNECTED(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Second battery not connected")),
		FAULT_REG3_SECOND_BATTERY_SW_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Second battery software overvoltage")),
		FAULT_REG3_SECOND_BATTERY_SW_UNDERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Second battery software undervoltage")),
		FAULT_REG3_PARALLEL_BATTERY_COM_ABNORMAL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Parallel battery communication abnormal")),
		FAULT_REG3_LOW_BATTERY_OFFGRID(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Low battery (off-grid)")),
		FAULT_REG3_RESERVED_12(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG3 BIT12)")),
		FAULT_REG3_RESERVED_13(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG3 BIT13)")),
		FAULT_REG3_RESERVED_14(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG3 BIT14)")),
		FAULT_REG3_RESERVED_15(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Reserved (REG3 BIT15)")),

		// ── Appendix 4 ── Register 33119 decoded bits ──
		FAULT_REG4_DC_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC overvoltage")),
		FAULT_REG4_DC_BUS_OVERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC Bus overvoltage")),
		FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC Bus unbalanced voltage")),
		FAULT_REG4_DC_BUS_UNDERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC Bus undervoltage")),
		FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE_2(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC Bus unbalanced voltage 2")),
		FAULT_REG4_DC_OVERCURRENT_A(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC overcurrent on A circuit")),
		FAULT_REG4_DC_OVERCURRENT_B(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC overcurrent on B circuit")),
		FAULT_REG4_DC_INPUT_INTERFERENCE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DC input interference")),
		FAULT_REG4_GRID_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid overcurrent")),
		FAULT_REG4_IGBT_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("IGBT overcurrent")),
		FAULT_REG4_GRID_INTERFERENCE_02(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid interference 02")),
		FAULT_REG4_AFCI_SELF_CHECK(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("AFCI self-check")),
		FAULT_REG4_ARC_FAULT_RESERVED(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Arc fault reserved")),
		FAULT_REG4_GRID_CURRENT_SAMPLING_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid current sampling fault")),
		FAULT_REG4_DSP_SELF_CHECK_ERROR(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DSP self-check error")),
		FAULT_REG4_BATTERY_DISCHARGE_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery Discharge Overcurrent")),

		// ── Appendix 4 ── Register 33120 decoded bits ──
		FAULT_REG5_GRID_INTERFERENCE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid interference")),
		FAULT_REG5_OVER_DC_COMPONENTS(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Over DC components")),
		FAULT_REG5_OVER_TEMPERATURE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Over temperature protection")),
		FAULT_REG5_RELAY_CHECK(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Relay check protection")),
		FAULT_REG5_UNDER_TEMPERATURE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Under temperature protection")),
		FAULT_REG5_PV_INSULATION_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("PV insulation fault")),
		FAULT_REG5_12V_UNDERVOLTAGE(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("12V undervoltage protection")),
		FAULT_REG5_LEAK_CURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Leak current protection")),
		FAULT_REG5_LEAK_CURRENT_SELF_CHECK(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Leak current self-check protection")),
		FAULT_REG5_DSP_INITIAL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DSP initial protection")),
		FAULT_REG5_DSP_B(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("DSP B protection")),
		FAULT_REG5_BATTERY_OVERVOLTAGE_HW(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery overvoltage hardware fault")),
		FAULT_REG5_LLC_HW_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("LLC hardware overcurrent")),
		FAULT_REG5_GRID_TRANSIENT_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid transient overcurrent")),
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
		FAULT_REG7_BATTERY_HW_OVERVOLTAGE_02(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery hardware overvoltage 02")),
		FAULT_REG7_BATTERY_HW_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery hardware overcurrent")),
		FAULT_REG7_BUS_MIDPOINT_HW_OVERCURRENT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Bus midpoint hardware overcurrent")),
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

		// -----------------------------------------------------------------------
		// Inverter operating status (reg 33287)
		// -----------------------------------------------------------------------

		/**
		 * Inverter operating status (reg 33287, U16).
		 * 0 = Stop, 1 = Open loop, 2 = Soft start, 3 = Grid-connected
		 * 4 = Off-grid/EPS, 5 = Off-grid to on-grid transition, 6 = Backup bypass
		 * 7 = Generator running
		 * See {@link OperatingStatus} enum
		 */
		INVERTER_OPERATING_STATUS(Doc.of(InverterOperatingStatus.values())),
		// ── Appendix 5 ── Register 33121 / 36026 decoded bits ──
		OPERATING_STAT_NORMAL_OPERATION(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Normal Operation")),
		OPERATING_STAT_INITIALIZING(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Initializing")),
		OPERATING_STAT_CONTROLLED_OFF(Doc.of(BOOLEAN).accessMode(READ_ONLY).text("Controlled turning OFF (reg 33121 BIT02)")),
		// Operating status bits (reg 33121). Only real faults are Level.FAULT: the
		// component State drives the work state machine (ERROR stops ApplyPower).
		// "Limited operation (external)" is set whenever reg 43052 < 100 %, i.e.
		// whenever the EMS limits the AC output - a status, not a fault.
		OPERATING_STAT_FAULT_OFF(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Fault leads to turning OFF")),
		OPERATING_STAT_STANDBY(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Stand-by")),
		OPERATING_STAT_LIMITED_TEMP_FREQ(Doc.of(Level.WARNING).accessMode(READ_ONLY).text("Limited Operation (temperature/frequency)")),
		OPERATING_STAT_LIMITED_EXTERNAL(Doc.of(Level.INFO).accessMode(READ_ONLY).text("Limited Operation (external reason)")),
		OPERATING_STAT_BACKUP_OVERLOAD(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Backup overload")),
		OPERATING_STAT_LOAD_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Load fault")),
		OPERATING_STAT_GRID_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Grid fault")),
		OPERATING_STAT_BATTERY_FAULT(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("Battery fault")),
		OPERATING_STAT_RESERVED_11(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (OPSTAT BIT11)")),
		OPERATING_STAT_GRID_SURGE_WARN(Doc.of(Level.WARNING).accessMode(READ_ONLY).text("Grid Surge (Warn)")),
		OPERATING_STAT_FAN_FAULT_WARN(Doc.of(Level.WARNING).accessMode(READ_ONLY).text("Fan fault (Warn)")),
		OPERATING_STAT_EXTERNAL_FAN_FAIL(Doc.of(Level.FAULT).accessMode(READ_ONLY).text("External fan failure")),
		OPERATING_STAT_RESERVED_15(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (OPSTAT BIT15)")),

		/** Operating Mode Raw Register (reg 33122, U16, FC4).
		 * Datasheet: "Only one bit is valid at any time." One-hot bitmask.
		 * Decoded to the human-readable {@link Appendix8} enum by decodeOperatingMode()
		 * and stored in {@link ChannelId#OPERATING_MODE_DECODE}. */
		OPERATING_MODE(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),
		
		/**
		 * Operating Mode Decoded (virtual channel, not a Modbus register).
		 * Populated each cycle by decodeOperatingMode() in PytesJs3Impl.
		 * Translates the one-hot bitmask from {@link ChannelId#OPERATING_MODE} (reg 33122)
		 * into the human-readable {@link Appendix8} enum value.
		 * Remains undefined until the first valid Modbus frame is received.
		 */
		OPERATING_MODE_DECODE(Doc.of(Appendix8.values())
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Working Mode Running Status (reg 33123, U16, read-only).
		 *
		 * <p>Datasheet: "Every bit represents one working mode. 0=Stop, 1=Run."
		 * For Hawaii standard (4777-A/B/C/N, TOR, UL0240-18) models this register
		 * shows which standard working mode is active. For other grid standards use
		 * reg 33091 (STANDARD_WORKING_MODE) instead.
		 *
		 * <p>Constraint: only one reactive power mode from BIT01–BIT04 can be active
		 * at the same time. BIT00 (Volt-watt) is independent.
		 *
		 * <pre>
		 *   BIT00 Volt-watt             0=stopped, 1=running
		 *   BIT01 Volt-var              0=stopped, 1=running
		 *   BIT02 Fixed power factor    0=stopped, 1=running
		 *   BIT03 Fix reactive power    0=stopped, 1=running
		 *   BIT04 Power-PF              0=stopped, 1=running
		 *   BIT05 Power-Q               0=stopped, 1=running
		 *   BIT06–BIT15 Reserved
		 * </pre>
		 */
			
		/** reg 33123 BIT00 – Volt-watt mode running. */
		WMODE_VOLT_WATT(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Volt-watt mode running (reg 33123 BIT00)")),
		
		/** reg 33123 BIT01 – Volt-var mode running. */
		WMODE_VOLT_VAR(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Volt-var mode running (reg 33123 BIT01)")),
		
		/** reg 33123 BIT02 – Fixed power factor mode running. */
		WMODE_FIXED_PF(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Fixed power factor mode running (reg 33123 BIT02)")),
		
		/** reg 33123 BIT03 – Fixed reactive power mode running. */
		WMODE_FIX_REACTIVE(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Fixed reactive power mode running (reg 33123 BIT03)")),
		
		/** reg 33123 BIT04 – Power-PF mode running. */
		WMODE_POWER_PF(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Power-PF mode running (reg 33123 BIT04)")),
		
		/** reg 33123 BIT05 – Power-Q mode running. */
		WMODE_POWER_Q(Doc.of(BOOLEAN)
				.accessMode(READ_ONLY)
				.text("Power-Q mode running (reg 33123 BIT05)")),

		// ── Appendix 6 ── Register 33132 decoded bits ──
		STORAGE_CTRL_SELF_USE_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Self-Use Mode")),
		STORAGE_CTRL_TIME_OF_USE_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Time of Use mode")),
		STORAGE_CTRL_OFFGRID_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("OFF-grid mode")),
		STORAGE_CTRL_BATT_WAKEUP(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery wakeup switch")),
		STORAGE_CTRL_RESERVE_BATT_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserve battery mode")),
		STORAGE_CTRL_ALLOW_GRID_CHARGE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Allow grid to charge battery")),
		STORAGE_CTRL_FEED_IN_PRIORITY(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Feed in priority mode")),
		STORAGE_CTRL_BATT_OVC(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Batt OVC Function")),
		STORAGE_CTRL_FORCE_CHARGE_PEAKSHAVING(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery force charge / peak shaving")),
		STORAGE_CTRL_BATT_CURRENT_CORRECTION(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery current correction enable")),
		STORAGE_CTRL_BATT_HEALING_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Battery healing mode")),
		STORAGE_CTRL_PEAK_SHAVING_MODE(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Peak-shaving mode")),
		STORAGE_CTRL_RESERVED_12(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (STORAGE BIT12)")),
		STORAGE_CTRL_RESERVED_13(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (STORAGE BIT13)")),
		STORAGE_CTRL_RESERVED_14(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (STORAGE BIT14)")),
		STORAGE_CTRL_RESERVED_15(Doc.of(OpenemsType.BOOLEAN).accessMode(READ_ONLY).text("Reserved (STORAGE BIT15)")),

		// -----------------------------------------------------------------------------------------------------------------------
		// Remote control legacy channels — register addresses not confirmed in current datasheet revision.
		// These channels are retained for API compatibility. Use the remote-dispatch path
		// (reg 44100–44108) for production use. See setRemoteControlMode() and setRemoteControlPower().
		// -----------------------------------------------------------------------------------------------------------------------

		SET_REMOTE_CONTROL_AC_GRID_PORT_POWER(Doc.of(INTEGER)
				.accessMode(AccessMode.WRITE_ONLY)
				.unit(Unit.WATT)),
		
		REMOTE_CONTROL_AC_GRID_PORT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)), //		

		SET_REMOTE_CONTROL_MODE(Doc.of(INTEGER)
				.accessMode(AccessMode.WRITE_ONLY)),
		
		REMOTE_CONTROL_MODE(Doc.of(OpenemsType.INTEGER) 
				.accessMode(AccessMode.READ_ONLY)),		

		SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER(Doc.of(INTEGER) 
				.accessMode(AccessMode.WRITE_ONLY)),

		REMOTE_DISPATCH_REALTIME_CONTROL_POWER(Doc.of(INTEGER) 
				.accessMode(AccessMode.READ_ONLY)),

		STARTER_BATTERY_VOLTAGE(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.VOLT)
				.persistencePriority(LOW)),

		INVERTED_RATED_APPARENT_POWER(Doc.of(INTEGER)
				.accessMode(READ_ONLY)
				.unit(Unit.VOLT_AMPERE)),

		FUNCTION_STATUS(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		INVERTER_INITIAL_SETTING_STATE(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		BATCH_UPGRADE_BOWL(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		PV_SHUTDOWN_SWITCH(Doc.of(EnableDisable.values())
				.accessMode(READ_ONLY)
				.text("PV shutdown mode")),
		
		GRID_CHARGE_ALLOWED(Doc.of(EnableDisable.values())
				.accessMode(READ_ONLY)
				.text("Battery grid charge allowed")),
		
		DO_CONTROL(Doc.of(EnableDisable.values())
				.accessMode(READ_ONLY)
				.text("DO Control enabled")),
		
		OFF_GRID_BATTERY_STANDBY(Doc.of(EnableDisable.values())
				.accessMode(READ_ONLY)
				.text("Off-grid battery standby")),

		SETTING_FLAG_BIT(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		OPERATING_STATUS(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		WORKING_MODE_RUNNING_STATUS(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		STORAGE_CONTROL_SWITCHING_VALUE(Doc.of(INTEGER)
				.accessMode(READ_ONLY)),

		/**
		 * Storage Control switch, reg 43110
		 * persistent values.
		 * BIT00 Self-Use Mode                         1 = ON
		 * BIT01 Time of Use mode                      1 = ON
		 * BIT02 Off-grid mode                         1 = ON
		 * BIT03 Battery wakeup switch                 1 = ON
		 * BIT04 Reserve battery mode                  1 = ON
		 * BIT05 Allow grid to charge battery          1 = Allow
		 * BIT06 Feed-in priority mode                 1 = ON
		 * BIT07 Batt OVC Function                     1 = ON
		 * BIT08 Battery force-charge peak-shaving     1 = ON
		 * BIT09 Battery current correction enable     1 = ON
		 * BIT10 Battery healing mode                  1 = ON
		 * BIT11 Peak-shaving mode                     1 = ON
		 * BIT12–15 Reserved
		 * See {@link OperatingStatus} enum
		 */
		SET_STORAGE_CTRL_SWITCH(Doc.of(INTEGER)
				.accessMode(WRITE_ONLY)),		
				
		
		
		ACTIVE_POWER_OLD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Discharge or charging Power (including DC-PV power, if applicable)."
						+ " For the actual charging or discharging power of the battery, please refer to address"
						+ " \"ess0/DcDischargePower\". Negative values for charge; positive for discharge.")//
		),
				
		
		ACTIVE_POWER_151(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Discharge or charging Power (including DC-PV power, if applicable)."
						+ " For the actual charging or discharging power of the battery, please refer to address"
						+ " \"ess0/DcDischargePower\". Negative values for charge; positive for discharge.")//
		),
		
		ACTIVE_POWER_157(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)//
				.text("Discharge or charging Power (including DC-PV power, if applicable)."
						+ " For the actual charging or discharging power of the battery, please refer to address"
						+ " \"ess0/DcDischargePower\". Negative values for charge; positive for discharge.")//
		);		
		


		// -----------------------------------------------------------------------------------------------------------------------
		// -----------------------------------------------------------------------------------------------------------------------
		// -----------------------------------------------------------------------------------------------------------------------

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	// -----------------------------------------------------------------------------
	// Helpers: WorkState (internal state)
	// -----------------------------------------------------------------------------

	/**
	 * Returns the Channel for {@link ChannelId#WORK_STATE}.
	 *
	 * @return the {@link Channel}
	 */
	public default Channel<WorkState> getWorkStateChannel() {
		return this.channel(ChannelId.WORK_STATE);
	}

	/**
	 * Gets the current internal work state of this component.
	 *
	 * @return the current {@link WorkState}
	 */
	public default WorkState getWorkState() {
		return this.getWorkStateChannel().value().asEnum();
	}

	/**
	 * Internal method to set {@link WorkState} of this component.
	 *
	 * <p>
	 * The value is written to the corresponding Channel using
	 * {@link io.openems.edge.common.channel.Channel#setNextValue(Object)} and will
	 * be applied in the next processing cycle.
	 * </p>
	 *
	 * @param value the new {@link WorkState} to set
	 */
	public default void _setWorkState(WorkState value) {
		this.getWorkStateChannel().setNextValue(value);
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

	/**
	 * Current remote dispatch switch state. See {@link ChannelId#REMOTE_DISPATCH_SWITCH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default EnableDisable getRemoteDispatchSwitch() {
	    return this.getRemoteDispatchSwitchChannel().value().asEnum();
	}

	/**
	 * Returns the read-back Channel for {@link ChannelId#REMOTE_DISPATCH_SWITCH}
	 * (reg 44100, FC3).
	 *
	 * @return the {@link Channel}
	 */
	public default Channel<EnableDisable> getRemoteDispatchSwitchChannel() {
	    return this.channel(ChannelId.REMOTE_DISPATCH_SWITCH);
	}

	/**
	 * Returns the write Channel for {@link ChannelId#SET_REMOTE_DISPATCH_SWITCH}
	 * (reg 44100, FC16).
	 *
	 * @return the {@link WriteChannel}
	 */
	public default WriteChannel<EnableDisable> setRemoteDispatchSwitchChannel() {
	    return this.channel(ChannelId.SET_REMOTE_DISPATCH_SWITCH);
	}

	// Enable / Disable Backup Port
	/**
	 * Enables or disables backup AC port.
	 * Register 43111.
	 *
	 * @param value the {@link EnableDisable} value
	 * @throws OpenemsNamedException on error
	 */
	public default void setBackupCircuitSetting(EnableDisable value) throws OpenemsNamedException {
	    this.setBackupCircuitSettingChannel().setNextWriteValue(value);
	}

	/**
	 * Current backup circuit relay state. See {@link ChannelId#BACKUP_CIRCUIT_SETTING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default EnableDisable getBackupCircuitSetting() {
	    return this.getBackupCircuitSettingChannel().value().asEnum();
	}

	/**
	 * Returns the read-back Channel for {@link ChannelId#BACKUP_CIRCUIT_SETTING}
	 * (reg 43111, FC3).
	 *
	 * @return the {@link Channel}
	 */
	public default Channel<EnableDisable> getBackupCircuitSettingChannel() {
	    return this.channel(ChannelId.BACKUP_CIRCUIT_SETTING);
	}

	/**
	 * Returns the write Channel for {@link ChannelId#SET_BACKUP_CIRCUIT_SETTING}
	 * (reg 43111, FC16).
	 *
	 * @return the {@link WriteChannel}
	 */
	public default WriteChannel<EnableDisable> setBackupCircuitSettingChannel() {
	    return this.channel(ChannelId.SET_BACKUP_CIRCUIT_SETTING);
	}

	// Set remote dispatch timeout
	/**
	 * Sets the Remote Dispatch Failsafe timeout (reg 44101, FC16).
	 * If the remote dispatch heartbeat stops, the inverter reverts to local control
	 * after this many minutes. Write 0xFFFF to reset to default (5 minutes).
	 * Range: 1–1440 minutes.
	 *
	 * @param value timeout in minutes
	 * @throws OpenemsNamedException on write error
	 */
	public default void setRemoteDispatchFailsafeSetting(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchFailsafeSettingChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the current failsafe timeout read-back value (reg 44101, FC3).
	 *
	 * @return the {@link IntegerReadChannel}
	 */
	public default IntegerReadChannel getRemoteDispatchFailsafeSettingChannel() {
		return this.channel(ChannelId.REMOTE_DISPATCH_FAILSAFE_SETTING);
	}

	public default IntegerWriteChannel getSetRemoteDispatchFailsafeSettingChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_FAILSAFE_SETTING);
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

	/**
	 * Current system limit switch state. See {@link ChannelId#REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default RemoteDispatchSystemLimitSwitch getRemoteDispatchSystemLimitSwitch() {
	    return this.getRemoteDispatchSystemLimitSwitchChannel().value().asEnum();
	}

	/**
	 * Returns the read-back Channel for
	 * {@link ChannelId#REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH} (reg 44102, FC3).
	 *
	 * @return the {@link Channel}
	 */
	public default Channel<RemoteDispatchSystemLimitSwitch> getRemoteDispatchSystemLimitSwitchChannel() {
	    return this.channel(ChannelId.REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH);
	}

	/**
	 * Returns the write Channel for
	 * {@link ChannelId#SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH} (reg 44102, FC16).
	 *
	 * @return the {@link WriteChannel}
	 */
	public default WriteChannel<RemoteDispatchSystemLimitSwitch> setRemoteDispatchSystemLimitSwitchChannel() {
	    return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH);
	}

	// Set remote dispatch system import limit
	/**
	 * Sets the system import power limit (reg 44103, FC16).
	 * Register resolution is 100 W; the channel converter scales from W.
	 * Active only when REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH BIT00 = 1 (import enabled).
	 *
	 * @param value import limit in W (rounded down to 100 W)
	 * @throws OpenemsNamedException on write error
	 */
	public default void setRemoteDispatchSystemImportLimit(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchSystemImportLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Returns the write Channel for
	 * {@link ChannelId#SET_REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT} (reg 44103, FC16).
	 *
	 * @return the {@link IntegerWriteChannel}
	 */
	public default IntegerWriteChannel getSetRemoteDispatchSystemImportLimitChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_IMPORT_LIMIT);
	}

	// Set remote dispatch system export limit
	/**
	 * Sets the system export power limit (reg 44104, FC16).
	 * Register resolution is 100 W; the channel converter scales from W.
	 * Active only when REMOTE_DISPATCH_SYSTEM_LIMIT_SWITCH BIT01 = 1 (export enabled).
	 *
	 * @param value export limit in W (rounded down to 100 W)
	 * @throws OpenemsNamedException on write error
	 */
	public default void setRemoteDispatchSystemExportLimit(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchSystemExportLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Returns the write Channel for
	 * {@link ChannelId#SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT} (reg 44104, FC16).
	 *
	 * @return the {@link IntegerWriteChannel}
	 */
	public default IntegerWriteChannel getSetRemoteDispatchSystemExportLimitChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_SYSTEM_EXPORT_LIMIT);
	}

	// Set realtime control power (S32 value)
	/**
	 * Sets the real-time control power setpoint (reg 44106–44107, S32, FC16).
	 * Resolution: 1 unit = 10 W.
	 * When reg 44105=2 (battery control): negative=discharge, positive=charge.
	 * When reg 44105=3 or 4 (grid control): negative=import, positive=export.
	 *
	 * @param value the power setpoint in units of 10 W
	 * @throws OpenemsNamedException on write error
	 */
	public default void setRemoteDispatchRealtimeControlPower(int value) throws OpenemsNamedException {
		this.getSetRemoteDispatchRealtimeControlPowerChannel().setNextWriteValue(value);
	}

	/**
	 * Returns the write Channel for
	 * {@link ChannelId#SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER} (reg 44106–44107, S32, FC16).
	 *
	 * @return the {@link IntegerWriteChannel}
	 */
	public default IntegerWriteChannel getSetRemoteDispatchRealtimeControlPowerChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_POWER);
	}

	// Get realtime control function switch
	/**
	 * Gets the current raw value of the Real-Time Control Function Switch (reg 44108).
	 * The raw word encodes four 2-bit function states decoded by installListeners()
	 * into PV_SHUTDOWN_SWITCH, DO_CONTROL, GRID_CHARGE_ALLOWED, OFF_GRID_BATTERY_STANDBY.
	 *
	 * @return the raw bitmask, or {@code null} if not yet available
	 */
	public default Integer getRemoteDispatchRealtimeControlFunctionSwitch() {
		return this.getRemoteDispatchRealtimeControlFunctionSwitchChannel().value().get();
	}

	/**
	 * Returns the read-back Channel for
	 * {@link ChannelId#REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH}
	 * (reg 44108, FC3).
	 *
	 * @return the {@link Channel}
	 */
	public default Channel<Integer> getRemoteDispatchRealtimeControlFunctionSwitchChannel() {
		return this.channel(ChannelId.REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH);
	}

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

	/**
	 * Returns the write Channel for
	 * {@link ChannelId#SET_REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH}
	 * (reg 44108, FC16).
	 *
	 * @return the {@link IntegerWriteChannel}
	 */
	public default IntegerWriteChannel getSetRemoteDispatchRealtimeControlFunctionSwitchChannel() {
		return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_FUNCTION_SWITCH);
	}

	// Operating Status
	/**
	 * Gets the Channel for {@link ChannelId#OPERATING_STATUS}.
	 *
	 * <p>0 = Stop
	 * 1 = Open loop
	 * 2 = Soft start
	 * 3 = Grid-connected
	 * 4 = Off-grid/EPS
	 * 5 = Off-grid to on-grid transition
	 * 6 = Backup bypass
	 * 7 = Generator running
	 * 
	 * @return the Channel
	 */
	public default Channel<InverterOperatingStatus> getInverterOperatingStatusChannel() {
		return this.channel(ChannelId.INVERTER_OPERATING_STATUS);
	}

	/**
	 * Gets the operating status.
	 *
	 * @return the {@link EnableDisable} value
	 */
	public default InverterOperatingStatus getInverterOperatingStatus() {
		return this.getInverterOperatingStatusChannel().value().asEnum();
	}	
	

	// PV Shutdown
	/**
	 * Gets the Channel for {@link ChannelId#PV_SHUTDOWN_SWITCH}.
	 *
	 * @return the Channel
	 */
	public default Channel<EnableDisable> getPvShutdownSwitchChannel() {
		return this.channel(ChannelId.PV_SHUTDOWN_SWITCH);
	}

	/**
	 * Gets the PV shutdown switch.
	 *
	 * @return the {@link EnableDisable} value
	 */
	public default EnableDisable getPvShutdownSwitch() {
		return this.getPvShutdownSwitchChannel().value().asEnum();
	}

	/**
	 * Internal method to set the PV shutdown switch.
	 *
	 * @param value the {@link EnableDisable} value
	 */
	public default void _setPvShutdownSwitch(EnableDisable value) {
		this.getPvShutdownSwitchChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#GRID_CHARGE_ALLOWED}.
	 *
	 * @return the Channel
	 */
	public default Channel<EnableDisable> getGridChargeAllowedChannel() {
		return this.channel(ChannelId.GRID_CHARGE_ALLOWED);
	}

	/**
	 * Gets if grid charging is allowed.
	 *
	 * @return the {@link EnableDisable} value
	 */
	public default EnableDisable getGridChargeAllowed() {
		return this.getGridChargeAllowedChannel().value().asEnum();
	}

	/**
	 * Internal method to set the grid charging allowed channel.
	 *
	 * @param value the {@link EnableDisable} value
	 */
	public default void _setGridChargeAllowed(EnableDisable value) {
		this.getGridChargeAllowedChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DO_CONTROL}.
	 *
	 * @return the Channel
	 */
	public default Channel<EnableDisable> getDoControlChannel() {
		return this.channel(ChannelId.DO_CONTROL);
	}

	/**
	 * Gets the DO control state.
	 *
	 * @return the {@link EnableDisable} value
	 */
	public default EnableDisable getDoControl() {
		return this.getDoControlChannel().value().asEnum();
	}

	/**
	 * Internal method to set the DO control state channel.
	 *
	 * @param value the {@link EnableDisable} value
	 */
	public default void _setDoControl(EnableDisable value) {
		this.getDoControlChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OFF_GRID_BATTERY_STANDBY}.
	 *
	 * @return the Channel
	 */
	public default Channel<EnableDisable> getOffGridBatteryStandbyChannel() {
		return this.channel(ChannelId.OFF_GRID_BATTERY_STANDBY);
	}

	/**
	 * Gets the off-grid battery standby state.
	 *
	 * @return the {@link EnableDisable} value
	 */
	public default EnableDisable getOffGridBatteryStandby() {
		return this.getOffGridBatteryStandbyChannel().value().asEnum();
	}

	/**
	 * Internal method to set the off-grid battery standby state.
	 *
	 * @param value the {@link EnableDisable} value
	 */
	public default void _setOffGridBatteryStandby(EnableDisable value) {
		this.getOffGridBatteryStandbyChannel().setNextValue(value);
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

	/**
	 * Current realtime control switch mode. See {@link ChannelId#REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default RemoteDispatchRealtimeControlSwitch getRemoteDispatchRealtimeControlSwitch() {
	    return this.getRemoteDispatchRealtimeControlSwitchChannel().value().asEnum();
	}

	/**
	 * Returns the read-back Channel for
	 * {@link ChannelId#REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH} (reg 44105, FC3).
	 *
	 * @return the {@link Channel}
	 */
	public default Channel<RemoteDispatchRealtimeControlSwitch> getRemoteDispatchRealtimeControlSwitchChannel() {
	    return this.channel(ChannelId.REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH);
	}

	/**
	 * Returns the write Channel for
	 * {@link ChannelId#SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH} (reg 44105, FC16).
	 *
	 * @return the {@link WriteChannel}
	 */
	public default WriteChannel<RemoteDispatchRealtimeControlSwitch> setRemoteDispatchRealtimeControlSwitchChannel() {
	    return this.channel(ChannelId.SET_REMOTE_DISPATCH_REALTIME_CONTROL_SWITCH);
	}


	// Set remote control mode
	/**
	 * Sets the Remote Control Mode (legacy direct-control register).
	 * Prefer the remote-dispatch path (reg 44100–44108) for production use.
	 *
	 * @param value the mode integer value
	 * @throws OpenemsNamedException on write error
	 */
	public default void setRemoteControlMode(int value) throws OpenemsNamedException {
		this.getSetRemoteControlModeChannel().setNextWriteValue(value);
	}

	/**
	 * Returns the write Channel for {@link ChannelId#SET_REMOTE_CONTROL_MODE}.
	 *
	 * @return the {@link IntegerWriteChannel}
	 */
	public default IntegerWriteChannel getSetRemoteControlModeChannel() {
		return this.channel(ChannelId.SET_REMOTE_CONTROL_MODE);
	}
	
	/**
	 * Remote control mode read-back value. See {@link ChannelId#REMOTE_CONTROL_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getRemoteControlMode() {
		return this.getRemoteControlModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#REMOTE_CONTROL_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRemoteControlModeChannel() {
		return this.channel(ChannelId.REMOTE_CONTROL_MODE);
	}		

	// Set power setpoint
	/**
	 * Sets the Remote Control AC Grid Port power setpoint.
	 * Positive = export, negative = import. Resolution: 1 W.
	 *
	 * @param value the power setpoint in watts
	 * @throws OpenemsNamedException on write error
	 */
	public default void setRemoteControlPower(int value) throws OpenemsNamedException {
		this.getSetRemoteControlPowerChannel().setNextWriteValue(value);
	}

	/**
	 * Returns the write Channel for
	 * {@link ChannelId#SET_REMOTE_CONTROL_AC_GRID_PORT_POWER}.
	 *
	 * @return the {@link IntegerWriteChannel}
	 */
	public default IntegerWriteChannel getSetRemoteControlPowerChannel() {
		return this.channel(ChannelId.SET_REMOTE_CONTROL_AC_GRID_PORT_POWER);
	}
	
	/**
	 * Remote control AC grid port power read-back [W]. See {@link ChannelId#REMOTE_CONTROL_AC_GRID_PORT_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getRemoteControlPower() {
		return this.getRemoteControlPowerChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#REMOTE_CONTROL_AC_GRID_PORT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getRemoteControlPowerChannel() {
		return this.channel(ChannelId.REMOTE_CONTROL_AC_GRID_PORT_POWER);
	}	

	/**
	 * Sets the Max Charge SoC (reg 43010).
	 * Valid range: 80–100%. Default: 100%.
	 *
	 * @param value the SoC percentage to set
	 * @throws OpenemsNamedException on error
	 */
	public default void setMaxChargeSoc(int value) throws OpenemsNamedException {
		this.getSetMaxChargeSocChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetMaxChargeSocChannel() {
		return this.channel(ChannelId.SET_MAX_CHARGE_SOC);
	}

	// Overdischarge SoC
	/**
	 * Sets the Overdischarge SoC (reg 43011).
	 * Valid range: 5–40%. Must be >= Force Charge SoC (reg 43018). Default: 20%.
	 *
	 * @param value the SoC percentage to set
	 * @throws OpenemsNamedException on error
	 */
	public default void setOverdischargeSoc(int value) throws OpenemsNamedException {
		this.getSetOverdischargeSocChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetOverdischargeSocChannel() {
		return this.channel(ChannelId.SET_OVERDISCHARGE_SOC);
	}

	/**
	 * Gets the read-back Channel for {@link ChannelId#OVERDISCHARGE_SOC} (reg 43011, FC3).
	 * Used by setDefaultValues() to confirm the write was accepted by the inverter.
	 *
	 * @return the {@link IntegerReadChannel}
	 */
	public default IntegerReadChannel getOverDischargeSocChannel() {
		return this.channel(ChannelId.OVERDISCHARGE_SOC);
	}

	/**
	 * Gets the overdischarge SoC read-back value [%].
	 * See {@link ChannelId#OVERDISCHARGE_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getOverDischargeSoc() {
		return this.getOverDischargeSocChannel().value();
	}

	// FORCE CHARGE SOC
	/**
	 * Sets the Force Charge SoC (reg 43018).
	 * Valid range: 4% up to reg 43011. Must be <= Overdischarge SoC. Default: 10%.
	 *
	 * @param value the SoC percentage to set
	 * @throws OpenemsNamedException on error
	 */
	public default void setForceChargeSoc(int value) throws OpenemsNamedException {
		this.getSetForceChargeSocChannel().setNextWriteValue(value);
	}

	public default IntegerWriteChannel getSetForceChargeSocChannel() {
		return this.channel(ChannelId.SET_FORCE_CHARGE_SOC);
	}

	/**
	 * Gets the read-back Channel for {@link ChannelId#FORCE_CHARGE_SOC} (reg 43018, FC3).
	 * Used by setDefaultValues() to confirm the write was accepted by the inverter.
	 *
	 * @return the {@link IntegerReadChannel}
	 */
	public default IntegerReadChannel getForceChargeSocChannel() {
		return this.channel(ChannelId.FORCE_CHARGE_SOC);
	}

	/**
	 * Gets the force charge SoC read-back value [%].
	 * See {@link ChannelId#FORCE_CHARGE_SOC}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getForceChargeSoc() {
		return this.getForceChargeSocChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Version and identification (reg 33068–33070)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#SAFETY_VERSION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSafetyVersionChannel() {
		return this.channel(ChannelId.SAFETY_VERSION);
	}

	/**
	 * Safety (grid-code) version number — raw integer, no unit. See {@link ChannelId#SAFETY_VERSION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSafetyVersion() {
		return this.getSafetyVersionChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#HMI_SUB_VERSION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getHmiSubVersionChannel() {
		return this.channel(ChannelId.HMI_SUB_VERSION);
	}

	/**
	 * HMI sub-version number — raw integer, no unit. See {@link ChannelId#HMI_SUB_VERSION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getHmiSubVersion() {
		return this.getHmiSubVersionChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#ALARM_CODE_DATA}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getAlarmCodeDataChannel() {
		return this.channel(ChannelId.ALARM_CODE_DATA);
	}

	/**
	 * Alarm code data bitmask (used with INVERTER_CURRENT_STATUS for fault display). See {@link ChannelId#ALARM_CODE_DATA}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAlarmCodeData() {
		return this.getAlarmCodeDataChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – DC bus measurements (reg 33071–33072)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#DC_BUS_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcBusVoltageChannel() {
		return this.channel(ChannelId.DC_BUS_VOLTAGE);
	}

	/**
	 * DC bus total voltage [mV]. See {@link ChannelId#DC_BUS_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcBusVoltage() {
		return this.getDcBusVoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#DC_BUS_HALF_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcBusHalfVoltageChannel() {
		return this.channel(ChannelId.DC_BUS_HALF_VOLTAGE);
	}

	/**
	 * DC bus half (split) voltage [mV]. See {@link ChannelId#DC_BUS_HALF_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcBusHalfVoltage() {
		return this.getDcBusHalfVoltageChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – AC phase voltages (reg 33073–33075)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#VOLTAGE_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL1Channel() {
		return this.channel(ChannelId.VOLTAGE_L1);
	}

	/**
	 * Phase L1 (or AB line) voltage [mV]. See {@link ChannelId#VOLTAGE_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL1() {
		return this.getVoltageL1Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#VOLTAGE_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL2Channel() {
		return this.channel(ChannelId.VOLTAGE_L2);
	}

	/**
	 * Phase L2 (or BC line) voltage [mV]. See {@link ChannelId#VOLTAGE_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL2() {
		return this.getVoltageL2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#VOLTAGE_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL3Channel() {
		return this.channel(ChannelId.VOLTAGE_L3);
	}

	/**
	 * Phase L3 (or CA line) voltage [mV]. See {@link ChannelId#VOLTAGE_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL3() {
		return this.getVoltageL3Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – AC phase currents (reg 33076–33078)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#CURRENT_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL1Channel() {
		return this.channel(ChannelId.CURRENT_L1);
	}

	/**
	 * Phase L1 current [mA]. See {@link ChannelId#CURRENT_L1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL1() {
		return this.getCurrentL1Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#CURRENT_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL2Channel() {
		return this.channel(ChannelId.CURRENT_L2);
	}

	/**
	 * Phase L2 current [mA]. See {@link ChannelId#CURRENT_L2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL2() {
		return this.getCurrentL2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#CURRENT_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL3Channel() {
		return this.channel(ChannelId.CURRENT_L3);
	}

	/**
	 * Phase L3 current [mA]. See {@link ChannelId#CURRENT_L3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL3() {
		return this.getCurrentL3Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – AC power measurements (reg 33083–33084)
	// -----------------------------------------------------------------------

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
	// Accessor methods – Grid frequency and mode (reg 33091, 33094, 33095)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#STANDARD_WORKING_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getStandardWorkingModeChannel() {
		return this.channel(ChannelId.STANDARD_WORKING_MODE);
	}

	/**
	 * Standard working mode enum (reg 33091). See {@link ChannelId#STANDARD_WORKING_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getStandardWorkingMode() {
		return this.getStandardWorkingModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FREQUENCY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getFrequencyChannel() {
		return this.channel(ChannelId.FREQUENCY);
	}

	/**
	 * Grid frequency [mHz]. See {@link ChannelId#FREQUENCY}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFrequency() {
		return this.getFrequencyChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INVERTER_CURRENT_STATUS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getInverterCurrentStatusChannel() {
		return this.channel(ChannelId.INVERTER_CURRENT_STATUS);
	}

	/**
	 * Inverter current status code (Appendix 2). See {@link ChannelId#INVERTER_CURRENT_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getInverterCurrentStatus() {
		return this.getInverterCurrentStatusChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Lead-acid battery temperature (reg 33096)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#LEAD_ACID_BATTERY_TEMP}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getLeadAcidBatteryTempChannel() {
		return this.channel(ChannelId.LEAD_ACID_BATTERY_TEMP);
	}

	/**
	 * Lead-acid battery temperature [°C, raw 0.1°C scale]. See {@link ChannelId#LEAD_ACID_BATTERY_TEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLeadAcidBatteryTemp() {
		return this.getLeadAcidBatteryTempChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Function status decoded bits (reg 33097)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_DRM}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatDrmChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_DRM);
	}

	/**
	 * true if DRM function enabled (reg 33097 BIT00). See {@link ChannelId#FUNCTION_STAT_DRM}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatDrm() {
		return this.getFunctionStatDrmChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_PARALLEL_RUNNING}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatParallelRunningChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_PARALLEL_RUNNING);
	}

	/**
	 * true if parallel system running (reg 33097 BIT01). See {@link ChannelId#FUNCTION_STAT_PARALLEL_RUNNING}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatParallelRunning() {
		return this.getFunctionStatParallelRunningChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_MASTER}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatMasterChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_MASTER);
	}

	/**
	 * true if this unit is master (1=master, 0=slave) (reg 33097 BIT02). See {@link ChannelId#FUNCTION_STAT_MASTER}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatMaster() {
		return this.getFunctionStatMasterChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_3PH_UNBALANCED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStat3phUnbalancedChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_3PH_UNBALANCED);
	}

	/**
	 * true if 3-phase unbalanced operation (reg 33097 BIT03). See {@link ChannelId#FUNCTION_STAT_3PH_UNBALANCED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStat3phUnbalanced() {
		return this.getFunctionStat3phUnbalancedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_GEN_START_CONDITIONS}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatGenStartConditionsChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_GEN_START_CONDITIONS);
	}

	/**
	 * true if generator start conditions met (reg 33097 BIT04). See {@link ChannelId#FUNCTION_STAT_GEN_START_CONDITIONS}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatGenStartConditions() {
		return this.getFunctionStatGenStartConditionsChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_GEN_STARTED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatGenStartedChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_GEN_STARTED);
	}

	/**
	 * true if generator started successfully (reg 33097 BIT05). See {@link ChannelId#FUNCTION_STAT_GEN_STARTED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatGenStarted() {
		return this.getFunctionStatGenStartedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_BATT_INDEPENDENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatBattIndependentChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_BATT_INDEPENDENT);
	}

	/**
	 * true if battery in independent mode (reg 33097 BIT06). See {@link ChannelId#FUNCTION_STAT_BATT_INDEPENDENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatBattIndependent() {
		return this.getFunctionStatBattIndependentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_AFCI_PRESENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatAfciPresentChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_AFCI_PRESENT);
	}

	/**
	 * true if AFCI board present (reg 33097 BIT07). See {@link ChannelId#FUNCTION_STAT_AFCI_PRESENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatAfciPresent() {
		return this.getFunctionStatAfciPresentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_AFCI_SELFTEST_DONE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatAfciSelftestDoneChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_AFCI_SELFTEST_DONE);
	}

	/**
	 * true if AFCI self-test finished (reg 33097 BIT08). See {@link ChannelId#FUNCTION_STAT_AFCI_SELFTEST_DONE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatAfciSelftestDone() {
		return this.getFunctionStatAfciSelftestDoneChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_GRID_CONNECTED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatGridConnectedChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_GRID_CONNECTED);
	}

	/**
	 * true if grid connected (1=on-grid, 0=off-grid) (reg 33097 BIT09). See {@link ChannelId#FUNCTION_STAT_GRID_CONNECTED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatGridConnected() {
		return this.getFunctionStatGridConnectedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_DOUBLE_BACKUP}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatDoubleBackupChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_DOUBLE_BACKUP);
	}

	/**
	 * true if double backup enabled (reg 33097 BIT10). See {@link ChannelId#FUNCTION_STAT_DOUBLE_BACKUP}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatDoubleBackup() {
		return this.getFunctionStatDoubleBackupChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_RSD_SWITCH}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatRsdSwitchChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_RSD_SWITCH);
	}

	/**
	 * true if RSD switch closed (S6 HV only) (reg 33097 BIT11). See {@link ChannelId#FUNCTION_STAT_RSD_SWITCH}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatRsdSwitch() {
		return this.getFunctionStatRsdSwitchChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_EMERGENCY_SWITCH}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatEmergencySwitchChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_EMERGENCY_SWITCH);
	}

	/**
	 * true if emergency switch closed (S6 HV only) (reg 33097 BIT12). See {@link ChannelId#FUNCTION_STAT_EMERGENCY_SWITCH}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatEmergencySwitch() {
		return this.getFunctionStatEmergencySwitchChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_AC_COUPLING}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatAcCouplingChannel() {
		return this.channel(ChannelId.FUNCTION_STAT_AC_COUPLING);
	}

	/**
	 * true if AC coupling running (reg 33097 BIT13). See {@link ChannelId#FUNCTION_STAT_AC_COUPLING}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatAcCoupling() {
		return this.getFunctionStatAcCouplingChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_RESERVED_14}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatReserved14Channel() {
		return this.channel(ChannelId.FUNCTION_STAT_RESERVED_14);
	}

	/**
	 * true if reserved bit 14 set (reg 33097 BIT14). See {@link ChannelId#FUNCTION_STAT_RESERVED_14}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatReserved14() {
		return this.getFunctionStatReserved14Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STAT_RESERVED_15}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFunctionStatReserved15Channel() {
		return this.channel(ChannelId.FUNCTION_STAT_RESERVED_15);
	}

	/**
	 * true if reserved bit 15 set (reg 33097 BIT15). See {@link ChannelId#FUNCTION_STAT_RESERVED_15}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFunctionStatReserved15() {
		return this.getFunctionStatReserved15Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Power quality measurements (reg 33098–33108)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#CURRENT_DRM_CODE_STATUS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentDrmCodeStatusChannel() {
		return this.channel(ChannelId.CURRENT_DRM_CODE_STATUS);
	}

	/**
	 * Current DRM code status bitmask (reg 33098). See {@link ChannelId#CURRENT_DRM_CODE_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentDrmCodeStatus() {
		return this.getCurrentDrmCodeStatusChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INVERTER_CABINET_TEMP}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getInverterCabinetTempChannel() {
		return this.channel(ChannelId.INVERTER_CABINET_TEMP);
	}

	/**
	 * Inverter cabinet temperature [°C] (reg 33099). See {@link ChannelId#INVERTER_CABINET_TEMP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getInverterCabinetTemp() {
		return this.getInverterCabinetTempChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#LIMITED_POWER_ACTUAL_VALUE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getLimitedPowerActualValueChannel() {
		return this.channel(ChannelId.LIMITED_POWER_ACTUAL_VALUE);
	}

	/**
	 * Limited power actual value [%] (reg 33104). See {@link ChannelId#LIMITED_POWER_ACTUAL_VALUE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLimitedPowerActualValue() {
		return this.getLimitedPowerActualValueChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#PF_ADJUSTMENT_ACTUAL_VALUE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getPfAdjustmentActualValueChannel() {
		return this.channel(ChannelId.PF_ADJUSTMENT_ACTUAL_VALUE);
	}

	/**
	 * Power factor adjustment actual value ×0.001 (reg 33105). See {@link ChannelId#PF_ADJUSTMENT_ACTUAL_VALUE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getPfAdjustmentActualValue() {
		return this.getPfAdjustmentActualValueChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#LIMITED_REACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getLimitedReactivePowerChannel() {
		return this.channel(ChannelId.LIMITED_REACTIVE_POWER);
	}

	/**
	 * Limited reactive power [%] (reg 33106). See {@link ChannelId#LIMITED_REACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLimitedReactivePower() {
		return this.getLimitedReactivePowerChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INVERTER_MODULE_TEMP2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getInverterModuleTemp2Channel() {
		return this.channel(ChannelId.INVERTER_MODULE_TEMP2);
	}

	/**
	 * Inverter module temperature 2 [°C] (reg 33107). See {@link ChannelId#INVERTER_MODULE_TEMP2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getInverterModuleTemp2() {
		return this.getInverterModuleTemp2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#VOLT_VAR_VREF_RT_VALUES}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltVarVrefRtValuesChannel() {
		return this.channel(ChannelId.VOLT_VAR_VREF_RT_VALUES);
	}

	/**
	 * Volt-var real-time Vref [mV] (reg 33108). See {@link ChannelId#VOLT_VAR_VREF_RT_VALUES}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltVarVrefRtValues() {
		return this.getVoltVarVrefRtValuesChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_CHARGING_VOLTAGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsChargingVoltageLimitChannel() {
		return this.channel(ChannelId.BMS_CHARGING_VOLTAGE_LIMIT);
	}

	/**
	 * BMS charging voltage limit [mV] (reg 33110). See {@link ChannelId#BMS_CHARGING_VOLTAGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsChargingVoltageLimit() {
		return this.getBmsChargingVoltageLimitChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Battery BMS status (reg 33111)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#BATTERY_BMS_STATUS}.
	 *
	 * @return the Channel
	 */
	public default Channel<BatteryBmsStatus> getBatteryBmsStatusChannel() {
		return this.channel(ChannelId.BATTERY_BMS_STATUS);
	}

	/**
	 * BMS communication status (reg 33111). See {@link ChannelId#BATTERY_BMS_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default BatteryBmsStatus getBatteryBmsStatus() {
		return this.getBatteryBmsStatusChannel().value().asEnum();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Inverter initial setting state decoded bits (reg 33112)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#INIT_STATE_MODEL_SET}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateModelSetChannel() {
		return this.channel(ChannelId.INIT_STATE_MODEL_SET);
	}

	/**
	 * true if model setting complete (reg 33112 BIT00). See {@link ChannelId#INIT_STATE_MODEL_SET}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateModelSet() {
		return this.getInitStateModelSetChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_GRID_CODE_SET}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateGridCodeSetChannel() {
		return this.channel(ChannelId.INIT_STATE_GRID_CODE_SET);
	}

	/**
	 * true if grid code setting complete (reg 33112 BIT01). See {@link ChannelId#INIT_STATE_GRID_CODE_SET}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateGridCodeSet() {
		return this.getInitStateGridCodeSetChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_POWER_CURVE_SET}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStatePowerCurveSetChannel() {
		return this.channel(ChannelId.INIT_STATE_POWER_CURVE_SET);
	}

	/**
	 * true if power curve setting complete (reg 33112 BIT02). See {@link ChannelId#INIT_STATE_POWER_CURVE_SET}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStatePowerCurveSet() {
		return this.getInitStatePowerCurveSetChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_MODULE_TYPE_INFINEON}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateModuleTypeInfineonChannel() {
		return this.channel(ChannelId.INIT_STATE_MODULE_TYPE_INFINEON);
	}

	/**
	 * true if module type is Infineon (1=Infineon, 0=Onsemi) (reg 33112 BIT03). See {@link ChannelId#INIT_STATE_MODULE_TYPE_INFINEON}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateModuleTypeInfineon() {
		return this.getInitStateModuleTypeInfineonChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_FAN_DETECTION_SUPPORTED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateFanDetectionSupportedChannel() {
		return this.channel(ChannelId.INIT_STATE_FAN_DETECTION_SUPPORTED);
	}

	/**
	 * true if fan detection hardware supported (reg 33112 BIT04). See {@link ChannelId#INIT_STATE_FAN_DETECTION_SUPPORTED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateFanDetectionSupported() {
		return this.getInitStateFanDetectionSupportedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_FCAS_RUNNING}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateFcasRunningChannel() {
		return this.channel(ChannelId.INIT_STATE_FCAS_RUNNING);
	}

	/**
	 * true if FCAS function currently running (reg 33112 BIT05). See {@link ChannelId#INIT_STATE_FCAS_RUNNING}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateFcasRunning() {
		return this.getInitStateFcasRunningChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_AFCI_TEST_ENDED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateAfciTestEndedChannel() {
		return this.channel(ChannelId.INIT_STATE_AFCI_TEST_ENDED);
	}

	/**
	 * true if AFCI self-test ended (reg 33112 BIT06). See {@link ChannelId#INIT_STATE_AFCI_TEST_ENDED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateAfciTestEnded() {
		return this.getInitStateAfciTestEndedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_AFCI_ARC_FOUND}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateAfciArcFoundChannel() {
		return this.channel(ChannelId.INIT_STATE_AFCI_ARC_FOUND);
	}

	/**
	 * true if AFCI self-test found arc — FAULT (reg 33112 BIT07). See {@link ChannelId#INIT_STATE_AFCI_ARC_FOUND}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateAfciArcFound() {
		return this.getInitStateAfciArcFoundChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_DSP_CHIP_TYPE_1}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateDspChipType1Channel() {
		return this.channel(ChannelId.INIT_STATE_DSP_CHIP_TYPE_1);
	}

	/**
	 * true if DSP chip type bit 1 (reg 33112 BIT08). See {@link ChannelId#INIT_STATE_DSP_CHIP_TYPE_1}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateDspChipType1() {
		return this.getInitStateDspChipType1Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_DSP_CHIP_TYPE_2}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateDspChipType2Channel() {
		return this.channel(ChannelId.INIT_STATE_DSP_CHIP_TYPE_2);
	}

	/**
	 * true if DSP chip type bit 2 (reg 33112 BIT09). See {@link ChannelId#INIT_STATE_DSP_CHIP_TYPE_2}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateDspChipType2() {
		return this.getInitStateDspChipType2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_IGBT_SCREENING_COMPLETED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateIgbtScreeningCompletedChannel() {
		return this.channel(ChannelId.INIT_STATE_IGBT_SCREENING_COMPLETED);
	}

	/**
	 * true if IGBT screening complete (reg 33112 BIT10). See {@link ChannelId#INIT_STATE_IGBT_SCREENING_COMPLETED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateIgbtScreeningCompleted() {
		return this.getInitStateIgbtScreeningCompletedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_RESERVED_11}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateReserved11Channel() {
		return this.channel(ChannelId.INIT_STATE_RESERVED_11);
	}

	/**
	 * true if reserved (reg 33112 BIT11). See {@link ChannelId#INIT_STATE_RESERVED_11}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateReserved11() {
		return this.getInitStateReserved11Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_RESERVED_12}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateReserved12Channel() {
		return this.channel(ChannelId.INIT_STATE_RESERVED_12);
	}

	/**
	 * true if reserved (reg 33112 BIT12). See {@link ChannelId#INIT_STATE_RESERVED_12}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateReserved12() {
		return this.getInitStateReserved12Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_RESERVED_13}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateReserved13Channel() {
		return this.channel(ChannelId.INIT_STATE_RESERVED_13);
	}

	/**
	 * true if reserved (reg 33112 BIT13). See {@link ChannelId#INIT_STATE_RESERVED_13}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateReserved13() {
		return this.getInitStateReserved13Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#INIT_STATE_WAVEFORM_READY}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getInitStateWaveformReadyChannel() {
		return this.channel(ChannelId.INIT_STATE_WAVEFORM_READY);
	}

	/**
	 * true if DSP waveform data ready (reg 33112 BIT14). See {@link ChannelId#INIT_STATE_WAVEFORM_READY}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getInitStateWaveformReady() {
		return this.getInitStateWaveformReadyChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Batch upgrade support (reg 33113)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#BATCH_UPGRADE_DSP}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBatchUpgradeDspChannel() {
		return this.channel(ChannelId.BATCH_UPGRADE_DSP);
	}

	/**
	 * true if DSP processor supports batch upgrade (reg 33113 BIT00). See {@link ChannelId#BATCH_UPGRADE_DSP}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBatchUpgradeDsp() {
		return this.getBatchUpgradeDspChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BATCH_UPGRADE_ARM}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBatchUpgradeArmChannel() {
		return this.channel(ChannelId.BATCH_UPGRADE_ARM);
	}

	/**
	 * true if ARM processor supports batch upgrade (reg 33113 BIT04). See {@link ChannelId#BATCH_UPGRADE_ARM}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBatchUpgradeArm() {
		return this.getBatchUpgradeArmChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – FCAS mode (reg 33114)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FCAS_MODE_RUNNING_STATUS}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFcasModeRunningStatusChannel() {
		return this.channel(ChannelId.FCAS_MODE_RUNNING_STATUS);
	}

	/**
	 * true if FCAS mode running status (reg 33114). See {@link ChannelId#FCAS_MODE_RUNNING_STATUS}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFcasModeRunningStatus() {
		return this.getFcasModeRunningStatusChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Setting flag decoded bits (reg 33115, Appendix 7)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_FLASH_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagFlashTimeoutChannel() {
		return this.channel(ChannelId.SETTING_FLAG_FLASH_TIMEOUT);
	}

	/**
	 * true if FLASH read/write timeout fault (reg 33115 BIT00). See {@link ChannelId#SETTING_FLAG_FLASH_TIMEOUT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagFlashTimeout() {
		return this.getSettingFlagFlashTimeoutChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_CLEAR_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagClearEnergyChannel() {
		return this.channel(ChannelId.SETTING_FLAG_CLEAR_ENERGY);
	}

	/**
	 * true if clear energy flag completed (reg 33115 BIT01). See {@link ChannelId#SETTING_FLAG_CLEAR_ENERGY}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagClearEnergy() {
		return this.getSettingFlagClearEnergyChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_02}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved02Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_02);
	}

	/**
	 * true if reserved (reg 33115 BIT02). See {@link ChannelId#SETTING_FLAG_RESERVED_02}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved02() {
		return this.getSettingFlagReserved02Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_03}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved03Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_03);
	}

	/**
	 * true if reserved (reg 33115 BIT03). See {@link ChannelId#SETTING_FLAG_RESERVED_03}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved03() {
		return this.getSettingFlagReserved03Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_04}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved04Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_04);
	}

	/**
	 * true if reserved (reg 33115 BIT04). See {@link ChannelId#SETTING_FLAG_RESERVED_04}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved04() {
		return this.getSettingFlagReserved04Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_05}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved05Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_05);
	}

	/**
	 * true if reserved (reg 33115 BIT05). See {@link ChannelId#SETTING_FLAG_RESERVED_05}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved05() {
		return this.getSettingFlagReserved05Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_06}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved06Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_06);
	}

	/**
	 * true if reserved (reg 33115 BIT06). See {@link ChannelId#SETTING_FLAG_RESERVED_06}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved06() {
		return this.getSettingFlagReserved06Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_07}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved07Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_07);
	}

	/**
	 * true if reserved (reg 33115 BIT07). See {@link ChannelId#SETTING_FLAG_RESERVED_07}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved07() {
		return this.getSettingFlagReserved07Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESET_DATALOGGER}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagResetDataloggerChannel() {
		return this.channel(ChannelId.SETTING_FLAG_RESET_DATALOGGER);
	}

	/**
	 * true if datalogger reset completed (reg 33115 BIT08). See {@link ChannelId#SETTING_FLAG_RESET_DATALOGGER}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagResetDatalogger() {
		return this.getSettingFlagResetDataloggerChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_FACTORY_RECOVER}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagFactoryRecoverChannel() {
		return this.channel(ChannelId.SETTING_FLAG_FACTORY_RECOVER);
	}

	/**
	 * true if factory settings recovered (reg 33115 BIT09). See {@link ChannelId#SETTING_FLAG_FACTORY_RECOVER}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagFactoryRecover() {
		return this.getSettingFlagFactoryRecoverChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_10}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved10Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_10);
	}

	/**
	 * true if reserved (reg 33115 BIT10). See {@link ChannelId#SETTING_FLAG_RESERVED_10}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved10() {
		return this.getSettingFlagReserved10Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_11}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved11Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_11);
	}

	/**
	 * true if reserved (reg 33115 BIT11). See {@link ChannelId#SETTING_FLAG_RESERVED_11}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved11() {
		return this.getSettingFlagReserved11Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_12}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved12Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_12);
	}

	/**
	 * true if reserved (reg 33115 BIT12). See {@link ChannelId#SETTING_FLAG_RESERVED_12}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved12() {
		return this.getSettingFlagReserved12Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_13}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved13Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_13);
	}

	/**
	 * true if reserved (reg 33115 BIT13). See {@link ChannelId#SETTING_FLAG_RESERVED_13}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved13() {
		return this.getSettingFlagReserved13Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_14}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved14Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_14);
	}

	/**
	 * true if reserved (reg 33115 BIT14). See {@link ChannelId#SETTING_FLAG_RESERVED_14}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved14() {
		return this.getSettingFlagReserved14Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_RESERVED_15}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getSettingFlagReserved15Channel() {
		return this.channel(ChannelId.SETTING_FLAG_RESERVED_15);
	}

	/**
	 * true if reserved (reg 33115 BIT15). See {@link ChannelId#SETTING_FLAG_RESERVED_15}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getSettingFlagReserved15() {
		return this.getSettingFlagReserved15Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault register 1 decoded bits (reg 33116, Appendix 4)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_NO_GRID}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1NoGridChannel() {
		return this.channel(ChannelId.FAULT_REG1_NO_GRID);
	}

	/**
	 * true if no grid detected (BIT00). See {@link ChannelId#FAULT_REG1_NO_GRID}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1NoGrid() {
		return this.getFaultReg1NoGridChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_GRID_OVERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1GridOvervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG1_GRID_OVERVOLTAGE);
	}

	/**
	 * true if grid overvoltage fault (BIT01). See {@link ChannelId#FAULT_REG1_GRID_OVERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1GridOvervoltage() {
		return this.getFaultReg1GridOvervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_GRID_UNDERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1GridUndervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG1_GRID_UNDERVOLTAGE);
	}

	/**
	 * true if grid undervoltage fault (BIT02). See {@link ChannelId#FAULT_REG1_GRID_UNDERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1GridUndervoltage() {
		return this.getFaultReg1GridUndervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_GRID_OVERFREQ}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1GridOverfreqChannel() {
		return this.channel(ChannelId.FAULT_REG1_GRID_OVERFREQ);
	}

	/**
	 * true if grid overfrequency fault (BIT03). See {@link ChannelId#FAULT_REG1_GRID_OVERFREQ}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1GridOverfreq() {
		return this.getFaultReg1GridOverfreqChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_GRID_UNDERFREQ}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1GridUnderfreqChannel() {
		return this.channel(ChannelId.FAULT_REG1_GRID_UNDERFREQ);
	}

	/**
	 * true if grid underfrequency fault (BIT04). See {@link ChannelId#FAULT_REG1_GRID_UNDERFREQ}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1GridUnderfreq() {
		return this.getFaultReg1GridUnderfreqChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_UNBALANCED_GRID}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1UnbalancedGridChannel() {
		return this.channel(ChannelId.FAULT_REG1_UNBALANCED_GRID);
	}

	/**
	 * true if unbalanced grid fault (BIT05). See {@link ChannelId#FAULT_REG1_UNBALANCED_GRID}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1UnbalancedGrid() {
		return this.getFaultReg1UnbalancedGridChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_GRID_FREQ_FLUCTUATION}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1GridFreqFluctuationChannel() {
		return this.channel(ChannelId.FAULT_REG1_GRID_FREQ_FLUCTUATION);
	}

	/**
	 * true if grid frequency fluctuation fault (BIT06). See {@link ChannelId#FAULT_REG1_GRID_FREQ_FLUCTUATION}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1GridFreqFluctuation() {
		return this.getFaultReg1GridFreqFluctuationChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_GRID_REVERSE_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1GridReverseCurrentChannel() {
		return this.channel(ChannelId.FAULT_REG1_GRID_REVERSE_CURRENT);
	}

	/**
	 * true if grid reverse current fault (BIT07). See {@link ChannelId#FAULT_REG1_GRID_REVERSE_CURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1GridReverseCurrent() {
		return this.getFaultReg1GridReverseCurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_GRID_CURRENT_TRACKING_ERROR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1GridCurrentTrackingErrorChannel() {
		return this.channel(ChannelId.FAULT_REG1_GRID_CURRENT_TRACKING_ERROR);
	}

	/**
	 * true if grid current tracking error (BIT08). See {@link ChannelId#FAULT_REG1_GRID_CURRENT_TRACKING_ERROR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1GridCurrentTrackingError() {
		return this.getFaultReg1GridCurrentTrackingErrorChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_METER_COM_FAIL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1MeterComFailChannel() {
		return this.channel(ChannelId.FAULT_REG1_METER_COM_FAIL);
	}

	/**
	 * true if meter communication failure (BIT09). See {@link ChannelId#FAULT_REG1_METER_COM_FAIL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1MeterComFail() {
		return this.getFaultReg1MeterComFailChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_FAILSAFE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1FailsafeChannel() {
		return this.channel(ChannelId.FAULT_REG1_FAILSAFE);
	}

	/**
	 * true if failsafe protection triggered (BIT10). See {@link ChannelId#FAULT_REG1_FAILSAFE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1Failsafe() {
		return this.getFaultReg1FailsafeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_METER_SELECT_FAIL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1MeterSelectFailChannel() {
		return this.channel(ChannelId.FAULT_REG1_METER_SELECT_FAIL);
	}

	/**
	 * true if meter select failure (BIT11). See {@link ChannelId#FAULT_REG1_METER_SELECT_FAIL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1MeterSelectFail() {
		return this.getFaultReg1MeterSelectFailChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_EPM_HARD_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1EpmHardLimitChannel() {
		return this.channel(ChannelId.FAULT_REG1_EPM_HARD_LIMIT);
	}

	/**
	 * true if EPM hard limit protection (BIT12). See {@link ChannelId#FAULT_REG1_EPM_HARD_LIMIT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1EpmHardLimit() {
		return this.getFaultReg1EpmHardLimitChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_G100_CURRENT_OVER_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1G100CurrentOverLimitChannel() {
		return this.channel(ChannelId.FAULT_REG1_G100_CURRENT_OVER_LIMIT);
	}

	/**
	 * true if G100 current over limit (BIT13). See {@link ChannelId#FAULT_REG1_G100_CURRENT_OVER_LIMIT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1G100CurrentOverLimit() {
		return this.getFaultReg1G100CurrentOverLimitChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_RESERVED_14}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1Reserved14Channel() {
		return this.channel(ChannelId.FAULT_REG1_RESERVED_14);
	}

	/**
	 * true if reserved fault bit 14 set (BIT14). See {@link ChannelId#FAULT_REG1_RESERVED_14}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1Reserved14() {
		return this.getFaultReg1Reserved14Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG1_ABNORMAL_GRID_PHASE_POLARITY}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg1AbnormalGridPhasePolarityChannel() {
		return this.channel(ChannelId.FAULT_REG1_ABNORMAL_GRID_PHASE_POLARITY);
	}

	/**
	 * true if abnormal grid phase polarity (BIT15). See {@link ChannelId#FAULT_REG1_ABNORMAL_GRID_PHASE_POLARITY}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg1AbnormalGridPhasePolarity() {
		return this.getFaultReg1AbnormalGridPhasePolarityChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault register 2 decoded bits (reg 33117, Appendix 4)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_BACKUP_OVERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2BackupOvervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG2_BACKUP_OVERVOLTAGE);
	}

	/**
	 * true if backup port overvoltage fault (BIT00). See {@link ChannelId#FAULT_REG2_BACKUP_OVERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2BackupOvervoltage() {
		return this.getFaultReg2BackupOvervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_BACKUP_OVERLOAD}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2BackupOverloadChannel() {
		return this.channel(ChannelId.FAULT_REG2_BACKUP_OVERLOAD);
	}

	/**
	 * true if backup port overload fault (BIT01). See {@link ChannelId#FAULT_REG2_BACKUP_OVERLOAD}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2BackupOverload() {
		return this.getFaultReg2BackupOverloadChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_GRID_BACKUP_OVERLOAD}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2GridBackupOverloadChannel() {
		return this.channel(ChannelId.FAULT_REG2_GRID_BACKUP_OVERLOAD);
	}

	/**
	 * true if grid+backup combined overload (BIT02). See {@link ChannelId#FAULT_REG2_GRID_BACKUP_OVERLOAD}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2GridBackupOverload() {
		return this.getFaultReg2GridBackupOverloadChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_OFFGRID_BACKUP_UNDERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2OffgridBackupUndervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG2_OFFGRID_BACKUP_UNDERVOLTAGE);
	}

	/**
	 * true if off-grid backup undervoltage fault (BIT03). See {@link ChannelId#FAULT_REG2_OFFGRID_BACKUP_UNDERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2OffgridBackupUndervoltage() {
		return this.getFaultReg2OffgridBackupUndervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_HUB_PANEL_OV_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2HubPanelOvCurrentChannel() {
		return this.channel(ChannelId.FAULT_REG2_HUB_PANEL_OV_CURRENT);
	}

	/**
	 * true if hub panel over-current fault (BIT04). See {@link ChannelId#FAULT_REG2_HUB_PANEL_OV_CURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2HubPanelOvCurrent() {
		return this.getFaultReg2HubPanelOvCurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_05}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved05Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_05);
	}

	/**
	 * true if reserved (BIT05). See {@link ChannelId#FAULT_REG2_RESERVED_05}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved05() {
		return this.getFaultReg2Reserved05Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_06}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved06Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_06);
	}

	/**
	 * true if reserved (BIT06). See {@link ChannelId#FAULT_REG2_RESERVED_06}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved06() {
		return this.getFaultReg2Reserved06Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_07}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved07Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_07);
	}

	/**
	 * true if reserved (BIT07). See {@link ChannelId#FAULT_REG2_RESERVED_07}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved07() {
		return this.getFaultReg2Reserved07Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_08}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved08Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_08);
	}

	/**
	 * true if reserved (BIT08). See {@link ChannelId#FAULT_REG2_RESERVED_08}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved08() {
		return this.getFaultReg2Reserved08Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_09}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved09Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_09);
	}

	/**
	 * true if reserved (BIT09). See {@link ChannelId#FAULT_REG2_RESERVED_09}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved09() {
		return this.getFaultReg2Reserved09Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_10}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved10Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_10);
	}

	/**
	 * true if reserved (BIT10). See {@link ChannelId#FAULT_REG2_RESERVED_10}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved10() {
		return this.getFaultReg2Reserved10Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_11}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved11Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_11);
	}

	/**
	 * true if reserved (BIT11). See {@link ChannelId#FAULT_REG2_RESERVED_11}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved11() {
		return this.getFaultReg2Reserved11Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_12}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved12Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_12);
	}

	/**
	 * true if reserved (BIT12). See {@link ChannelId#FAULT_REG2_RESERVED_12}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved12() {
		return this.getFaultReg2Reserved12Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_13}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved13Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_13);
	}

	/**
	 * true if reserved (BIT13). See {@link ChannelId#FAULT_REG2_RESERVED_13}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved13() {
		return this.getFaultReg2Reserved13Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_14}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved14Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_14);
	}

	/**
	 * true if reserved (BIT14). See {@link ChannelId#FAULT_REG2_RESERVED_14}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved14() {
		return this.getFaultReg2Reserved14Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG2_RESERVED_15}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg2Reserved15Channel() {
		return this.channel(ChannelId.FAULT_REG2_RESERVED_15);
	}

	/**
	 * true if reserved (BIT15). See {@link ChannelId#FAULT_REG2_RESERVED_15}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg2Reserved15() {
		return this.getFaultReg2Reserved15Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault register 3 decoded bits (reg 33118, Appendix 4)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_BATTERY_NOT_CONNECTED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3BatteryNotConnectedChannel() {
		return this.channel(ChannelId.FAULT_REG3_BATTERY_NOT_CONNECTED);
	}

	/**
	 * true if battery not connected (BIT00). See {@link ChannelId#FAULT_REG3_BATTERY_NOT_CONNECTED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3BatteryNotConnected() {
		return this.getFaultReg3BatteryNotConnectedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_BATTERY_OVERVOLTAGE_CHECK}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3BatteryOvervoltageCheckChannel() {
		return this.channel(ChannelId.FAULT_REG3_BATTERY_OVERVOLTAGE_CHECK);
	}

	/**
	 * true if battery overvoltage check fault (BIT01). See {@link ChannelId#FAULT_REG3_BATTERY_OVERVOLTAGE_CHECK}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3BatteryOvervoltageCheck() {
		return this.getFaultReg3BatteryOvervoltageCheckChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_BATTERY_UNDERVOLTAGE_CHECK}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3BatteryUndervoltageCheckChannel() {
		return this.channel(ChannelId.FAULT_REG3_BATTERY_UNDERVOLTAGE_CHECK);
	}

	/**
	 * true if battery undervoltage check fault (BIT02). See {@link ChannelId#FAULT_REG3_BATTERY_UNDERVOLTAGE_CHECK}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3BatteryUndervoltageCheck() {
		return this.getFaultReg3BatteryUndervoltageCheckChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_BATTERY_BMS_ALARM}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3BatteryBmsAlarmChannel() {
		return this.channel(ChannelId.FAULT_REG3_BATTERY_BMS_ALARM);
	}

	/**
	 * true if battery BMS alarm (BIT03). See {@link ChannelId#FAULT_REG3_BATTERY_BMS_ALARM}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3BatteryBmsAlarm() {
		return this.getFaultReg3BatteryBmsAlarmChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_INCONSISTENT_BATTERY_SELECTION}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3InconsistentBatterySelectionChannel() {
		return this.channel(ChannelId.FAULT_REG3_INCONSISTENT_BATTERY_SELECTION);
	}

	/**
	 * true if inconsistent battery type selection (BIT04). See {@link ChannelId#FAULT_REG3_INCONSISTENT_BATTERY_SELECTION}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3InconsistentBatterySelection() {
		return this.getFaultReg3InconsistentBatterySelectionChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_LEAD_ACID_TEMP_TOO_LOW}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3LeadAcidTempTooLowChannel() {
		return this.channel(ChannelId.FAULT_REG3_LEAD_ACID_TEMP_TOO_LOW);
	}

	/**
	 * true if lead-acid battery temperature too low (BIT05). See {@link ChannelId#FAULT_REG3_LEAD_ACID_TEMP_TOO_LOW}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3LeadAcidTempTooLow() {
		return this.getFaultReg3LeadAcidTempTooLowChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_LEAD_ACID_TEMP_TOO_HIGH}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3LeadAcidTempTooHighChannel() {
		return this.channel(ChannelId.FAULT_REG3_LEAD_ACID_TEMP_TOO_HIGH);
	}

	/**
	 * true if lead-acid battery temperature too high (BIT06). See {@link ChannelId#FAULT_REG3_LEAD_ACID_TEMP_TOO_HIGH}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3LeadAcidTempTooHigh() {
		return this.getFaultReg3LeadAcidTempTooHighChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_SECOND_BATTERY_NOT_CONNECTED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3SecondBatteryNotConnectedChannel() {
		return this.channel(ChannelId.FAULT_REG3_SECOND_BATTERY_NOT_CONNECTED);
	}

	/**
	 * true if second battery not connected (BIT07). See {@link ChannelId#FAULT_REG3_SECOND_BATTERY_NOT_CONNECTED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3SecondBatteryNotConnected() {
		return this.getFaultReg3SecondBatteryNotConnectedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_SECOND_BATTERY_SW_OVERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3SecondBatterySwOvervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG3_SECOND_BATTERY_SW_OVERVOLTAGE);
	}

	/**
	 * true if second battery software overvoltage (BIT08). See {@link ChannelId#FAULT_REG3_SECOND_BATTERY_SW_OVERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3SecondBatterySwOvervoltage() {
		return this.getFaultReg3SecondBatterySwOvervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_SECOND_BATTERY_SW_UNDERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3SecondBatterySwUndervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG3_SECOND_BATTERY_SW_UNDERVOLTAGE);
	}

	/**
	 * true if second battery software undervoltage (BIT09). See {@link ChannelId#FAULT_REG3_SECOND_BATTERY_SW_UNDERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3SecondBatterySwUndervoltage() {
		return this.getFaultReg3SecondBatterySwUndervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_PARALLEL_BATTERY_COM_ABNORMAL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3ParallelBatteryComAbnormalChannel() {
		return this.channel(ChannelId.FAULT_REG3_PARALLEL_BATTERY_COM_ABNORMAL);
	}

	/**
	 * true if parallel battery communication abnormal (BIT10). See {@link ChannelId#FAULT_REG3_PARALLEL_BATTERY_COM_ABNORMAL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3ParallelBatteryComAbnormal() {
		return this.getFaultReg3ParallelBatteryComAbnormalChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_LOW_BATTERY_OFFGRID}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3LowBatteryOffgridChannel() {
		return this.channel(ChannelId.FAULT_REG3_LOW_BATTERY_OFFGRID);
	}

	/**
	 * true if low battery in off-grid mode (BIT11). See {@link ChannelId#FAULT_REG3_LOW_BATTERY_OFFGRID}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3LowBatteryOffgrid() {
		return this.getFaultReg3LowBatteryOffgridChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_RESERVED_12}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3Reserved12Channel() {
		return this.channel(ChannelId.FAULT_REG3_RESERVED_12);
	}

	/**
	 * true if reserved (BIT12). See {@link ChannelId#FAULT_REG3_RESERVED_12}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3Reserved12() {
		return this.getFaultReg3Reserved12Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_RESERVED_13}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3Reserved13Channel() {
		return this.channel(ChannelId.FAULT_REG3_RESERVED_13);
	}

	/**
	 * true if reserved (BIT13). See {@link ChannelId#FAULT_REG3_RESERVED_13}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3Reserved13() {
		return this.getFaultReg3Reserved13Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_RESERVED_14}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3Reserved14Channel() {
		return this.channel(ChannelId.FAULT_REG3_RESERVED_14);
	}

	/**
	 * true if reserved (BIT14). See {@link ChannelId#FAULT_REG3_RESERVED_14}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3Reserved14() {
		return this.getFaultReg3Reserved14Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG3_RESERVED_15}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg3Reserved15Channel() {
		return this.channel(ChannelId.FAULT_REG3_RESERVED_15);
	}

	/**
	 * true if reserved (BIT15). See {@link ChannelId#FAULT_REG3_RESERVED_15}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg3Reserved15() {
		return this.getFaultReg3Reserved15Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault register 4 decoded bits (reg 33119, Appendix 4)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DC_OVERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DcOvervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG4_DC_OVERVOLTAGE);
	}

	/**
	 * true if DC overvoltage fault (BIT00). See {@link ChannelId#FAULT_REG4_DC_OVERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DcOvervoltage() {
		return this.getFaultReg4DcOvervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DC_BUS_OVERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DcBusOvervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG4_DC_BUS_OVERVOLTAGE);
	}

	/**
	 * true if DC bus overvoltage fault (BIT01). See {@link ChannelId#FAULT_REG4_DC_BUS_OVERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DcBusOvervoltage() {
		return this.getFaultReg4DcBusOvervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DcBusUnbalancedVoltageChannel() {
		return this.channel(ChannelId.FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE);
	}

	/**
	 * true if DC bus unbalanced voltage fault (BIT02). See {@link ChannelId#FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DcBusUnbalancedVoltage() {
		return this.getFaultReg4DcBusUnbalancedVoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DC_BUS_UNDERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DcBusUndervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG4_DC_BUS_UNDERVOLTAGE);
	}

	/**
	 * true if DC bus undervoltage fault (BIT03). See {@link ChannelId#FAULT_REG4_DC_BUS_UNDERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DcBusUndervoltage() {
		return this.getFaultReg4DcBusUndervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE_2}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DcBusUnbalancedVoltage2Channel() {
		return this.channel(ChannelId.FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE_2);
	}

	/**
	 * true if DC bus unbalanced voltage 2 fault (BIT04). See {@link ChannelId#FAULT_REG4_DC_BUS_UNBALANCED_VOLTAGE_2}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DcBusUnbalancedVoltage2() {
		return this.getFaultReg4DcBusUnbalancedVoltage2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DC_OVERCURRENT_A}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DcOvercurrentAChannel() {
		return this.channel(ChannelId.FAULT_REG4_DC_OVERCURRENT_A);
	}

	/**
	 * true if DC overcurrent on A circuit (BIT05). See {@link ChannelId#FAULT_REG4_DC_OVERCURRENT_A}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DcOvercurrentA() {
		return this.getFaultReg4DcOvercurrentAChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DC_OVERCURRENT_B}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DcOvercurrentBChannel() {
		return this.channel(ChannelId.FAULT_REG4_DC_OVERCURRENT_B);
	}

	/**
	 * true if DC overcurrent on B circuit (BIT06). See {@link ChannelId#FAULT_REG4_DC_OVERCURRENT_B}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DcOvercurrentB() {
		return this.getFaultReg4DcOvercurrentBChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DC_INPUT_INTERFERENCE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DcInputInterferenceChannel() {
		return this.channel(ChannelId.FAULT_REG4_DC_INPUT_INTERFERENCE);
	}

	/**
	 * true if DC input interference fault (BIT07). See {@link ChannelId#FAULT_REG4_DC_INPUT_INTERFERENCE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DcInputInterference() {
		return this.getFaultReg4DcInputInterferenceChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_GRID_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4GridOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG4_GRID_OVERCURRENT);
	}

	/**
	 * true if grid overcurrent fault (BIT08). See {@link ChannelId#FAULT_REG4_GRID_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4GridOvercurrent() {
		return this.getFaultReg4GridOvercurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_IGBT_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4IgbtOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG4_IGBT_OVERCURRENT);
	}

	/**
	 * true if IGBT overcurrent fault (BIT09). See {@link ChannelId#FAULT_REG4_IGBT_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4IgbtOvercurrent() {
		return this.getFaultReg4IgbtOvercurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_GRID_INTERFERENCE_02}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4GridInterference02Channel() {
		return this.channel(ChannelId.FAULT_REG4_GRID_INTERFERENCE_02);
	}

	/**
	 * true if grid interference 02 fault (BIT10). See {@link ChannelId#FAULT_REG4_GRID_INTERFERENCE_02}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4GridInterference02() {
		return this.getFaultReg4GridInterference02Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_AFCI_SELF_CHECK}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4AfciSelfCheckChannel() {
		return this.channel(ChannelId.FAULT_REG4_AFCI_SELF_CHECK);
	}

	/**
	 * true if AFCI self-check fault (BIT11). See {@link ChannelId#FAULT_REG4_AFCI_SELF_CHECK}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4AfciSelfCheck() {
		return this.getFaultReg4AfciSelfCheckChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_ARC_FAULT_RESERVED}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4ArcFaultReservedChannel() {
		return this.channel(ChannelId.FAULT_REG4_ARC_FAULT_RESERVED);
	}

	/**
	 * true if arc fault reserved (BIT12). See {@link ChannelId#FAULT_REG4_ARC_FAULT_RESERVED}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4ArcFaultReserved() {
		return this.getFaultReg4ArcFaultReservedChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_GRID_CURRENT_SAMPLING_FAULT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4GridCurrentSamplingFaultChannel() {
		return this.channel(ChannelId.FAULT_REG4_GRID_CURRENT_SAMPLING_FAULT);
	}

	/**
	 * true if grid current sampling fault (BIT13). See {@link ChannelId#FAULT_REG4_GRID_CURRENT_SAMPLING_FAULT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4GridCurrentSamplingFault() {
		return this.getFaultReg4GridCurrentSamplingFaultChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_DSP_SELF_CHECK_ERROR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4DspSelfCheckErrorChannel() {
		return this.channel(ChannelId.FAULT_REG4_DSP_SELF_CHECK_ERROR);
	}

	/**
	 * true if DSP self-check error (BIT14). See {@link ChannelId#FAULT_REG4_DSP_SELF_CHECK_ERROR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4DspSelfCheckError() {
		return this.getFaultReg4DspSelfCheckErrorChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG4_BATTERY_DISCHARGE_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg4BatteryDischargeOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG4_BATTERY_DISCHARGE_OVERCURRENT);
	}

	/**
	 * true if battery discharge overcurrent (BIT15). See {@link ChannelId#FAULT_REG4_BATTERY_DISCHARGE_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg4BatteryDischargeOvercurrent() {
		return this.getFaultReg4BatteryDischargeOvercurrentChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault register 5 decoded bits (reg 33120, Appendix 4)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_GRID_INTERFERENCE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5GridInterferenceChannel() {
		return this.channel(ChannelId.FAULT_REG5_GRID_INTERFERENCE);
	}

	/**
	 * true if grid interference protection (BIT00). See {@link ChannelId#FAULT_REG5_GRID_INTERFERENCE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5GridInterference() {
		return this.getFaultReg5GridInterferenceChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_OVER_DC_COMPONENTS}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5OverDcComponentsChannel() {
		return this.channel(ChannelId.FAULT_REG5_OVER_DC_COMPONENTS);
	}

	/**
	 * true if over DC components protection (BIT01). See {@link ChannelId#FAULT_REG5_OVER_DC_COMPONENTS}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5OverDcComponents() {
		return this.getFaultReg5OverDcComponentsChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_OVER_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5OverTemperatureChannel() {
		return this.channel(ChannelId.FAULT_REG5_OVER_TEMPERATURE);
	}

	/**
	 * true if over temperature protection (BIT02). See {@link ChannelId#FAULT_REG5_OVER_TEMPERATURE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5OverTemperature() {
		return this.getFaultReg5OverTemperatureChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_RELAY_CHECK}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5RelayCheckChannel() {
		return this.channel(ChannelId.FAULT_REG5_RELAY_CHECK);
	}

	/**
	 * true if relay check protection (BIT03). See {@link ChannelId#FAULT_REG5_RELAY_CHECK}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5RelayCheck() {
		return this.getFaultReg5RelayCheckChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_UNDER_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5UnderTemperatureChannel() {
		return this.channel(ChannelId.FAULT_REG5_UNDER_TEMPERATURE);
	}

	/**
	 * true if under temperature protection (BIT04). See {@link ChannelId#FAULT_REG5_UNDER_TEMPERATURE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5UnderTemperature() {
		return this.getFaultReg5UnderTemperatureChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_PV_INSULATION_FAULT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5PvInsulationFaultChannel() {
		return this.channel(ChannelId.FAULT_REG5_PV_INSULATION_FAULT);
	}

	/**
	 * true if PV insulation fault (BIT05). See {@link ChannelId#FAULT_REG5_PV_INSULATION_FAULT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5PvInsulationFault() {
		return this.getFaultReg5PvInsulationFaultChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_12V_UNDERVOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg512vUndervoltageChannel() {
		return this.channel(ChannelId.FAULT_REG5_12V_UNDERVOLTAGE);
	}

	/**
	 * true if 12V auxiliary undervoltage protection (BIT06). See {@link ChannelId#FAULT_REG5_12V_UNDERVOLTAGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg512vUndervoltage() {
		return this.getFaultReg512vUndervoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_LEAK_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5LeakCurrentChannel() {
		return this.channel(ChannelId.FAULT_REG5_LEAK_CURRENT);
	}

	/**
	 * true if leakage current protection (BIT07). See {@link ChannelId#FAULT_REG5_LEAK_CURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5LeakCurrent() {
		return this.getFaultReg5LeakCurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_LEAK_CURRENT_SELF_CHECK}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5LeakCurrentSelfCheckChannel() {
		return this.channel(ChannelId.FAULT_REG5_LEAK_CURRENT_SELF_CHECK);
	}

	/**
	 * true if leakage current self-check protection (BIT08). See {@link ChannelId#FAULT_REG5_LEAK_CURRENT_SELF_CHECK}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5LeakCurrentSelfCheck() {
		return this.getFaultReg5LeakCurrentSelfCheckChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_DSP_INITIAL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5DspInitialChannel() {
		return this.channel(ChannelId.FAULT_REG5_DSP_INITIAL);
	}

	/**
	 * true if DSP initial protection (BIT09). See {@link ChannelId#FAULT_REG5_DSP_INITIAL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5DspInitial() {
		return this.getFaultReg5DspInitialChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_DSP_B}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5DspBChannel() {
		return this.channel(ChannelId.FAULT_REG5_DSP_B);
	}

	/**
	 * true if DSP B protection (BIT10). See {@link ChannelId#FAULT_REG5_DSP_B}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5DspB() {
		return this.getFaultReg5DspBChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_BATTERY_OVERVOLTAGE_HW}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5BatteryOvervoltageHwChannel() {
		return this.channel(ChannelId.FAULT_REG5_BATTERY_OVERVOLTAGE_HW);
	}

	/**
	 * true if battery overvoltage hardware fault (BIT11). See {@link ChannelId#FAULT_REG5_BATTERY_OVERVOLTAGE_HW}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5BatteryOvervoltageHw() {
		return this.getFaultReg5BatteryOvervoltageHwChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_LLC_HW_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5LlcHwOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG5_LLC_HW_OVERCURRENT);
	}

	/**
	 * true if LLC hardware overcurrent (BIT12). See {@link ChannelId#FAULT_REG5_LLC_HW_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5LlcHwOvercurrent() {
		return this.getFaultReg5LlcHwOvercurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_GRID_TRANSIENT_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5GridTransientOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG5_GRID_TRANSIENT_OVERCURRENT);
	}

	/**
	 * true if grid transient overcurrent (BIT13). See {@link ChannelId#FAULT_REG5_GRID_TRANSIENT_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5GridTransientOvercurrent() {
		return this.getFaultReg5GridTransientOvercurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_BATTERY_COM_FAILURE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5BatteryComFailureChannel() {
		return this.channel(ChannelId.FAULT_REG5_BATTERY_COM_FAILURE);
	}

	/**
	 * true if battery communication failure (BIT14). See {@link ChannelId#FAULT_REG5_BATTERY_COM_FAILURE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5BatteryComFailure() {
		return this.getFaultReg5BatteryComFailureChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG5_DSP_COM_FAIL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg5DspComFailChannel() {
		return this.channel(ChannelId.FAULT_REG5_DSP_COM_FAIL);
	}

	/**
	 * true if DSP communication failure (BIT15). See {@link ChannelId#FAULT_REG5_DSP_COM_FAIL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg5DspComFail() {
		return this.getFaultReg5DspComFailChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault register 6 decoded bits (reg 33124, Appendix 4)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_SLAVE_LOSE_ERR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6SlaveLoseErrChannel() {
		return this.channel(ChannelId.FAULT_REG6_SLAVE_LOSE_ERR);
	}

	/**
	 * true if slave sync-signal loss error (BIT00). See {@link ChannelId#FAULT_REG6_SLAVE_LOSE_ERR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6SlaveLoseErr() {
		return this.getFaultReg6SlaveLoseErrChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_MASTER_LOSE_ERR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6MasterLoseErrChannel() {
		return this.channel(ChannelId.FAULT_REG6_MASTER_LOSE_ERR);
	}

	/**
	 * true if master sync-signal loss error (BIT01). See {@link ChannelId#FAULT_REG6_MASTER_LOSE_ERR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6MasterLoseErr() {
		return this.getFaultReg6MasterLoseErrChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_SLAVE_PRD_ERR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6SlavePrdErrChannel() {
		return this.channel(ChannelId.FAULT_REG6_SLAVE_PRD_ERR);
	}

	/**
	 * true if slave sync period error (BIT02). See {@link ChannelId#FAULT_REG6_SLAVE_PRD_ERR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6SlavePrdErr() {
		return this.getFaultReg6SlavePrdErrChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_MASTER_PRD_ERR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6MasterPrdErrChannel() {
		return this.channel(ChannelId.FAULT_REG6_MASTER_PRD_ERR);
	}

	/**
	 * true if master sync period error (BIT03). See {@link ChannelId#FAULT_REG6_MASTER_PRD_ERR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6MasterPrdErr() {
		return this.getFaultReg6MasterPrdErrChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_ADDR_CONFLICT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6AddrConflictChannel() {
		return this.channel(ChannelId.FAULT_REG6_ADDR_CONFLICT);
	}

	/**
	 * true if address conflict between parallel units (BIT04). See {@link ChannelId#FAULT_REG6_ADDR_CONFLICT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6AddrConflict() {
		return this.getFaultReg6AddrConflictChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_HEARTBEAT_LOSE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6HeartbeatLoseChannel() {
		return this.channel(ChannelId.FAULT_REG6_HEARTBEAT_LOSE);
	}

	/**
	 * true if heartbeat loss (BIT05). See {@link ChannelId#FAULT_REG6_HEARTBEAT_LOSE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6HeartbeatLose() {
		return this.getFaultReg6HeartbeatLoseChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_DCAN_ERR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6DcanErrChannel() {
		return this.channel(ChannelId.FAULT_REG6_DCAN_ERR);
	}

	/**
	 * true if DCAN register error (BIT06). See {@link ChannelId#FAULT_REG6_DCAN_ERR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6DcanErr() {
		return this.getFaultReg6DcanErrChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_MUL_MASTER_ERR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6MulMasterErrChannel() {
		return this.channel(ChannelId.FAULT_REG6_MUL_MASTER_ERR);
	}

	/**
	 * true if multiple master units detected (BIT07). See {@link ChannelId#FAULT_REG6_MUL_MASTER_ERR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6MulMasterErr() {
		return this.getFaultReg6MulMasterErrChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_MODE_CONFLICT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6ModeConflictChannel() {
		return this.channel(ChannelId.FAULT_REG6_MODE_CONFLICT);
	}

	/**
	 * true if mode conflict between parallel units (BIT08). See {@link ChannelId#FAULT_REG6_MODE_CONFLICT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6ModeConflict() {
		return this.getFaultReg6ModeConflictChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_S_PLUG_VOLT_ERR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6SPlugVoltErrChannel() {
		return this.channel(ChannelId.FAULT_REG6_S_PLUG_VOLT_ERR);
	}

	/**
	 * true if S-plug voltage error (BIT09). See {@link ChannelId#FAULT_REG6_S_PLUG_VOLT_ERR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6SPlugVoltErr() {
		return this.getFaultReg6SPlugVoltErrChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_OTHERS_FAULT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6OthersFaultChannel() {
		return this.channel(ChannelId.FAULT_REG6_OTHERS_FAULT);
	}

	/**
	 * true if fault reported by another parallel unit (BIT10). See {@link ChannelId#FAULT_REG6_OTHERS_FAULT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6OthersFault() {
		return this.getFaultReg6OthersFaultChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_CAN_BUS_LOSE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6CanBusLoseChannel() {
		return this.channel(ChannelId.FAULT_REG6_CAN_BUS_LOSE);
	}

	/**
	 * true if CAN bus lost (BIT11). See {@link ChannelId#FAULT_REG6_CAN_BUS_LOSE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6CanBusLose() {
		return this.getFaultReg6CanBusLoseChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_MODEL_MISMATCH}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6ModelMismatchChannel() {
		return this.channel(ChannelId.FAULT_REG6_MODEL_MISMATCH);
	}

	/**
	 * true if model mismatch between parallel units (BIT12). See {@link ChannelId#FAULT_REG6_MODEL_MISMATCH}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6ModelMismatch() {
		return this.getFaultReg6ModelMismatchChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_3P_CREATE_FAIL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg63pCreateFailChannel() {
		return this.channel(ChannelId.FAULT_REG6_3P_CREATE_FAIL);
	}

	/**
	 * true if 3-phase parallel group creation failed (BIT13). See {@link ChannelId#FAULT_REG6_3P_CREATE_FAIL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg63pCreateFail() {
		return this.getFaultReg63pCreateFailChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_ACBK_OPEN}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6AcbkOpenChannel() {
		return this.channel(ChannelId.FAULT_REG6_ACBK_OPEN);
	}

	/**
	 * true if AC breaker open (BIT14). See {@link ChannelId#FAULT_REG6_ACBK_OPEN}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6AcbkOpen() {
		return this.getFaultReg6AcbkOpenChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG6_RESERVED_15}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg6Reserved15Channel() {
		return this.channel(ChannelId.FAULT_REG6_RESERVED_15);
	}

	/**
	 * true if reserved (BIT15). See {@link ChannelId#FAULT_REG6_RESERVED_15}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg6Reserved15() {
		return this.getFaultReg6Reserved15Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault register 7 decoded bits (reg 33125, Appendix 4)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_REVE_DC}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7ReveDcChannel() {
		return this.channel(ChannelId.FAULT_REG7_REVE_DC);
	}

	/**
	 * true if reverse DC polarity fault (BIT00). See {@link ChannelId#FAULT_REG7_REVE_DC}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7ReveDc() {
		return this.getFaultReg7ReveDcChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_BATTERY_HW_OVERVOLTAGE_02}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7BatteryHwOvervoltage02Channel() {
		return this.channel(ChannelId.FAULT_REG7_BATTERY_HW_OVERVOLTAGE_02);
	}

	/**
	 * true if battery hardware overvoltage 02 (BIT01). See {@link ChannelId#FAULT_REG7_BATTERY_HW_OVERVOLTAGE_02}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7BatteryHwOvervoltage02() {
		return this.getFaultReg7BatteryHwOvervoltage02Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_BATTERY_HW_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7BatteryHwOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG7_BATTERY_HW_OVERCURRENT);
	}

	/**
	 * true if battery hardware overcurrent (BIT02). See {@link ChannelId#FAULT_REG7_BATTERY_HW_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7BatteryHwOvercurrent() {
		return this.getFaultReg7BatteryHwOvercurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_BUS_MIDPOINT_HW_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7BusMidpointHwOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG7_BUS_MIDPOINT_HW_OVERCURRENT);
	}

	/**
	 * true if bus midpoint hardware overcurrent (BIT03). See {@link ChannelId#FAULT_REG7_BUS_MIDPOINT_HW_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7BusMidpointHwOvercurrent() {
		return this.getFaultReg7BusMidpointHwOvercurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_BATTERY_STARTUP_FAIL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7BatteryStartupFailChannel() {
		return this.channel(ChannelId.FAULT_REG7_BATTERY_STARTUP_FAIL);
	}

	/**
	 * true if battery startup failure (BIT04). See {@link ChannelId#FAULT_REG7_BATTERY_STARTUP_FAIL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7BatteryStartupFail() {
		return this.getFaultReg7BatteryStartupFailChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_DC3_AVG_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7Dc3AvgOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG7_DC3_AVG_OVERCURRENT);
	}

	/**
	 * true if DC string 3 average overcurrent (BIT05). See {@link ChannelId#FAULT_REG7_DC3_AVG_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7Dc3AvgOvercurrent() {
		return this.getFaultReg7Dc3AvgOvercurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_DC4_AVG_OVERCURRENT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7Dc4AvgOvercurrentChannel() {
		return this.channel(ChannelId.FAULT_REG7_DC4_AVG_OVERCURRENT);
	}

	/**
	 * true if DC string 4 average overcurrent (BIT06). See {@link ChannelId#FAULT_REG7_DC4_AVG_OVERCURRENT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7Dc4AvgOvercurrent() {
		return this.getFaultReg7Dc4AvgOvercurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_SOFTRUN_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7SoftrunTimeoutChannel() {
		return this.channel(ChannelId.FAULT_REG7_SOFTRUN_TIMEOUT);
	}

	/**
	 * true if soft-run timeout (BIT07). See {@link ChannelId#FAULT_REG7_SOFTRUN_TIMEOUT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7SoftrunTimeout() {
		return this.getFaultReg7SoftrunTimeoutChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_OFFGRID_TO_GRID_TIMEOUT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7OffgridToGridTimeoutChannel() {
		return this.channel(ChannelId.FAULT_REG7_OFFGRID_TO_GRID_TIMEOUT);
	}

	/**
	 * true if off-grid to on-grid transition timeout (BIT08). See {@link ChannelId#FAULT_REG7_OFFGRID_TO_GRID_TIMEOUT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7OffgridToGridTimeout() {
		return this.getFaultReg7OffgridToGridTimeoutChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_DRM_NOT_CONNECT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7DrmNotConnectChannel() {
		return this.channel(ChannelId.FAULT_REG7_DRM_NOT_CONNECT);
	}

	/**
	 * true if DRM port not connected (BIT09). See {@link ChannelId#FAULT_REG7_DRM_NOT_CONNECT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7DrmNotConnect() {
		return this.getFaultReg7DrmNotConnectChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_RESERVED_10}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7Reserved10Channel() {
		return this.channel(ChannelId.FAULT_REG7_RESERVED_10);
	}

	/**
	 * true if reserved (BIT10). See {@link ChannelId#FAULT_REG7_RESERVED_10}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7Reserved10() {
		return this.getFaultReg7Reserved10Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_RESERVED_11}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7Reserved11Channel() {
		return this.channel(ChannelId.FAULT_REG7_RESERVED_11);
	}

	/**
	 * true if reserved (BIT11). See {@link ChannelId#FAULT_REG7_RESERVED_11}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7Reserved11() {
		return this.getFaultReg7Reserved11Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_RESERVED_12}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7Reserved12Channel() {
		return this.channel(ChannelId.FAULT_REG7_RESERVED_12);
	}

	/**
	 * true if reserved (BIT12). See {@link ChannelId#FAULT_REG7_RESERVED_12}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7Reserved12() {
		return this.getFaultReg7Reserved12Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_RESERVED_13}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7Reserved13Channel() {
		return this.channel(ChannelId.FAULT_REG7_RESERVED_13);
	}

	/**
	 * true if reserved (BIT13). See {@link ChannelId#FAULT_REG7_RESERVED_13}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7Reserved13() {
		return this.getFaultReg7Reserved13Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_RESERVED_14}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7Reserved14Channel() {
		return this.channel(ChannelId.FAULT_REG7_RESERVED_14);
	}

	/**
	 * true if reserved (BIT14). See {@link ChannelId#FAULT_REG7_RESERVED_14}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7Reserved14() {
		return this.getFaultReg7Reserved14Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#FAULT_REG7_RESERVED_15}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getFaultReg7Reserved15Channel() {
		return this.channel(ChannelId.FAULT_REG7_RESERVED_15);
	}

	/**
	 * true if reserved (BIT15). See {@link ChannelId#FAULT_REG7_RESERVED_15}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getFaultReg7Reserved15() {
		return this.getFaultReg7Reserved15Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Operating status decoded bits (reg 33121, Appendix 5)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_NORMAL_OPERATION}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatNormalOperationChannel() {
		return this.channel(ChannelId.OPERATING_STAT_NORMAL_OPERATION);
	}

	/**
	 * true if inverter in normal operation (BIT00). See {@link ChannelId#OPERATING_STAT_NORMAL_OPERATION}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatNormalOperation() {
		return this.getOperatingStatNormalOperationChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_INITIALIZING}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatInitializingChannel() {
		return this.channel(ChannelId.OPERATING_STAT_INITIALIZING);
	}

	/**
	 * true if inverter initializing (BIT01). See {@link ChannelId#OPERATING_STAT_INITIALIZING}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatInitializing() {
		return this.getOperatingStatInitializingChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_CONTROLLED_OFF}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatControlledOffChannel() {
		return this.channel(ChannelId.OPERATING_STAT_CONTROLLED_OFF);
	}

	/**
	 * true if controlled turn-off in progress (BIT02). See {@link ChannelId#OPERATING_STAT_CONTROLLED_OFF}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatControlledOff() {
		return this.getOperatingStatControlledOffChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_FAULT_OFF}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatFaultOffChannel() {
		return this.channel(ChannelId.OPERATING_STAT_FAULT_OFF);
	}

	/**
	 * true if fault has forced inverter off (BIT03). See {@link ChannelId#OPERATING_STAT_FAULT_OFF}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatFaultOff() {
		return this.getOperatingStatFaultOffChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_STANDBY}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatStandbyChannel() {
		return this.channel(ChannelId.OPERATING_STAT_STANDBY);
	}

	/**
	 * true if inverter in standby (BIT04). See {@link ChannelId#OPERATING_STAT_STANDBY}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatStandby() {
		return this.getOperatingStatStandbyChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_LIMITED_TEMP_FREQ}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatLimitedTempFreqChannel() {
		return this.channel(ChannelId.OPERATING_STAT_LIMITED_TEMP_FREQ);
	}

	/**
	 * true if limited operation — temperature or frequency (BIT05). See {@link ChannelId#OPERATING_STAT_LIMITED_TEMP_FREQ}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatLimitedTempFreq() {
		return this.getOperatingStatLimitedTempFreqChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_LIMITED_EXTERNAL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatLimitedExternalChannel() {
		return this.channel(ChannelId.OPERATING_STAT_LIMITED_EXTERNAL);
	}

	/**
	 * true if limited operation — external reason (BIT06). See {@link ChannelId#OPERATING_STAT_LIMITED_EXTERNAL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatLimitedExternal() {
		return this.getOperatingStatLimitedExternalChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_BACKUP_OVERLOAD}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatBackupOverloadChannel() {
		return this.channel(ChannelId.OPERATING_STAT_BACKUP_OVERLOAD);
	}

	/**
	 * true if backup port overloaded (BIT07). See {@link ChannelId#OPERATING_STAT_BACKUP_OVERLOAD}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatBackupOverload() {
		return this.getOperatingStatBackupOverloadChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_LOAD_FAULT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatLoadFaultChannel() {
		return this.channel(ChannelId.OPERATING_STAT_LOAD_FAULT);
	}

	/**
	 * true if load fault detected (BIT08). See {@link ChannelId#OPERATING_STAT_LOAD_FAULT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatLoadFault() {
		return this.getOperatingStatLoadFaultChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_GRID_FAULT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatGridFaultChannel() {
		return this.channel(ChannelId.OPERATING_STAT_GRID_FAULT);
	}

	/**
	 * true if grid fault / abnormal grid (BIT09). See {@link ChannelId#OPERATING_STAT_GRID_FAULT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatGridFault() {
		return this.getOperatingStatGridFaultChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_BATTERY_FAULT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatBatteryFaultChannel() {
		return this.channel(ChannelId.OPERATING_STAT_BATTERY_FAULT);
	}

	/**
	 * true if battery fault (BIT10). See {@link ChannelId#OPERATING_STAT_BATTERY_FAULT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatBatteryFault() {
		return this.getOperatingStatBatteryFaultChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_RESERVED_11}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatReserved11Channel() {
		return this.channel(ChannelId.OPERATING_STAT_RESERVED_11);
	}

	/**
	 * true if reserved (BIT11). See {@link ChannelId#OPERATING_STAT_RESERVED_11}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatReserved11() {
		return this.getOperatingStatReserved11Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_GRID_SURGE_WARN}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatGridSurgeWarnChannel() {
		return this.channel(ChannelId.OPERATING_STAT_GRID_SURGE_WARN);
	}

	/**
	 * true if grid surge warning (BIT12). See {@link ChannelId#OPERATING_STAT_GRID_SURGE_WARN}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatGridSurgeWarn() {
		return this.getOperatingStatGridSurgeWarnChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_FAN_FAULT_WARN}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatFanFaultWarnChannel() {
		return this.channel(ChannelId.OPERATING_STAT_FAN_FAULT_WARN);
	}

	/**
	 * true if fan fault warning (BIT13). See {@link ChannelId#OPERATING_STAT_FAN_FAULT_WARN}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatFanFaultWarn() {
		return this.getOperatingStatFanFaultWarnChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_EXTERNAL_FAN_FAIL}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatExternalFanFailChannel() {
		return this.channel(ChannelId.OPERATING_STAT_EXTERNAL_FAN_FAIL);
	}

	/**
	 * true if external fan failure (BIT14). See {@link ChannelId#OPERATING_STAT_EXTERNAL_FAN_FAIL}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatExternalFanFail() {
		return this.getOperatingStatExternalFanFailChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STAT_RESERVED_15}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getOperatingStatReserved15Channel() {
		return this.channel(ChannelId.OPERATING_STAT_RESERVED_15);
	}

	/**
	 * true if reserved (BIT15). See {@link ChannelId#OPERATING_STAT_RESERVED_15}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getOperatingStatReserved15() {
		return this.getOperatingStatReserved15Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Operating mode (reg 33122, Appendix 8)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#OPERATING_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getOperatingModeChannel() {
		return this.channel(ChannelId.OPERATING_MODE);
	}

	/**
	 * operating mode one-hot raw bitmask [reg 33122]. See {@link ChannelId#OPERATING_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getOperatingMode() {
		return this.getOperatingModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_MODE_DECODE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Appendix8> getOperatingModeDecodeChannel() {
		return this.channel(ChannelId.OPERATING_MODE_DECODE);
	}

	/**
	 * decoded operating mode (Appendix 8 enum). See {@link ChannelId#OPERATING_MODE_DECODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Appendix8 getOperatingModeDecode() {
		return this.getOperatingModeDecodeChannel().value().asEnum();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Working mode running status decoded bits (reg 33123)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#WMODE_VOLT_WATT}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getWmodeVoltWattChannel() {
		return this.channel(ChannelId.WMODE_VOLT_WATT);
	}

	/**
	 * true if Volt-watt mode running (BIT00). See {@link ChannelId#WMODE_VOLT_WATT}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getWmodeVoltWatt() {
		return this.getWmodeVoltWattChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#WMODE_VOLT_VAR}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getWmodeVoltVarChannel() {
		return this.channel(ChannelId.WMODE_VOLT_VAR);
	}

	/**
	 * true if Volt-var mode running (BIT01). See {@link ChannelId#WMODE_VOLT_VAR}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getWmodeVoltVar() {
		return this.getWmodeVoltVarChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#WMODE_FIXED_PF}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getWmodeFixedPfChannel() {
		return this.channel(ChannelId.WMODE_FIXED_PF);
	}

	/**
	 * true if fixed power factor mode running (BIT02). See {@link ChannelId#WMODE_FIXED_PF}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getWmodeFixedPf() {
		return this.getWmodeFixedPfChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#WMODE_FIX_REACTIVE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getWmodeFixReactiveChannel() {
		return this.channel(ChannelId.WMODE_FIX_REACTIVE);
	}

	/**
	 * true if fixed reactive power mode running (BIT03). See {@link ChannelId#WMODE_FIX_REACTIVE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getWmodeFixReactive() {
		return this.getWmodeFixReactiveChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#WMODE_POWER_PF}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getWmodePowerPfChannel() {
		return this.channel(ChannelId.WMODE_POWER_PF);
	}

	/**
	 * true if Power-PF mode running (BIT04). See {@link ChannelId#WMODE_POWER_PF}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getWmodePowerPf() {
		return this.getWmodePowerPfChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#WMODE_POWER_Q}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getWmodePowerQChannel() {
		return this.channel(ChannelId.WMODE_POWER_Q);
	}

	/**
	 * true if Power-Q mode running (BIT05). See {@link ChannelId#WMODE_POWER_Q}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getWmodePowerQ() {
		return this.getWmodePowerQChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Storage control decoded bits (reg 33132, Appendix 6)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_SELF_USE_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlSelfUseModeChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_SELF_USE_MODE);
	}

	/**
	 * true if self-use mode active (BIT00). See {@link ChannelId#STORAGE_CTRL_SELF_USE_MODE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlSelfUseMode() {
		return this.getStorageCtrlSelfUseModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_TIME_OF_USE_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlTimeOfUseModeChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_TIME_OF_USE_MODE);
	}

	/**
	 * true if time-of-use mode active (BIT01). See {@link ChannelId#STORAGE_CTRL_TIME_OF_USE_MODE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlTimeOfUseMode() {
		return this.getStorageCtrlTimeOfUseModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_OFFGRID_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlOffgridModeChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_OFFGRID_MODE);
	}

	/**
	 * true if off-grid mode active (BIT02). See {@link ChannelId#STORAGE_CTRL_OFFGRID_MODE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlOffgridMode() {
		return this.getStorageCtrlOffgridModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_BATT_WAKEUP}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlBattWakeupChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_BATT_WAKEUP);
	}

	/**
	 * true if battery wakeup switch active (BIT03). See {@link ChannelId#STORAGE_CTRL_BATT_WAKEUP}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlBattWakeup() {
		return this.getStorageCtrlBattWakeupChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_RESERVE_BATT_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlReserveBattModeChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_RESERVE_BATT_MODE);
	}

	/**
	 * true if reserve battery mode active (BIT04). See {@link ChannelId#STORAGE_CTRL_RESERVE_BATT_MODE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlReserveBattMode() {
		return this.getStorageCtrlReserveBattModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_ALLOW_GRID_CHARGE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlAllowGridChargeChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_ALLOW_GRID_CHARGE);
	}

	/**
	 * true if grid-to-battery charging allowed (BIT05). See {@link ChannelId#STORAGE_CTRL_ALLOW_GRID_CHARGE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlAllowGridCharge() {
		return this.getStorageCtrlAllowGridChargeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_FEED_IN_PRIORITY}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlFeedInPriorityChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_FEED_IN_PRIORITY);
	}

	/**
	 * true if feed-in priority mode active (BIT06). See {@link ChannelId#STORAGE_CTRL_FEED_IN_PRIORITY}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlFeedInPriority() {
		return this.getStorageCtrlFeedInPriorityChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_BATT_OVC}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlBattOvcChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_BATT_OVC);
	}

	/**
	 * true if battery OVC function active (BIT07). See {@link ChannelId#STORAGE_CTRL_BATT_OVC}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlBattOvc() {
		return this.getStorageCtrlBattOvcChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_FORCE_CHARGE_PEAKSHAVING}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlForceChargePeakshavingChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_FORCE_CHARGE_PEAKSHAVING);
	}

	/**
	 * true if force charge / peak shaving active (BIT08). See {@link ChannelId#STORAGE_CTRL_FORCE_CHARGE_PEAKSHAVING}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlForceChargePeakshaving() {
		return this.getStorageCtrlForceChargePeakshavingChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_BATT_CURRENT_CORRECTION}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlBattCurrentCorrectionChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_BATT_CURRENT_CORRECTION);
	}

	/**
	 * true if battery current correction enabled (BIT09). See {@link ChannelId#STORAGE_CTRL_BATT_CURRENT_CORRECTION}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlBattCurrentCorrection() {
		return this.getStorageCtrlBattCurrentCorrectionChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_BATT_HEALING_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlBattHealingModeChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_BATT_HEALING_MODE);
	}

	/**
	 * true if battery healing mode active (BIT10). See {@link ChannelId#STORAGE_CTRL_BATT_HEALING_MODE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlBattHealingMode() {
		return this.getStorageCtrlBattHealingModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_PEAK_SHAVING_MODE}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlPeakShavingModeChannel() {
		return this.channel(ChannelId.STORAGE_CTRL_PEAK_SHAVING_MODE);
	}

	/**
	 * true if peak-shaving mode active (BIT11). See {@link ChannelId#STORAGE_CTRL_PEAK_SHAVING_MODE}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlPeakShavingMode() {
		return this.getStorageCtrlPeakShavingModeChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_RESERVED_12}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlReserved12Channel() {
		return this.channel(ChannelId.STORAGE_CTRL_RESERVED_12);
	}

	/**
	 * true if reserved (BIT12). See {@link ChannelId#STORAGE_CTRL_RESERVED_12}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlReserved12() {
		return this.getStorageCtrlReserved12Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_RESERVED_13}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlReserved13Channel() {
		return this.channel(ChannelId.STORAGE_CTRL_RESERVED_13);
	}

	/**
	 * true if reserved (BIT13). See {@link ChannelId#STORAGE_CTRL_RESERVED_13}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlReserved13() {
		return this.getStorageCtrlReserved13Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_RESERVED_14}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlReserved14Channel() {
		return this.channel(ChannelId.STORAGE_CTRL_RESERVED_14);
	}

	/**
	 * true if reserved (BIT14). See {@link ChannelId#STORAGE_CTRL_RESERVED_14}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlReserved14() {
		return this.getStorageCtrlReserved14Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#STORAGE_CTRL_RESERVED_15}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getStorageCtrlReserved15Channel() {
		return this.channel(ChannelId.STORAGE_CTRL_RESERVED_15);
	}

	/**
	 * true if reserved (BIT15). See {@link ChannelId#STORAGE_CTRL_RESERVED_15}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getStorageCtrlReserved15() {
		return this.getStorageCtrlReserved15Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Legacy / raw-word channels (superseded by per-bit decoded sub-channels)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#STARTER_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getStarterBatteryVoltageChannel() {
		return this.channel(ChannelId.STARTER_BATTERY_VOLTAGE);
	}

	/**
	 * starter battery voltage [V] — register not yet identified. See {@link ChannelId#STARTER_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getStarterBatteryVoltage() {
		return this.getStarterBatteryVoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INVERTED_RATED_APPARENT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getInvertedRatedApparentPowerChannel() {
		return this.channel(ChannelId.INVERTED_RATED_APPARENT_POWER);
	}

	/**
	 * inverter rated apparent power [VA]. See {@link ChannelId#INVERTED_RATED_APPARENT_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getInvertedRatedApparentPower() {
		return this.getInvertedRatedApparentPowerChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#FUNCTION_STATUS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getFunctionStatusChannel() {
		return this.channel(ChannelId.FUNCTION_STATUS);
	}

	/**
	 * function status raw word [reg 33097] — use FUNCTION_STAT_* instead. See {@link ChannelId#FUNCTION_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getFunctionStatus() {
		return this.getFunctionStatusChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#INVERTER_INITIAL_SETTING_STATE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getInverterInitialSettingStateChannel() {
		return this.channel(ChannelId.INVERTER_INITIAL_SETTING_STATE);
	}

	/**
	 * initial setting state raw word [reg 33112] — use INIT_STATE_* instead. See {@link ChannelId#INVERTER_INITIAL_SETTING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getInverterInitialSettingState() {
		return this.getInverterInitialSettingStateChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BATCH_UPGRADE_BOWL}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatchUpgradeBowlChannel() {
		return this.channel(ChannelId.BATCH_UPGRADE_BOWL);
	}

	/**
	 * batch upgrade support raw word [reg 33113] — use BATCH_UPGRADE_* instead. See {@link ChannelId#BATCH_UPGRADE_BOWL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatchUpgradeBowl() {
		return this.getBatchUpgradeBowlChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#SETTING_FLAG_BIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getSettingFlagBitChannel() {
		return this.channel(ChannelId.SETTING_FLAG_BIT);
	}

	/**
	 * setting flag raw word [reg 33115] — use SETTING_FLAG_* instead. See {@link ChannelId#SETTING_FLAG_BIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSettingFlagBit() {
		return this.getSettingFlagBitChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#OPERATING_STATUS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getOperatingStatusChannel() {
		return this.channel(ChannelId.OPERATING_STATUS);
	}

	/**
	 * operating status raw word [reg 33121] — use OPERATING_STAT_* instead. See {@link ChannelId#OPERATING_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getOperatingStatus() {
		return this.getOperatingStatusChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#WORKING_MODE_RUNNING_STATUS}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getWorkingModeRunningStatusChannel() {
		return this.channel(ChannelId.WORKING_MODE_RUNNING_STATUS);
	}

	/**
	 * working mode running status raw word [reg 33123] — use WMODE_* instead. See {@link ChannelId#WORKING_MODE_RUNNING_STATUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getWorkingModeRunningStatus() {
		return this.getWorkingModeRunningStatusChannel().value();
	}
	
	// storage control switch 
	/**
	 * Channel for {@link ChannelId#STORAGE_CONTROL_SWITCHING_VALUE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetStorageControlSwitchChannel() {
		return this.channel(ChannelId.SET_STORAGE_CTRL_SWITCH);
	}
	
	private static int setBit(int word, int bit, boolean enabled) {
		return enabled
				? word | (1 << bit)
				: word & ~(1 << bit);
	}

	/**
	 * Reconstructs the current raw value of reg 43110 from the STORAGE_CTRL_* bit
	 * channels (Appendix 6).
	 *
	 * @return the raw word, or null if the register has not been read yet
	 */
	public default Integer getStorageControlSwitchRaw() {
		var bits = new ChannelId[] { //
				ChannelId.STORAGE_CTRL_SELF_USE_MODE, ChannelId.STORAGE_CTRL_TIME_OF_USE_MODE,
				ChannelId.STORAGE_CTRL_OFFGRID_MODE, ChannelId.STORAGE_CTRL_BATT_WAKEUP,
				ChannelId.STORAGE_CTRL_RESERVE_BATT_MODE, ChannelId.STORAGE_CTRL_ALLOW_GRID_CHARGE,
				ChannelId.STORAGE_CTRL_FEED_IN_PRIORITY, ChannelId.STORAGE_CTRL_BATT_OVC,
				ChannelId.STORAGE_CTRL_FORCE_CHARGE_PEAKSHAVING, ChannelId.STORAGE_CTRL_BATT_CURRENT_CORRECTION,
				ChannelId.STORAGE_CTRL_BATT_HEALING_MODE, ChannelId.STORAGE_CTRL_PEAK_SHAVING_MODE,
				ChannelId.STORAGE_CTRL_RESERVED_12, ChannelId.STORAGE_CTRL_RESERVED_13,
				ChannelId.STORAGE_CTRL_RESERVED_14, ChannelId.STORAGE_CTRL_RESERVED_15 };
		int word = 0;
		for (int i = 0; i < bits.length; i++) {
			BooleanReadChannel channel = this.channel(bits[i]);
			Boolean value = channel.value().get();
			if (value == null) {
				return null;
			}
			if (value) {
				word |= 1 << i;
			}
		}
		return word;
	}

	/**
	 * Sets bits 0-6 of the storage control switch (reg 43110). Datasheet requires
	 * read-modify-write, so bits 7-15 are taken from the current read-back value;
	 * nothing is written while the read-back is still unknown or when the value
	 * would not change.
	 *
	 * @param selfUse           BIT00 Self-Use mode
	 * @param timeOfUse         BIT01 Time of use mode
	 * @param offGrid           BIT02 Off-grid mode
	 * @param batteryWakeup     BIT03 Battery wakeup switch
	 * @param reserveBattery    BIT04 Reserve battery mode
	 * @param gridChargeAllowed BIT05 Allow grid to charge the battery
	 * @param feedInPriority    BIT06 Feed in priority mode
	 * @throws OpenemsNamedException on write error
	 */
	public default void setStorageControlSwitch(
			boolean selfUse,
			boolean timeOfUse,
			boolean offGrid,
			boolean batteryWakeup,
			boolean reserveBattery,
			boolean gridChargeAllowed,
			boolean feedInPriority) throws OpenemsNamedException {

		Integer current = this.getStorageControlSwitchRaw();
		if (current == null) {
			return;
		}

		int value = current;
		value = setBit(value, 0, selfUse);
		value = setBit(value, 1, timeOfUse);
		value = setBit(value, 2, offGrid);
		value = setBit(value, 3, batteryWakeup);
		value = setBit(value, 4, reserveBattery);
		value = setBit(value, 5, gridChargeAllowed);
		value = setBit(value, 6, feedInPriority);

		if (value != current) {
			this.getSetStorageControlSwitchChannel().setNextWriteValue(value);
		}
	}


	// -----------------------------------------------------------------------
	// Accessor methods – Unconfirmed register channels (register address to be verified)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#SET_REMOTE_CONTROL_AC_GRID_PORT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetRemoteControlAcGridPortPowerChannel() {
		return this.channel(ChannelId.SET_REMOTE_CONTROL_AC_GRID_PORT_POWER);
	}

	/**
	 * Channel for {@link ChannelId#REMOTE_CONTROL_AC_GRID_PORT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getRemoteControlAcGridPortPowerChannel() {
		return this.channel(ChannelId.REMOTE_CONTROL_AC_GRID_PORT_POWER);
	}


	//
	/**
	 * Adds Battery to ESS hybrid system.
	 *
	 * @param battery link to Pytes battery
	 */
	public void addBattery(PytesBattery battery);

	/**
	 * Removes link to battery.
	 *
	 * @param battery link to Pytes battery
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
	 * returns ModbusBridgeId from config.
	 *
	 * @return ModbusBridgeId from config
	 */
	public String getModbusBridgeId();

	/**
	 * Gets the Modbus unit ID from config.
	 *
	 * @return the Modbus unit ID
	 */
	public Integer getUnitId();
}
