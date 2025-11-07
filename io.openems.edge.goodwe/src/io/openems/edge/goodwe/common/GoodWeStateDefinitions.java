package io.openems.edge.goodwe.common;

import java.util.List;

public class GoodWeStateDefinitions {

	public record GwState(int bit, String description) {
	}

	public record GwStateTask(int register, List<GwState> gwStates) {
	}

	public static final List<GwStateTask> GOODWE_STATE_REGISTER_TASKS = List.of(
			// Register 32000: GridFault1
			new GwStateTask(32000, List.of(//
					new GwState(0, "Grid Power Outage detected"), //
					new GwState(1, "Grid Overvoltage detected"), //
					new GwState(2, "Grid Undervoltage detected"), //
					new GwState(3, "Grid Rapid Overvoltage detected"), //
					new GwState(4, "Grid 10min Overvoltage detected"), //
					new GwState(5, "Grid Overfrequency detected"), //
					new GwState(6, "Grid Underfrequency detected"), //
					new GwState(7, "Grid Frequency Unstable detected"), //
					new GwState(8, "Grid Phase Unstable detected"), //
					new GwState(9, "Anti-islanding Protection triggered"), //
					new GwState(10, "LVRT Undervoltage detected"), //
					new GwState(11, "HVRT Overvoltage detected"), //
					new GwState(12, "Grid Waveform Abnormality detected"), //
					new GwState(13, "Grid Phase Loss detected"), //
					new GwState(14, "Grid Voltage Imbalance detected"), //
					new GwState(15, "Grid Phase Sequence Abnormal detected"))),

			// Register 32001: GridFault2
			new GwStateTask(32001, List.of(//
					new GwState(0, "Grid Rapid Shutdown Protection triggered"), //
					new GwState(1, "Neutral Line Loss detected (Split Grid)"))),

			// Register 32002: SystemFault1
			new GwStateTask(32002, List.of(//
					new GwState(0, "GFCI Protection triggered (30mA)"), //
					new GwState(1, "GFCI Protection triggered (60mA)"), //
					new GwState(2, "GFCI Protection triggered (150mA)"), //
					new GwState(3, "GFCI Protection triggered (300mA)"), //
					new GwState(4, "DCI Protection Level 1 triggered"), //
					new GwState(5, "DCI Protection Level 2 triggered"), //
					new GwState(6, "Low Insulation Resistance detected"), //
					new GwState(7, "Grounding Abnormality detected"), //
					new GwState(8, "L-PE Short Circuit detected"), //
					new GwState(9, "DCV Protection Level 1 triggered"), //
					new GwState(10, "DCV Protection Level 2 triggered"), //
					new GwState(11, "Hard Export Limit Protection triggered"), //
					new GwState(12, "Internal Communication Loss detected"),
					new GwState(13, "Multiple GFCI Failures detected"), //
					new GwState(14, "Multiple AFCI Failures detected"), //
					new GwState(15, "External Communication Loss detected"))),

			// Register 32003: SystemFault2
			new GwStateTask(32003, List.of(//
					new GwState(0, "AC Switch Abnormality detected"), //
					new GwState(1, "AC Relay Abnormality detected"), //
					new GwState(2, "DC Relay Abnormality detected"), //
					new GwState(3, "Contactor Abnormality detected"), //
					new GwState(4, "DSP Hardware Error detected"), //
					new GwState(5, "ARM Hardware Error detected"), //
					new GwState(6, "Bus Voltage Sampling Error detected"), //
					new GwState(7, "Bus Current Sampling Error detected"), //
					new GwState(8, "Grid Voltage Sampling Error detected"), //
					new GwState(9, "Grid Current Sampling Error detected"), //
					new GwState(10, "Relay Check Failure detected"), //
					new GwState(11, "Measurement Unit Error detected"), //
					new GwState(12, "Temperature Sampling Error detected"), //
					new GwState(13, "EEPROM Failure detected"), //
					new GwState(14, "RTC Error detected"), //
					new GwState(15, "Internal Fan Failure detected"))),

			// Register 32004: DeviceFault1
			new GwStateTask(32004, List.of(//
					new GwState(0, "AC Overcurrent detected"), //
					new GwState(1, "AC Short Circuit detected"), //
					new GwState(2, "DC Bus Overvoltage detected"), //
					new GwState(3, "DC Bus Undervoltage detected"), //
					new GwState(4, "DC Bus Voltage Imbalance detected"), //
					new GwState(5, "DC Component Over Limit detected"), //
					new GwState(6, "Device Power Limit Overrun detected"), //
					new GwState(7, "Inverter Output Power Over Limit detected"), //
					new GwState(8, "Inverter Input Power Over Limit detected"), //
					new GwState(9, "Reactive Power Over Limit detected"), //
					new GwState(10, "Apparent Power Over Limit detected"), //
					new GwState(11, "AC Voltage Over Limit detected"), //
					new GwState(12, "AC Current Over Limit detected"), //
					new GwState(13, "Phase Current Imbalance detected"), //
					new GwState(14, "Neutral Current Over Limit detected"), //
					new GwState(15, "Harmonic Over Limit detected"))),

			// Register 32005: DeviceFault2
			new GwStateTask(32005, List.of(//
					new GwState(0, "Internal Temperature Over Limit detected"), //
					new GwState(1, "External Temperature Over Limit detected"), //
					new GwState(2, "IGBT Temperature Over Limit detected"), //
					new GwState(3, "Transformer Temperature Over Limit detected"), //
					new GwState(4, "Radiator Temperature Over Limit detected"), //
					new GwState(5, "Temperature Sensor Failure detected"), //
					new GwState(6, "Cooling System Failure detected"), //
					new GwState(7, "Fan Speed Abnormality detected"), //
					new GwState(8, "Heatsink Temperature Sensor Error detected"), //
					new GwState(9, "Ambient Temperature Sensor Error detected"), //
					new GwState(10, "Inlet Air Temperature Over Limit detected"), //
					new GwState(11, "Exhaust Air Temperature Over Limit detected"), //
					new GwState(12, "Temperature Deviation Too Large detected"), //
					new GwState(13, "Derating Due to Overtemperature detected"), //
					new GwState(14, "Inverter Overheating Shutdown triggered"), //
					new GwState(15, "Overtemperature Recovery detected"))),

			// Register 32006: DCFault1
			new GwStateTask(32006, List.of(//
					new GwState(0, "BUS Overvoltage detected"), //
					new GwState(1, "P-BUS Overvoltage detected"), //
					new GwState(2, "N-BUS Overvoltage detected"), //
					new GwState(3, "BUS Overvoltage (Slave CPU 1) detected"), //
					new GwState(4, "P-BUS Overvoltage (Slave CPU 1) detected"), //
					new GwState(5, "N-BUS Overvoltage (Slave CPU 1) detected"), //
					new GwState(6, "BUS Overvoltage (Slave CPU 2) detected"), //
					new GwState(7, "P-BUS Overvoltage (Slave CPU 2) detected"), //
					new GwState(8, "N-BUS Overvoltage (Slave CPU 2) detected"), //
					new GwState(9, "P-BUS Overvoltage (CPLD) detected"), //
					new GwState(10, "N-BUS Overvoltage (CPLD) detected"), //
					new GwState(11, "Multiple MOS Continuous Overvoltage detected"), //
					new GwState(12, "Bus Short Circuit detected"), //
					new GwState(13, "Bus Sample Error detected"), //
					new GwState(14, "DC Sample Error detected"))),

			// Register 32007: DCFault2
			new GwStateTask(32007, List.of(//
					new GwState(0, "PV Input Overvoltage detected"), //
					new GwState(1, "PV Continuous Hardware Overcurrent detected"), //
					new GwState(2, "PV Continuous Software Overcurrent detected"), //
					new GwState(3, "FlyCap Software Overvoltage detected"), //
					new GwState(4, "FlyCap Hardware Overvoltage detected"), //
					new GwState(5, "FlyCap Undervoltage detected"), //
					new GwState(6, "FlyCap Precharge Failure detected"), //
					new GwState(7, "FlyCap Precharge Abnormal detected"), //
					new GwState(8, "PV String Overcurrent detected (String 1~16)"), //
					new GwState(9, "PV String Overcurrent detected (String 17~32)"), //
					new GwState(10, "PV String Reversed detected (String 1~16)"), //
					new GwState(11, "PV String Reversed detected (String 17~32)"), //
					new GwState(12, "PV String Loss detected (String 1~16)"), //
					new GwState(13, "PV String Loss detected (String 17~32)"))),

			// Register 32008: DCFault3
			new GwStateTask(32008, List.of(//
					new GwState(0, "PV Input Mode Error detected"), //
					new GwState(1, "PV String Reversed (String 33~48)"), //
					new GwState(2, "PV String Loss (String 33~48)"), //
					new GwState(3, "PV String Overcurrent detected (String 33~48)"), //
					new GwState(4, "PV TBPHS Fault detected"))),

			// Register 32009: BatFault1
			new GwStateTask(32009, List.of(//
					new GwState(0, "BAT 1 Precharge Failure detected"), //
					new GwState(1, "BAT 1 Relay Failure detected"), //
					new GwState(2, "BAT 1 Overvoltage detected"), //
					new GwState(3, "BAT 2 Precharge Failure detected"), //
					new GwState(4, "BAT 2 Relay Failure detected"), //
					new GwState(5, "BAT 2 Overvoltage detected"), //
					new GwState(6, "BAT 1 Reversed detected"), //
					new GwState(7, "BAT 2 Reversed detected"), //
					new GwState(8, "BAT Connection Abnormal detected"))),

			// Register 32010: BatFault2 (dummy)
			new GwStateTask(32010, List.of()),

			// Register 32011: DeviceFault3
			new GwStateTask(32011, List.of(//
					new GwState(0, "Battery Overtemperature detected"), //
					new GwState(1, "Reference Voltage Abnormal detected"))),

			// Register 32012: InnerFault1
			new GwStateTask(32012, List.of(//
					new GwState(0, "PV Voltage Low detected"), //
					new GwState(1, "Bus Voltage Low detected"), //
					new GwState(2, "Bus Soft Start Failure detected"), //
					new GwState(3, "Bus Voltage Imbalance detected"), //
					new GwState(4, "Grid Phase Lock Failure detected"), //
					new GwState(5, "Inverter Continuous Overcurrent detected"), //
					new GwState(6, "Output Current Imbalance detected"), //
					new GwState(7, "Low Insulation Resistance (Internal)"), //
					new GwState(8, "GFCI Protection (30mA Internal)"), //
					new GwState(9, "GFCI Protection (60mA Internal)"), //
					new GwState(10, "GFCI Protection (150mA Internal)"), //
					new GwState(11, "GFCI Protection (300mA Internal)"), //
					new GwState(12, "Parallell I/O Check Failure"), //
					new GwState(13, "BUS (by BAT) Soft Start Failure"), //
					new GwState(14, "BAT Voltage Low"), //
					new GwState(15, "BUS Continuous Overvoltage"))),

			// Register 32013: InnerFault2
			new GwStateTask(32013, List.of(//
					new GwState(0, "Inverter Software Overcurrent detected"), //
					new GwState(1, "R Phase Hardware Overcurrent detected"), //
					new GwState(2, "S Phase Hardware Overcurrent detected"), //
					new GwState(3, "T Phase Hardware Overcurrent detected"), //
					new GwState(4, "PV Hardware Overcurrent detected"), //
					new GwState(5, "PV Software Overcurrent detected"), //
					new GwState(6, "PV Power Low"), //
					new GwState(7, "PWM Once Abnormal"), //
					new GwState(8, "R Phase Software Overcurrent"), //
					new GwState(9, "S Phase Software Overcurrent"), //
					new GwState(10, "T Phase Software Overcurrent"), //
					new GwState(11, "SPS Undervoltage"), //
					new GwState(12, "PID_SVG Boost Module Failure"), //
					new GwState(13, "Single FlyCap Precharge Failure"), //
					new GwState(14, "MOS Hardware Overvoltage"), //
					new GwState(15, "SVG Precharge Failure"))),

			// Register 32014: InnerFault3
			new GwStateTask(32014, List.of(//
					new GwState(0, "BAT 1 Hardware Overcurrent"), //
					new GwState(1, "BAT 1 Software Overcurrent"), //
					new GwState(2, "BAT 1 Undervoltage Shutdown (Off-grid Mode)"), //
					new GwState(3, "BAT 1 Abnormal Connection"), //
					new GwState(4, "BAT 1 Abnormal Disonnection"), //
					new GwState(5, "BAT 2 Hardware Overcurrent"), //
					new GwState(6, "BAT 2 Software Overcurrent"), //
					new GwState(7, "BAT 2 Undervoltage Shutdown (Off-grid Mode)"), //
					new GwState(8, "BAT 2 Abnormal Connection"), //
					new GwState(9, "BAT 2 Abnormal Disconnection"), //
					new GwState(10, "PCS Voltage/Current Protection"), //
					new GwState(11, "Inverter Multiple Overcurrent"), //
					new GwState(12, "DAB Overload"), //
					new GwState(13, "DAB Overcurrent (Turn-off)"), //
					new GwState(14, "DAB Hardware Overcurrent"), //
					new GwState(15, "DAB Driver Undervoltage"))),

			// Register 32015: InnerFault4
			new GwStateTask(32015, List.of(//
					new GwState(0, "Back-up Output Undervoltage"), //
					new GwState(2, "Back-up Output Voltage Loss"), //
					new GwState(3, "CPLD Protection"), //
					new GwState(5, "AC Reconnect Failure"), //
					new GwState(6, "Module Failure(Slave CPU)"), //
					new GwState(8, "Microgrid Failure"), //
					new GwState(10, "Generator Port Overload"))),

			// Register 32016: Warning1
			new GwStateTask(32016, List.of(//
					new GwState(0, "AC SPD Fault"), //
					new GwState(1, "DC SPD Fault"), //
					new GwState(2, "Internal Fan Abnormal"), //
					new GwState(3, "External Fan Abnormal"), //
					new GwState(4, "PID Abnormal"), //
					new GwState(5, "Trip-Switch Trip Warning"), //
					new GwState(6, "PV IGBT Short Circuit Warning"), //
					new GwState(7, "PV String Reversed Warning (String 1~16)"), //
					new GwState(8, "PV String Reversed Warning (String 17~32)"), //
					new GwState(9, "Flash R/W Error"), //
					new GwState(10, "Meter Comm Loss"), //
					new GwState(11, "PV Type Identification Failure"), //
					new GwState(12, "PV String Mismatch"), //
					new GwState(13, "CT Loss"), //
					new GwState(14, "CT Reversed"), //
					new GwState(15, "PE Loss"))),

			// Register 32017: Warning2
			new GwStateTask(32017, List.of(//
					new GwState(0, "PV String Terminal Overtemperature"), //
					new GwState(1, "PV String Terminal Overtemperature"), //
					new GwState(2, "PV String Terminal Overtemperature"), //
					new GwState(3, "PV String Reversed Warning(String 33~48)"))),

			// Register 32018: InnerFault5
			new GwStateTask(32018, List.of(//
					new GwState(0, "Balanced Bridge Hardware Overcurrent"), //
					new GwState(1, "Balanced Bridge Software Overcurrent"), //
					new GwState(2, "PLL Udp Err"), //
					new GwState(3, "Microgrid Overload Warning"))),

			// Registers 32019-32025: Undefined, treated as dummy.
			new GwStateTask(32019, List.of()), //
			new GwStateTask(32020, List.of()), //
			new GwStateTask(32021, List.of()), //
			new GwStateTask(32022, List.of()), //
			new GwStateTask(32023, List.of()), //
			new GwStateTask(32024, List.of()), //
			new GwStateTask(32025, List.of()), //

			// Register 32026: FaultGuide1
			new GwStateTask(32026, List.of(//
					new GwState(0, "Grid Failure 1"), //
					new GwState(1, "Grid Failure 2"), //
					new GwState(2, "System Failure 1"), //
					new GwState(3, "System Failure 2"), //
					new GwState(4, "Device Failure 1"), //
					new GwState(5, "Device Failure 2"), //
					new GwState(6, "DCLink Failure"), //
					new GwState(7, "PVLink Failure 1"), //
					new GwState(8, "PVLink Failure 2"), //
					new GwState(9, "BAT Failure 1"), //
					new GwState(10, "BAT Failure 2"), //
					new GwState(11, "BAT Failure 3"))),

			// Register 32027: FaultGuide2 (dummy)
			new GwStateTask(32027, List.of()),

			// Register 32028: WarningGuide1
			new GwStateTask(32028, List.of(//
					new GwState(0, "Internal Warning 1"), //
					new GwState(1, "Internal Warning 2"), //
					new GwState(2, "Internal Warning 3"), //
					new GwState(3, "Internal Warning 4"), //
					new GwState(4, "Warning 1"))));
}
