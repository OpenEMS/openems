package io.openems.edge.consolinno.sensor.signal.api;


import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface SignalSensor extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Has an Error occurred.
         *
         * <ul>
         * <li>Interface: ErrorSensorSpi
         * <li>Type: Boolean
         * </ul>
         */
        SIGNAL_ACTIVE(Doc.of(OpenemsType.BOOLEAN)),

        /**
         * Error Message String.
         * <ul>
         *     <li> Interface: ErrorSensorSpi
         *     <li> Type: String
         * </ul>>
         */
        SIGNAL_MESSAGE(Doc.of(OpenemsType.STRING)),

        /**
         * SignalType Stored As String
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
     * Gets if Error Occured or not.
     *
     * @return the Channel
     */
    default Channel<Boolean> signalActive() {
        return this.channel(ChannelId.SIGNAL_ACTIVE);
    }

    /**
     * Gets the Error Message.
     * Status
     * Error
     * Fault Message
     * @return the Channel
     */
    default Channel<String> getSignalMessage() {
        return this.channel(ChannelId.SIGNAL_MESSAGE);
    }

    default Channel<String> getSignalType() {
        return this.channel(ChannelId.SIGNAL_TYPE);
    }
}
