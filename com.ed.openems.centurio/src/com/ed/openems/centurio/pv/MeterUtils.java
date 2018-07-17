package com.ed.openems.centurio.pv;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.Meter;
import io.openems.edge.meter.symmetric.api.SymmetricMeter;

public class MeterUtils {

	public static Stream<? extends AbstractReadChannel<?>> initializeChannels(CenturioPVMeter c) {
		// TODO Auto-generated method stub
		return Stream.of(Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
			switch (channelId) {
			case STATE:
				return new StateCollectorChannel(c, channelId);
			}
			return null;
		}), Arrays.stream(Meter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case FREQUENCY:
						return new IntegerReadChannel(c, channelId, 0);

					}
					return null;
				})

				, Arrays.stream(SymmetricMeter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER:
					case MAX_ACTIVE_POWER:
					case MIN_ACTIVE_POWER:
					case REACTIVE_POWER:
					case CONSUMPTION_ACTIVE_POWER:
					case CONSUMPTION_REACTIVE_POWER:
					case PRODUCTION_ACTIVE_POWER:
					case PRODUCTION_REACTIVE_POWER:
					case CURRENT:
					case VOLTAGE:
						return new IntegerReadChannel(c, channelId);
					}
					return null;
				})).flatMap(channel -> channel);
	}

}
