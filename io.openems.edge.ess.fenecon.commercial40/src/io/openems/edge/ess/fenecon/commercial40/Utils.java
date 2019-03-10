package io.openems.edge.ess.fenecon.commercial40;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssFeneconCommercial40 c) {
		List<AbstractReadChannel<?>> result = new ArrayList<>();
		for (io.openems.edge.common.component.OpenemsComponent.ChannelId channelId : OpenemsComponent.ChannelId
				.values()) {
			switch (channelId) {
			case STATE:
				result.add(new StateCollectorChannel(c, channelId));
				break;
			}
		}
		for (io.openems.edge.ess.api.SymmetricEss.ChannelId channelId : SymmetricEss.ChannelId.values()) {
			switch (channelId) {
			case SOC:
			case ACTIVE_POWER:
			case REACTIVE_POWER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case MAX_APPARENT_POWER:
				result.add(new IntegerReadChannel(c, channelId, 3000));
				break;
			case GRID_MODE:
				result.add(new EnumReadChannel(c, channelId, GridMode.UNDEFINED));
				break;
			case ACTIVE_DISCHARGE_ENERGY:
			case ACTIVE_CHARGE_ENERGY:
				result.add(new LongReadChannel(c, channelId));
				break;
			}
		}
		for (io.openems.edge.ess.api.ManagedSymmetricEss.ChannelId channelId : ManagedSymmetricEss.ChannelId.values()) {
			switch (channelId) {
			case DEBUG_SET_ACTIVE_POWER:
			case DEBUG_SET_REACTIVE_POWER:
			case ALLOWED_CHARGE_POWER:
			case ALLOWED_DISCHARGE_POWER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case SET_ACTIVE_POWER_EQUALS:
			case SET_REACTIVE_POWER_EQUALS:
			case SET_ACTIVE_POWER_LESS_OR_EQUALS:
			case SET_ACTIVE_POWER_GREATER_OR_EQUALS:
			case SET_REACTIVE_POWER_LESS_OR_EQUALS:
			case SET_REACTIVE_POWER_GREATER_OR_EQUALS:
				result.add(new IntegerWriteChannel(c, channelId));
				break;
			case APPLY_POWER_FAILED:
				result.add(new StateChannel(c, channelId));
				break;
			}
		}
		for (EssFeneconCommercial40.ChannelId channelId : EssFeneconCommercial40.ChannelId.values()) {
			switch (channelId) {
			case PROTOCOL_VERSION:
			case BATTERY_VOLTAGE:
			case BATTERY_CURRENT:
			case BATTERY_POWER:
			case GRID_ACTIVE_POWER:
			case APPARENT_POWER:
			case CURRENT_L1:
			case CURRENT_L2:
			case CURRENT_L3:
			case FREQUENCY:
			case VOLTAGE_L1:
			case VOLTAGE_L2:
			case VOLTAGE_L3:
			case INVERTER_CURRENT_L1:
			case INVERTER_CURRENT_L2:
			case INVERTER_CURRENT_L3:
			case INVERTER_VOLTAGE_L1:
			case INVERTER_VOLTAGE_L2:
			case INVERTER_VOLTAGE_L3:
			case IPM_TEMPERATURE_L1:
			case IPM_TEMPERATURE_L2:
			case IPM_TEMPERATURE_L3:
			case TRANSFORMER_TEMPERATURE_L2:
			case AC_CHARGE_ENERGY:
			case AC_DISCHARGE_ENERGY:
			case ORIGINAL_ALLOWED_CHARGE_POWER:
			case ORIGINAL_ALLOWED_DISCHARGE_POWER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case BATTERY_MAINTENANCE_STATE:
				result.add(new EnumReadChannel(c, channelId, BatteryMaintenanceState.UNDEFINED));
				break;
			case BATTERY_STRING_SWITCH_STATE:
				result.add(new EnumReadChannel(c, channelId, BatteryStringSwitchState.UNDEFINED));
				break;
			case BMS_DCDC_WORK_MODE:
				result.add(new EnumReadChannel(c, channelId, BmsDcdcWorkMode.UNDEFINED));
				break;
			case BMS_DCDC_WORK_STATE:
				result.add(new EnumReadChannel(c, channelId, BmsDcdcWorkState.UNDEFINED));
				break;
			case CONTROL_MODE:
				result.add(new EnumReadChannel(c, channelId, ControlMode.UNDEFINED));
				break;
			case INVERTER_STATE:
				result.add(new EnumReadChannel(c, channelId, InverterState.UNDEFINED));
				break;
			case SYSTEM_MANUFACTURER:
				result.add(new EnumReadChannel(c, channelId, SystemManufacturer.UNDEFINED));
				break;
			case SYSTEM_STATE:
				result.add(new EnumReadChannel(c, channelId, SystemState.UNDEFINED));
				break;
			case SYSTEM_TYPE:
				result.add(new EnumReadChannel(c, channelId, SystemType.UNDEFINED));
				break;
			case SET_ACTIVE_POWER:
			case SET_REACTIVE_POWER:
			case SET_PV_POWER_LIMIT:
				result.add(new IntegerWriteChannel(c, channelId));
				break;
			case SET_WORK_STATE:
				result.add(new EnumWriteChannel(c, channelId, SetWorkState.UNDEFINED));
				break;
			case STATE_0:
			case STATE_1:
			case STATE_2:
			case STATE_3:
			case STATE_4:
			case STATE_5:
			case STATE_6:
			case STATE_7:
			case STATE_8:
			case STATE_9:
			case STATE_10:
			case STATE_11:
			case STATE_12:
			case STATE_13:
			case STATE_14:
			case STATE_15:
			case STATE_16:
			case STATE_17:
			case STATE_18:
			case STATE_19:
			case STATE_20:
			case STATE_21:
			case STATE_22:
			case STATE_23:
			case STATE_24:
			case STATE_25:
			case STATE_26:
			case STATE_27:
			case STATE_28:
			case STATE_29:
			case STATE_30:
			case STATE_31:
			case STATE_32:
			case STATE_33:
			case STATE_34:
			case STATE_35:
			case STATE_36:
			case STATE_37:
			case STATE_38:
			case STATE_39:
			case STATE_40:
			case STATE_41:
			case STATE_42:
			case STATE_43:
			case STATE_44:
			case STATE_45:
			case STATE_46:
			case STATE_47:
			case STATE_48:
			case STATE_49:
			case STATE_50:
			case STATE_51:
			case STATE_52:
			case STATE_53:
			case STATE_54:
			case STATE_55:
			case STATE_56:
			case STATE_57:
			case STATE_58:
			case STATE_59:
			case STATE_60:
			case STATE_61:
			case STATE_62:
			case STATE_63:
			case STATE_64:
			case STATE_65:
			case STATE_66:
			case STATE_67:
			case STATE_68:
			case STATE_69:
			case STATE_70:
			case STATE_71:
			case STATE_72:
			case STATE_73:
			case STATE_74:
			case STATE_75:
			case STATE_76:
			case STATE_77:
			case STATE_78:
			case STATE_79:
			case STATE_80:
			case STATE_81:
			case STATE_82:
			case STATE_83:
			case STATE_84:
			case STATE_85:
			case STATE_86:
			case STATE_87:
			case STATE_88:
			case STATE_89:
			case STATE_90:
			case STATE_91:
			case STATE_92:
			case STATE_93:
			case STATE_94:
			case STATE_95:
			case STATE_96:
			case STATE_97:
			case STATE_98:
			case STATE_99:
			case STATE_100:
			case STATE_101:
			case STATE_102:
			case STATE_103:
			case STATE_104:
			case STATE_105:
			case STATE_106:
			case STATE_107:
			case STATE_108:
			case STATE_109:
			case STATE_110:
			case STATE_111:
			case STATE_112:
			case STATE_113:
			case STATE_114:
			case STATE_115:
			case STATE_116:
			case STATE_117:
			case STATE_118:
			case STATE_119:
			case STATE_120:
			case STATE_121:
			case STATE_122:
			case STATE_123:
			case STATE_124:
			case STATE_125:
			case STATE_126:
			case STATE_127:
			case STATE_128:
			case STATE_129:
			case STATE_130:
			case STATE_131:
			case STATE_132:
			case STATE_133:
			case STATE_134:
			case STATE_135:
			case STATE_136:
			case STATE_137:
			case STATE_138:
			case STATE_139:
			case STATE_140:
			case STATE_141:
			case STATE_142:
			case STATE_143:
			case STATE_144:
			case STATE_145:
			case STATE_146:
			case STATE_147:
			case STATE_148:
			case STATE_149:
				result.add(new StateChannel(c, channelId));
				break;
			case CELL_1_VOLTAGE:
			case CELL_2_VOLTAGE:
			case CELL_3_VOLTAGE:
			case CELL_4_VOLTAGE:
			case CELL_5_VOLTAGE:
			case CELL_6_VOLTAGE:
			case CELL_7_VOLTAGE:
			case CELL_8_VOLTAGE:
			case CELL_9_VOLTAGE:
			case CELL_10_VOLTAGE:
			case CELL_11_VOLTAGE:
			case CELL_12_VOLTAGE:
			case CELL_13_VOLTAGE:
			case CELL_14_VOLTAGE:
			case CELL_15_VOLTAGE:
			case CELL_16_VOLTAGE:
			case CELL_17_VOLTAGE:
			case CELL_18_VOLTAGE:
			case CELL_19_VOLTAGE:
			case CELL_20_VOLTAGE:
			case CELL_21_VOLTAGE:
			case CELL_22_VOLTAGE:
			case CELL_23_VOLTAGE:
			case CELL_24_VOLTAGE:
			case CELL_25_VOLTAGE:
			case CELL_26_VOLTAGE:
			case CELL_27_VOLTAGE:
			case CELL_28_VOLTAGE:
			case CELL_29_VOLTAGE:
			case CELL_30_VOLTAGE:
			case CELL_31_VOLTAGE:
			case CELL_32_VOLTAGE:
			case CELL_33_VOLTAGE:
			case CELL_34_VOLTAGE:
			case CELL_35_VOLTAGE:
			case CELL_36_VOLTAGE:
			case CELL_37_VOLTAGE:
			case CELL_38_VOLTAGE:
			case CELL_39_VOLTAGE:
			case CELL_40_VOLTAGE:
			case CELL_41_VOLTAGE:
			case CELL_42_VOLTAGE:
			case CELL_43_VOLTAGE:
			case CELL_44_VOLTAGE:
			case CELL_45_VOLTAGE:
			case CELL_46_VOLTAGE:
			case CELL_47_VOLTAGE:
			case CELL_48_VOLTAGE:
			case CELL_49_VOLTAGE:
			case CELL_50_VOLTAGE:
			case CELL_51_VOLTAGE:
			case CELL_52_VOLTAGE:
			case CELL_53_VOLTAGE:
			case CELL_54_VOLTAGE:
			case CELL_55_VOLTAGE:
			case CELL_56_VOLTAGE:
			case CELL_57_VOLTAGE:
			case CELL_58_VOLTAGE:
			case CELL_59_VOLTAGE:
			case CELL_60_VOLTAGE:
			case CELL_61_VOLTAGE:
			case CELL_62_VOLTAGE:
			case CELL_63_VOLTAGE:
			case CELL_64_VOLTAGE:
			case CELL_65_VOLTAGE:
			case CELL_66_VOLTAGE:
			case CELL_67_VOLTAGE:
			case CELL_68_VOLTAGE:
			case CELL_69_VOLTAGE:
			case CELL_70_VOLTAGE:
			case CELL_71_VOLTAGE:
			case CELL_72_VOLTAGE:
			case CELL_73_VOLTAGE:
			case CELL_74_VOLTAGE:
			case CELL_75_VOLTAGE:
			case CELL_76_VOLTAGE:
			case CELL_77_VOLTAGE:
			case CELL_78_VOLTAGE:
			case CELL_79_VOLTAGE:
			case CELL_80_VOLTAGE:
			case CELL_81_VOLTAGE:
			case CELL_82_VOLTAGE:
			case CELL_83_VOLTAGE:
			case CELL_84_VOLTAGE:
			case CELL_85_VOLTAGE:
			case CELL_86_VOLTAGE:
			case CELL_87_VOLTAGE:
			case CELL_88_VOLTAGE:
			case CELL_89_VOLTAGE:
			case CELL_90_VOLTAGE:
			case CELL_91_VOLTAGE:
			case CELL_92_VOLTAGE:
			case CELL_93_VOLTAGE:
			case CELL_94_VOLTAGE:
			case CELL_95_VOLTAGE:
			case CELL_96_VOLTAGE:
			case CELL_97_VOLTAGE:
			case CELL_98_VOLTAGE:
			case CELL_99_VOLTAGE:
			case CELL_100_VOLTAGE:
			case CELL_101_VOLTAGE:
			case CELL_102_VOLTAGE:
			case CELL_103_VOLTAGE:
			case CELL_104_VOLTAGE:
			case CELL_105_VOLTAGE:
			case CELL_106_VOLTAGE:
			case CELL_107_VOLTAGE:
			case CELL_108_VOLTAGE:
			case CELL_109_VOLTAGE:
			case CELL_110_VOLTAGE:
			case CELL_111_VOLTAGE:
			case CELL_112_VOLTAGE:
			case CELL_113_VOLTAGE:
			case CELL_114_VOLTAGE:
			case CELL_115_VOLTAGE:
			case CELL_116_VOLTAGE:
			case CELL_117_VOLTAGE:
			case CELL_118_VOLTAGE:
			case CELL_119_VOLTAGE:
			case CELL_120_VOLTAGE:
			case CELL_121_VOLTAGE:
			case CELL_122_VOLTAGE:
			case CELL_123_VOLTAGE:
			case CELL_124_VOLTAGE:
			case CELL_125_VOLTAGE:
			case CELL_126_VOLTAGE:
			case CELL_127_VOLTAGE:
			case CELL_128_VOLTAGE:
			case CELL_129_VOLTAGE:
			case CELL_130_VOLTAGE:
			case CELL_131_VOLTAGE:
			case CELL_132_VOLTAGE:
			case CELL_133_VOLTAGE:
			case CELL_134_VOLTAGE:
			case CELL_135_VOLTAGE:
			case CELL_136_VOLTAGE:
			case CELL_137_VOLTAGE:
			case CELL_138_VOLTAGE:
			case CELL_139_VOLTAGE:
			case CELL_140_VOLTAGE:
			case CELL_141_VOLTAGE:
			case CELL_142_VOLTAGE:
			case CELL_143_VOLTAGE:
			case CELL_144_VOLTAGE:
			case CELL_145_VOLTAGE:
			case CELL_146_VOLTAGE:
			case CELL_147_VOLTAGE:
			case CELL_148_VOLTAGE:
			case CELL_149_VOLTAGE:
			case CELL_150_VOLTAGE:
			case CELL_151_VOLTAGE:
			case CELL_152_VOLTAGE:
			case CELL_153_VOLTAGE:
			case CELL_154_VOLTAGE:
			case CELL_155_VOLTAGE:
			case CELL_156_VOLTAGE:
			case CELL_157_VOLTAGE:
			case CELL_158_VOLTAGE:
			case CELL_159_VOLTAGE:
			case CELL_160_VOLTAGE:
			case CELL_161_VOLTAGE:
			case CELL_162_VOLTAGE:
			case CELL_163_VOLTAGE:
			case CELL_164_VOLTAGE:
			case CELL_165_VOLTAGE:
			case CELL_166_VOLTAGE:
			case CELL_167_VOLTAGE:
			case CELL_168_VOLTAGE:
			case CELL_169_VOLTAGE:
			case CELL_170_VOLTAGE:
			case CELL_171_VOLTAGE:
			case CELL_172_VOLTAGE:
			case CELL_173_VOLTAGE:
			case CELL_174_VOLTAGE:
			case CELL_175_VOLTAGE:
			case CELL_176_VOLTAGE:
			case CELL_177_VOLTAGE:
			case CELL_178_VOLTAGE:
			case CELL_179_VOLTAGE:
			case CELL_180_VOLTAGE:
			case CELL_181_VOLTAGE:
			case CELL_182_VOLTAGE:
			case CELL_183_VOLTAGE:
			case CELL_184_VOLTAGE:
			case CELL_185_VOLTAGE:
			case CELL_186_VOLTAGE:
			case CELL_187_VOLTAGE:
			case CELL_188_VOLTAGE:
			case CELL_189_VOLTAGE:
			case CELL_190_VOLTAGE:
			case CELL_191_VOLTAGE:
			case CELL_192_VOLTAGE:
			case CELL_193_VOLTAGE:
			case CELL_194_VOLTAGE:
			case CELL_195_VOLTAGE:
			case CELL_196_VOLTAGE:
			case CELL_197_VOLTAGE:
			case CELL_198_VOLTAGE:
			case CELL_199_VOLTAGE:
			case CELL_200_VOLTAGE:
			case CELL_201_VOLTAGE:
			case CELL_202_VOLTAGE:
			case CELL_203_VOLTAGE:
			case CELL_204_VOLTAGE:
			case CELL_205_VOLTAGE:
			case CELL_206_VOLTAGE:
			case CELL_207_VOLTAGE:
			case CELL_208_VOLTAGE:
			case CELL_209_VOLTAGE:
			case CELL_210_VOLTAGE:
			case CELL_211_VOLTAGE:
			case CELL_212_VOLTAGE:
			case CELL_213_VOLTAGE:
			case CELL_214_VOLTAGE:
			case CELL_215_VOLTAGE:
			case CELL_216_VOLTAGE:
			case CELL_217_VOLTAGE:
			case CELL_218_VOLTAGE:
			case CELL_219_VOLTAGE:
			case CELL_220_VOLTAGE:
			case CELL_221_VOLTAGE:
			case CELL_222_VOLTAGE:
			case CELL_223_VOLTAGE:
			case CELL_224_VOLTAGE:
				result.add(new IntegerReadChannel(c, channelId));				
				break;
	
			}
		}
		return result.stream();
	}
}