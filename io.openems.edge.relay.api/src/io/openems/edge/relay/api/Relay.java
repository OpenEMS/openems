package io.openems.edge.relay.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Relay extends OpenemsComponent {
    /**
     * Is active or not.
     *
     * <ul>
     * <li>Interface: ActuatorRelays
     * <li>Type: boolean
     * <li>Unit: ON_OFF
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        WRITE_ON_OFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        READ_ON_OFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
        //
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
     * Gets the On or Off Value as Boolean.
     *
     * @return the Channel
     */
    default WriteChannel<Boolean> getRelaysWriteChannel() {
        return this.channel(ChannelId.WRITE_ON_OFF);
    }

    default Channel<Boolean> getRelaysReadChannel() {
        return this.channel(ChannelId.READ_ON_OFF);
    }

    default boolean getRelayStatus() {
        if (getRelaysReadChannel().value().isDefined()) {
            return getRelaysReadChannel().value().get();
        }
        if (getRelaysReadChannel().getNextValue().isDefined()) {
            return getRelaysReadChannel().getNextValue().get();
        } else {
            return false;
        }
    }

    default void setRelayStatus(boolean status) {
        try {
            getRelaysWriteChannel().setNextWriteValue(status);
        } catch (OpenemsError.OpenemsNamedException ignored) {

        }

    }

    default Channel<Boolean> isCloser() {
        return this.channel(ChannelId.IS_CLOSER);
    }


}
