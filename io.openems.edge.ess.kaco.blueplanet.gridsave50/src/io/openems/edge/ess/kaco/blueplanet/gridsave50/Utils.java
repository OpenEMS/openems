package io.openems.edge.ess.kaco.blueplanet.gridsave50;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Utils {
	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(EssKacoBlueplanetGridsave50 c) {
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
		for (EssKacoBlueplanetGridsave50.ChannelId channelId : EssKacoBlueplanetGridsave50.ChannelId.values()) {
			switch (channelId) {
//					case CHA_CUTOFF_A:
			case CHA_MAX_A:
			case CHA_MAX_V:
//					case DIS_CUTOFF_A:
			case DIS_MAX_A:
			case DIS_MIN_V:
			case EN_LIMIT:
			case W_SET_PCT:
			case WATCHDOG:
			case BAT_SOC:
			case BAT_SOH:
			case BAT_TEMP:
			case COMMAND_ID_REQ:
			case REQ_PARAM_0:
			case COMMAND_ID_REQ_ENA:
				result.add(new IntegerWriteChannel(c, channelId));
				break;
			case REQUESTED_STATE:
				result.add(new EnumWriteChannel(c, channelId));
				break;
			case A_SF:
			case V_SF:
			case W_MAX:
			case W_MAX_SF:
			case W_SET_PCT_SF:
			case SOC_SF:
			case SOH_SF:
			case TEMP_SF:
			case COMMAND_ID_RES:
			case RETURN_CODE:
			case DEBUG_REQUESTED_STATE:
			case DEBUG_CHA_MAX_A:
			case DEBUG_CHA_MAX_V:
			case DEBUG_DIS_MAX_A:
			case DEBUG_DIS_MIN_V:
			case DEBUG_EN_LIMIT:
			case AC_ENERGY_SF:
			case DC_CURRENT:
			case DC_CURRENT_SF:
			case DC_POWER:
			case DC_POWER_SF:
			case DC_VOLTAGE:
			case DC_VOLTAGE_SF:
				result.add(new IntegerReadChannel(c, channelId));
				break;
			case CURRENT_STATE:
				result.add(new EnumReadChannel(c, channelId, CurrentState.UNDEFINED));
				break;
			case VENDOR_OPERATING_STATE:
				result.add(new EnumReadChannel(c, channelId, ErrorCode.UNDEFINED));
				break;
			case AC_ENERGY:
				result.add(new LongReadChannel(c, channelId));
				break;
			}
			return null;
		} //
		return result.stream();
	}
}
