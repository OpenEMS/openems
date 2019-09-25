package io.openems.edge.sensors.temperature.mcp3208;

import java.util.Arrays;
import java.util.stream.Stream;

import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.sensors.temperature.api.TemperatureSensor;

public class Utils {
    //TODO look into AbstractReadChannel; added one ? here
    public static Stream<? extends AbstractReadChannel<?, ?>> initializeChannels(Mcp3208TemperatureSensor c) {
        return Stream.of( //
                Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {
                    switch (channelId) {
                        case STATE:
                            return new StateCollectorChannel(c, channelId);
                    }
                    return null;
                }), Arrays.stream(TemperatureSensor.ChannelId.values()).map(channelId -> {
                    switch (channelId) {
                        case TEMPERATURE:
                            return new IntegerReadChannel(c, channelId);
                    }

                    return null;
                })/*
                 * , Arrays.stream(MeterSocomecDirisA10.ChannelId.values()).map(channelId -> {
                 * switch (channelId) { } return null; })
                 */ //
        ).flatMap(channel -> channel);
    }
}