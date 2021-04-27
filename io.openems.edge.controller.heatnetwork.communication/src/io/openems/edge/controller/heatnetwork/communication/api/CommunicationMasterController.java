package io.openems.edge.controller.heatnetwork.communication.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StringWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface CommunicationMasterController extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Set Autorun for this, and containing CommunicationController.
         */
        AUTO_RUN(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * ForceHeating. Sets The CallbackValue of each containing Requests to True.
         */
        FORCE_HEATING(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Adds another single Request. Config must be Matching. (Separate Values by ':')
         */
        ADDITIONAL_REMOTE_REQUEST(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Removes one Request. By Same Config as in Additional Remote Request.
         */
        REMOVE_REMOTE_REQUEST(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * WaitTime till fallback logic starts if connection is not ok.
         */
        KEEP_ALIVE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Type of Execution on Fallback.
         */
        EXECUTION_ON_FALLBACK(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((StringWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * This Temperature Will be set on Fallback Logic.
         */
        SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).onInit(
                channel -> ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue)
        )),
        /**
         * Maximum allowed Request for the current Communication
         */
        MAXIMUM_REQUESTS(Doc.of(OpenemsType.INTEGER)),
        /**
         * Set the Maximum allowed Requests
         */
        SET_MAXIMUM_REQUESTS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Is Extra Heat Requests by Remote Components
         */
        EXTRA_HEAT(Doc.of(OpenemsType.BOOLEAN)),
        CURRENT_REQUESTS(Doc.of(OpenemsType.INTEGER));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }

    default WriteChannel<Boolean> getAutoRunChannel() {
        return this.channel(ChannelId.AUTO_RUN);
    }


    default void setAutoRun(boolean autoRun) {
        this.getAutoRunChannel().setNextValue(autoRun);
    }

    default boolean getAutoRun() {
        if (this.getAutoRunChannel().value().isDefined()) {
            return this.getAutoRunChannel().value().get();
        } else if (this.getAutoRunChannel().getNextValue().isDefined()) {
            return this.getAutoRunChannel().getNextValue().get();
        } else {
            return true;
        }
    }

    default WriteChannel<Boolean> getForceHeatingChannel() {
        return this.channel(ChannelId.FORCE_HEATING);
    }

    default void setForceHeating(boolean autoRun) {
        this.getForceHeatingChannel().setNextValue(autoRun);
    }

    default boolean getForceHeating() {
        if (this.getForceHeatingChannel().value().isDefined()) {
            return this.getForceHeatingChannel().value().get();
        } else if (this.getForceHeatingChannel().getNextValue().isDefined()) {
            return this.getForceHeatingChannel().getNextValue().get();
        } else {
            return false;
        }
    }

    default WriteChannel<String> getAdditionalRemoteRequestChannel() {
        return this.channel(ChannelId.ADDITIONAL_REMOTE_REQUEST);
    }

    default String getAdditionalRemoteRequest() {
        if (this.getAdditionalRemoteRequestChannel().value().isDefined()) {
            return this.getAdditionalRemoteRequestChannel().value().get();
        } else {
            return "";
        }
    }

    default WriteChannel<String> getRemoveRemoteRequestChannel() {
        return this.channel(ChannelId.REMOVE_REMOTE_REQUEST);
    }

    default String getRemoveRemoteRequest() {
        if (this.getRemoveRemoteRequestChannel().value().isDefined()) {
            return this.getRemoveRemoteRequestChannel().value().get();
        } else {
            return "";
        }
    }

    default WriteChannel<Integer> getKeepAliveChannel() {
        return this.channel(ChannelId.KEEP_ALIVE);
    }

    default void setKeepAlive(int waitTime) {
        this.getKeepAliveChannel().setNextValue(waitTime);
    }

    default int getKeepAlive() {
        if (this.getKeepAliveChannel().value().isDefined()) {
            return this.getKeepAliveChannel().value().get();
        } else if (this.getKeepAliveChannel().getNextValue().isDefined()) {
            return this.getKeepAliveChannel().getNextValue().get();
        } else {
            return 0;
        }
    }

    default WriteChannel<String> getExecutionOnFallbackChannel() {
        return this.channel(ChannelId.EXECUTION_ON_FALLBACK);
    }

    default String getExecutionOnFallback() {
        if (this.getExecutionOnFallbackChannel().value().isDefined()) {
            return this.getExecutionOnFallbackChannel().value().get();
        } else {
            return "";
        }
    }

    default void setFallbackLogic(String logic) {
        this.getExecutionOnFallbackChannel().setNextValue(logic);
    }

    default WriteChannel<Integer> getSetPointTemperatureChannel() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE);
    }

    default int getSetPointTemperature() {
        if (this.getSetPointTemperatureChannel().value().isDefined()) {
            return this.getSetPointTemperatureChannel().value().get();
        } else if (this.getSetPointTemperatureChannel().getNextValue().isDefined()) {
            return this.getSetPointTemperatureChannel().getNextValue().get();
        } else {
            return -1;
        }
    }

    default Channel<Integer> getMaximumRequestChannel() {
        return this.channel(ChannelId.MAXIMUM_REQUESTS);
    }

    default WriteChannel<Integer> getSetMaximumRequestChannel() {
        return this.channel(ChannelId.SET_MAXIMUM_REQUESTS);
    }

    default int getMaximumRequests() {
        if (this.getMaximumRequestChannel().value().isDefined()) {
            return this.getMaximumRequestChannel().value().get();
        } else if (this.getMaximumRequestChannel().getNextValue().isDefined()) {
            return this.getMaximumRequestChannel().getNextValue().get();
        } else {
            return 0;
        }
    }

    default void setMaximumRequests(int maximum) {
        this.getMaximumRequestChannel().setNextValue(maximum);
    }

    default Channel<Boolean> getExtraHeatChannel() {
        return this.channel(ChannelId.EXTRA_HEAT);
    }

    default boolean isExtraHeating() {
        if (this.getExtraHeatChannel().value().isDefined()) {
            return this.getExtraHeatChannel().value().get();
        } else if (this.getExtraHeatChannel().getNextValue().isDefined()) {
            return this.getExtraHeatChannel().value().get();
        }
        return false;
    }

    default Channel<Integer> getCurrentRequestsChannel() {
        return this.channel(ChannelId.CURRENT_REQUESTS);
    }

    default int getCurrentRequests() {
        Integer request = (Integer) this.getValueOfChannel(this.getCurrentRequestsChannel());
        if (request == null) {
            request = (Integer) this.getNextValueOfChannel(this.getCurrentRequestsChannel());
        }
        return request == null ? 0 : request;
    }

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

    CommunicationController getCommunicationController();
}
