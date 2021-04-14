package io.openems.edge.heater.decentral.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.heater.api.Heater;

public interface DecentralHeater extends Heater {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        NEED_HEAT(Doc.of(OpenemsType.BOOLEAN)),
        NEED_HEAT_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)),

        NEED_MORE_HEAT(Doc.of(OpenemsType.BOOLEAN)),
        NEED_MORE_HEAT_ENABLE_SIGNAL(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),

        FORCE_HEAT(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
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

    //---------------NEED_HEAT--------------------//
    default Channel<Boolean> getNeedHeatChannel() {
        return this.channel(ChannelId.NEED_HEAT);
    }

    default boolean getNeedHeat() {
        Boolean needHeat = (Boolean) getCurrentChannelValue(this.getNeedHeatChannel());
        if (needHeat == null) {
            needHeat = (Boolean) getNextChannelValue(this.getNeedHeatChannel());
        }
        return needHeat != null ? needHeat : false;
    }
    //--------------------------------------------//

    //---------------NEED_HEAT_ENABLE_SIGNAL--------------------//
    default WriteChannel<Boolean> getNeedHeatEnableSignalChannel() {
        return this.channel(ChannelId.NEED_HEAT_ENABLE_SIGNAL);
    }
    //REWORK at Bastis branch --> DecentralHeater uses Channel directly
    default boolean getNeedHeatEnableSignal() {
        Boolean needHeatEnableSignal = (Boolean) getCurrentChannelValue(this.getNeedHeatEnableSignalChannel());
        if (needHeatEnableSignal == null) {
            needHeatEnableSignal = (Boolean) getNextChannelValue(this.getNeedHeatEnableSignalChannel());
        }
        return needHeatEnableSignal != null ? needHeatEnableSignal : false;
    }
    //--------------------------------------------//

    //---------------NEED_MORE_HEAT--------------------//
    default Channel<Boolean> getNeedMoreHeatChannel() {
        return this.channel(ChannelId.NEED_MORE_HEAT);
    }

    default boolean getNeedMoreHeat() {
        Boolean needMoreHeat = (Boolean) getCurrentChannelValue(this.getNeedMoreHeatChannel());
        if (needMoreHeat == null) {
            needMoreHeat = (Boolean) getNextChannelValue(this.getNeedMoreHeatChannel());
        }
        return needMoreHeat != null ? needMoreHeat : false;
    }
    //--------------------------------------------//

    //---------------NEED_MORE_HEAT_ENABLE_SIGNAL--------------------//

    default Channel<Boolean> getNeedMoreHeatEnableSignalChannel() {
        return this.channel(ChannelId.NEED_MORE_HEAT);
    }

    default boolean getNeedMoreHeatEnableSignal() {
        Boolean needMoreHeat = (Boolean) getCurrentChannelValue(this.getNeedMoreHeatChannel());
        if (needMoreHeat == null) {
            needMoreHeat = (Boolean) getNextChannelValue(this.getNeedMoreHeatChannel());
        }
        return needMoreHeat != null ? needMoreHeat : false;
    }
    //--------------------------------------------------------------//


    //-----------------FORCE_HEAT---------------------------//

    default WriteChannel<Boolean> getForceHeatChannel() {
        return this.channel(ChannelId.FORCE_HEAT);
    }

    default void setForceHeating(boolean forceHeating) {
        this.getForceHeatChannel().setNextValue(forceHeating);
    }

    default boolean getIsForceHeating() {
        Boolean forceHeating = (Boolean) this.getCurrentChannelValue(this.getForceHeatChannel());
        if (forceHeating == null) {
            forceHeating = (Boolean) this.getNextChannelValue(this.getForceHeatChannel());
        }
        return forceHeating != null ? forceHeating : false;
    }

    //---------------------------------------------------//


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

}
