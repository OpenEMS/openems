package io.openems.edge.controller.heatnetwork.pid.heatsystem.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.*;
import io.openems.edge.common.component.OpenemsComponent;

public interface PidHeatsystemController extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {


        /**
         * Turn PID on or off.
         * <ul>
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        ON_OFF(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((BooleanWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                })),


        /**
         * Min Temperature.
         * <ul>
         * <li> Min Temperature that has to be reached
         * <li>Type: Integer
         * <li>Unit: Decimal degrees Celsius
         * </ul>
         */

        MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE).onInit(channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                }
        ));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Turn PID on or off.
     *
     * @return the Channel
     */

    default WriteChannel<Boolean> turnOn() {
        return this.channel(ChannelId.ON_OFF);
    }

    /**
     * Min Temperature you want to reach / check if it can be reached.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> setMinTemperature() {
        return this.channel(ChannelId.MIN_TEMPERATURE);
    }


}