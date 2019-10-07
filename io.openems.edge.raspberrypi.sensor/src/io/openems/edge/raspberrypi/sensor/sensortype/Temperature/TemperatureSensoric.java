package io.openems.edge.raspberrypi.sensor.sensortype.Temperature;

import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.annotation.versioning.ProviderType;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;


@ProviderType
public interface TemperatureSensoric extends OpenemsComponent {

    //TODO Different Sensoric Interfaces --> Different Channels

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId{

       TEMPERATURE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEZIDEGREE_CELSIUS)); //
        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }
       public Doc doc(){return this.doc;}


    }
        default Channel<Integer> getTemperature() {
            return this.channel(ChannelId.TEMPERATURE);
        }
}
