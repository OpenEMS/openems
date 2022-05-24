package io.openems.edge.consolinno.evcs.limiter;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This interface holds the Channels of the Limiter for the Power_Limit and communication for MQTT etc.
 */
public interface EvcsLimiterPower extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * This provides the Channel to give the EVCS limiter a new Power Limit value.
         * <ul>
         * <li>Interface: PowerLimitChannel
         * <li>Type: Integer
         * <li>Unit: Watt
         * </ul>
         */
        POWER_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        /**
         * Current Power being drawn by all EVCS.
         * <ul>
         * <li>Interface: PowerLimitChannel
         * <li>Type: Integer
         * <li>Unit: Watt
         * </ul>
         */
        CURRENT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        /**
         * Current Power being drawn by all EVCS and other devices on the meter.
         * <ul>
         * <li>Interface: PowerLimitChannel
         * <li>Type: Integer
         * <li>Unit: Watt
         * </ul>
         */
        CURRENT_POWER_WITH_OFFSET(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        /**
         * Free Resources of the limiter.
         * For debugging use only.
         */
        FREE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        /**
         * Number of active evcs.
         */
        ACTIVE(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)),
        ;
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Integer> getCurrentPowerChannel() {
        return this.channel(ChannelId.CURRENT_POWER);
    }

    default void setCurrentPower(int power) {
        this.getCurrentPowerChannel().setNextValue(power);
    }

    default Channel<Integer> getCurrentPowerWithOffsetChannel() {
        return this.channel(ChannelId.CURRENT_POWER_WITH_OFFSET);
    }

    default void setCurrentPowerWithOffset(int power) {
        this.getCurrentPowerWithOffsetChannel().setNextValue(power);
    }

    default Channel<Integer> getPowerLimitChannel() {
        return this.channel(ChannelId.POWER_LIMIT);
    }

    default int getPowerLimitValue() {
        if (this.getPowerLimitChannel().value().isDefined()) {
            return this.getPowerLimitChannel().value().get();
        } else if (this.getPowerLimitChannel().getNextValue().isDefined()) {
            return this.getPowerLimitChannel().getNextValue().orElse(0);
        } else {
            return -1;
        }
    }

    default void setPowerLimit(int limit) {
        this.getPowerLimitChannel().setNextValue(limit);
    }
    default Channel<Integer> getFreePowerChannel() {
        return this.channel(ChannelId.FREE_POWER);
    }
    default void setFreePower(int limit) {
        this.getFreePowerChannel().setNextValue(limit);
    }
    default Channel<Integer> getActive() {
        return this.channel(ChannelId.ACTIVE);
    }
    default void setActive(int limit) {
        this.getActive().setNextValue(limit);
    }
}

