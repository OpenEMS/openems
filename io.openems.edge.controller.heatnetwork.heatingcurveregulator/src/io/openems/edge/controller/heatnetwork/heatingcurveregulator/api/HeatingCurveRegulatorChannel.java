package io.openems.edge.controller.heatnetwork.heatingcurveregulator.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface HeatingCurveRegulatorChannel extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {


        ROOM_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        ACTIVATION_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        OFFSET(Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)),
        SLOPE(Doc.of(OpenemsType.DOUBLE).accessMode(AccessMode.READ_WRITE)),
        /**
         * Heating Temperature.
         * <ul>
         * <li>Controller output. Value for the heating temperature.
         * <li>Type: Integer
         * <li>Unit: Decimal degrees Celsius
         * </ul>
         */

        HEATING_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Activate the heater.
         * <ul>
         * <li>Controller output. If the heater should be turned on.
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        ACTIVATE_HEATER(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),

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

    default WriteChannel<Integer> getRoomTemperature() {
        return this.channel(ChannelId.ROOM_TEMPERATURE);
    }
    default WriteChannel<Integer> getActivationTemperature() {
        return this.channel(ChannelId.ACTIVATION_TEMPERATURE);
    }
    default WriteChannel<Integer> getOffset() {
        return this.channel(ChannelId.OFFSET);
    } default WriteChannel<Double> getSlope() {
        return this.channel(ChannelId.SLOPE);
    }
    /**
     * Read the output temperature of the heating curve regulator. To output this number is the main purpose of the
     * controller.
     * Unit is decimal degree celsius.
     *
     * @return the Channel
     */
    default Channel<Integer> getHeatingTemperature() {
        return this.channel(ChannelId.HEATING_TEMPERATURE);
    }

    /**
     * Controller output: Signal for the heater to be turned on.
     * This is true when the temperature measured by the allocated sensor is lower than the activation temperature set
     * in the configuration.
     *
     * @return the Channel
     */

    default Channel<Boolean> signalTurnOnHeater() {
        return this.channel(ChannelId.ACTIVATE_HEATER);
    }

    /**
     * Is true when no error has occurred.
     *
     * @return the Channel
     */

    default Channel<Boolean> noError() {
        return this.channel(ChannelId.NO_ERROR);
    }

}
