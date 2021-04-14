package io.openems.edge.controller.heatnetwork.performancebooster.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeatnetworkPerformanceBooster extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * SetPoint Temperature When the Controller should Activate offset to controller.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Dezidegree Celsius
         * </ul>
         */
        SET_POINT_TEMPERATURE_ACTIVATE_OFFSET(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.DEZIDEGREE_CELSIUS).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                }
        )),
        /**
         * SetPoint Valve Percent Standard. This Percent is standard configuration if no errors occurred but heat demand present.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Percent
         * </ul>
         */
        SET_POINT_VALVE_PERCENT_STANDARD(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                })),
        /**
         * SetPoint Valve Percent Addition. This Percent is added to standard configuration if errors occurred at primary Heater and heat demand present.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Percent
         * </ul>
         */
        SET_POINT_VALVE_PERCENT_ADDITION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                })),

        /**
         * SetPoint Valve Percent Subtraction. This Percent is added to standard configuration if errors occurred at secondary Heater and heat demand present.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Percent
         * </ul>
         */

        SET_POINT_VALVE_PERCENT_SUBTRACTION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                })),
        /**
         * SetPoint Heater Percent Standard. This Percent is standard configuration if no errors occurred but heat demand present.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Percent
         * </ul>
         */
        SET_POINT_HEATER_PERCENT_STANDARD(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                })),
        /**
         * SetPoint Heater Percent Addition. This Percent is added to standard configuration if errors occurred and heat demand present.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Percent
         * </ul>
         */
        SET_POINT_HEATER_PERCENT_ADDITION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                })),

        /**
         * Primary Forward Temperature.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li>Unit: dezidegree celsius
         * </ul>
         */
        WAIT_TILL_START(Doc.of(OpenemsType.INTEGER)),
        /**
         * Primary Forward Temperature.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li>Unit: dezidegree celsius
         * </ul>
         */
        PRIMARY_FORWARD(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS)),
        /**
         * Primary Rewind Temperature.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li>Unit: dezidegree celsius
         * </ul>
         */
        PRIMARY_REWIND(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS)),
        /**
         * Secondary Forward Temperature.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li>Unit: dezidegree celsius
         * </ul>
         */
        SECONDARY_FORWARD(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS)),
        /**
         * Secondary Rewind Temperature.
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li>Unit: dezidegree celsius
         * </ul>
         */
        SECONDARY_REWIND(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS)),

        BOOSTER_ACTIVE(Doc.of(OpenemsType.BOOLEAN) //
                );
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    /**
     * Gets the Temperature in [degree celsius].
     *
     * @return the Channel
     */
    default Channel<Integer> getPrimaryForward() {
        return this.channel(ChannelId.PRIMARY_FORWARD);
    }

    /**
     * Gets the Temperature in [degree celsius].
     *
     * @return the Channel
     */
    default Channel<Integer> getPrimaryRewind() {
        return this.channel(ChannelId.PRIMARY_REWIND);
    }

    /**
     * Gets the Temperature in [degree celsius].
     *
     * @return the Channel
     */
    default Channel<Integer> getSecondaryForward() {
        return this.channel(ChannelId.SECONDARY_FORWARD);
    }

    /**
     * Waiting time to start
     *
     * @return the Channel
     */
    default Channel<Integer> getWaitTillStart() {
        return this.channel(ChannelId.WAIT_TILL_START);
    }

    /**
     * Gets the Temperature in [degree celsius].
     *
     * @return the Channel
     */
    default Channel<Integer> getSecondaryRewind() {
        return this.channel(ChannelId.SECONDARY_REWIND);
    }

    default Channel<Integer> isBoosterActive() {
        return this.channel(ChannelId.BOOSTER_ACTIVE);
    }

    default WriteChannel<Integer> temperatureSetPointOffset() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE_ACTIVATE_OFFSET);
    }

    default WriteChannel<Integer> valveSetPointStandard() {
        return this.channel(ChannelId.SET_POINT_VALVE_PERCENT_STANDARD);
    }

    default WriteChannel<Integer> valveSetPointAddition() {
        return this.channel(ChannelId.SET_POINT_VALVE_PERCENT_ADDITION);
    }

    default WriteChannel<Integer> valveSetPointSubtraction() {
        return this.channel(ChannelId.SET_POINT_VALVE_PERCENT_SUBTRACTION);
    }

    default WriteChannel<Integer> heaterSetPointStandard() {
        return this.channel(ChannelId.SET_POINT_HEATER_PERCENT_STANDARD);
    }

    default WriteChannel<Integer> heaterSetPointAddition() {
        return this.channel(ChannelId.SET_POINT_HEATER_PERCENT_ADDITION);
    }

}
