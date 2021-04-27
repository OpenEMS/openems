package io.openems.edge.apartmenthuf.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.*;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface ApartmentHuFChannel extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        // Input Registers

        /**
         * Version number.
         * <ul>
         *      <li> Type: Integer
         * </ul>
         */

        IR_0_VERSION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error code. Three error bits transmitted as an integer. 0 means no error.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0 - 7
         * </ul>
         */

        IR_2_ERROR(Doc.of(Error.values()).accessMode(AccessMode.READ_ONLY)),

        /**
         * Loop time. How long it takes the Apartment Module to execute the main software loop. This is the rate at
         * which the Apartment Module updates it’s values.
         * <ul>
         *      <li> Type: Integer
         *      <li> Unit: milliseconds
         * </ul>
         */

        IR_3_LOOP_TIME(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLISECONDS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Wall temperature. HUF for Huf communication --> if valid value --> IR_6_Wall_Temperature
         * <li>
         * <li> Type: Integer
         * <li> Unit: dezidegree celsius
         * </ul>
         */

        IR_6_WALL_TEMPERATURE_HUF(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        IR_6_WALL_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Air temperature.
         * <li>
         * <li> Type: Integer
         * <li> Unit: dezidegree celsius
         * </ul>
         */
        IR_7_AIR_TEMPERATURE_HUF(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS)),
        IR_7_AIR_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Air humidity. Channel unit is percent, so a channel value of 50 then means 50%.
         * <li>
         * <li> Type: Float
         * <li> Unit: percent
         * </ul>
         */
        IR_8_AIR_HUMIDITY_HUF(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)),
        IR_8_AIR_HUMIDITY(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT).accessMode(AccessMode.READ_ONLY)),

        /**
         * Air pressure.
         * <li>
         * <li> Type: Float
         * <li> Unit: hectopascal
         * </ul>
         */
        IR_9_AIR_PRESSURE_HUF(Doc.of(OpenemsType.FLOAT).unit(Unit.HECTO_PASCAL)),
        IR_9_AIR_PRESSURE(Doc.of(OpenemsType.FLOAT).unit(Unit.HECTO_PASCAL).accessMode(AccessMode.READ_ONLY)),


        // Holding Registers

        /**
         * Modbus communication check. The master should continuously write 1 in this register. If this does not happen
         * for 2 minutes, the Apartment Module will restart. The Apartment Module will reset this to 0 every 5 seconds.
         * <ul>
         *      <li> Type: Integer
         *      <li> Possible values: 0, 1
         *      <li> State 0: Slave is waiting for signal
         *      <li> State 1: Signal has been set.
         * </ul>
         */

        HR_0_COMMUNICATION_CHECK(Doc.of(CommunicationCheck.values()).accessMode(AccessMode.READ_WRITE)),

        /**
         * Temperature calibration. Value to calibrate the PT1000 sensor.
         * <li>
         * <li> Type: Integer
         * </ul>
         */

        HR_2_TEMPERATURE_CALIBRATION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Gets the Channel for {@link ChannelId#IR_0_VERSION}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getVersionChannel() {
        return this.channel(ChannelId.IR_0_VERSION);
    }

    /**
     * Gets the version number of the software running on the Apartment module.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getVersionNumber() {
        return this.getVersionChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_2_ERROR}.
     *
     * @return the Channel
     */
    public default Channel<Error> getErrorChannel() {
        return this.channel(ChannelId.IR_2_ERROR);
    }

    /**
     * Returns the error message of the Apartment Module.
     *
     * @return the Channel {@link Value}
     */
    public default Error getError() {
        return this.getErrorChannel().value().asEnum();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_3_LOOP_TIME}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getLoopTimeChannel() {
        return this.channel(ChannelId.IR_3_LOOP_TIME);
    }

    /**
     * Gets the execution time of the main software loop in ms. This is the rate at which the Apartment Module updates
     * it’s values.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getLoopTime() {
        return this.getLoopTimeChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_6_WALL_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getWallTemperatureChannel() {
        return this.channel(ChannelId.IR_6_WALL_TEMPERATURE);
    }

    default Channel<Integer> getWallTemperatureToHufChannel() {
        return this.channel(ChannelId.IR_6_WALL_TEMPERATURE_HUF);
    }

    /**
     * Gets the value of the wall temperature sensor in dezidegree Celsius. (1/10 °C)
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getWallTemperature() {
        return this.getWallTemperatureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_7_AIR_TEMPERATURE}.
     *
     * @return the Channel
     */
    public default IntegerReadChannel getAirTemperatureChannel() {
        return this.channel(ChannelId.IR_7_AIR_TEMPERATURE);
    }

    default Channel<Integer> getAirTemperatureToHufChannel() {
        return this.channel(ChannelId.IR_7_AIR_TEMPERATURE_HUF);
    }

    /**
     * Gets the value of the air temperature sensor in dezidegree Celsius. (1/10 °C)
     *
     * @return the Channel {@link Value}
     */
    public default Value<Integer> getAirTemperature() {
        return this.getAirTemperatureChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_8_AIR_HUMIDITY}.
     *
     * @return the Channel
     */
    public default FloatReadChannel getAirHumidityChannel() {
        return this.channel(ChannelId.IR_8_AIR_HUMIDITY);
    }

    default Channel<Float> getAirHumidityToHufChannel() {
        return this.channel(ChannelId.IR_8_AIR_HUMIDITY_HUF);
    }

    /**
     * Gets the value of the air humidity sensor in percent. A channel value of 50 then means 50%.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Float> getAirHumidity() {
        return this.getAirHumidityChannel().value();
    }

    /**
     * Gets the Channel for {@link ChannelId#IR_9_AIR_PRESSURE}.
     *
     * @return the Channel
     */
    public default FloatReadChannel getAirPressureChannel() {
        return this.channel(ChannelId.IR_9_AIR_PRESSURE);
    }

    default Channel<Float> getAirPressureToHufChannel() {
        return this.channel(ChannelId.IR_9_AIR_PRESSURE_HUF);
    }

    /**
     * Gets the value of the air pressure sensor in Hectopascal (equivalent to mBar).
     *
     * @return the Channel {@link Value}
     */
    public default Value<Float> getAirPressure() {
        return this.getAirPressureChannel().value();
    }


    /**
     * Gets the Channel for {@link ChannelId#HR_0_COMMUNICATION_CHECK}.
     *
     * @return the Channel
     */
    public default WriteChannel<CommunicationCheck> getSetCommunicationCheckChannel() {
        return this.channel(ChannelId.HR_0_COMMUNICATION_CHECK);
    }


    /**
     * Gets the Channel for {@link ChannelId#HR_2_TEMPERATURE_CALIBRATION}.
     *
     * @return the Channel
     */
    public default IntegerWriteChannel setTemperatureCalibrationChannel() {
        return this.channel(ChannelId.HR_2_TEMPERATURE_CALIBRATION);
    }


}
