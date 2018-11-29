package io.openems.edge.fenecon.pro.ess;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(FeneconProEss c) {
		return Stream.of( //
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SOC:
					case ACTIVE_POWER:
					case REACTIVE_POWER:
					case ACTIVE_CHARGE_ENERGY:
					case ACTIVE_DISCHARGE_ENERGY:
						return new IntegerReadChannel(c, channelId);
					case GRID_MODE:
						return new IntegerReadChannel(c, channelId, SymmetricEss.GridMode.UNDEFINED.ordinal());
					case MAX_APPARENT_POWER:
						return new IntegerReadChannel(c, channelId, FeneconProEss.MAX_APPARENT_POWER);
					}
					return null;
				}), Arrays.stream(ManagedSymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER:
					case DEBUG_SET_REACTIVE_POWER:
					case ALLOWED_CHARGE_POWER:
					case ALLOWED_DISCHARGE_POWER:
						return new IntegerReadChannel(c, channelId);
					case SET_ACTIVE_POWER_EQUALS:
					case SET_REACTIVE_POWER_EQUALS:
					case SET_ACTIVE_POWER_LESS_OR_EQUALS:
						return new IntegerWriteChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(AsymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_L1:
					case ACTIVE_POWER_L2:
					case ACTIVE_POWER_L3:
					case REACTIVE_POWER_L1:
					case REACTIVE_POWER_L2:
					case REACTIVE_POWER_L3:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				}), Arrays.stream(ManagedAsymmetricEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case DEBUG_SET_ACTIVE_POWER_L1:
					case DEBUG_SET_ACTIVE_POWER_L2:
					case DEBUG_SET_ACTIVE_POWER_L3:
					case DEBUG_SET_REACTIVE_POWER_L1:
					case DEBUG_SET_REACTIVE_POWER_L2:
					case DEBUG_SET_REACTIVE_POWER_L3:
						return new IntegerReadChannel(c, channelId);
					case SET_ACTIVE_POWER_L1_EQUALS:
					case SET_ACTIVE_POWER_L2_EQUALS:
					case SET_ACTIVE_POWER_L3_EQUALS:
					case SET_REACTIVE_POWER_L1_EQUALS:
					case SET_REACTIVE_POWER_L2_EQUALS:
					case SET_REACTIVE_POWER_L3_EQUALS:
						return new IntegerWriteChannel(c, channelId);
					}
					return null;

				}), Arrays.stream(FeneconProEss.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case SYSTEM_STATE:
					case TOTAL_BATTERY_CHARGE_ENERGY:
					case TOTAL_BATTERY_DISCHARGE_ENERGY:
					case WORK_MODE:
					case BATTERY_GROUP_STATE:
					case CONTROL_MODE:
					case BATTERY_CURRENT:
					case PCS_OPERATION_STATE:
					case CURRENT_L1:
					case CURRENT_L2:
					case CURRENT_L3:
					case VOLTAGE_L1:
					case VOLTAGE_L2:
					case VOLTAGE_L3:
					case BATTERY_VOLTAGE:
					case BATTERY_POWER:
					case FREQUENCY_L1:
					case FREQUENCY_L2:
					case FREQUENCY_L3:
					case SINGLE_PHASE_ALLOWED_APPARENT:
					case BATTERY_TEMPERATURE_SECTION_1:
					case BATTERY_TEMPERATURE_SECTION_10:
					case BATTERY_TEMPERATURE_SECTION_11:
					case BATTERY_TEMPERATURE_SECTION_12:
					case BATTERY_TEMPERATURE_SECTION_14:
					case BATTERY_TEMPERATURE_SECTION_15:
					case BATTERY_TEMPERATURE_SECTION_16:
					case BATTERY_TEMPERATURE_SECTION_2:
					case BATTERY_TEMPERATURE_SECTION_3:
					case BATTERY_TEMPERATURE_SECTION_4:
					case BATTERY_TEMPERATURE_SECTION_5:
					case BATTERY_TEMPERATURE_SECTION_6:
					case BATTERY_TEMPERATURE_SECTION_7:
					case BATTERY_TEMPERATURE_SECTION_8:
					case BATTERY_TEMPERATURE_SECTION_9:
					case BATTERY_VOLTAGE_SECTION_1:
					case BATTERY_VOLTAGE_SECTION_10:
					case BATTERY_VOLTAGE_SECTION_11:
					case BATTERY_VOLTAGE_SECTION_12:
					case BATTERY_VOLTAGE_SECTION_13:
					case BATTERY_VOLTAGE_SECTION_14:
					case BATTERY_VOLTAGE_SECTION_15:
					case BATTERY_VOLTAGE_SECTION_16:
					case BATTERY_VOLTAGE_SECTION_2:
					case BATTERY_VOLTAGE_SECTION_3:
					case BATTERY_VOLTAGE_SECTION_4:
					case BATTERY_VOLTAGE_SECTION_5:
					case BATTERY_VOLTAGE_SECTION_6:
					case BATTERY_VOLTAGE_SECTION_7:
					case BATTERY_VOLTAGE_SECTION_8:
					case BATTERY_VOLTAGE_SECTION_9:
					case BATTERY_TEMPERATURE_SECTION_13:
						return new IntegerReadChannel(c, channelId);

					case PCS_MODE:
					case RTC_DAY:
					case RTC_HOUR:
					case RTC_MINUTE:
					case RTC_MONTH:
					case RTC_SECOND:
					case RTC_YEAR:
					case SETUP_MODE:
					case SET_PCS_MODE:
					case SET_SETUP_MODE:
					case SET_WORK_STATE:
					case SET_ACTIVE_POWER_L1:
					case SET_ACTIVE_POWER_L2:
					case SET_ACTIVE_POWER_L3:
					case SET_REACTIVE_POWER_L1:
					case SET_REACTIVE_POWER_L2:
					case SET_REACTIVE_POWER_L3:
						return new IntegerWriteChannel(c, channelId);

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
					case STATE_11:
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
					case STATE_12:
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
					case STATE_13:
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
					case STATE_14:
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
					case STATE_15:
					case STATE_150:
					case STATE_151:
					case STATE_152:
					case STATE_153:
					case STATE_154:
					case STATE_155:
					case STATE_156:
					case STATE_157:
					case STATE_158:
					case STATE_159:
					case STATE_16:
					case STATE_160:
					case STATE_161:
					case STATE_162:
					case STATE_163:
					case STATE_164:
					case STATE_165:
					case STATE_166:
					case STATE_167:
					case STATE_168:
					case STATE_169:
					case STATE_17:
					case STATE_170:
					case STATE_171:
					case STATE_172:
					case STATE_173:
					case STATE_174:
					case STATE_175:
					case STATE_176:
					case STATE_177:
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
						return new StateChannel(c, channelId);
					}
					return null;
				})).flatMap(channel -> channel);
	}
}
