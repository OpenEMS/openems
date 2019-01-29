package io.openems.edge.battery.soltaro.master;

import io.openems.edge.battery.soltaro.master.ClusterRunState;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.Unit;

public enum MasterChannelId implements io.openems.edge.common.channel.doc.ChannelId {
	STATE_MACHINE(new Doc().level(Level.INFO).text("Current State of State-Machine").options(State.values())), //
	START_STOP(new Doc().options(Enums.StartStop.values())), //
	RACK_1_USAGE(new Doc().options(Enums.RackUsage.values())), //
	RACK_2_USAGE(new Doc().options(Enums.RackUsage.values())), //
	RACK_3_USAGE(new Doc().options(Enums.RackUsage.values())), //
	CHARGE_INDICATION(new Doc().options(Enums.ChargeIndication.values())), //
	CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
	SYSTEM_RUNNING_STATE(new Doc().options(Enums.RunningState.values())), //
	VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	MASTER_ALARM_PCS_OUT_OF_CONTROL(new Doc().level(Level.FAULT).text("PCS out of control alarm")),
	MASTER_ALARM_PCS_COMMUNICATION_FAULT(new Doc().level(Level.FAULT).text("PCS communication fault alarm")),
	SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_3(new Doc().level(Level.FAULT).text("Communication to sub master 3 fault")),
	SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_2(new Doc().level(Level.FAULT).text("Communication to sub master 2 fault")),
	SUB_MASTER_COMMUNICATION_FAULT_ALARM_MASTER_1(new Doc().level(Level.FAULT).text("Communication to sub master 1 fault")),
	
	RACK_1_STATE(new Doc().unit(Unit.NONE)), //
	RACK_1_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
	RACK_1_CHARGE_INDICATION(new Doc().options(Enums.ChargeIndication.values())), //
	RACK_1_SOC(new Doc().unit(Unit.PERCENT)), //
	RACK_1_SOH(new Doc().unit(Unit.PERCENT)), //
	RACK_1_MAX_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
	RACK_1_MAX_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_MIN_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
	RACK_1_MIN_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_MAX_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
	RACK_1_MAX_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_MIN_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
	RACK_1_MIN_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature Low Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature High Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Charge Temperature Low Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Charge Temperature High Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Discharge Current High Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(
			new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage Low Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Low Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_CHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Charge Current High Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage High Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage High Alarm Level 2")), //
	RACK_1_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature Low Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Discharge Temperature High Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Total Voltage Diff High Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_INSULATION_LOW(new Doc().level(Level.WARNING).text("Cluster 3 Insulation Low Alarm Level1")), //
	RACK_1_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Diff High Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster X Cell temperature Diff High Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_SOC_LOW(new Doc().level(Level.WARNING).text("Cluster 1 SOC Low Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Charge Temperature Low Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Charge Temperature High Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Discharge Current High Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(
			new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage Low Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage Low Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_CHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Charge Current High Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Total Voltage High Alarm Level 1")), //
	RACK_1_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 1 Cell Voltage High Alarm Level 1")), //
	RACK_1_RUN_STATE(new Doc().options(ClusterRunState.values())), //
	RACK_1_FAILURE_INITIALIZATION(new Doc().level(Level.FAULT).text("Initialization failure")), //
	RACK_1_FAILURE_EEPROM(new Doc().level(Level.FAULT).text("EEPROM fault")), //
	RACK_1_FAILURE_INTRANET_COMMUNICATION(new Doc().level(Level.FAULT).text("Intranet communication fault")), //
	RACK_1_FAILURE_TEMP_SAMPLING_LINE(new Doc().level(Level.FAULT).text("Temperature sampling line fault")), //
	RACK_1_FAILURE_BALANCING_MODULE(new Doc().level(Level.FAULT).text("Balancing module fault")), //
	RACK_1_FAILURE_TEMP_SENSOR(new Doc().level(Level.FAULT).text("Temperature sensor fault")), //
	RACK_1_FAILURE_TEMP_SAMPLING(new Doc().level(Level.FAULT).text("Temperature sampling fault")), //
	RACK_1_FAILURE_VOLTAGE_SAMPLING(new Doc().level(Level.FAULT).text("Voltage sampling fault")), //
	RACK_1_FAILURE_LTC6803(new Doc().level(Level.FAULT).text("LTC6803 fault")), //
	RACK_1_FAILURE_CONNECTOR_WIRE(new Doc().level(Level.FAULT).text("connector wire fault")), //
	RACK_1_FAILURE_SAMPLING_WIRE(new Doc().level(Level.FAULT).text("sampling wire fault")), //

	RACK_1_BATTERY_000_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_001_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_002_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_003_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_004_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_005_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_006_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_007_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_008_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_009_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_010_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_011_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_012_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_013_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_014_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_015_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_016_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_017_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_018_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_019_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_020_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_021_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_022_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_023_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_024_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_025_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_026_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_027_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_028_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_029_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_030_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_031_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_032_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_033_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_034_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_035_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_036_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_037_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_038_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_039_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_040_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_041_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_042_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_043_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_044_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_045_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_046_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_047_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_048_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_049_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_050_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_051_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_052_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_053_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_054_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_055_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_056_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_057_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_058_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_059_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_060_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_061_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_062_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_063_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_064_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_065_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_066_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_067_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_068_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_069_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_070_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_071_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_072_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_073_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_074_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_075_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_076_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_077_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_078_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_079_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_080_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_081_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_082_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_083_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_084_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_085_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_086_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_087_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_088_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_089_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_090_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_091_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_092_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_093_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_094_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_095_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_096_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_097_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_098_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_099_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_100_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_101_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_102_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_103_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_104_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_105_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_106_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_107_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_108_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_109_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_110_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_111_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_112_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_113_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_114_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_115_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_116_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_117_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_118_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_119_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_120_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_121_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_122_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_123_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_124_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_125_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_126_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_127_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_128_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_129_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_130_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_131_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_132_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_133_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_134_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_135_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_136_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_137_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_138_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_139_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_140_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_141_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_142_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_143_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_144_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_145_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_146_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_147_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_148_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_149_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_150_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_151_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_152_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_153_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_154_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_155_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_156_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_157_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_158_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_159_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_160_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_161_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_162_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_163_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_164_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_165_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_166_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_167_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_168_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_169_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_170_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_171_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_172_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_173_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_174_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_175_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_176_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_177_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_178_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_179_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_180_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_181_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_182_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_183_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_184_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_185_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_186_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_187_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_188_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_189_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_190_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_191_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_192_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_193_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_194_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_195_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_196_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_197_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_198_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_199_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_200_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_201_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_202_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_203_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_204_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_205_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_206_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_207_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_208_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_209_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_210_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_211_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_212_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_213_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_214_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_1_BATTERY_215_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	
	RACK_1_BATTERY_000_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_001_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_002_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_003_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_004_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_005_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_006_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_007_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_008_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_009_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_010_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_011_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_012_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_013_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_014_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_015_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_016_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_017_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_018_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_019_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_020_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_021_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_022_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_023_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_024_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_025_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_026_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_027_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_028_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_029_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_030_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_031_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_032_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_033_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_034_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_035_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_036_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_037_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_038_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_039_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_040_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_041_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_042_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_043_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_044_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_045_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_046_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_047_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_048_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_049_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_050_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_051_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_052_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_053_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_054_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_055_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_056_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_057_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_058_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_059_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_060_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_061_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_062_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_063_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_064_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_065_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_066_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_067_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_068_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_069_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_070_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_071_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_072_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_073_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_074_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_075_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_076_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_077_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_078_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_079_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_080_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_081_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_082_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_083_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_084_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_085_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_086_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_087_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_088_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_089_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_090_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_091_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_092_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_093_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_094_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_095_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_096_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_097_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_098_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_099_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_100_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_101_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_102_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_103_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_104_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_105_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_106_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_1_BATTERY_107_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	
	RACK_2_STATE(new Doc().unit(Unit.NONE)), //
	RACK_2_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
	RACK_2_CHARGE_INDICATION(new Doc().options(Enums.ChargeIndication.values())), //
	RACK_2_SOC(new Doc().unit(Unit.PERCENT)), //
	RACK_2_SOH(new Doc().unit(Unit.PERCENT)), //
	RACK_2_MAX_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
	RACK_2_MAX_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_MIN_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
	RACK_2_MIN_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_MAX_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
	RACK_2_MAX_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_MIN_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
	RACK_2_MIN_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Discharge Temperature Low Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Discharge Temperature High Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Charge Temperature Low Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster2 Cell Charge Temperature High Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Discharge Current High Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(
			new Doc().level(Level.WARNING).text("Cluster 2 Total Voltage Low Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 2 Cell Voltage Low Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_CHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Charge Current High Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Total Voltage High Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Voltage High Alarm Level 2")), //
	RACK_2_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Discharge Temperature Low Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Discharge Temperature High Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Total Voltage Diff High Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_INSULATION_LOW(new Doc().level(Level.WARNING).text("Cluster 2 Insulation Low Alarm Level1")), //
	RACK_2_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Voltage Diff High Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster X Cell temperature Diff High Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_SOC_LOW(new Doc().level(Level.WARNING).text("Cluster 2 SOC Low Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Charge Temperature Low Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Charge Temperature High Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Discharge Current High Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(
			new Doc().level(Level.WARNING).text("Cluster 2 Total Voltage Low Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 2 Cell Voltage Low Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_CHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Charge Current High Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Total Voltage High Alarm Level 1")), //
	RACK_2_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 2 Cell Voltage High Alarm Level 1")), //
	RACK_2_RUN_STATE(new Doc().options(ClusterRunState.values())), //
	RACK_2_FAILURE_INITIALIZATION(new Doc().level(Level.FAULT).text("Initialization failure")), //
	RACK_2_FAILURE_EEPROM(new Doc().level(Level.FAULT).text("EEPROM fault")), //
	RACK_2_FAILURE_INTRANET_COMMUNICATION(new Doc().level(Level.FAULT).text("Intranet communication fault")), //
	RACK_2_FAILURE_TEMP_SAMPLING_LINE(new Doc().level(Level.FAULT).text("Temperature sampling line fault")), //
	RACK_2_FAILURE_BALANCING_MODULE(new Doc().level(Level.FAULT).text("Balancing module fault")), //
	RACK_2_FAILURE_TEMP_SENSOR(new Doc().level(Level.FAULT).text("Temperature sensor fault")), //
	RACK_2_FAILURE_TEMP_SAMPLING(new Doc().level(Level.FAULT).text("Temperature sampling fault")), //
	RACK_2_FAILURE_VOLTAGE_SAMPLING(new Doc().level(Level.FAULT).text("Voltage sampling fault")), //
	RACK_2_FAILURE_LTC6803(new Doc().level(Level.FAULT).text("LTC6803 fault")), //
	RACK_2_FAILURE_CONNECTOR_WIRE(new Doc().level(Level.FAULT).text("connector wire fault")), //
	RACK_2_FAILURE_SAMPLING_WIRE(new Doc().level(Level.FAULT).text("sampling wire fault")), //

	RACK_2_BATTERY_000_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_001_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_002_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_003_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_004_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_005_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_006_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_007_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_008_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_009_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_010_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_011_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_012_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_013_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_014_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_015_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_016_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_017_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_018_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_019_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_020_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_021_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_022_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_023_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_024_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_025_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_026_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_027_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_028_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_029_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_030_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_031_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_032_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_033_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_034_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_035_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_036_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_037_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_038_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_039_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_040_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_041_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_042_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_043_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_044_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_045_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_046_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_047_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_048_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_049_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_050_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_051_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_052_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_053_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_054_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_055_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_056_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_057_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_058_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_059_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_060_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_061_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_062_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_063_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_064_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_065_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_066_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_067_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_068_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_069_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_070_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_071_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_072_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_073_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_074_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_075_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_076_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_077_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_078_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_079_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_080_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_081_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_082_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_083_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_084_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_085_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_086_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_087_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_088_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_089_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_090_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_091_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_092_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_093_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_094_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_095_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_096_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_097_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_098_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_099_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_100_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_101_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_102_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_103_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_104_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_105_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_106_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_107_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_108_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_109_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_110_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_111_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_112_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_113_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_114_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_115_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_116_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_117_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_118_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_119_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_120_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_121_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_122_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_123_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_124_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_125_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_126_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_127_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_128_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_129_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_130_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_131_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_132_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_133_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_134_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_135_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_136_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_137_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_138_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_139_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_140_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_141_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_142_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_143_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_144_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_145_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_146_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_147_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_148_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_149_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_150_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_151_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_152_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_153_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_154_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_155_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_156_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_157_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_158_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_159_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_160_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_161_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_162_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_163_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_164_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_165_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_166_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_167_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_168_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_169_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_170_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_171_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_172_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_173_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_174_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_175_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_176_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_177_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_178_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_179_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_180_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_181_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_182_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_183_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_184_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_185_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_186_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_187_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_188_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_189_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_190_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_191_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_192_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_193_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_194_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_195_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_196_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_197_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_198_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_199_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_200_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_201_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_202_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_203_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_204_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_205_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_206_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_207_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_208_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_209_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_210_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_211_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_212_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_213_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_214_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_2_BATTERY_215_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	
	RACK_2_BATTERY_000_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_001_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_002_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_003_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_004_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_005_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_006_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_007_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_008_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_009_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_010_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_011_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_012_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_013_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_014_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_015_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_016_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_017_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_018_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_019_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_020_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_021_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_022_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_023_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_024_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_025_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_026_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_027_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_028_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_029_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_030_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_031_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_032_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_033_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_034_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_035_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_036_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_037_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_038_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_039_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_040_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_041_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_042_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_043_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_044_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_045_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_046_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_047_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_048_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_049_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_050_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_051_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_052_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_053_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_054_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_055_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_056_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_057_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_058_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_059_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_060_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_061_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_062_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_063_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_064_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_065_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_066_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_067_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_068_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_069_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_070_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_071_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_072_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_073_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_074_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_075_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_076_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_077_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_078_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_079_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_080_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_081_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_082_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_083_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_084_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_085_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_086_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_087_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_088_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_089_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_090_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_091_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_092_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_093_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_094_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_095_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_096_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_097_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_098_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_099_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_100_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_101_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_102_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_103_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_104_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_105_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_106_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_2_BATTERY_107_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	
	RACK_3_STATE(new Doc().unit(Unit.NONE)), //
	RACK_3_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_CURRENT(new Doc().unit(Unit.MILLIAMPERE)), //
	RACK_3_CHARGE_INDICATION(new Doc().options(Enums.ChargeIndication.values())), //
	RACK_3_SOC(new Doc().unit(Unit.PERCENT)), //
	RACK_3_SOH(new Doc().unit(Unit.PERCENT)), //
	RACK_3_MAX_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
	RACK_3_MAX_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_MIN_CELL_VOLTAGE_ID(new Doc().unit(Unit.NONE)), //
	RACK_3_MIN_CELL_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_MAX_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
	RACK_3_MAX_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_MIN_CELL_TEMPERATURE_ID(new Doc().unit(Unit.NONE)), //
	RACK_3_MIN_CELL_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_ALARM_LEVEL_2_CELL_DISCHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Discharge Temperature Low Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_CELL_DISCHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Discharge Temperature High Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_CELL_CHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Charge Temperature Low Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_CELL_CHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Charge Temperature High Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_DISCHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Discharge Current High Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_TOTAL_VOLTAGE_LOW(
			new Doc().level(Level.WARNING).text("Cluster 3 Total Voltage Low Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 3 Cell Voltage Low Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_CHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Charge Current High Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_TOTAL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Total Voltage High Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_2_CELL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Voltage High Alarm Level 2")), //
	RACK_3_ALARM_LEVEL_1_CELL_DISCHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Discharge Temperature Low Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_CELL_DISCHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Discharge Temperature High Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_TOTAL_VOLTAGE_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Total Voltage Diff High Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_INSULATION_LOW(new Doc().level(Level.WARNING).text("Cluster 3 Insulation Low Alarm Level1")), //
	RACK_3_ALARM_LEVEL_1_CELL_VOLTAGE_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Voltage Diff High Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_CELL_TEMP_DIFF_HIGH(
			new Doc().level(Level.WARNING).text("Cluster X Cell temperature Diff High Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_SOC_LOW(new Doc().level(Level.WARNING).text("Cluster 3 SOC Low Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_CELL_CHA_TEMP_LOW(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Charge Temperature Low Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_CELL_CHA_TEMP_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Charge Temperature High Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_DISCHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Discharge Current High Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_TOTAL_VOLTAGE_LOW(
			new Doc().level(Level.WARNING).text("Cluster 3 Total Voltage Low Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_CELL_VOLTAGE_LOW(new Doc().level(Level.WARNING).text("Cluster 3 Cell Voltage Low Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_CHA_CURRENT_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Charge Current High Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_TOTAL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Total Voltage High Alarm Level 1")), //
	RACK_3_ALARM_LEVEL_1_CELL_VOLTAGE_HIGH(
			new Doc().level(Level.WARNING).text("Cluster 3 Cell Voltage High Alarm Level 1")), //
	RACK_3_RUN_STATE(new Doc().options(ClusterRunState.values())), //
	RACK_3_FAILURE_INITIALIZATION(new Doc().level(Level.FAULT).text("Initialization failure")), //
	RACK_3_FAILURE_EEPROM(new Doc().level(Level.FAULT).text("EEPROM fault")), //
	RACK_3_FAILURE_INTRANET_COMMUNICATION(new Doc().level(Level.FAULT).text("Intranet communication fault")), //
	RACK_3_FAILURE_TEMP_SAMPLING_LINE(new Doc().level(Level.FAULT).text("Temperature sampling line fault")), //
	RACK_3_FAILURE_BALANCING_MODULE(new Doc().level(Level.FAULT).text("Balancing module fault")), //
	RACK_3_FAILURE_TEMP_SENSOR(new Doc().level(Level.FAULT).text("Temperature sensor fault")), //
	RACK_3_FAILURE_TEMP_SAMPLING(new Doc().level(Level.FAULT).text("Temperature sampling fault")), //
	RACK_3_FAILURE_VOLTAGE_SAMPLING(new Doc().level(Level.FAULT).text("Voltage sampling fault")), //
	RACK_3_FAILURE_LTC6803(new Doc().level(Level.FAULT).text("LTC6803 fault")), //
	RACK_3_FAILURE_CONNECTOR_WIRE(new Doc().level(Level.FAULT).text("connector wire fault")), //
	RACK_3_FAILURE_SAMPLING_WIRE(new Doc().level(Level.FAULT).text("sampling wire fault")), //

	RACK_3_BATTERY_000_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_001_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_002_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_003_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_004_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_005_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_006_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_007_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_008_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_009_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_010_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_011_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_012_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_013_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_014_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_015_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_016_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_017_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_018_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_019_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_020_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_021_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_022_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_023_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_024_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_025_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_026_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_027_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_028_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_029_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_030_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_031_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_032_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_033_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_034_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_035_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_036_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_037_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_038_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_039_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_040_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_041_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_042_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_043_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_044_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_045_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_046_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_047_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_048_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_049_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_050_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_051_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_052_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_053_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_054_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_055_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_056_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_057_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_058_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_059_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_060_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_061_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_062_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_063_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_064_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_065_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_066_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_067_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_068_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_069_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_070_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_071_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_072_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_073_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_074_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_075_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_076_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_077_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_078_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_079_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_080_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_081_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_082_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_083_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_084_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_085_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_086_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_087_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_088_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_089_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_090_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_091_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_092_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_093_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_094_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_095_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_096_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_097_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_098_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_099_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_100_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_101_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_102_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_103_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_104_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_105_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_106_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_107_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_108_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_109_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_110_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_111_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_112_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_113_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_114_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_115_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_116_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_117_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_118_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_119_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_120_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_121_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_122_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_123_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_124_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_125_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_126_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_127_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_128_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_129_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_130_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_131_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_132_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_133_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_134_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_135_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_136_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_137_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_138_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_139_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_140_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_141_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_142_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_143_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_144_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_145_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_146_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_147_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_148_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_149_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_150_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_151_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_152_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_153_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_154_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_155_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_156_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_157_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_158_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_159_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_160_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_161_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_162_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_163_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_164_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_165_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_166_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_167_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_168_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_169_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_170_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_171_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_172_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_173_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_174_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_175_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_176_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_177_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_178_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_179_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_180_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_181_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_182_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_183_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_184_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_185_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_186_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_187_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_188_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_189_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_190_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_191_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_192_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_193_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_194_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_195_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_196_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_197_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_198_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_199_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_200_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_201_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_202_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_203_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_204_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_205_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_206_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_207_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_208_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_209_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_210_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_211_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_212_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_213_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_214_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	RACK_3_BATTERY_215_VOLTAGE(new Doc().unit(Unit.MILLIVOLT)), //
	
	RACK_3_BATTERY_000_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_001_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_002_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_003_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_004_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_005_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_006_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_007_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_008_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_009_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_010_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_011_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_012_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_013_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_014_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_015_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_016_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_017_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_018_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_019_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_020_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_021_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_022_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_023_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_024_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_025_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_026_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_027_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_028_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_029_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_030_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_031_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_032_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_033_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_034_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_035_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_036_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_037_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_038_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_039_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_040_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_041_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_042_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_043_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_044_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_045_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_046_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_047_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_048_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_049_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_050_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_051_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_052_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_053_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_054_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_055_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_056_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_057_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_058_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_059_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_060_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_061_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_062_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_063_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_064_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_065_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_066_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_067_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_068_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_069_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_070_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_071_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_072_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_073_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_074_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_075_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_076_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_077_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_078_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_079_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_080_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_081_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_082_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_083_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_084_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_085_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_086_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_087_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_088_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_089_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_090_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_091_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_092_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_093_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_094_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_095_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_096_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_097_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_098_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_099_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_100_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_101_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_102_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_103_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_104_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_105_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_106_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	RACK_3_BATTERY_107_TEMPERATURE(new Doc().unit(Unit.DEZIDEGREE_CELSIUS)), //
	
	;
	private final Doc doc;

	private MasterChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}

