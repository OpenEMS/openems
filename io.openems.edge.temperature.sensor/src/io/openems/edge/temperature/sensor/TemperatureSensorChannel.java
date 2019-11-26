package io.openems.edge.temperature.sensor;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.annotation.versioning.ProviderType;


@ProviderType
public interface TemperatureSensorChannel extends OpenemsComponent {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**.
         * Temperature
         *
         * <ul>
         * <li>Interface: TemperatureSensoric
         * <li>Type: Float
         * <li>Unit: Degree Celsius
         * </ul>
         */
        TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)); //
        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    /**.
     * @return Gets the Temperature in [dezidegree celsius].
     */

    public default Channel<Integer> getTemperature() {
        return this.channel(ChannelId.TEMPERATURE);
    }
}

