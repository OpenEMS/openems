package io.openems.edge.controller.heatnetwork.signalhotwater.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface SignalHotWater extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        MIN_T_UPPER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE).accessMode(AccessMode.READ_WRITE)),
        MAX_T_LOWER(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Controller output, request to heat the water tank. Low temperature detected in the water tank, requesting to
         * turn on the heat network so the tank can be heated.
         * <li>
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        HEAT_TANK_REQUEST(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Controller input. Remote signal about the heat network status.
         * <li>
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        HEAT_NETWORK_READY(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> { //
                    // on each Write to the channel -> set the value
                    ((BooleanWriteChannel) channel).onSetNextWrite(value -> {
                        channel.setNextValue(value);
                    });
                })),

        /**
         * Controller output. Signal to the next controller to start heating the water tank.
         * <li>
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        START_HEATING_TANK(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

        /**
         * Water tank temperature
         * <li>
         * <li>Type: Integer
         * <li>
         * </ul>
         */

        WATER_TANK_TEMP(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

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

    default WriteChannel<Integer> minTempUpper() {
        return this.channel(ChannelId.MIN_T_UPPER);
    }
    default WriteChannel<Integer> maxTempLower() {
        return this.channel(ChannelId.MAX_T_LOWER);
    }
    /**
     * Controller output, request to heat the water tank. Low temperature detected in the water tank, requesting to turn
     * on the heat network so the tank can be heated.
     *
     * @return the Channel
     */

    default Channel<Boolean> heatTankRequest() {
        return this.channel(ChannelId.HEAT_TANK_REQUEST);
    }

    /**
     * Controller input. Status of the heat network needs to be written in this channel. When the water tank is within
     * temperature bounds, changes (!) in this channel will be forwarded to the needHotWater channel. That means, if
     * this channel changes from true to false while the water tank is heating up and already above min temperature,
     * needHotWater will change to false and the heating will stop. If this channel changes from false to true while
     * the tank is below max temperature, needHotWater will change to true and heating will start.
     *
     * @return the Channel
     */

    default WriteChannel<Boolean> heatNetworkReadySignal() {
        return this.channel(ChannelId.HEAT_NETWORK_READY);
    }

    /**
     * Controller output, signalling to commence the procedure to heat the tank.
     *
     * @return the Channel
     */

    default Channel<Boolean> startHeatingTank() {
        return this.channel(ChannelId.START_HEATING_TANK);
    }

    /**
     * Temperature of the water tank lower sensor, which monitors the maximum temperature in the water tank.
     *
     * @return the Channel
     */

    default Channel<Integer> waterTankTemp() {
        return this.channel(ChannelId.WATER_TANK_TEMP);
    }


    /**
     * Is true when no error has occurred. An error occurs when the controller does not get a signal from one of the
     * temperature sensors.
     *
     * @return the Channel
     */

    default Channel<Boolean> noError() {
        return this.channel(ChannelId.NO_ERROR);
    }

}
