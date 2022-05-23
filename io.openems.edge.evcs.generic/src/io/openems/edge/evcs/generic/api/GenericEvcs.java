package io.openems.edge.evcs.generic.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This interface contains the channels used for the Modbus communication.
 * These Channels will be translated to fit in the Evcs/ManagedEvcs Interface.
 */
public interface GenericEvcs extends OpenemsComponent {


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Sets the Maximum allowed Current the Station can charge with.
         * The value is in 100mA so 130 in the Channel means 13A in the Station.
         * <ul>
         * <li>Interface: GenericEvcs
         * <li>Type: Short
         * <li>Unit: A
         * </ul>
         */
        MAXIMUM_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_WRITE)),
        /**
         * The Current Status of the EV.
         * See WallbeStatus for more info.
         * <ul>
         * <li>Interface: GenericEvcs
         * <li>Type: String
         * <li>Unit: Na
         * </ul>
         */
        GENERIC_STATUS(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L1.
         * <ul>
         * <li>Interface: GenericEvcs
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L2.
         * <ul>
         * <li>Interface: GenericEvcs
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current on Phase L3.
         * <ul>
         * <li>Interface: GenericEvcs
         * <li>Type: Integer
         * <li>Unit: A
         * </ul>
         */
        CURRENT_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
        /**
         * Current Power being drawn.
         * <ul>
         * <li>Interface: GenericEvcs
         * <li>Type: Integer
         * <li>Unit: W
         * </ul>
         */
        APPARENT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
        ;
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link GenericEvcs.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> getMaximumChargeCurrentChannel() {
        return this.channel(ChannelId.MAXIMUM_CHARGE_CURRENT);
    }

    /**
     * Gets the Value of {@link GenericEvcs.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @return the value
     */
    default int getMaximumChargeCurrent() {
        WriteChannel<Integer> channel = this.getMaximumChargeCurrentChannel();
        return channel.value().orElse(channel.getNextWriteValue().orElse(0));
    }

    /**
     * Sets the Value of {@link GenericEvcs.ChannelId#MAXIMUM_CHARGE_CURRENT}.
     *
     * @param value the new value
     */
    default void setMaximumChargeCurrent(int value) throws OpenemsError.OpenemsNamedException {
        WriteChannel<Integer> channel = this.getMaximumChargeCurrentChannel();
        channel.setNextWriteValue(value);
    }

    /**
     * Gets the Channel for {@link GenericEvcs.ChannelId#GENERIC_STATUS}.
     *
     * @return the Channel
     */
    default Channel<String> getGenericEvcsStatusChannel() {
        return this.channel(ChannelId.GENERIC_STATUS);
    }

    /**
     * Gets the Value of {@link GenericEvcs.ChannelId#GENERIC_STATUS}.
     *
     * @return the value
     */
    default String getGenericEvcsStatus() {
        Channel<String> channel = this.getGenericEvcsStatusChannel();
        return channel.value().orElse(channel.getNextValue().orElse("0"));
    }


    /**
     * Gets the Channel for {@link GenericEvcs.ChannelId#CURRENT_L1}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL1Channel() {
        return this.channel(ChannelId.CURRENT_L1);
    }

    /**
     * Gets the Value of {@link GenericEvcs.ChannelId#CURRENT_L1}.
     *
     * @return the value
     */
    default int getCurrentL1() {
        Channel<Integer> channel = this.getCurrentL1Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link GenericEvcs.ChannelId#CURRENT_L2}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL2Channel() {
        return this.channel(ChannelId.CURRENT_L2);
    }

    /**
     * Gets the Value of {@link GenericEvcs.ChannelId#CURRENT_L2}.
     *
     * @return the value
     */
    default int getCurrentL2() {
        Channel<Integer> channel = this.getCurrentL2Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link GenericEvcs.ChannelId#CURRENT_L3}.
     *
     * @return the Channel
     */
    default Channel<Integer> getCurrentL3Channel() {
        return this.channel(ChannelId.CURRENT_L3);
    }

    /**
     * Gets the Value of {@link GenericEvcs.ChannelId#CURRENT_L3}.
     *
     * @return the value
     */
    default int getCurrentL3() {
        Channel<Integer> channel = this.getCurrentL3Channel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }

    /**
     * Gets the Channel for {@link GenericEvcs.ChannelId#APPARENT_POWER}.
     *
     * @return the Channel
     */
    default Channel<Integer> getApparentPowerChannel() {
        return this.channel(ChannelId.APPARENT_POWER);
    }

    /**
     * Gets the Value of {@link GenericEvcs.ChannelId#APPARENT_POWER}.
     *
     * @return the value
     */
    default int getApparentPower() {
        Channel<Integer> channel = this.getApparentPowerChannel();
        return channel.value().orElse(channel.getNextValue().orElse(0));
    }


}

