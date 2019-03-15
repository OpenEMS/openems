package io.openems.edge.ess.byd.container;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
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
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssFeneconBydContainer c) {
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
				result.add(new IntegerReadChannel(c, channelId, 100000)); // TODO
				break;
			case GRID_MODE:
				result.add(new IntegerReadChannel(c, channelId, GridMode.ON_GRID));
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
			}
		}
		for (EssFeneconBydContainer.ChannelId channelId : EssFeneconBydContainer.ChannelId.values()) {
			switch (channelId) {
			case READ_ONLY_MODE:
				result.add(new StateChannel(c, channelId));
				break;
			// RTU
			case SYSTEM_WORKSTATE:
			case LIMIT_INDUCTIVE_REACTIVE_POWER:
			case LIMIT_CAPACITIVE_REACTIVE_POWER:
			case CONTAINER_RUN_NUMBER:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case SYSTEM_WORKMODE:
			case SET_SYSTEM_WORKSTATE:
			case SET_ACTIVE_POWER:
			case SET_REACTIVE_POWER:
				result.add(new IntegerWriteChannel(c, channelId));
				break;
			// PCS
			case PCS_SYSTEM_WORKSTATE:
			case PCS_SYSTEM_WORKMODE:
			case PHASE3_ACTIVE_POWER:
			case PHASE3_REACTIVE_POWER:
			case PHASE3_INSPECTING_POWER:
			case PCS_DISCHARGE_LIMIT_ACTIVE_POWER:
			case PCS_CHARGE_LIMIT_ACTIVE_POWER:
			case POSITIVE_REACTIVE_POWER_LIMIT:
			case NEGATIVE_REACTIVE_POWER_LIMIT:
			case CURRENT_L1:
			case CURRENT_L2:
			case CURRENT_L3:
			case VOLTAGE_L1:
			case VOLTAGE_L2:
			case VOLTAGE_L3:
			case VOLTAGE_L12:
			case VOLTAGE_L23:
			case VOLTAGE_L31:
			case SYSTEM_FREQUENCY:
			case DC_VOLTAGE:
			case DC_CURRENT:
			case DC_POWER:
			case IGBT_TEMPERATURE_L1:
			case IGBT_TEMPERATURE_L2:
			case IGBT_TEMPERATURE_L3:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			/*
			 * Pcs warning and alarms are split into state channels
			 */	
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
				result.add(new StateChannel(c, channelId));
				break;
			// BECU
			case BATTERY_STRING_WORK_STATE:
			case BATTERY_STRING_TOTAL_VOLTAGE:
			case BATTERY_STRING_CURRENT:
			case BATTERY_STRING_SOC:
			case BATTERY_STRING_AVERAGE_TEMPERATURE:
			case BATTERY_NUMBER_MAX_STRING_VOLTAGE:
			case BATTERY_STRING_MAX_VOLTAGE:
			case BATTERY_STRING_MAX_VOLTAGE_TEMPARATURE:
			case BATTERY_NUMBER_MIN_STRING_VOLTAGE:
			case BATTERY_STRING_MIN_VOLTAGE:
			case BATTERY_STRING_MIN_VOLTAGE_TEMPARATURE:
			case BATTERY_NUMBER_MAX_STRING_TEMPERATURE:
			case BATTERY_STRING_MAX_TEMPERATURE:
			case BATTERY_STRING_MAX_TEMPARATURE_VOLTAGE:
			case BATTERY_NUMBER_MIN_STRING_TEMPERATURE:
			case BATTERY_STRING_MIN_TEMPERATURE:
			case BATTERY_STRING_MIN_TEMPARATURE_VOLTAGE:
			case BATTERY_STRING_CHARGE_CURRENT_LIMIT:
			case BATTERY_STRING_DISCHARGE_CURRENT_LIMIT:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			/*
			  * BECU warning and alarms are split into state channels
		     */	
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
			case STATE_160:
			case STATE_161:
			case STATE_162:
				result.add(new StateChannel(c, channelId));
				break;				
			// ADAS
			case CONTAINER_IMMERSION_STATE:
			case CONTAINER_FIRE_STATUS:
			case CONTROL_CABINET_STATE:
			case CONTAINER_GROUNDING_FAULT:
			case CONTAINER_DOOR_STATUS_0:
			case CONTAINER_DOOR_STATUS_1:
			case CONTAINER_AIRCONDITION_POWER_SUPPLY_STATE:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case STATE_163:
			case STATE_164:
			case STATE_165:
			case STATE_166:
			case STATE_167:
			case STATE_168:
			case STATE_169:
			case STATE_170:
			case STATE_171:
			case STATE_172:
			case STATE_173:
			case STATE_174:
			case STATE_175:
			case STATE_176:
			case STATE_177:
			case STATE_178:
			case STATE_179:
			case STATE_180:
			case STATE_181:
			case STATE_182:
			case STATE_183:
			case STATE_184:
			case STATE_185:
			case STATE_186:
			case STATE_187:
			case STATE_188:
			case STATE_189:
			case STATE_190:
			case STATE_191:
			case STATE_192:
			case STATE_193:
			case STATE_194:
			case STATE_195:
			case STATE_196:
			case STATE_197:
			case STATE_198:
			case STATE_199:
			case STATE_200:
			case STATE_201:
			case STATE_202:	
			case STATE_203:	
				result.add(new StateChannel(c, channelId));
				break;
			}
		}
		return result.stream();
	}
}
