package io.openems.edge.io.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Pwm extends OpenemsComponent {

    int MAX_THOUSANDTH_VALUE = 1000;
    int MIN_THOUSANDTH_VALUE = 0;
    int MISSING_THOUSANDTH_VALUE = -9001;

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * * How much of the low/highFlank the device uses. e.g. 80% power --> like a dimming light and
         * it's "level" of brightness.
         * <ul>
         * <li>Interface: PowerLevel
         * <li>Type: Integer
         * </ul>
         */
        WRITE_POWER_LEVEL_THOUSANDTH(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_WRITE)),
        READ_POWER_LEVEL_THOUSANDTH(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH).accessMode(AccessMode.READ_ONLY)), //
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
        return this.channel(ChannelId.READ_POWER_LEVEL_THOUSANDTH);
    }

    /**
     * Returns Configuration WriteChannel for Pwm PowerLevel.
     *
     * @return the WriteChannel
     */
    default WriteChannel<Integer> getWritePwmPowerLevelChannel() {
        return this.channel(ChannelId.WRITE_POWER_LEVEL_THOUSANDTH);
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
     * Returns current Power Level in Percent.
     *
     * @return float PowerLevel
     */
    default float getPowerLevelPercentValue() {
        int thousandthValue = this.getPowerLevelThousandthValue();
        if (thousandthValue == MISSING_THOUSANDTH_VALUE) {
            return MISSING_THOUSANDTH_VALUE;
        } else {
            return ((float) thousandthValue / 10.f);
        }
    }

    /**
     * Returns the current Power Level in Thousandth.
     *
     * @return Power Level in Thousandth or else MISSING_THOUSANDTH_VALUE
     */
    default int getPowerLevelThousandthValue() {
        Channel<Integer> powerLevelThousandth = this.getReadPwmPowerLevelChannel();
        if (powerLevelThousandth.value().isDefined()) {
            return powerLevelThousandth.value().get();
        } else if (powerLevelThousandth.getNextValue().isDefined()) {
            return powerLevelThousandth.getNextValue().get();
        } else {
            return MISSING_THOUSANDTH_VALUE;
        }
    }

    /**
     * Sets the PowerLevel as a Thousandth Value
     *
     * @param thousandth
     * @throws OpenemsError.OpenemsNamedException
     */
    default void setPowerLevelThousandth(int thousandth) throws OpenemsError.OpenemsNamedException {
        this.getWritePwmPowerLevelChannel().setNextWriteValueFromObject(Math.max(MIN_THOUSANDTH_VALUE, Math.min(thousandth, MAX_THOUSANDTH_VALUE)));
    }

    default void setPowerLevelPercent(float percent) throws OpenemsError.OpenemsNamedException {
        this.setPowerLevelThousandth((int) (percent * 10));
    }

    default void setPowerLevelPercent(int percent) throws OpenemsError.OpenemsNamedException {
        this.setPowerLevelThousandth(percent * 10);
    }

}
