package io.openems.edge.pwm.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Pwm extends OpenemsComponent {


    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * * How much of the low/highFlank the device uses. e.g. 80% power --> like a dimming light and
         * it's "level" of brightness.
         * <ul>
         * <li>Interface: PowerLevel
         * <li>Type: Integer
         * </ul>
         */
        WRITE_POWER_LEVEL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        READ_POWER_LEVEL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)), //
        /**
         * Inverts the Power Level Logic.
         * <ul>
         * <li>Interface: PowerLevel
         * <li>Type: Boolean
         * </ul>
         */
        INVERTED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Returns Channel for the Read-Only Pwm Power Level.
     *
     * @return the Channel
     */
    default Channel<Integer> getReadPwmPowerLevelChannel() {
        return this.channel(ChannelId.READ_POWER_LEVEL);
    }

    /**
     * Returns Configuration WriteChannel for Pwm PowerLevel.
     *
     * @return the WriteChannel
     */
    default WriteChannel<Integer> getWritePwmPowerLevelChannel() {
        return this.channel(ChannelId.WRITE_POWER_LEVEL);
    }

    /**
     * Return the Inversion Status Channel.
     *
     * @return the WriteChannel
     */
    default WriteChannel<Boolean> getInvertedStatus() {
        return this.channel(ChannelId.INVERTED);
    }

    /**
     * Returns current Power Level.
     *
     * @return Integer PowerLevel
     */
    default float getPowerLevelValue() {
        if (this.getReadPwmPowerLevelChannel().value().isDefined()) {
            return (float) this.getReadPwmPowerLevelChannel().value().get() / 10;
        } else if (this.getReadPwmPowerLevelChannel().getNextValue().isDefined()) {
            return (float) this.getReadPwmPowerLevelChannel().getNextValue().get() / 10;
        } else {
            return -9001;
        }
    }
}
