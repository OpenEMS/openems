package io.openems.edge.relay.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Relay extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Is active or not.
         *
         * <ul>
         * <li>Interface: Relay
         * <li>Type: boolean
         * <li>Unit: ON_OFF
         * </ul>
         */
        WRITE_ON_OFF(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF).accessMode(AccessMode.READ_WRITE)),
        READ_ON_OFF(Doc.of(OpenemsType.BOOLEAN).unit(Unit.ON_OFF).accessMode(AccessMode.READ_ONLY)),
        /**
         * Inverts logic of Relay.
         *
         * <ul>
         * <li>Interface: Relay
         * <li>Type: boolean
         * </ul>
         */
        IS_CLOSER(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;


        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Return the WriteChannel of the Current Status of the Relay.
     *
     * @return the WriteChannel
     */
    default WriteChannel<Boolean> getRelaysWriteChannel() {
        return this.channel(ChannelId.WRITE_ON_OFF);
    }

    /**
     * Return the Current Status of the Relay, communicated over Modbus.
     *
     * @return the Channel
     */
    default Channel<Boolean> getRelaysReadChannel() {
        return this.channel(ChannelId.READ_ON_OFF);
    }

    /**
     * Return the Current Inversion Status of the Relay.
     *
     * @return True if inverted
     */
    default boolean getRelayStatus() {
        if (this.getRelaysReadChannel().value().isDefined()) {
            return this.getRelaysReadChannel().value().get();
        }
        if (this.getRelaysReadChannel().getNextValue().isDefined()) {
            return this.getRelaysReadChannel().getNextValue().get();
        } else {
            return false;
        }
    }

    /**
     * Sets the Inversion Status.
     *
     * @param status Inversion Status that should be configured
     */
    default void setRelayStatus(boolean status) throws OpenemsError.OpenemsNamedException {

            this.getRelaysWriteChannel().setNextWriteValue(status);

    }

    /**
     * Returns the Inversion Status.
     *
     * @return true if inverted
     */
    default Channel<Boolean> isCloser() {
        return this.channel(ChannelId.IS_CLOSER);
    }


}
