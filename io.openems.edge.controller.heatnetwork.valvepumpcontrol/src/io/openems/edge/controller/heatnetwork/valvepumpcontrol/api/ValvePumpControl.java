package io.openems.edge.controller.heatnetwork.valvepumpcontrol.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface ValvePumpControl extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        VALVE_CLOSING(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),
        /**
         * Open or close the valve. True means open, false means close. Override needs to be active for this to do anything.
         * <ul>
         * <li>Open or close
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        VALVE_OVERRIDE_OPEN_CLOSE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((BooleanWriteChannel) channel).onSetNextWrite(value -> {
                        channel.setNextValue(value);
                    });
                })),

        /**
         * Tells the controller to activate the valve override.
         * <ul>
         * <li>If the override is active.
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        ACTIVATE_VALVE_OVERRIDE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((BooleanWriteChannel) channel).onSetNextWrite(value -> {
                        channel.setNextValue(value);
                    });
                })),

        /**
         * No error in this controller.
         * <ul>
         * <li>False if an Error occurred within this Controller.
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        NO_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Open or close the valve. True means open, false means close. Override needs to be active for this to do anything.
     *
     * @return the Channel
     */
    default WriteChannel<Boolean> setValveOverrideOpenClose() {
        return this.channel(ChannelId.VALVE_OVERRIDE_OPEN_CLOSE);
    }

    /**
     * Activate the valve override.
     *
     * @return the Channel
     */

    default WriteChannel<Boolean> activateValveOverride() {
        return this.channel(ChannelId.ACTIVATE_VALVE_OVERRIDE);
    }


    /**
     * Is true when no error has occurred.
     *
     * @return the Channel
     */

    default Channel<Boolean> noError() {
        return this.channel(ChannelId.NO_ERROR);
    }

    default WriteChannel<Integer> valveClosing() {
        return this.channel(ChannelId.VALVE_CLOSING);
    }

}
