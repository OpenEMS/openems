package io.openems.edge.ess.fenecon.commercial40;

import java.util.Optional;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.fenecon.commercial40.charger.EssDcChargerFeneconCommercial40;
import io.openems.edge.timedata.api.TimedataProvider;

public interface EssFeneconCommercial40
		extends ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	public Integer getUnitId();

	public String getModbusBridgeId();

	public void addCharger(EssDcChargerFeneconCommercial40 charger);

	public void removeCharger(EssDcChargerFeneconCommercial40 charger);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		SYSTEM_STATE(Doc.of(SystemState.values())), //
		CONTROL_MODE(Doc.of(ControlMode.values())), //
		BATTERY_MAINTENANCE_STATE(Doc.of(BatteryMaintenanceState.values())), //
		INVERTER_STATE(Doc.of(InverterState.values())), //
		SYSTEM_MANUFACTURER(Doc.of(SystemManufacturer.values())), //
		SYSTEM_TYPE(Doc.of(SystemType.values())), //
		BATTERY_STRING_SWITCH_STATE(Doc.of(BatteryStringSwitchState.values())), //
		BMS_DCDC_WORK_STATE(Doc.of(BmsDcdcWorkState.values())), //
		BMS_DCDC_WORK_MODE(Doc.of(BmsDcdcWorkMode.values())), //

		// EnumWriteChannels
		SET_WORK_STATE(Doc.of(SetWorkState.values()) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// IntegerWriteChannel
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.accessMode(AccessMode.WRITE_ONLY)), //

		// LongReadChannel
		ORIGINAL_ACTIVE_CHARGE_ENERGY(Doc.of(OpenemsType.LONG)), //
		ORIGINAL_ACTIVE_DISCHARGE_ENERGY(Doc.of(OpenemsType.LONG)), //

		// IntegerReadChannels
		ORIGINAL_ALLOWED_CHARGE_POWER(new IntegerDoc() //
				.onInit(channel -> { //
					// on each Update to the channel -> set the ALLOWED_CHARGE_POWER value with a
					// delta of max 500
					channel.onUpdate(newValue -> {
						IntegerReadChannel currentValueChannel = channel.getComponent()
								.channel(ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER);
						Optional<Integer> originalValue = newValue.asOptional();
						Optional<Integer> currentValue = currentValueChannel.value().asOptional();
						int value;
						if (!originalValue.isPresent() && !currentValue.isPresent()) {
							value = 0;
						} else if (originalValue.isPresent() && !currentValue.isPresent()) {
							value = originalValue.get();
						} else if (!originalValue.isPresent() && currentValue.isPresent()) {
							value = currentValue.get();
						} else {
							value = Math.max(originalValue.get(), currentValue.get() - 500);
						}
						currentValueChannel.setNextValue(value);
					});
				})), //
		ORIGINAL_ALLOWED_DISCHARGE_POWER(new IntegerDoc() //
				.onInit(channel -> { //
					// on each Update to the channel -> set the ALLOWED_DISCHARGE_POWER value with a
					// delta of max 500
					channel.onUpdate(newValue -> {
						IntegerReadChannel currentValueChannel = channel.getComponent()
								.channel(ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER);
						Optional<Integer> originalValue = newValue.asOptional();
						Optional<Integer> currentValue = currentValueChannel.value().asOptional();
						int value;
						if (!originalValue.isPresent() && !currentValue.isPresent()) {
							value = 0;
						} else if (originalValue.isPresent() && !currentValue.isPresent()) {
							value = originalValue.get();
						} else if (!originalValue.isPresent() && currentValue.isPresent()) {
							value = currentValue.get();
						} else {
							value = Math.min(originalValue.get(), currentValue.get() + 500);
						}
						currentValueChannel.setNextValue(value);
					});
				})), //
		PROTOCOL_VERSION(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BATTERY_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		AC_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		AC_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		GRID_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE)), //
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ)), //
		INVERTER_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		INVERTER_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		INVERTER_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		INVERTER_CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		INVERTER_CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		INVERTER_CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		IPM_TEMPERATURE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		IPM_TEMPERATURE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		IPM_TEMPERATURE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		TRANSFORMER_TEMPERATURE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_STRING_TOTAL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_STRING_TOTAL_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BATTERY_STRING_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)), //
		BATTERY_STRING_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_STRING_CHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BATTERY_STRING_DISCHARGE_CURRENT_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		BATTERY_STRING_CYCLES(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_STRING_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		BATTERY_STRING_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		BATTERY_STRING_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		BATTERY_STRING_MAX_CELL_VOLTAGE_NO(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_STRING_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_STRING_MAX_CELL_VOLTAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_STRING_MIN_CELL_VOLTAGE_NO(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_STRING_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_STRING_MIN_CELL_VOLTAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_STRING_MAX_CELL_TEMPERATURE_NO(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_STRING_MAX_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_STRING_MAX_CELL_TEMPERATURE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		BATTERY_STRING_MIN_CELL_TEMPERATURE_NO(Doc.of(OpenemsType.INTEGER)), //
		BATTERY_STRING_MIN_CELL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS)), //
		BATTERY_STRING_MIN_CELL_TEMPERATURE_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_2_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_3_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_4_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_5_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_6_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_7_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_8_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_9_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_10_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_11_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_12_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_13_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_14_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_15_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_16_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_17_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_18_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_19_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_20_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_21_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_22_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_23_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_24_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_25_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_26_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_27_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_28_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_29_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_30_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_31_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_32_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_33_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_34_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_35_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_36_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_37_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_38_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_39_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_40_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_41_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_42_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_43_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_44_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_45_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_46_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_47_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_48_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_49_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_50_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_51_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_52_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_53_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_54_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_55_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_56_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_57_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_58_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_59_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_60_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_61_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_62_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_63_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_64_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_65_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_66_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_67_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_68_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_69_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_70_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_71_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_72_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_73_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_74_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_75_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_76_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_77_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_78_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_79_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_80_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_81_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_82_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_83_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_84_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_85_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_86_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_87_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_88_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_89_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_90_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_91_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_92_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_93_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_94_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_95_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_96_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_97_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_98_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_99_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_100_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_101_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_102_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_103_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_104_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_105_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_106_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_107_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_108_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_109_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_110_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_111_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_112_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_113_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_114_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_115_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_116_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_117_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_118_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_119_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_120_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_121_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_122_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_123_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_124_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_125_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_126_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_127_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_128_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_129_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_130_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_131_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_132_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_133_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_134_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_135_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_136_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_137_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_138_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_139_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_140_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_141_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_142_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_143_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_144_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_145_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_146_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_147_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_148_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_149_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_150_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_151_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_152_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_153_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_154_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_155_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_156_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_157_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_158_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_159_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_160_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_161_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_162_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_163_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_164_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_165_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_166_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_167_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_168_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_169_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_170_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_171_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_172_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_173_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_174_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_175_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_176_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_177_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_178_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_179_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_180_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_181_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_182_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_183_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_184_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_185_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_186_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_187_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_188_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_189_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_190_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_191_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_192_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_193_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_194_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_195_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_196_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_197_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_198_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_199_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_200_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_201_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_202_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_203_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_204_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_205_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_206_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_207_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_208_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_209_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_210_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_211_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_212_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_213_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_214_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_215_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_216_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_217_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_218_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_219_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_220_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_221_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_222_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_223_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		CELL_224_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //

		// StateChannels
		STATE_0(Doc.of(Level.INFO) //
				.text("Emergency Stop")), //
		STATE_1(Doc.of(Level.INFO) //
				.text("Key Manual Stop")), //
		STATE_2(Doc.of(Level.INFO) //
				.text("Transformer Phase B Temperature Sensor Invalidation")), //
		STATE_3(Doc.of(Level.INFO) //
				.text("SD Memory Card Invalidation")), //
		STATE_4(Doc.of(Level.INFO) //
				.text("Inverter Communication Abnormity")), //
		STATE_5(Doc.of(Level.INFO) //
				.text("Battery Stack Communication Abnormity")), //
		STATE_6(Doc.of(Level.INFO) //
				.text("Multifunctional Ammeter Communication Abnormity")), //
		STATE_7(Doc.of(Level.INFO) //
				.text("Remote Communication Abnormity")), //
		STATE_8(Doc.of(Level.INFO) //
				.text("PVDC1 Communication Abnormity")), //
		STATE_9(Doc.of(Level.INFO) //
				.text("PVDC2 Communication Abnormity")), //
		STATE_10(Doc.of(Level.INFO) //
				.text("Transformer Severe Overtemperature")), //
		STATE_11(Doc.of(Level.INFO) //
				.text("DC Precharge Contactor Close Unsuccessfully")), //
		STATE_12(Doc.of(Level.INFO) //
				.text("AC Precharge Contactor Close Unsuccessfully")), //
		STATE_13(Doc.of(Level.INFO) //
				.text("AC Main Contactor Close Unsuccessfully")), //
		STATE_14(Doc.of(Level.INFO) //
				.text("DC Electrical Breaker1 Close Unsuccessfully")), //
		STATE_15(Doc.of(Level.INFO) //
				.text("DC Main Contactor Close Unsuccessfully")), //
		STATE_16(Doc.of(Level.INFO) //
				.text("AC Breaker Trip")), //
		STATE_17(Doc.of(Level.INFO) //
				.text("AC Main Contactor Open When Running")), //
		STATE_18(Doc.of(Level.INFO) //
				.text("DC Main Contactor Open When Running")), //
		STATE_19(Doc.of(Level.INFO) //
				.text("AC Main Contactor Open Unsuccessfully")), //
		STATE_20(Doc.of(Level.INFO) //
				.text("DC Electrical Breaker1 Open Unsuccessfully")), //
		STATE_21(Doc.of(Level.INFO) //
				.text("DC Main Contactor Open Unsuccessfully")), //
		STATE_22(Doc.of(Level.INFO) //
				.text("Hardware PDP Fault")), //
		STATE_23(Doc.of(Level.INFO) //
				.text("Master Stop Suddenly")), //
		STATE_24(Doc.of(Level.INFO) //
				.text("DCShortCircuitProtection")), //
		STATE_25(Doc.of(Level.INFO) //
				.text("DCOvervoltageProtection")), //
		STATE_26(Doc.of(Level.INFO) //
				.text("DCUndervoltageProtection")), //
		STATE_27(Doc.of(Level.INFO) //
				.text("DCInverseNoConnectionProtection")), //
		STATE_28(Doc.of(Level.INFO) //
				.text("DCDisconnectionProtection")), //
		STATE_29(Doc.of(Level.INFO) //
				.text("CommutingVoltageAbnormityProtection")), //
		STATE_30(Doc.of(Level.INFO) //
				.text("DCOvercurrentProtection")), //
		STATE_31(Doc.of(Level.INFO) //
				.text("Phase1PeakCurrentOverLimitProtection")), //
		STATE_32(Doc.of(Level.INFO) //
				.text("Phase2PeakCurrentOverLimitProtection")), //
		STATE_33(Doc.of(Level.INFO) //
				.text("Phase3PeakCurrentOverLimitProtection")), //
		STATE_34(Doc.of(Level.INFO) //
				.text("Phase1GridVoltageSamplingInvalidation")), //
		STATE_35(Doc.of(Level.INFO) //
				.text("Phase2VirtualCurrentOverLimitProtection")), //
		STATE_36(Doc.of(Level.INFO) //
				.text("Phase3VirtualCurrentOverLimitProtection")), //
		STATE_37(Doc.of(Level.INFO) //
				.text("Phase1GridVoltageSamplingInvalidation2")), //
		STATE_38(Doc.of(Level.INFO) //
				.text("Phase2ridVoltageSamplingInvalidation")), //
		STATE_39(Doc.of(Level.INFO) //
				.text("Phase3GridVoltageSamplingInvalidation")), //
		STATE_40(Doc.of(Level.INFO) //
				.text("Phase1InvertVoltageSamplingInvalidation")), //
		STATE_41(Doc.of(Level.INFO) //
				.text("Phase2InvertVoltageSamplingInvalidation")), //
		STATE_42(Doc.of(Level.INFO) //
				.text("Phase3InvertVoltageSamplingInvalidation")), //
		STATE_43(Doc.of(Level.INFO) //
				.text("ACCurrentSamplingInvalidation")), //
		STATE_44(Doc.of(Level.INFO) //
				.text("DCCurrentSamplingInvalidation")), //
		STATE_45(Doc.of(Level.INFO) //
				.text("Phase1OvertemperatureProtection")), //
		STATE_46(Doc.of(Level.INFO) //
				.text("Phase2OvertemperatureProtection")), //
		STATE_47(Doc.of(Level.INFO) //
				.text("Phase3OvertemperatureProtection")), //
		STATE_48(Doc.of(Level.INFO) //
				.text("Phase1TemperatureSamplingInvalidation")), //
		STATE_49(Doc.of(Level.INFO) //
				.text("Phase2TemperatureSamplingInvalidation")), //
		STATE_50(Doc.of(Level.INFO) //
				.text("Phase3TemperatureSamplingInvalidation")), //
		STATE_51(Doc.of(Level.INFO) //
				.text("Phase1PrechargeUnmetProtection")), //
		STATE_52(Doc.of(Level.INFO) //
				.text("Phase2PrechargeUnmetProtection")), //
		STATE_53(Doc.of(Level.INFO) //
				.text("Phase3PrechargeUnmetProtection")), //
		STATE_54(Doc.of(Level.INFO) //
				.text("UnadaptablePhaseSequenceErrorProtection")), //
		STATE_55(Doc.of(Level.INFO) //
				.text("DSPProtection")), //
		STATE_56(Doc.of(Level.INFO) //
				.text("Phase1GridVoltageSevereOvervoltageProtection")), //
		STATE_57(Doc.of(Level.INFO) //
				.text("Phase1GridVoltageGeneralOvervoltageProtection")), //
		STATE_58(Doc.of(Level.INFO) //
				.text("Phase2GridVoltageSevereOvervoltageProtection")), //
		STATE_59(Doc.of(Level.INFO) //
				.text("Phase2GridVoltageGeneralOvervoltageProtection")), //
		STATE_60(Doc.of(Level.INFO) //
				.text("Phase3GridVoltageSevereOvervoltageProtection")), //
		STATE_61(Doc.of(Level.INFO) //
				.text("Phase3GridVoltageGeneralOvervoltageProtection")), //
		STATE_62(Doc.of(Level.INFO) //
				.text("Phase1GridVoltageSevereUndervoltageProtection")), //
		STATE_63(Doc.of(Level.INFO) //
				.text("Phase1GridVoltageGeneralUndervoltageProtection")), //
		STATE_64(Doc.of(Level.INFO) //
				.text("Phase2GridVoltageSevereUndervoltageProtection")), //
		STATE_65(Doc.of(Level.INFO) //
				.text("Phase2GridVoltageGeneralUndervoltageProtection")), //
		STATE_66(Doc.of(Level.INFO) //
				.text("Phase3GridVoltageSevereUndervoltageProtection")), //
		STATE_67(Doc.of(Level.INFO) //
				.text("Phase3GridVoltageGeneralUndervoltageProtection")), //
		STATE_68(Doc.of(Level.INFO) //
				.text("SevereOverfrequncyProtection")), //
		STATE_69(Doc.of(Level.INFO) //
				.text("GeneralOverfrequncyProtection")), //
		STATE_70(Doc.of(Level.INFO) //
				.text("SevereUnderfrequncyProtection")), //
		STATE_71(Doc.of(Level.INFO) //
				.text("GeneralsUnderfrequncyProtection")), //
		STATE_72(Doc.of(Level.INFO) //
				.text("Phase1Gridloss")), //
		STATE_73(Doc.of(Level.INFO) //
				.text("Phase2Gridloss")), //
		STATE_74(Doc.of(Level.INFO) //
				.text("Phase3Gridloss")), //
		STATE_75(Doc.of(Level.INFO) //
				.text("IslandingProtection")), //
		STATE_76(Doc.of(Level.INFO) //
				.text("Phase1UnderVoltageRideThrough")), //
		STATE_77(Doc.of(Level.INFO) //
				.text("Phase2UnderVoltageRideThrough")), //
		STATE_78(Doc.of(Level.INFO) //
				.text("Phase3UnderVoltageRideThrough")), //
		STATE_79(Doc.of(Level.INFO) //
				.text("Phase1InverterVoltageSevereOvervoltageProtection")), //
		STATE_80(Doc.of(Level.INFO) //
				.text("Phase1InverterVoltageGeneralOvervoltageProtection")), //
		STATE_81(Doc.of(Level.INFO) //
				.text("Phase2InverterVoltageSevereOvervoltageProtection")), //
		STATE_82(Doc.of(Level.INFO) //
				.text("Phase2InverterVoltageGeneralOvervoltageProtection")), //
		STATE_83(Doc.of(Level.INFO) //
				.text("Phase3InverterVoltageSevereOvervoltageProtection")), //
		STATE_84(Doc.of(Level.INFO) //
				.text("Phase3InverterVoltageGeneralOvervoltageProtection")), //
		STATE_85(Doc.of(Level.INFO) //
				.text("InverterPeakVoltageHighProtectionCauseByACDisconnect")), //
		STATE_86(Doc.of(Level.INFO) //
				.text("DCPrechargeContactorInspectionAbnormity")), //
		STATE_87(Doc.of(Level.INFO) //
				.text("DCBreaker1InspectionAbnormity")), //
		STATE_88(Doc.of(Level.INFO) //
				.text("DCBreaker2InspectionAbnormity")), //
		STATE_89(Doc.of(Level.INFO) //
				.text("ACPrechargeContactorInspectionAbnormity")), //
		STATE_90(Doc.of(Level.INFO) //
				.text("ACMainontactorInspectionAbnormity")), //
		STATE_91(Doc.of(Level.INFO) //
				.text("ACBreakerInspectionAbnormity")), //
		STATE_92(Doc.of(Level.INFO) //
				.text("DCBreaker1CloseUnsuccessfully")), //
		STATE_93(Doc.of(Level.INFO) //
				.text("DCBreaker2CloseUnsuccessfully")), //
		STATE_94(Doc.of(Level.INFO) //
				.text("ControlSignalCloseAbnormallyInspectedBySystem")), //
		STATE_95(Doc.of(Level.INFO) //
				.text("ControlSignalOpenAbnormallyInspectedBySystem")), //
		STATE_96(Doc.of(Level.INFO) //
				.text("NeutralWireContactorCloseUnsuccessfully")), //
		STATE_97(Doc.of(Level.INFO) //
				.text("NeutralWireContactorOpenUnsuccessfully")), //
		STATE_98(Doc.of(Level.INFO) //
				.text("WorkDoorOpen")), //
		STATE_99(Doc.of(Level.INFO) //
				.text("Emergency1Stop")), //
		STATE_100(Doc.of(Level.INFO) //
				.text("ACBreakerCloseUnsuccessfully")), //
		STATE_101(Doc.of(Level.INFO) //
				.text("ControlSwitchStop")), //
		STATE_102(Doc.of(Level.INFO) //
				.text("GeneralOverload")), //
		STATE_103(Doc.of(Level.INFO) //
				.text("SevereOverload")), //
		STATE_104(Doc.of(Level.INFO) //
				.text("BatteryCurrentOverLimit")), //
		STATE_105(Doc.of(Level.INFO) //
				.debounce(5 * 60 /* 5 minutes with a cycle time of 1 sec */,
						Debounce.FALSE_VALUES_IN_A_ROW_TO_SET_FALSE)
				.text("PowerDecreaseCausedByOvertemperature")), //
		STATE_106(Doc.of(Level.INFO) //
				.text("InverterGeneralOvertemperature")), //
		STATE_107(Doc.of(Level.INFO) //
				.text("ACThreePhaseCurrentUnbalance")), //
		STATE_108(Doc.of(Level.INFO) //
				.text("RestoreFactorySettingUnsuccessfully")), //
		STATE_109(Doc.of(Level.INFO) //
				.text("PoleBoardInvalidation")), //
		STATE_110(Doc.of(Level.INFO) //
				.text("SelfInspectionFailed")), //
		STATE_111(Doc.of(Level.INFO) //
				.text("ReceiveBMSFaultAndStop")), //
		STATE_112(Doc.of(Level.INFO) //
				.text("RefrigerationEquipmentinvalidation")), //
		STATE_113(Doc.of(Level.INFO) //
				.text("LargeTemperatureDifferenceAmongIGBTThreePhases")), //
		STATE_114(Doc.of(Level.INFO) //
				.text("EEPROMParametersOverRange")), //
		STATE_115(Doc.of(Level.INFO) //
				.text("EEPROMParametersBackupFailed")), //
		STATE_116(Doc.of(Level.INFO) //
				.text("DCBreakerCloseunsuccessfully")), //
		STATE_117(Doc.of(Level.INFO) //
				.text("CommunicationBetweenInverterAndBSMUDisconnected")), //
		STATE_118(Doc.of(Level.INFO) //
				.text("CommunicationBetweenInverterAndMasterDisconnected")), //
		STATE_119(Doc.of(Level.INFO) //
				.text("CommunicationBetweenInverterAndUCDisconnected")), //
		STATE_120(Doc.of(Level.INFO) //
				.text("BMSStartOvertimeControlledByPCS")), //
		STATE_121(Doc.of(Level.INFO) //
				.text("BMSStopOvertimeControlledByPCS")), //
		STATE_122(Doc.of(Level.INFO) //
				.text("SyncSignalInvalidation")), //
		STATE_123(Doc.of(Level.INFO) //
				.text("SyncSignalContinuousCaputureFault")), //
		STATE_124(Doc.of(Level.INFO) //
				.text("SyncSignalSeveralTimesCaputureFault")), //
		STATE_125(Doc.of(Level.INFO) //
				.text("CurrentSamplingChannelAbnormityOnHighVoltageSide")), //
		STATE_126(Doc.of(Level.INFO) //
				.text("CurrentSamplingChannelAbnormityOnLowVoltageSide")), //
		STATE_127(Doc.of(Level.INFO) //
				.text("EEPROMParametersOverRange")), //
		STATE_128(Doc.of(Level.INFO) //
				.text("UpdateEEPROMFailed")), //
		STATE_129(Doc.of(Level.INFO) //
				.text("ReadEEPROMFailed")), //
		STATE_130(Doc.of(Level.INFO) //
				.text("CurrentSamplingChannelAbnormityBeforeInductance")), //
		STATE_131(Doc.of(Level.INFO) //
				.text("ReactorPowerDecreaseCausedByOvertemperature")), //
		STATE_132(Doc.of(Level.INFO) //
				.text("IGBTPowerDecreaseCausedByOvertemperature")), //
		STATE_133(Doc.of(Level.INFO) //
				.text("TemperatureChanel3PowerDecreaseCausedByOvertemperature")), //
		STATE_134(Doc.of(Level.INFO) //
				.text("TemperatureChanel4PowerDecreaseCausedByOvertemperature")), //
		STATE_135(Doc.of(Level.INFO) //
				.text("TemperatureChanel5PowerDecreaseCausedByOvertemperature")), //
		STATE_136(Doc.of(Level.INFO) //
				.text("TemperatureChanel6PowerDecreaseCausedByOvertemperature")), //
		STATE_137(Doc.of(Level.INFO) //
				.text("TemperatureChanel7PowerDecreaseCausedByOvertemperature")), //
		STATE_138(Doc.of(Level.INFO) //
				.text("TemperatureChanel8PowerDecreaseCausedByOvertemperature")), //
		STATE_139(Doc.of(Level.INFO) //
				.text("Fan1StopFailed")), //
		STATE_140(Doc.of(Level.INFO) //
				.text("Fan2StopFailed")), //
		STATE_141(Doc.of(Level.INFO) //
				.text("Fan3StopFailed")), //
		STATE_142(Doc.of(Level.INFO) //
				.text("Fan4StopFailed")), //
		STATE_143(Doc.of(Level.INFO) //
				.text("Fan1StartupFailed")), //
		STATE_144(Doc.of(Level.INFO) //
				.text("Fan2StartupFailed")), //
		STATE_145(Doc.of(Level.INFO) //
				.text("Fan3StartupFailed")), //
		STATE_146(Doc.of(Level.INFO) //
				.text("Fan4StartupFailed")), //
		STATE_147(Doc.of(Level.INFO) //
				.text("HighVoltageSideOvervoltage")), //
		STATE_148(Doc.of(Level.INFO) //
				.text("HighVoltageSideUndervoltage")), //
		STATE_149(Doc.of(Level.INFO) //
				.text("HighVoltageSideVoltageChangeUnconventionally")); //

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
