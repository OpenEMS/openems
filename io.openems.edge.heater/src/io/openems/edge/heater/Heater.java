package io.openems.edge.heater;

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

import java.util.Optional;


public interface Heater extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        EMERGENCY_OFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        SET_POINT_POWER_PERCENT(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> ((DoubleWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        SET_POINT_POWER(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.KILOWATT).onInit(
                channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        READ_EFFECTIVE_POWER_PERCENT(Doc.of(OpenemsType.DOUBLE).unit(Unit.PERCENT)),
        READ_EFFECTIVE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.KILOWATT)),
        HEATER_STATE(Doc.of(OpenemsType.STRING)),
        SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        FLOW_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        RETURN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS));

        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    // **************************** CHANNEL LOGIC ************************ //

    //---------------------ENABLE SIGNAL -------------------------//
    default WriteChannel<Boolean> getEnableSignalChannel() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

    // The method isEnabledSignal() does get and reset. If you call it once, it's gone (for that cycle). So you need
    // to store the Optional in a local variable.
    // If the channel is not refilled with a value after a certain amount of time, assume the value is false. For a code
    // example, look at modules that use the method.
    default Optional<Boolean> isEnabledSignal() {
        return getEnableSignalChannel().getNextWriteValueAndReset();
    }

    //allowedToHeat
    default void setEnableSignal(boolean active) {
        this.getEnableSignalChannel().setNextValue(active);
    }
    //---------------------------------------------------------//

    //------------------SET_POINT_POWER_PERCENT-------------------//

    // Returns false when the device does not support control by setSetPointPowerPercent()
    boolean setPointPowerPercentAvailable();

    default WriteChannel<Double> getSetPointPowerPercentChannel() {
        return this.channel(ChannelId.SET_POINT_POWER_PERCENT);
    }

    default double getSetPointPowerPercent() {
        WriteChannel<Double> selectedChannel = getSetPointPowerPercentChannel();
        if (selectedChannel.value().isDefined()) {
            return selectedChannel.value().get();
        } else if (selectedChannel.getNextValue().isDefined()) {
            return selectedChannel.getNextValue().get();
        } else {
            return -1;
        }
    }

    // This channel goes directly to a modbus holding register in the components using this interface. When writing to
    // a holding register, getNextWriteValue is called on the channel. So a channel being used to send a Modbus write
    // needs the setter to point to setNextWriteValue.
    default void setSetPointPowerPercent(double request) throws OpenemsError.OpenemsNamedException {
        this.getSetPointPowerPercentChannel().setNextWriteValue(request);
    }
    //---------------------------------------------------------//

    //--------------------SET_POINT_POWER----------------------//

    // Returns false when the device does not support control by setSetPointPower()
    boolean setPointPowerAvailable();

    default WriteChannel<Double> getSetPointPowerChannel() {
        return this.channel(ChannelId.SET_POINT_POWER);
    }

    default double getSetPointPercent() {
        WriteChannel<Double> selectedChannel = getSetPointPowerChannel();
        if (selectedChannel.value().isDefined()) {
            return selectedChannel.value().get();
        } else if (selectedChannel.getNextValue().isDefined()) {
            return selectedChannel.getNextValue().get();
        } else {
            return -1;
        }
    }

    // This channel goes directly to a modbus holding register in the components using this interface. When writing to
    // a holding register, getNextWriteValue is called on the channel. So a channel being used to send a Modbus write
    // needs the setter to point to setNextWriteValue.
    default void setSetPointPower(double request) throws OpenemsError.OpenemsNamedException {
        this.getSetPointPowerChannel().setNextWriteValue(request);
    }

    //---------------------------------------------------------------------//

    //----------------- READ_EFFECTIVE_POWER_PERCENT --------------------- //

    default Channel<Double> getEffectivePowerPercentChannel() {
        return this.channel(ChannelId.READ_EFFECTIVE_POWER_PERCENT);
    }

    default double getEffectivePowerPercent() {
        Channel<Double> selectedChannel = getEffectivePowerPercentChannel();
        if (selectedChannel.value().isDefined()) {
            return selectedChannel.value().get();
        } else if (selectedChannel.getNextValue().isDefined()) {
            return selectedChannel.getNextValue().get();
        } else {
            return -1;
        }
    }

    default void setEffectivePowerPercentRead(double percent) {
        this.getEffectivePowerPercentChannel().setNextValue(percent);
    }
    //---------------------------------------------------------//
    //----------------- READ_EFFECTIVE_POWER ------------------ //

    default Channel<Integer> getEffectivePowerChannel() {
        return this.channel(ChannelId.READ_EFFECTIVE_POWER);
    }

    default int getEffectivePower() {
        Channel<Integer> selectedChannel = this.getEffectivePowerChannel();
        if (selectedChannel.value().isDefined()) {
            return selectedChannel.value().get();
        } else if (selectedChannel.getNextValue().isDefined()) {
            return selectedChannel.getNextValue().get();
        } else {
            return -1;
        }
    }

    default void setEffectivePowerRead(int effectivePower) {
        this.getEffectivePowerChannel().setNextValue(effectivePower);
    }
    //---------------------------------------------------------//

    //-------------------   STATE    --------------------- //

    //ToDO: OpenEMS hat auch einen State Channel. Evtl. Getter + Setter in getHeaterState/setHeaterState umbenennen.
    default Channel<String> getHeaterStateChannel() {
        return this.channel(ChannelId.HEATER_STATE);
    }

    default String getCurrentState() {
        Channel<String> selectedChannel = getHeaterStateChannel();
        if (selectedChannel.value().isDefined()) {
            return selectedChannel.value().get();
        } else if (selectedChannel.getNextValue().isDefined()) {
            return selectedChannel.getNextValue().get();
        } else {
            return "";
        }
    }

    default void setState(String state) {
        if (HeaterState.contains(state)) {
            this.getStateChannel().setNextValue(state);
        }
    }

    //---------------------------------------------------------//
    //-------------------SET_POINT_TEMPERATURE --------------------- //

    // Returns false when the device does not support control by setSetPointTemperature()
    boolean setPointTemperatureAvailable();

    default WriteChannel<Integer> getSetPointTemperatureChannel() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE);
    }

    default int getSetPointTemperature() {
        Channel<Integer> selectedChannel = this.getSetPointTemperatureChannel();
        if (selectedChannel.value().isDefined()) {
            return selectedChannel.value().get();
        } else if (selectedChannel.getNextValue().isDefined()) {
            return selectedChannel.getNextValue().get();
        } else {
            return -1;
        }
    }

    // This channel goes directly to a modbus holding register in the components using this interface. When writing to
    // a holding register, getNextWriteValue is called on the channel. So a channel being used to send a Modbus write
    // needs the setter to point to setNextWriteValue.
    default void setSetPointTemperature(int setPointTemperature) throws OpenemsError.OpenemsNamedException {
        this.getSetPointTemperatureChannel().setNextWriteValue(setPointTemperature);
    }
    //---------------------------------------------------------//
    //-------------------FLOW_TEMPERATURE --------------------- //

    default Channel<Integer> getFlowTemperatureChannel() {
        return this.channel(ChannelId.FLOW_TEMPERATURE);
    }

    default int getFlowTemperature() {
        Channel<Integer> selectedChannel = this.getFlowTemperatureChannel();
        if (selectedChannel.value().isDefined()) {
            return selectedChannel.value().get();
        } else if (selectedChannel.getNextValue().isDefined()) {
            return selectedChannel.getNextValue().get();
        } else {
            return -1;
        }
    }

    //---------------------------------------------------------//
    //-------------------RETURN_TEMPERATURE --------------------- //
    default Channel<Integer> getReturnTemperatureChannel() {
        return this.channel(ChannelId.RETURN_TEMPERATURE);
    }

    default int getReturnTemperature() {
        Channel<Integer> selectedChannel = this.getReturnTemperatureChannel();
        if (selectedChannel.value().isDefined()) {
            return selectedChannel.value().get();
        } else if (selectedChannel.getNextValue().isDefined()) {
            return selectedChannel.getNextValue().get();
        } else {
            return -1;
        }
    }
    //---------------------------------------------------------//


    //--------------------EMERGENCY_OFF-----------------//

    default WriteChannel<Boolean> getEmergencyOffChannel() {
        return this.channel(ChannelId.EMERGENCY_OFF);
    }

    // ********************************************************************** //

    int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException;

    int getMaximumThermalOutput();

    void setOffline() throws OpenemsError.OpenemsNamedException;

    boolean hasError();

    void requestMaximumPower();

    void setIdle();

    default boolean errorInHeater() {
        return this.getCurrentState().equals(HeaterState.ERROR);
    }

}
