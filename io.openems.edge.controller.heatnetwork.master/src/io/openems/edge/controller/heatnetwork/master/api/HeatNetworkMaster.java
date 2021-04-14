package io.openems.edge.controller.heatnetwork.master.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeatNetworkMaster extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * Temperature Set-point.
         * React to this temperature.
         *
         * <ul>
         * <li>Interface: HeatnetworkMaster
         * <li>Type: Integer
         * <li> Unit: Dezidegree Celsius
         * </ul>
         */

        SET_POINT_TEMPERATURE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.DEZIDEGREE_CELSIUS).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                }
        ));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }


    }

    default WriteChannel<Integer> temperatureSetPointChannel() {
        return this.channel(ChannelId.SET_POINT_TEMPERATURE);
    }


}
