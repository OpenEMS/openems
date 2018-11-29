package io.openems.edge.pvinverter.test;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateCollectorChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.SymmetricPvInverter;

/**
 * Provides a simple, simulated SymmetricPvInverter component that can be used
 * together with the OpenEMS Component test framework.
 */
public class DummySymmetricPvInverter extends AbstractOpenemsComponent implements SymmetricPvInverter {

	public DummySymmetricPvInverter(String id) {
		Stream.of(//
				Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case STATE:
						return new StateCollectorChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricMeter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER:
					case REACTIVE_POWER:
					case ACTIVE_CONSUMPTION_ENERGY:
					case ACTIVE_PRODUCTION_ENERGY:
					case CURRENT:
					case FREQUENCY:
					case MAX_ACTIVE_POWER:
					case MIN_ACTIVE_POWER:
					case VOLTAGE:
						return new IntegerReadChannel(this, channelId);
					}
					return null;
				}), Arrays.stream(SymmetricPvInverter.ChannelId.values()).map(channelId -> {
					switch (channelId) {
					case ACTIVE_POWER_LIMIT:
						return new IntegerWriteChannel(this, channelId);
					}
					return null;
				})).flatMap(channel -> channel).forEach(channel -> {
					channel.nextProcessImage();
					this.addChannel(channel);
				});
		super.activate(null, "", id, true);
	}

}
