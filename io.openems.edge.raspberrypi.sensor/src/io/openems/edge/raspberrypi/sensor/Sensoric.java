package io.openems.edge.raspberrypi.sensor;

import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.common.channel.Unit;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface Sensoric extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId{
       ;
       /*TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)); //
        private final Doc doc;
        private ChannelId(Doc doc) {
            this.doc = doc;
        }
        public Doc doc() {
            return this.doc;
        }


       //Sensoric stellt auswahl an OpenemsChannel bereit; in Utils werden sie der Liste zugefügt
        @Override
        public String id() {
            return null;
        }

        @Override
        public Doc doc() {
            return null;
        }
    }
        /*default Channel<Integer> getTemperature() {
            return this.channel(ChannelId.TEMPERATURE);
        }*/
}
