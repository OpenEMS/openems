package io.openems.edge.heatsystem.components;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This Nature provides Channel and Methods for a HeatsystemComponent, such as a Valve or a Pump.
 * You can set e.g. SetPointPowerLevel or change a HeatsystemComponent by a Percentage Value etc etc.
 */
public interface HeatsystemComponent extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Current Power Level Of a HeatsystemComponent.
         * <ul>
         *     <li>Interfce: HeatsystemComponent
         *     <li>Type: Double
         *     <li>Unit: Percentage
         *     <li>Indicates the Current Power Level.
         *     </ul>
         */
        CURRENT_POWER_LEVEL(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT)),

        /**
         * PowerLevel Goal. == Future PowerLevel that needs to be reached.
         *
         * <ul>
         * <li>Interface: HeatsystemComponent
         * <li>Type: Double
         * <li> Unit: Percentage
         * </ul>
         */

        FUTURE_POWER_LEVEL(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),

        /**
         * LastPowerLevel. The prev. PowerLevel
         *
         * <ul>
         * <li>Interface: HeatsystemComponent
         * <li>Type: Double
         * <li>Unit: Percentage
         * </ul>
         */

        LAST_POWER_LEVEL(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT)),
        /**
         * Tells if the Device is Busy or not. Usually happens when valve get's force Closed or opened.
         *
         * <ul>
         * <li> Interface: HeatsystemComponent
         * <li>Type: Boolean
         * </ul>
         */

        BUSY(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Set Power Level of e.g. Valve.
         * <ul>
         *     <li> Interface: HeatsystemComponent
         *     <li> Type: Integer
         * </ul>
         */
        SET_POINT_POWER_LEVEL(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(channel ->
                ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue))),

        /**
         * Reset the HeatsystemComponent e.g. closes the Valve.
         * <ul>
         *     <li> Interface: HeatsystemComponent
         *     <li> Type: Boolean
         * </ul>
         */

        RESET(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Forces the HeatsystemComponent to run at full power.
         * Note: This has to be written constantly.
         * <ul>
         *     <li>Interface: HeatsystemComponent
         *     <li>Type: Boolean
         * </ul>
         */
        FORCE_FULL_POWER(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        /**
         * Maximum value in % the Valve is allowed to be open.
         *
         * <ul>
         * <li> Interface: HeatsystemComponent
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        MAX_ALLOWED_VALUE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Minimum value in % the Valve has to be open.
         *
         * <ul>
         * <li> Interface: HeatsystemComponent
         * <li>Type: Boolean
         * <li> Unit: none
         * </ul>
         */

        MIN_ALLOWED_VALUE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),


        /**
         * How Long does the Device need to do something(e.g. Valve Opening/Closing time)
         *
         * <ul>
         * <li> Interface: HeatsystemComponent
         * <li>Type: Double
         * </ul>
         */

        TIME(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_ONLY).unit(Unit.SECONDS));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }


    // ---------------------- ----------- ---------------------- //
    // ---------------------- POWER LEVEL ---------------------- //
    // ---------------------- ----------- ---------------------- //

    /**
     * .
     * <ul>
     * <li> Tells how much percent of the Device is used aka how much the valve is opened or
     *      how much % of the Pump is on a high/low.
     * <li> Unit: Double
     * </ul>
     *
     * @return the Channel
     */

    default Channel<Double> getPowerLevelChannel() {
        return this.channel(ChannelId.CURRENT_POWER_LEVEL);
    }

    /**
     * Get the Current PowerLevel Value.
     *
     * @return the current PowerLevel
     */
    default Double getPowerLevelValue() {
        Double currentPowerLevel = (Double) this.getCurrentValueOfChannel(this.getPowerLevelChannel());
        if (currentPowerLevel == null) {
            currentPowerLevel = (Double) this.getNextValueOfChannel(this.getPowerLevelChannel());
        }
        if (currentPowerLevel == null) {
            currentPowerLevel = 0.d;
        }
        return currentPowerLevel;
    }

    // ---------------------- ----------- ---------------------- //
    // ------------------- LAST POWER LEVEL -------------------- //
    // ---------------------- ----------- ---------------------- //

    /**
     * Get the LastPowerLevel Channel of the HeatsystemComponent.
     *
     * @return the Channel
     */

    default Channel<Double> getLastPowerLevelChannel() {
        return this.channel(ChannelId.LAST_POWER_LEVEL);
    }

    /**
     * Get the LastPowerLevel value of the HeatsystemComponent.
     *
     * @return the lastPowerLevelValue
     */
    default double getLastPowerLevelValue() {
        Double lastPowerValue = (Double) this.getValueOfChannel(this.getLastPowerLevelChannel());
        if (lastPowerValue == null) {
            lastPowerValue = 0.d;
        }
        return lastPowerValue;
    }

    // ---------------------- ----------- ---------------------- //
    // -------------------- SET POWER LEVEL -------------------- //
    // ---------------------- ----------- ---------------------- //

    /**
     * Get the Channel to set a Power Level.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> setPointPowerLevelChannel() {
        return this.channel(ChannelId.SET_POINT_POWER_LEVEL);
    }

    /**
     * Set the Valve PowerLevel by a Percent Value.
     *
     * @return the SetPoint Value
     */
    default int setPointPowerLevelValue() {
        Integer powerLevelValue = (Integer) this.getValueOfChannel(this.setPointPowerLevelChannel());
        if (powerLevelValue == null) {
            powerLevelValue = -1;
        }
        return powerLevelValue;
    }
    // ---------------------- ----------- ---------------------- //
    // ------------------- FUTURE POWER LEVEL ------------------ //
    // ---------------------- ----------- -----------------------//

    /**
     * Future PowerLevel. Will be set by the HeatsystemComponents after "ChangeByPercentage" method is called.
     *
     * @return the channel
     */
    default Channel<Double> futurePowerLevelChannel() {
        return this.channel(ChannelId.FUTURE_POWER_LEVEL);
    }

    /**
     * Get the futurePowerLevel Value.
     *
     * @return the futurePowerLevel.
     */
    default double getFuturePowerLevelValue() {
        Double futurePowerLevel = (Double) this.getValueOfChannel(this.futurePowerLevelChannel());
        if (futurePowerLevel == null) {
            futurePowerLevel = 0.d;
        }
        return futurePowerLevel;
    }

    // ---------------------- ----------- ---------------------- //
    // ----------------------- MAX VALUE ----------------------- //
    // ---------------------- ----------- ---------------------- //

    /**
     * The Max Value Channel. This tells the Component the Maximum allowed value to apply.
     * Usually a HeatsystemComponent regulates itself if the maxAllowedValue is defined.
     *
     * @return the Channel.
     */

    default WriteChannel<Double> maxValueChannel() {
        return this.channel(HeatsystemComponent.ChannelId.MAX_ALLOWED_VALUE);
    }

    /**
     * Get the maxValue if it is defined (needs to be written every cycle). Otherwise return null.
     *
     * @return the Max allowed Value
     */
    default Double getMaxAllowedValue() {
        return this.maxValueChannel().getNextWriteValueAndReset().orElse(null);

    }
    // ---------------------- ----------- ---------------------- //
    // ----------------------- MIN VALUE ----------------------- //
    // ---------------------- ----------- ---------------------- //

    /**
     * The Min Value Channel. This tells the Component the Minimum allowed value to apply.
     * Usually a HeatsystemComponent regulates itself if the minAllowedValue is defined.
     *
     * @return the Channel.
     */

    default WriteChannel<Double> minValueChannel() {
        return this.channel(HeatsystemComponent.ChannelId.MIN_ALLOWED_VALUE);
    }

    /**
     * Get the minValue if it is defined (needs to be written every cycle). Otherwise return null.
     *
     * @return the Min allowed Value.
     */
    default Double getMinAllowedValue() {
        return this.minValueChannel().getNextWriteValueAndReset().orElse(null);

    }

    // ---------------------- ----------- ---------------------- //
    // ------------------------ IS BUSY ------------------------ //
    // ---------------------- ----------- ---------------------- //


    /**
     * Get the IsBusy Channel. This tells any Calling component if the value can be applied and handled.
     *
     * @return the Channel
     */

    default Channel<Boolean> getIsBusyChannel() {
        return this.channel(ChannelId.BUSY);
    }

    /**
     * Get if the HeatsystemComponent is busy or not.
     * Therefore a Calling component can tell if the value can be applied/handled.
     *
     * @return a Boolean.
     */

    default boolean isBusy() {
        Boolean isBusy = (Boolean) this.getValueOfChannel(this.getIsBusyChannel());
        if (isBusy == null) {
            isBusy = false;
        }
        return isBusy;
    }

    // ---------------------- ----------- ---------------------- //
    // ------------------------- RESET ------------------------- //
    // ---------------------- ----------- ---------------------- //


    /**
     * Ability to Reset a HeatsystemComponent.
     *
     * @return the Channel.
     */
    default WriteChannel<Boolean> getResetChannel() {
        return this.channel(ChannelId.RESET);
    }

    /**
     * Get the Reset Channel and resets it, this is used internally for the HeatsystemComponents.
     * You shouldn't call this method outside of the HeatsystemComponents.
     *
     * @return a boolean.
     */

    default boolean getResetValueAndResetChannel() {
        return this.getResetChannel().getNextWriteValueAndReset().orElse(false);
    }
    // ---------------------- ----------- ---------------------- //
    // ------------------------- FORCE ------------------------- //
    // ---------------------- ----------- ---------------------- //

    /**
     * Get the Force Full Power Channel. If the nextWriteValue is true -> e.g. :This will open a valve completely or
     * set a Pump to 100%
     *
     * @return the channel.
     */
    default WriteChannel<Boolean> getForceFullPowerChannel() {
        return this.channel(ChannelId.FORCE_FULL_POWER);
    }

    /**
     * Get the nextWriteValue and reset the channel. This allows HeatPump/Valve/HeatsystemComponents in general to
     * force the activation
     *
     * @return true if nextWriteValue is true else false
     */

    default boolean getForceFullPowerAndResetChannel() {
        return this.getForceFullPowerChannel().getNextWriteValueAndReset().orElse(false);
    }

    // ---------------------- ----------- ---------------------- //
    // ------------------------- TIME -------------------------- //
    // ---------------------- ----------- ---------------------- //

    /**
     * Tells how much time is needed for e.g. Valve to Open or Close 100%.
     *
     * @return the Channel
     */
    default Channel<Double> timeChannel() {
        return this.channel(ChannelId.TIME);
    }

    /**
     * Tells how much time is needed to do something within a HeatsystemComponent. (Most likely apply the PowerLevel)
     *
     * @return the Time needed to fulfill HeatsystemComponent task/Reach PowerLevel.
     */

    default Double timeNeeded() {
        Channel<Double> timeNeededChannel = this.timeChannel();
        Double timeNeeded = (Double) this.getCurrentValueOfChannel(timeNeededChannel);
        if (timeNeeded == null) {
            timeNeeded = (Double) this.getNextValueOfChannel(timeNeededChannel);
        }
        if (timeNeeded == null) {
            timeNeeded = 1.d;
        }
        return timeNeeded;
    }


    /**
     * Returns the Value of a Channel, either by getting current Value or on null the next Value
     * if the next Value is also null, calling methods have to apply to this occurrence.
     *
     * @param channel the given Channel
     * @return the Value of the Channel.
     */
    default Object getValueOfChannel(Channel<?> channel) {
        Object value = this.getCurrentValueOfChannel(channel);
        if (value == null) {
            value = this.getNextValueOfChannel(channel);
        }
        return value;
    }

    /**
     * Get the current Value of the Channel.
     *
     * @param requestedChannel given Channel
     * @return the Value.
     */
    default Object getCurrentValueOfChannel(Channel<?> requestedChannel) {
        return requestedChannel.value().get();
    }

    /**
     * Get the Next Value of the Channel.
     *
     * @param requestedChannel the given Channel
     * @return the next Value.
     */
    default Object getNextValueOfChannel(Channel<?> requestedChannel) {
        return requestedChannel.getNextValue().get();
    }

    /**
     * Called internally or by other Components. Tells the calling device if the HeatsystemComponent is ready to apply any Changes.
     *
     * @return a Boolean
     */
    boolean readyToChange();

    /**
     * Usually used internally but can be used by calling Components/Controller.
     * Changes the Current Powerlevel by given Percentage. Can be positive or negative.
     * The reached Powerlevel can be at most 100 and at least 0 but can be modified by the Max/Min Value.
     *
     * @param percentage the percentage to change the HeatsystemComponent by this value.
     * @return true on success
     */
    boolean changeByPercentage(double percentage);
}
