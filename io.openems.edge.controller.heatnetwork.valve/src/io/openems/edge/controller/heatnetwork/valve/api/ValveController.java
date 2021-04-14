package io.openems.edge.controller.heatnetwork.valve.api;


import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface ValveController extends OpenemsComponent {


    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * This channel is to Request a Position <-- Only necessary if you want to control Valve by position.
         */
        REQUEST_POSITION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Current SetPoint Position.
         */
        SET_POINT_POSITION(Doc.of(OpenemsType.INTEGER)),
        /**
         * Check if Valve forced Open.
         */
        IS_FORCED_OPEN(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * Check if Valve is forced Close.
         */
        IS_FORCED_CLOSE(Doc.of(OpenemsType.BOOLEAN)),
        /**
         * Enable/Disable ForceControl --> Force Open/Close.
         */
        FORCE_CONTROL_ALLOWED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * ControlType --> Position (Percent Value) or Temperature see "ControlType" for possible ControlTypes.
         */
        CONTROL_TYPE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * "Enable" The Controller --> Run Logic.
         */
        ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),
        AUTO_RUN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
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

    //----------------REQUEST_POSITION--------------------//
    default WriteChannel<Integer> getRequestedPositionChannel() {
        return this.channel(ChannelId.REQUEST_POSITION);
    }

    default int getRequestedValvePosition() {
        if (this.getRequestedPositionChannel().value().isDefined()) {
            return this.getRequestedPositionChannel().value().get();
        }
        if (this.getRequestedPositionChannel().getNextValue().isDefined()) {
            return this.getRequestedPositionChannel().getNextValue().get();
        } else {
            return Integer.MIN_VALUE;
        }
    }

    //----------------------------------------------------//
    //----------------POSITION--------------------//
    default Channel<Integer> getSetPointPositionChannel() {
        return this.channel(ChannelId.SET_POINT_POSITION);
    }

    default void setSetPointPosition(int position) {
        this.getSetPointPositionChannel().setNextValue(position);
    }

    default int getSetPointPosition() {
        if (this.getSetPointPositionChannel().value().isDefined()) {
            return this.getSetPointPositionChannel().value().get();
        } else if (this.getSetPointPositionChannel().getNextValue().isDefined()) {
            return this.getSetPointPositionChannel().getNextValue().get();
        } else {
            return Integer.MIN_VALUE;
        }
    }

    //----------------------------------------------------//
    //----------------IS_FORCED_OPEN--------------------//
    default Channel<Boolean> isForcedOpenChannel() {
        return this.channel(ChannelId.IS_FORCED_OPEN);
    }

    default boolean isForcedOpen() {
        if (this.isForcedOpenChannel().value().isDefined()) {
            return this.isForcedOpenChannel().value().get();
        } else if (this.isForcedOpenChannel().getNextValue().isDefined()) {
            return this.isForcedOpenChannel().getNextValue().get();
        } else {
            return false;
        }
    }

    default void requestForceOpen() {
        if (forceAllowed()) {
            this.isForcedOpenChannel().setNextValue(true);
            this.isForcedCloseChannel().setNextValue(false);
        }
    }

    //----------------------------------------------------//
    //----------------IS_FORCED_CLOSE--------------------//
    default Channel<Boolean> isForcedCloseChannel() {
        return this.channel(ChannelId.IS_FORCED_CLOSE);
    }

    default boolean isForcedClose() {

        if (this.isForcedCloseChannel().value().isDefined()) {
            return this.isForcedCloseChannel().value().get();
        } else if (this.isForcedCloseChannel().getNextValue().isDefined()) {
            return this.isForcedCloseChannel().getNextValue().get();
        } else {
            return false;
        }
    }

    default void requestForceClose() {
        if (forceAllowed()) {
            this.isForcedCloseChannel().setNextValue(true);
            this.isForcedOpenChannel().setNextValue(false);
        }
    }

    //----------------------------------------------------//
    //----------------FORCE_CONTROL_ALLOWED--------------------//
    default WriteChannel<Boolean> forceAllowedChannel() {
        return this.channel(ChannelId.FORCE_CONTROL_ALLOWED);
    }

    default boolean forceAllowed() {
        if (this.forceAllowedChannel().value().isDefined()) {
            return this.forceAllowedChannel().value().get();
        } else if (this.forceAllowedChannel().getNextValue().isDefined()) {
            return this.forceAllowedChannel().getNextValue().get();
        } else {
            return false;
        }
    }

    //----------------------------------------------------//
    //----------------CONTROL_TYPE--------------------//
    default WriteChannel<String> getControlTypeChannel() {
        return this.channel(ChannelId.CONTROL_TYPE);
    }

    default void setControlType(ControlType controlType) {
        this.getControlTypeChannel().setNextValue(controlType.name());
    }

    default ControlType getControlType() {
        String controlTypeString = (String) getCurrentChannelValue(this.getControlTypeChannel());
        if (controlTypeString == null) {
            controlTypeString = (String) getNextChannelValue(this.getControlTypeChannel());
        }
        if (controlTypeString != null) {
            return ControlType.valueOf(controlTypeString);
        } else {
            return null;
        }
    }

    //----------------------------------------------------//
    //----------------ENABLE_SIGNAL--------------------//
    default WriteChannel<Boolean> getEnableSignalChannel() {
        return this.channel(ChannelId.ENABLE_SIGNAL);
    }

    default void setEnableSignal(boolean enable) {
        this.getEnableSignalChannel().setNextValue(enable);
    }

    //----------------------------------------------------//

    default void setAutoRun(boolean autoRun) {
        try {
            this.autoRunChannel().setNextWriteValue(autoRun);
        } catch (OpenemsError.OpenemsNamedException e) {

        }
    }

    default WriteChannel<Boolean> autoRunChannel() {
        return this.channel(ChannelId.AUTO_RUN);
    }


    default boolean isAutorun(){
        Boolean enabledSignal = (Boolean) this.getCurrentChannelValue(this.autoRunChannel());
        if (enabledSignal == null) {
            enabledSignal = (Boolean) this.getNextChannelValue(this.autoRunChannel());
        }
        return enabledSignal != null ? enabledSignal : false;
    }


    default void setRequestPosition(int percent) {
        this.getRequestedPositionChannel().setNextValue(percent);
    }

    default Object getCurrentChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.value().isDefined()) {
            return requestedChannel.value().get();
        } else {
            return null;
        }
    }

    default Object getNextChannelValue(Channel<?> requestedChannel) {
        if (requestedChannel.getNextValue().isDefined()) {
            return requestedChannel.getNextValue().get();
        } else {
            return null;
        }
    }

    double getCurrentPositionOfValve();
}
