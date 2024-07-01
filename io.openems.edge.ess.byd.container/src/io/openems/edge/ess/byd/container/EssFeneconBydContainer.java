package io.openems.edge.ess.byd.container;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public interface EssFeneconBydContainer extends ManagedSymmetricEss, SymmetricEss, ModbusComponent, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		READ_ONLY_MODE(Doc.of(Level.INFO)),
		// RTU registers
		SYSTEM_WORKSTATE(Doc.of(SystemWorkstate.values())), //
		SYSTEM_WORKMODE(Doc.of(SystemWorkmode.values())), //
		LIMIT_INDUCTIVE_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		LIMIT_CAPACITIVE_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)),
		CONTAINER_RUN_NUMBER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		SET_SYSTEM_WORKSTATE(Doc.of(SetSystemWorkstate.values())//
				.accessMode(AccessMode.WRITE_ONLY)),
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)//
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)//
				.accessMode(AccessMode.WRITE_ONLY)), //
		// PCS registers
		PCS_SYSTEM_WORKSTATE(Doc.of(SystemWorkstate.values())), //
		PCS_SYSTEM_WORKMODE(Doc.of(SystemWorkmode.values())), //
		PHASE3_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		PHASE3_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)), //
		PHASE3_INSPECTING_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE)), //
		PCS_DISCHARGE_LIMIT_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		PCS_CHARGE_LIMIT_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		POSITIVE_REACTIVE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)), //
		NEGATIVE_REACTIVE_POWER_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOVOLT_AMPERE_REACTIVE)), //
		CURRENT_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		CURRENT_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L12(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L23(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		VOLTAGE_L31(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		SYSTEM_FREQUENCY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.HERTZ)),
		DC_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		DC_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)), //
		DC_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.KILOWATT)), //
		IGBT_TEMPERATURE_L1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		IGBT_TEMPERATURE_L2(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		IGBT_TEMPERATURE_L3(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		// PCS_WARNING_0
		STATE_0(Doc.of(Level.WARNING).text("DC pre-charging contactor checkback abnormal")),
		STATE_1(Doc.of(Level.WARNING).text("AC pre-charging contactor checkback abnormal")),
		STATE_2(Doc.of(Level.WARNING).text("AC main contactor checkback abnormal")),
		STATE_3(Doc.of(Level.WARNING).text("AC circuit breaker checkback abnormal")),
		STATE_4(Doc.of(Level.WARNING).text("Container door open")), //
		STATE_5(Doc.of(Level.WARNING).text("Reserved")),
		STATE_6(Doc.of(Level.WARNING).text("AC circuit breaker is not closed")),
		STATE_7(Doc.of(Level.WARNING).text("Reserved")),
		// PCS_WARNING_1
		STATE_8(Doc.of(Level.WARNING).text("General overload")), //
		STATE_9(Doc.of(Level.WARNING).text("Severe overload")),
		STATE_10(Doc.of(Level.WARNING).text("Over temperature drop power")),
		STATE_11(Doc.of(Level.WARNING).text("AC three-phase current imbalance alarm")),
		STATE_12(Doc.of(Level.WARNING).text("Failed to reset factory settings")),
		STATE_13(Doc.of(Level.WARNING).text("Hardware board invalidation")),
		STATE_14(Doc.of(Level.WARNING).text("Self-test failure alarm")),
		STATE_15(Doc.of(Level.WARNING).text("Receive BMS stop signal")),
		STATE_16(Doc.of(Level.WARNING).text("Air-conditioner")),
		STATE_17(Doc.of(Level.WARNING).text("IGBT Three phase temperature difference is large")),
		STATE_18(Doc.of(Level.WARNING).text("EEPROM Input data overrun")),
		STATE_19(Doc.of(Level.WARNING).text("Back up EEPROM data failure")),
		STATE_20(Doc.of(Level.WARNING).text("DC circuit breaker checkback abnormal")),
		STATE_21(Doc.of(Level.WARNING).text("DC main contactor checkback abnormal")),
		// PCS_WARNING_2
		STATE_22(Doc.of(Level.WARNING).text("Interruption of communication between PCS and Master")),
		STATE_23(Doc.of(Level.WARNING).text("Interruption of communication between PCS and unit controller")),
		STATE_24(Doc.of(Level.WARNING).text("Excessive temperature")),
		STATE_25(Doc.of(Level.WARNING).text("Excessive humidity")),
		STATE_26(Doc.of(Level.WARNING).text("Accept H31 control board signal shutdown")),
		STATE_27(Doc.of(Level.WARNING).text("Radiator A temperature sampling failure")),
		STATE_28(Doc.of(Level.WARNING).text("Radiator B temperature sampling failure")),
		STATE_29(Doc.of(Level.WARNING).text("Radiator C temperature sampling failure")),
		STATE_30(Doc.of(Level.WARNING).text("Reactor temperature sampling failure")),
		STATE_31(Doc.of(Level.WARNING).text("PCS cabinet environmental temperature sampling failure")),
		STATE_32(Doc.of(Level.WARNING).text("DC circuit breaker not engaged")),
		STATE_33(Doc.of(Level.WARNING).text("Controller of receive system shutdown because of abnormal command")),
		// PCS_WARNING_3
		STATE_34(Doc.of(Level.WARNING).text("Interruption of communication between PCS and RTU0 ")),
		STATE_35(Doc.of(Level.WARNING).text("Interruption of communication between PCS and RTU1AN")),
		STATE_36(Doc.of(Level.WARNING).text("Interruption of communication between PCS and MasterCAN")),
		STATE_37(Doc.of(Level.WARNING).text("Short-term access too many times to hot standby status in a short term")),
		STATE_38(Doc.of(Level.WARNING).text("entry and exit dynamic monitoring too many times in a short term")),
		STATE_39(Doc.of(Level.WARNING).text("AC preload contactor delay closure ")),
		// PCS_FAULTS_0
		STATE_40(Doc.of(Level.FAULT).text("DC pre-charge contactor cannot pull in")),
		STATE_41(Doc.of(Level.FAULT).text("AC pre-charge contactor cannot pull in")),
		STATE_42(Doc.of(Level.FAULT).text("AC main contactor cannot pull in")),
		STATE_43(Doc.of(Level.FAULT).text("AC breaker is abnormally disconnected during operation")),
		STATE_44(Doc.of(Level.FAULT).text("AC main contactor disconnected during operation")),
		STATE_45(Doc.of(Level.FAULT).text("AC main contactor cannot be disconnected")),
		STATE_46(Doc.of(Level.FAULT).text("Hardware PDP failure")),
		STATE_47(Doc.of(Level.FAULT).text("DC midpoint 1 high voltage protection")),
		STATE_48(Doc.of(Level.FAULT).text("DC midpoint 2 high voltage protection")),
		// PCS_FAULTS_1
		STATE_49(Doc.of(Level.FAULT).text("Radiator A over-temperature protection")),
		STATE_50(Doc.of(Level.FAULT).text("Radiator B over-temperature protection")),
		STATE_51(Doc.of(Level.FAULT).text("Radiator C over-temperature protection")),
		STATE_52(Doc.of(Level.FAULT).text("Electric reactor core over temperature protection")),
		STATE_53(Doc.of(Level.FAULT).text("DC breaker disconnected abnormally in operation")),
		STATE_54(Doc.of(Level.FAULT).text("DC main contactor disconnected abnormally in operation")),
		// PCS_FAULTS_2
		STATE_55(Doc.of(Level.FAULT).text("DC short-circuit protection")),
		STATE_56(Doc.of(Level.FAULT).text("DC overvoltage protection")),
		STATE_57(Doc.of(Level.FAULT).text("DC undervoltage protection")),
		STATE_58(Doc.of(Level.FAULT).text("DC reverse or missed connection protection")),
		STATE_59(Doc.of(Level.FAULT).text("DC disconnection protection")),
		STATE_60(Doc.of(Level.FAULT).text("DC overcurrent protection")),
		STATE_61(Doc.of(Level.FAULT).text("AC Phase A Peak Protection")),
		STATE_62(Doc.of(Level.FAULT).text("AC Phase B Peak Protection")),
		STATE_63(Doc.of(Level.FAULT).text("AC Phase C Peak Protection")),
		STATE_64(Doc.of(Level.FAULT).text("AC phase A effective value high protection")),
		STATE_65(Doc.of(Level.FAULT).text("AC phase B effective value high protection")),
		STATE_66(Doc.of(Level.FAULT).text("AC phase C effective value high protection")),
		STATE_67(Doc.of(Level.FAULT).text("A-phase voltage sampling Failure")),
		STATE_68(Doc.of(Level.FAULT).text("B-phase voltage sampling Failure")),
		STATE_69(Doc.of(Level.FAULT).text("C-phase voltage sampling Failure")),
		// PCS_FAULTS_3
		STATE_70(Doc.of(Level.FAULT).text("Inverted Phase A Voltage Sampling Failure")),
		STATE_71(Doc.of(Level.FAULT).text("Inverted Phase B Voltage Sampling Failure")),
		STATE_72(Doc.of(Level.FAULT).text("Inverted Phase C Voltage Sampling Failure")),
		STATE_73(Doc.of(Level.FAULT).text("AC current sampling failure")),
		STATE_74(Doc.of(Level.FAULT).text("DC current sampling failure")),
		STATE_75(Doc.of(Level.FAULT).text("Phase A over-temperature protection")),
		STATE_76(Doc.of(Level.FAULT).text("Phase B over-temperature protection")),
		STATE_77(Doc.of(Level.FAULT).text("Phase C over-temperature protection")),
		STATE_78(Doc.of(Level.FAULT).text("A phase temperature sampling failure")),
		STATE_79(Doc.of(Level.FAULT).text("B phase temperature sampling failure")),
		STATE_80(Doc.of(Level.FAULT).text("C phase temperature sampling failure")),
		STATE_81(Doc.of(Level.FAULT).text("AC Phase A not fully pre-charged under-protection")),
		STATE_82(Doc.of(Level.FAULT).text("AC Phase B not fully pre-charged under-protection")),
		STATE_83(Doc.of(Level.FAULT).text("AC Phase C not fully pre-charged under-protection")),
		STATE_84(Doc.of(Level.FAULT).text("Non-adaptable phase sequence error protection")),
		STATE_85(Doc.of(Level.FAULT).text("DSP protection")),
		// PCS_FAULTS_4
		STATE_86(Doc.of(Level.FAULT).text("A-phase grid voltage serious high protection")),
		STATE_87(Doc.of(Level.FAULT).text("A-phase grid voltage general high protection")),
		STATE_88(Doc.of(Level.FAULT).text("B-phase grid voltage serious high protection")),
		STATE_89(Doc.of(Level.FAULT).text("B-phase grid voltage general high protection")),
		STATE_90(Doc.of(Level.FAULT).text("C-phase grid voltage serious high protection")),
		STATE_91(Doc.of(Level.FAULT).text("C-phase grid voltage general high protection")),
		STATE_92(Doc.of(Level.FAULT).text("A-phase grid voltage serious low  protection")),
		STATE_93(Doc.of(Level.FAULT).text("A-phase grid voltage general low protection")),
		STATE_94(Doc.of(Level.FAULT).text("B-phase grid voltage serious low  protection")),
		STATE_95(Doc.of(Level.FAULT).text("B-phase grid voltage general low protection")),
		STATE_96(Doc.of(Level.FAULT).text("C-phase grid voltage serious low  protection")),
		STATE_97(Doc.of(Level.FAULT).text("C-phase grid voltage general low protection")),
		STATE_98(Doc.of(Level.FAULT).text("serious high frequency")),
		STATE_99(Doc.of(Level.FAULT).text("general high frequency")),
		STATE_100(Doc.of(Level.FAULT).text("serious low frequency")),
		STATE_101(Doc.of(Level.FAULT).text("general low frequency")),
		// PCS_FAULTS_5
		STATE_102(Doc.of(Level.FAULT).text("Grid A phase loss")),
		STATE_103(Doc.of(Level.FAULT).text("Grid B phase loss")),
		STATE_104(Doc.of(Level.FAULT).text("Grid C phase loss")),
		STATE_105(Doc.of(Level.FAULT).text("Island protection")),
		STATE_106(Doc.of(Level.FAULT).text("A-phase low voltage ride through")),
		STATE_107(Doc.of(Level.FAULT).text("B-phase low voltage ride through")),
		STATE_108(Doc.of(Level.FAULT).text("C-phase low voltage ride through")),
		STATE_109(Doc.of(Level.FAULT).text("A phase inverter voltage serious high protection")),
		STATE_110(Doc.of(Level.FAULT).text("A phase inverter voltage general high protection")),
		STATE_111(Doc.of(Level.FAULT).text("B phase inverter voltage serious high protection")),
		STATE_112(Doc.of(Level.FAULT).text("B phase inverter voltage general high protection")),
		STATE_113(Doc.of(Level.FAULT).text("C phase inverter voltage serious high protection")),
		STATE_114(Doc.of(Level.FAULT).text("C phase inverter voltage general high protection")),
		STATE_115(Doc.of(Level.FAULT).text("Inverter peak voltage high protection cause by AC disconnection")),
		// BECU registers
		BATTERY_STRING_WORK_STATE(Doc.of(BatteryStringWorkState.values())),
		BATTERY_STRING_TOTAL_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)), //
		BATTERY_STRING_CURRENT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),
		BATTERY_STRING_SOC(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_AVERAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MAX_STRING_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)),
		BATTERY_STRING_MAX_VOLTAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MIN_STRING_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)),
		BATTERY_STRING_MIN_VOLTAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_NUMBER_MAX_STRING_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_STRING_MAX_TEMPERATURE_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)),
		BATTERY_NUMBER_MIN_STRING_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		BATTERY_STRING_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)),
		BATTERY_STRING_MIN_TEMPERATURE_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT)),
		BATTERY_STRING_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),
		BATTERY_STRING_DISCHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.AMPERE)),
		// BATTERY_STRING_WARNING_0_0
		STATE_116(Doc.of(Level.WARNING).text("Charging overcurrent general alarm")), //
		STATE_117(Doc.of(Level.WARNING).text("discharging overcurrent general alarm")), //
		STATE_118(Doc.of(Level.WARNING).text("Charge current over-limit alarm")), //
		STATE_119(Doc.of(Level.WARNING).text("discharge current over-limit alarm")), //
		STATE_120(Doc.of(Level.WARNING).text("General high voltage alarm")), //
		STATE_121(Doc.of(Level.WARNING).text("General low voltage alarm")), //
		STATE_122(Doc.of(Level.WARNING).text("Abnormal voltage change alarm")), //
		STATE_123(Doc.of(Level.WARNING).text("General high temperature alarm")), //
		STATE_124(Doc.of(Level.WARNING).text("General low temperature alarm")), //
		STATE_125(Doc.of(Level.WARNING).text("Abnormal temperature change alarm")), //
		STATE_126(Doc.of(Level.WARNING).text("Severe high voltage alarm")), //
		STATE_127(Doc.of(Level.WARNING).text("Severe low voltage alarm")), //
		STATE_128(Doc.of(Level.WARNING).text("Severe low temperature alarm")), //
		STATE_129(Doc.of(Level.WARNING).text("Charge current severe over-limit alarm")), //
		STATE_130(Doc.of(Level.WARNING).text("Discharge current severe over-limit alarm")), //
		STATE_131(Doc.of(Level.WARNING).text("Total voltage over limit alarm")), //
		// BATTERY_STRING_WARNING_0_1
		STATE_132(Doc.of(Level.WARNING).text("Balanced sampling abnormal alarm")), //
		STATE_133(Doc.of(Level.WARNING).text("Balanced control abnormal alarm")), //
		STATE_134(Doc.of(Level.WARNING).text("Isolation switch is not closed")), //
		STATE_135(Doc.of(Level.WARNING).text("Pre-charge current abnormal")), //
		STATE_136(Doc.of(Level.WARNING).text("Disconnected contactor current is not safe")), //
		STATE_137(Doc.of(Level.WARNING).text("Value of the current limit reduce")), //
		STATE_138(Doc.of(Level.WARNING).text("Isolation Switch Checkback Abnormal")), //
		STATE_139(Doc.of(Level.WARNING).text("Over temperature drop power")), //
		STATE_140(Doc.of(Level.WARNING).text("Pulse charge approaching maximum load time")), //
		STATE_141(Doc.of(Level.WARNING).text("Pulse charge timeout alarm")), //
		STATE_142(Doc.of(Level.WARNING).text("Pulse discharge approaching maximum load time")), //
		STATE_143(Doc.of(Level.WARNING).text("Pulse discharge timeout alarm")), //
		STATE_144(Doc.of(Level.WARNING).text("Battery string undervoltage")), //
		STATE_145(Doc.of(Level.WARNING).text("High voltage offset")), //
		STATE_146(Doc.of(Level.WARNING).text("Low pressure offset")), //
		STATE_147(Doc.of(Level.WARNING).text("High temperature offset")), //
		// BATTERY_STRING_WARNING_1_0
		STATE_148(Doc.of(Level.FAULT).text("Start timeout")), //
		STATE_149(Doc.of(Level.FAULT).text("Total operating voltage sampling abnormal")), //
		STATE_150(Doc.of(Level.FAULT).text("BMU Sampling circuit abnormal")), //
		STATE_151(Doc.of(Level.FAULT).text("Stop total voltage sampling abnormal")), //
		STATE_152(Doc.of(Level.FAULT).text("voltage sampling line open")), //
		STATE_153(Doc.of(Level.FAULT).text("Temperature sample line open")), //
		STATE_154(Doc.of(Level.FAULT).text("Main-auxiliary internal CAN open")), //
		STATE_155(Doc.of(Level.FAULT).text("Interruption with system controller communication")), //
		// BATTERY_STRING_WARNING_1_1
		STATE_156(Doc.of(Level.FAULT).text("Severe high temperature failure")), //
		STATE_157(Doc.of(Level.FAULT).text("Smoke alarm")), //
		STATE_158(Doc.of(Level.FAULT).text("Fuse failure")), //
		STATE_159(Doc.of(Level.FAULT).text("General leakage")), //
		STATE_160(Doc.of(Level.FAULT).text("Severe leakage")), //
		STATE_161(Doc.of(Level.FAULT).text("Repair switch disconnected")), //
		STATE_162(Doc.of(Level.FAULT).text("Emergency stop pressed down")), //
		// ADAS register addresses
		CONTAINER_IMMERSION_STATE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		CONTAINER_FIRE_STATUS(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		CONTROL_CABINET_STATE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		CONTAINER_GROUNDING_FAULT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		CONTAINER_DOOR_STATUS_0(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)), //
		CONTAINER_DOOR_STATUS_1(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.NONE)),
		// ADAS_WARNING_0_0
		STATE_163(Doc.of(Level.WARNING).text("ups1 Power down")), //
		STATE_164(Doc.of(Level.WARNING).text("Immersion sensor abnormal")), //
		STATE_165(Doc.of(Level.WARNING).text("switch 2 (battery room door) abnormal")), //
		STATE_166(Doc.of(Level.WARNING).text("switch 1 (PCS room door) abnormal")), //
		STATE_167(Doc.of(Level.WARNING).text("Firefighting fault")), //
		STATE_168(Doc.of(Level.WARNING).text("Lightning arrester abnormal")), //
		STATE_169(Doc.of(Level.WARNING).text("Fire alarm")), //
		STATE_170(Doc.of(Level.WARNING).text("Fire detector works")), //
		STATE_171(Doc.of(Level.WARNING).text("pcs1 Ground fault alarm signal")), //
		STATE_172(Doc.of(Level.WARNING).text("Integrated fire extinguishing")), //
		STATE_173(Doc.of(Level.WARNING).text("Emergency stop signal")), //
		STATE_174(Doc.of(Level.WARNING).text("Air conditioning fault contactor signal")), //
		STATE_175(Doc.of(Level.WARNING).text("pcs1 Ground fault shutdown signal")), //
		// ADAS_WARNING_0_1
		STATE_176(Doc.of(Level.WARNING).text("PCS room ambient temperature sensor failure")), //
		STATE_177(Doc.of(Level.WARNING).text("Battery room ambient temperature sensor failure")), //
		STATE_178(Doc.of(Level.WARNING).text("container external ambient temperature sensor failure")), //
		STATE_179(Doc.of(Level.WARNING).text("The temperature sensor on the top of the control cabinet failed")), //
		STATE_180(Doc.of(Level.WARNING).text("PCS room ambient humidity sensor failure")), //
		STATE_181(Doc.of(Level.WARNING).text("Battery room ambient humidity sensor failure")), //
		STATE_182(Doc.of(Level.WARNING).text("container external humidity sensor failure")), //
		STATE_183(Doc.of(Level.WARNING).text("SD card failure")), //
		STATE_184(Doc.of(Level.WARNING).text("PCS room ambient humidity alarm")), //
		STATE_185(Doc.of(Level.WARNING).text("battery room ambient humidity alarm")), //
		STATE_186(Doc.of(Level.WARNING).text("container external humidity alarm")), //
		// ADAS_WARNING_0_2
		STATE_187(Doc.of(Level.WARNING).text("Master Firefighting fault")), //
		STATE_188(Doc.of(Level.WARNING).text("Harmonic protection")), //
		STATE_189(Doc.of(Level.WARNING).text("Battery emergency stop")), //
		// ADAS_WARNING_1_0
		STATE_190(Doc.of(Level.FAULT).text("Reserved")), //
		STATE_203(Doc.of(Level.FAULT).text("Reserved")), //
		// ADAS_WARNING_1_1
		STATE_191(Doc.of(Level.FAULT).text("Serious overheating of ambient temperature in PCS room")), //
		STATE_192(Doc.of(Level.FAULT).text("Serious overheating of ambient temperature in Battery room")), //
		STATE_193(Doc.of(Level.FAULT).text("Serious overheating of ambient temperature outside the container")), //
		STATE_194(Doc.of(Level.FAULT).text("Serious overheating on the top of the electric control cabinet ")), //
		STATE_195(Doc.of(Level.FAULT).text("Serious overheating of transformer")), //
		// ADAS_WARNING_1_2
		STATE_196(Doc.of(Level.FAULT).text("DCAC module 1 Communication disconnected")), //
		STATE_197(Doc.of(Level.FAULT).text("DCAC module 2 Communication disconnected")), //
		STATE_198(Doc.of(Level.FAULT).text("DCAC module 3 Communication disconnected")), //
		STATE_199(Doc.of(Level.FAULT).text("DCAC module 4 Communication disconnected")), //
		STATE_200(Doc.of(Level.FAULT).text("BECU1 Communication disconnected")), //
		STATE_201(Doc.of(Level.FAULT).text("BECU2 Communication disconnected")), //
		STATE_202(Doc.of(Level.FAULT).text("BECU3 Communication disconnected")); //

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
