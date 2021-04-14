package io.openems.edge.controller.heatnetwork.signalhotwater.valvecontroller.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface SignalHotWaterValvecontroller extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        VALVE_OPENING(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT).accessMode(AccessMode.READ_WRITE)),

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
     * Is true when no error has occurred. An error occurs when the controller does not get a signal
     * from the temperature sensor or there is a problem with the valve or the heat source.
     *
     * @return the Channel
     */

    default Channel<Boolean> noError() {
        return this.channel(ChannelId.NO_ERROR);
    }

    default WriteChannel<Integer> valveOpening() {
        return this.channel(ChannelId.VALVE_OPENING);
    }

}
