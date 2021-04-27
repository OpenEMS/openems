package io.openems.edge.heatsystem.components;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.*;
import io.openems.edge.common.component.OpenemsComponent;

public interface PassingChannel extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Current Power Level Of Valve.
         * <ul>
         *     <li>Interfce: PassingChannel
         *     <li>Type: Double
         *     <li>Unit: Percentage
         *     <p> Indicates the Current Power Level.
         *     </ul>
         */
        CURRENT_POWER_LEVEL(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT)),


        // TODO: Why is this a write channel? It does not seem it needs to be. Can it be changed to a read channel if that is sufficient?
        /**
         * PowerLevel Goal. == Future PowerLevel that needs to be reached.
         *
         * <ul>
         * <li>Interface: PassingChannel
         * <li>Type: Double
         * <li> Unit: Percentage
         * </ul>
         */

        POWER_LEVEL_GOAL(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),

        /**
         * LastPowerLevel.
         *
         * <ul>
         * <li>
         * <li>Type: Double
         * <li>Unit: Percentage
         * </ul>
         */

        LAST_POWER_LEVEL(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT)),
        /**
         * Tells if the Device is Busy or not.
         *
         * <ul>
         * <li>Type: Boolean
         * </ul>
         */

        BUSY(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Set Power Level of e.g. Valve.
         * Handled before Controllers.
         * <ul>
         *     <li> Type: Integer
         * </ul>
         */
        SET_POWER_LEVEL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(channel ->
                ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue))),

        /**
         * Reset the Valve.
         * <ul>
         *     <li> Type: Boolean
         * </ul>
         */

        RESET_VALVE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue))),
        /**
         * Maximum value in % the Valve is allowed to be open
         *
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        MAX_VALVE_VALUE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Minimum value in % the Valve has to be open
         *
         * <ul>
         * <li>Interface: HydraulicLineHeaterApi
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        MIN_VALVE_VALUE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),


        /**
         * How Long does the Device need to do something(e.g. Valve Opening/Closing time)
         *
         * <ul>
         * <li>Type: Double
         * </ul>
         */


        TIME(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY).unit(Unit.SECONDS));

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
     * .
     * <ul>
     * <li> Tells how much percent of the Device is used aka how much the valve is opened or
     *      how much % of the Pump is on a high / low.
     * <li> Unit: Double
     * </ul>
     *
     * @return the Channel
     */

    default Channel<Double> getPowerLevel() {
        return this.channel(ChannelId.CURRENT_POWER_LEVEL);
    }

    /**
     * <ul>
     * Same as above, but LastPowerLevel; For calculation purposes and for checking.
     *
     * <li> Type: Double
     * </ul>
     *
     * @return the Channel
     */

    default Channel<Double> getLastPowerLevel() {
        return this.channel(ChannelId.LAST_POWER_LEVEL);
    }

    /**
     * Tells if the PassingDevice is busy or not.
     * <li> Type: Boolean
     *
     * @return the Channel
     */

    default Channel<Boolean> getIsBusy() {
        return this.channel(ChannelId.BUSY);
    }

    /**
     * Set the Valve PowerLevel by a Percent Value.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> setPowerLevelPercent() {
        return this.channel(ChannelId.SET_POWER_LEVEL);
    }

    /**
     * Future PowerLevel.
     *
     * @return the channel
     */
    default WriteChannel<Double> setGoalPowerLevel() {
        return this.channel(ChannelId.POWER_LEVEL_GOAL);
    }

    /**
     * Resets Valve if set to true
     *
     * @return this Channel
     */
    default WriteChannel<Boolean> shouldForceClose() {
        return this.channel(ChannelId.RESET_VALVE);
    }


    /**
     * Tells how much time is needed for e.g. Valve to Open or Close 100%.
     * <li> Type: Double
     *
     * @return the Channel
     */


    default Channel<Double> getTimeNeeded() {
        return this.channel(ChannelId.TIME);
    }

    default Double getCurrentPowerLevelValue() {
        Double currentPowerLevel = (Double) this.getValueOfChannel(this.getPowerLevel());
        if (currentPowerLevel == null) {
            currentPowerLevel = (Double) this.getNextValueOfChannel(this.getPowerLevel());
        }
        if (currentPowerLevel == null) {
            currentPowerLevel = -1.d;
        }
        return currentPowerLevel;
    }

    default Object getValueOfChannel(Channel<?> requestedChannel) {
        return requestedChannel.value().get();
    }

    default Object getNextValueOfChannel(Channel<?> requestedChannel) {
        return requestedChannel.getNextValue().get();
    }
    default WriteChannel<Double> maxValue() {
        return this.channel(PassingChannel.ChannelId.MAX_VALVE_VALUE);
    }

    default WriteChannel<Double> minValue() {
        return this.channel(PassingChannel.ChannelId.MIN_VALVE_VALUE);
    }

    default Double getMaxValue() {
        if (maxValue().getNextWriteValue().isPresent()) {
            return maxValue().getNextWriteValueAndReset().get();
        }
        return null;
    }

    default Double getMinValue() {
        if (minValue().getNextWriteValue().isPresent()) {
            return minValue().getNextWriteValueAndReset().get();
        }
        return null;
    }
}
