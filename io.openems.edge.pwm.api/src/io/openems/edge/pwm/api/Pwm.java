package io.openems.edge.pwm.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Pwm extends OpenemsComponent {

    /**
     * * How much of the low/highFlank the device uses. e.g. 80% power --> like a dimming light and
     * it's "level" of brightness.
     * <ul>
     * <li>Interface: PowerLevel
     * <li>Type: Float
     * <li>Unit: Percent
     * </ul>
     */
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        WRITE_POWER_LEVEL(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
        READ_POWER_LEVEL(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)), //
        INVERTED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Float> getReadPwmPowerLevelChannel() {
        return this.channel(ChannelId.READ_POWER_LEVEL);
    }

    default WriteChannel<Float> getWritePwmPowerLevelChannel() {
        return this.channel(ChannelId.WRITE_POWER_LEVEL);
    }

    default WriteChannel<Boolean> getInvertedStatus() {
        return this.channel(ChannelId.INVERTED);
    }

    default float getPowerLevelValue() {
        if (this.getReadPwmPowerLevelChannel().value().isDefined()) {
            return this.getReadPwmPowerLevelChannel().value().get();
        } else if (this.getReadPwmPowerLevelChannel().getNextValue().isDefined()) {
            return this.getReadPwmPowerLevelChannel().getNextValue().get();
        } else {
            return -9001;
        }
    }
}

