package io.openems.edge.controller.heatnetwork.apartmentmodule.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControllerHeatingApartmentModule extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Set the Temperature to look out for.
         * E.g. check your threshold thermometer if this temperature is above/below reference temperature.
         *
         * <ul>
         * <li>Interface: Thermometer
         * <li>Type: Integer
         * <li>Unit: dezidegree celsius
         * </ul>
         */
        SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE).onInit(
                        channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
                )),
        /**
         * Set the PowerLevel of Pump.
         * If Requests are available --> activate pump with given PowerLevel
         */
        SET_POINT_PUMP_POWER_LEVEL(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * The State of the Controller.
         */
        CONTROLLER_APARTMENT_STATE(Doc.of(State.values())),

        /**
         * If this Flag is set-->Pump starts. Except if EmergencyStop is enabled.
         */
        EMERGENCY_PUMP_START((Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE))),
        /**
         * All ResponseFlags will be Set. Except if EmergencyStop is enabled.
         */
        EMERGENCY_ENABLE_EVERY_RESPONSE((Doc.of(OpenemsType.BOOLEAN)).accessMode(AccessMode.READ_WRITE)),
        /**
         * Stops this controller.
         */

        EMERGENCY_STOP((Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    //--------------- SET POINT TEMPERATURE ------------------//
    default WriteChannel<Integer> getSetPointTemperatureChannel() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE);
    }

    default void setSetPointTemperature(int temperature) {
        this.getSetPointTemperatureChannel().setNextValue(temperature);
    }

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
    //----------------------------------------------------------------------//

    //--------------- SET_POINT_PUMP_POWER_LEVEL ------------------//
    default WriteChannel<Double> getSetPointPowerLevelChannel() {
        return this.channel(ChannelId.SET_POINT_PUMP_POWER_LEVEL);
    }

    default void setSetPointPowerLevel(double powerLevel) {
        this.getSetPointPowerLevelChannel().setNextValue(powerLevel);
    }

    default double getSetPointPowerLevel() {

        Double setPointPower = (Double) this.getValueOfChannel(this.getSetPointPowerLevelChannel());
        if (setPointPower == null) {
            setPointPower = (Double) this.getNextValueOfChannel(this.getSetPointPowerLevelChannel());
        }
        if (setPointPower == null) {
            setPointPower = (double) Double.MIN_EXPONENT;
        }
        return setPointPower;
    }
    //----------------------------------------------------------------------//
    //--------------- STATE ------------------//

    default Channel<State> getControllerStateChannel() {
        return this.channel(ChannelId.CONTROLLER_APARTMENT_STATE);
    }

    default State getControllerState() {
        State currentState = (State) getValueOfChannel(getControllerStateChannel());
        if (currentState.isUndefined()) {
            currentState = (State) getNextValueOfChannel(getControllerStateChannel());
        }
        return currentState;
    }

    default void setState(State state) {
        this.getControllerStateChannel().setNextValue(state);
    }

    //----------------------------------------------------------------------//
    //--------------- EMERGENCY_PUMP_START ------------------//
    default WriteChannel<Boolean> getEmergencyPumpStartChannel() {
        return this.channel(ChannelId.EMERGENCY_PUMP_START);
    }

    //----------------------------------------------------------------------//
    //--------------- EMERGENCY_ENABLE_EVERY_RESPONSE ------------------//
    default WriteChannel<Boolean> getEmergencyEnableEveryResponseChannel() {
        return this.channel(ChannelId.EMERGENCY_ENABLE_EVERY_RESPONSE);
    }

    //----------------------------------------------------------------------//
    //--------------- EMERGENCY_STOP ------------------//
    default WriteChannel<Boolean> getEmergencyStopChannel() {
        return this.channel(ChannelId.EMERGENCY_STOP);
    }
    //----------------------------------------------------------------------//

    default Object getValueOfChannel(Channel<?> requestedValue) {
        if (requestedValue.value().isDefined()) {
            return requestedValue.value().get();
        } else {
            return null;
        }
    }

    default Object getNextValueOfChannel(Channel<?> requestedValue) {
        if (requestedValue.getNextValue().isDefined()) {
            return requestedValue.getNextValue().get();
        }
        return null;
    }

}
