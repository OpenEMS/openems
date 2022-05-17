package io.openems.edge.evcs.easee.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This interface contains the channels used for the AMQP communication.
 * These Channels will be translated to fit in the Evcs/ManagedEvcs Interface.
 */
public interface Easee extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Sets the Maximum allowed Current the Station can charge with.
         * <ul>
         * <li>Interface: Easee
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        MAXIMUM_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Current Power being drawn.
         * <ul>
         * <li>Interface: Easee
         * <li>Type: Integer
         * <li>Unit: W
         * </ul>
         */
        APPARENT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L1.
         * <ul>
         * <li>Interface: Easee
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L2.
         * <ul>
         * <li>Interface: Easee
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L3.
         * <ul>
         * <li>Interface: Easee
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link Easee.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getMaximumChargeCurrentChannel() {
        return this.channel(ChannelId.MAXIMUM_CHARGE_CURRENT);
    }

    /**
     * Gets the Value of {@link Easee.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @return the value
     */
    default int getMaximumChargeCurrent() {
        WriteChannel<Integer> channel = this.getMaximumChargeCurrentChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0));
    }

    /**
     * Sets the Value of {@link Easee.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @param value the new value
     */
    default void setMaximumChargeCurrent(int value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Integer> channel = this.getMaximumChargeCurrentChannel();
        channel.setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link Easee.ChannelId#CURRENT_L1}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL1Channel() {
        return this.channel(ChannelId.CURRENT_L1);
    }

    /**
     * Gets the Value of {@link Easee.ChannelId#CURRENT_L1}.
     *
     * @return the value
     */
    default float getCurrentL1() {
        Channel<Float> channel = this.getCurrentL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Easee.ChannelId#CURRENT_L2}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL2Channel() {
        return this.channel(ChannelId.CURRENT_L2);
    }

    /**
     * Gets the Value of {@link Easee.ChannelId#CURRENT_L2}.
     *
     * @return the value
     */
    default float getCurrentL2() {
        Channel<Float> channel = this.getCurrentL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Easee.ChannelId#CURRENT_L3}.
     *
     * @return the Channel
     */
    default Channel<Float> getCurrentL3Channel() {
        return this.channel(ChannelId.CURRENT_L3);
    }

    /**
     * Gets the Value of {@link Easee.ChannelId#CURRENT_L3}.
     *
     * @return the value
     */
    default float getCurrentL3() {
        Channel<Float> channel = this.getCurrentL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0.f));
    }

    /**
     * Gets the Channel for {@link Easee.ChannelId#APPARENT_POWER}.
     *
     * @return the Channel
     */
    default Channel<Integer> getApparentPowerChannel() {
        return this.channel(ChannelId.APPARENT_POWER);
    }

    /**
     * Gets the Value of {@link Easee.ChannelId#APPARENT_POWER}.
     *
     * @return the value
     */
    default int getApparentPower() {
        Channel<Integer> channel = this.getApparentPowerChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

}

