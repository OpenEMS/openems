package io.openems.edge.raspberrypi.sensors.temperaturesensor;

import io.openems.edge.common.channel.FloatDoc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.internal.AbstractReadChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannel;
import io.openems.edge.common.channel.internal.StateCollectorChannelDoc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.raspberrypi.sensors.temperaturesensor.TemperatureSensor;
import io.openems.edge.raspberrypi.sensors.temperaturesensor.TemperatureSensoric;

import java.util.Arrays;
import java.util.stream.Stream;

public class Utils {

    public static Stream<? extends AbstractReadChannel<?, ?>> initializeChannels(TemperatureSensor c) {

        return Stream.of(Arrays.stream(OpenemsComponent.ChannelId.values()).map(channelId -> {

            switch (channelId) {
                case STATE:
                    StateCollectorChannelDoc doc = new StateCollectorChannelDoc();
                    return new StateCollectorChannel(c, channelId, doc);
            }
            return null;
        }), Arrays.stream(TemperatureSensoric.ChannelId.values()).map(channelId -> {

            switch (channelId) {
                case TEMPERATURE:
                    FloatDoc doc = new FloatDoc();
                    return new FloatReadChannel(c, channelId, doc);
            }
            return null;
        })).flatMap(channel -> channel);
    }
}
