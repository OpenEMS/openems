package io.openems.edge.fenecon.mini.ess;

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
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(FeneconMiniEss c) {
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
				result.add(new IntegerReadChannel(c, channelId, GridMode.UNDEFINED));
				break;
			case ACTIVE_DISCHARGE_ENERGY:
			case ACTIVE_CHARGE_ENERGY:
				result.add(new LongReadChannel(c, channelId));
				break;
			}
		}
		for (io.openems.edge.ess.api.AsymmetricEss.ChannelId channelId : AsymmetricEss.ChannelId.values()) {
			switch (channelId) {
			case ACTIVE_POWER_L1:
			case ACTIVE_POWER_L2:
			case ACTIVE_POWER_L3:
			case REACTIVE_POWER_L1:
			case REACTIVE_POWER_L2:
			case REACTIVE_POWER_L3:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			}
		}
		for (io.openems.edge.ess.api.SinglePhaseEss.ChannelId channelId : SinglePhaseEss.ChannelId.values()) {
			switch (channelId) {
			case PHASE:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			}
		}
		for (FeneconMiniEss.ChannelId channelId : FeneconMiniEss.ChannelId.values()) {
			switch (channelId) {
			case SYSTEM_WORK_MODE_STATE:
			case SYSTEM_STATE:
			case BATTERY_CURRENT:
			case BATTERY_GROUP_STATE:
			case BATTERY_POWER:
			case BATTERY_VOLTAGE:
			case CONTROL_MODE:
			case BECU1_CHARGE_CURRENT:
			case BECU1_CURRENT:
			case BECU1_DISCHARGE_CURRENT:
			case BECU1_MAX_TEMP:
			case BECU1_MAX_TEMP_NO:
			case BECU1_MIN_TEMP:
			case BECU1_MIN_TEMP_NO:
			case BECU1_SOC:
			case BECU1_VERSION:
			case BECU1_VOLT:
			case BECU2_MAX_TEMP:
			case BECU2_MAX_TEMP_NO:
			case BECU2_MAX_VOLT:
			case BECU2_MAX_VOLT_NO:
			case BECU2_MIN_TEMP:
			case BECU2_MIN_TEMP_NO:
			case BECU2_MIN_VOLT:
			case BECU2_MIN_VOLT_NO:
			case BECU2_SOC:
			case BECU2_VERSION:
			case BECU1_MAX_VOLT:
			case BECU1_MAX_VOLT_NO:
			case BECU1_MIN_VOLT:
			case BECU1_MIN_VOLT_NO:
			case BECU2_CHARGE_CURRENT:
			case BECU2_CURRENT:
			case BECU2_DISCHARGE_CURRENT:
			case BECU2_VOLT:
			case BECU_CHARGE_CURRENT:
			case BECU_CURRENT:
			case BECU_DISCHARGE_CURRENT:
			case BECU_NUM:
			case BECU_VOLT:
			case BECU_WORK_STATE:
			case SYSTEM_WORK_STATE:
				result.add(new IntegerReadChannel(c, channelId));
				break;

			case RTC_DAY:
			case RTC_HOUR:
			case RTC_MINUTE:
			case RTC_MONTH:
			case RTC_SECOND:
			case RTC_YEAR:
			case SETUP_MODE:
			case PCS_MODE:
			case SET_WORK_STATE:
				result.add(new IntegerWriteChannel(c, channelId));
				break;

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
				result.add(new StateChannel(c, channelId));
				break;
			}
		}
		return result.stream();
	}
}