package io.openems.edge.consolinno.leaflet.sensor.signal.api;


import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Provides a modified Temperature Sensor that detects if the Temperature is above or below a set Value.
 */
public interface SignalSensor extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Has a Signal active (true) or not (false).
         *
         * <ul>
         * <li>Interface: SignalSensor
         * <li>Type: Boolean
         * </ul>
         */
        SIGNAL_ACTIVE(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * SignalType Stored As String.
         * <ul>
         * <li>Interface: SignalSensor
         * <li>Type: String
         * </ul>
         */
        SIGNAL_TYPE(Doc.of(OpenemsType.STRING));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets if Error/Signal is (in)active.
     *
     * @return the Channel
     */
    default Channel<Boolean> signalActive() {
        return this.channel(ChannelId.SIGNAL_ACTIVE);
    }


    /**
     * Gets the Type of Signal (e.g. Signal or Error).
     *
     * @return the Channel
     */
    default Channel<String> getSignalType() {
        return this.channel(ChannelId.SIGNAL_TYPE);
    }
}
