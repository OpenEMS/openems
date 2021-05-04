package io.openems.edge.thermometer.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;


/**
 * The Nature of the ThermometerThreshold, an expansion of the Thermometer, helps other controller and Heatsystem-Components
 * to decide what to do on changing temperatures, preventing fluctuations.
 * The Threshold can be overwritten.
 * The ThermometerState represents rising or falling in temperature.
 * The TemperatureSetPoint can be set by a component and deposit it's id. The temperature will be locked.
 * However it is possible to overwrite the Temperature with any device, if the requested Temperature is above the current SetPoint.
 * If you need to implement a cooling system where a lower Temperature should always be accepted, you need to implement that yourself.
 * The Temperature is stored in SetPointTemperature.
 */
public interface ThermometerThreshold extends Thermometer {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Threshold.
         * Set the threshold for TemperatureSettings.
         * <ul>
         * <li>Interface: ThermometerThreshold
         * <li>Type: Integer
         * <li>Unit: Dezidegree celsius
         * </ul>
         */
        THRESHOLD(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE).onInit(
                        channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
                )),
        /**
         * State of the Thermometer -> rise or fall; depending on regression values --> Threshold.
         * <ul>
         *     <li>Interface ThermometerThreshold
         *     <li>Type: String
         * </ul>
         */
        THERMOMETER_STATE(Doc.of(ThermometerState.values())),
        /**
         * Check which component sets the most recent setPoint Temperature. Only Higher SetPointTemperatures will be accepted.
         * The Component with the latest setPoint set can only reset the setPoint.
         * <ul>
         * * <li>Interface: ThermometerThreshold
         * * <li>Type: String
         * * </ul>
         */
        SET_POINT_TEMPERATURE_SET_BY_ID(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * This is the Temperature you want to compare to the Current Threshold etc. e.g.
         * * Is my Temperature (600dC) beneath or above my thermometer to react correctly
         * TemperatureFluctuations will be calculated via threshold etc.
         * Alternatively you can use thermometerAboveGivenTemperature/thermometerBelowGivenTemperature to check
         * if the Temperature is beneath/above your temperature you want to compare it to.
         * <ul>
         *          <li>Interface: ThermometerThreshold
         *          <li>Type: Integer
         *          <li> Unit: Dezidegree_Celsius
         *          </ul>
         */
        SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        ));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }


    // ------------------ THRESHOLD ---------------- //

    /**
     * get ThresholdTemperatureChannel. Only for the Owning component! Never call channel directley
     *
     * @return the channel.
     */

    default WriteChannel<Integer> getThresholdChannel() {
        return this.channel(ChannelId.THRESHOLD);
    }

    /**
     * Get The Thresholdvalue of the Channel or 1 on null.
     *
     * @return the Threshold.
     */
    default int getThreshold() {
        Integer thresholdValue = (Integer) this.getValueOfChannel(this.getThresholdChannel());
        if (thresholdValue == null) {
            thresholdValue = (Integer) this.getNextValueOfChannel(this.getThresholdChannel());
        }
        if (thresholdValue == null) {
            thresholdValue = 1;
        }
        return thresholdValue;
    }

    /**
     * Set the Threshold to new value in dC.
     *
     * @param threshold the new Value for threshold.
     */
    default void setThresholdInDecidegree(int threshold) {
        this.getThresholdChannel().setNextValue(threshold);
    }

    // --------------------------------------------------------------//

    //-----------------ThermometerState---------------------//

    /**
     * Get the ThermometerState Channel.
     *
     * @return the channel.
     */

    default Channel<ThermometerState> getThermometerStateChannel() {
        return this.channel(ChannelId.THERMOMETER_STATE);
    }

    /**
     * get the ThermometerState or undefined on null.
     *
     * @return the ThermometerState.
     */

    default ThermometerState getThermometerState() {
        ThermometerState thermometerState = (ThermometerState) this.getValueOfChannel(this.getThermometerStateChannel());
        if (thermometerState == null) {
            thermometerState = (ThermometerState) this.getNextValueOfChannel(this.getThermometerStateChannel());
        }
        if (thermometerState != null) {
            return thermometerState;
        } else {
            return ThermometerState.UNDEFINED;
        }
    }

    /**
     * Set the ThermometerState, depending on Falling or Rising Temperatures. will be decided by the ThresholdThermometer.
     *
     * @param state the ThermometerState.
     */
    default void setThermometerState(ThermometerState state) {
        this.getThermometerStateChannel().setNextValue(state);
    }
    //--------------------------------------------------------//


    //-----------------Set Point Temperature-------------//

    /**
     * get the SetPoint Temperature Channel.
     *
     * @return the Channel.
     */

    default WriteChannel<Integer> getSetPointTemperatureChannel() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE);
    }

    /**
     * Returns the setPoint Temperature or MinValue on Null.
     *
     * @return the SetPointTemperature.
     */
    default int getSetPointTemperature() {
        Integer setPointTemperature = (Integer) this.getValueOfChannel(this.getSetPointTemperatureChannel());
        if (setPointTemperature == null) {
            setPointTemperature = (Integer) this.getNextValueOfChannel(this.getSetPointTemperatureChannel());
        }
        if (setPointTemperature == null) {
            setPointTemperature = Integer.MIN_VALUE;
        }
        return setPointTemperature;
    }


    //----------------------------------------------------//


    //------------- Set Point Temperature Id ------------ //

    /**
     * Get the Channel to get the ID who set the TemperatureSetPoint.
     *
     * @return the Channel.
     */
    default WriteChannel<String> getSetPointTemperatureSetByIdChannel() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE_SET_BY_ID);
    }

    /**
     * Get the Id who set the TemperatureSetPoint.
     *
     * @return the Id.
     */
    default String getSetPointTemperatureSetById() {
        String id = (String) this.getValueOfChannel(this.getSetPointTemperatureSetByIdChannel());
        if (id != null) {
            return id;
        } else {
            return (String) this.getNextValueOfChannel(this.getSetPointTemperatureSetByIdChannel());
        }
    }
    //------------------------------------------------------------------//

    /**
     * Accept always > temperautres, if < temperature is given,
     * calling Component has to be in "GuideValueSetById".
     * EXAMPLE:
     * current GuideValue is 400 dC and set By CommunicationMaster0
     * now CommunicationMaster1 wants to set 450dC
     * This will be accepted bc 450>400
     * And CommunicationMaster1 is now in the "GuideValueSetById".
     * <p>
     * After that CommunicationMaster0 wants to set the GuideValue to 420 blaze it
     * That's not possible bc 420<450 and Id is CommunicationMaster1.
     * If CommunicationMaster1 "Releases" it's Id --> CommunicationMaster0 can Set their Temperature and ID
     * </p>
     *
     * @param temperature the temperature in dC
     * @param id          the Id of the calling component
     */
    default void setSetPointTemperature(int temperature, String id) {
        String setPointId = this.getSetPointTemperatureSetById();
        int setPointTemperature = this.getSetPointTemperature();
        if (setPointId == null || temperature > setPointTemperature || setPointId.equals(id)) {
            this.getSetPointTemperatureChannel().setNextValue(temperature);
            this.getSetPointTemperatureSetByIdChannel().setNextValue(id);
        }
    }

    /**
     * Activates the SetPoint Temperature as well as setting the ID and the SetPointTemperature as well.
     * Only possible if either the id is equal to the "owning" setpointTemperatureId OR is null OR
     *
     * @param temperature the new setpoint  temperature
     * @param id          the id of the calling component.
     */
    default void setSetPointTemperatureAndActivate(int temperature, String id) {
        if (this.getSetPointTemperatureSetById() == null || this.getSetPointTemperatureSetById().equals(id) || temperature > this.getSetPointTemperature()) {
            this.getSetPointTemperatureChannel().setNextValue(temperature);
            this.getSetPointTemperatureSetByIdChannel().setNextValue(id);
        }
    }

    /**
     * Set Your Id to null if you're the current "Owner" of the SetPointTemperature.
     *
     * @param id id of the calling component
     */
    default void releaseSetPointTemperatureId(String id) {
        String setPointTemperatureSetById = this.getSetPointTemperatureSetById();
        if (setPointTemperatureSetById == null || setPointTemperatureSetById.equals(id)) {
            this.getSetPointTemperatureSetByIdChannel().setNextValue(null);
        }
    }

    /**
     * Important: If Temperature is rising --> "Greater than" setpoint temperature is still ok if the current Temperature
     * is greater than or equal to the setpoint.
     * <p>
     * Example: Temperature is rising: 400 dc 450 dc 500dc Set point is 500 dC.
     * --> Since the temperature is rising --> 500dC of thermometer will be equal or greater than setpoint so return true.
     * However if the temperature is falling ---> 600dC --> 550 dC 500 dC and the Setpoint is 500 -->
     * The Temperature will only be checked if it's greater not greater than or equals (> instead of >=)
     * Since the expected temperature will be falling further.
     * </p>
     * <p>
     * This will be Equivalent to "SetPointTemperatureBelowThermometer"
     * </p>
     *
     * @return a boolean: result -> greaterOrEquals than Setpoint /greater than Setpoint
     */

    default boolean setPointTemperatureAboveThermometer() {
        return this.thermometerAboveGivenTemperature(this.getSetPointTemperature());
    }

    /**
     * Important: If Temperature is falling --> "less than" setpoint temperature is still ok if the current Temperature
     * is less than or equal to the setpoint.
     * <p>
     * Example: Temperature is falling: 500 dc 450 dc 400dc Set point is 400 dC.
     * --> Since the temperature is falling --> 400 of thermometer will be equal or greater than setpoint so return true.
     * However if the temperature is rising ---> 500 --> 550 dC 600 dC and the Setpoint is 600 -->
     * The Temperature will only be checked if it's less not less than or equals (< instead of <=)
     * Since the expected temperature will be rising further.
     * </p>
     * <p>
     * This will be Equivalent to "SetPointTemperatureBelowThermometer"
     * </p>
     *
     * @return a boolean: result -> lessOrEquals than setpoint /less than setpoint
     */
    default boolean setPointTemperatureBelowThermometer() {
        return this.thermometerBelowGivenTemperature(this.getSetPointTemperature());
    }

    /**
     * Important: If Temperature is falling --> "less than" given temperature is still ok if the current Temperature
     * is less than or equal to the given Temperature.
     * <p>
     * Example: Temperature is falling: 500 dc 450 dc 400dc Set point is 400 dC.
     * --> Since the temperature is falling --> 400 of thermometer will be equal or greater than given so return true.
     * However if the temperature is rising ---> 500 --> 550 dC 600 dC and the given is 600 -->
     * The Temperature will only be checked if it's less not less than or equals (< instead of <=)
     * Since the expected temperature will be rising further.
     * </p>
     * <p>
     * This will be Equivalent to "givenTemperatureAboveThermometer"
     * </p>
     *
     * @param temperature the given temperature either by calling component or this.
     * @return a boolean: result -> lessOrEquals than given Temperature /less than given Temperature
     */
    default boolean thermometerBelowGivenTemperature(int temperature) {
        boolean result;
        if (this.getThermometerState().equals(ThermometerState.FALLING)) {
            result = this.getTemperatureValue() <= temperature;
        } else {
            result = this.getTemperatureValue() < temperature;
        }
        return result;
    }

    /**
     * Important: If Temperature is rising --> "Greater than" given temperature is still ok if the current Temperature
     * is greater than or equal to the given Temperature.
     * <p>
     * Example: Temperature is rising: 400 dc 450 dc 500dc Set point is 500 dC.
     * --> Since the temperature is rising --> 500dC of thermometer will be equal or greater than given Temperature so return true.
     * However if the temperature is falling ---> 600dC --> 550 dC 500 dC and the given Temperature is 500 -->
     * The Temperature will only be checked if it's greater not greater than or equals (> instead of >=)
     * Since the expected temperature will be falling further.
     * </p>
     * <p>
     * This will be Equivalent to "givenTemperatureBelowThermometer"
     * </p>
     *
     * @param temperature the given temperature either by calling component or this.
     * @return a boolean: result -> greaterOrEquals than given Temperature /greater than given Temperature
     */
    default boolean thermometerAboveGivenTemperature(int temperature) {
        boolean result;
        if (this.getThermometerState().equals(ThermometerState.RISING)) {
            result = this.getTemperatureValue() >= temperature;
        } else {
            result = this.getTemperatureValue() > temperature;
        }
        return result;
    }

    /**
     * Returns the current value of a Channel.
     *
     * @param requestedChannel the Channel, usually from this nature.
     * @return the Value or null if not defined.
     */
    default Object getValueOfChannel(Channel<?> requestedChannel) {
        if (requestedChannel.value().isDefined()) {
            return requestedChannel.value().get();
        } else {
            return null;
        }
    }

    /**
     * get the next value of a Channel. Happens if current value is not defined.
     *
     * @param requestedChannel the Channel, usually from this nature.
     * @return the Value or null if not defined.
     */

    default Object getNextValueOfChannel(Channel<?> requestedChannel) {
        if (requestedChannel.getNextValue().isDefined()) {
            return requestedChannel.getNextValue().get();
        }
        return null;
    }

}
