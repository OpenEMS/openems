package io.openems.edge.controller.heatnetwork.controlcenter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.*;
import io.openems.edge.common.component.OpenemsComponent;

public interface ControlCenter extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {


        /**
         * Temperature Override.
         * <ul>
         * <li>Overrides output value for the heating temperature.
         * <li>Type: Integer
         * <li>Unit: Decimal degrees Celsius
         * </ul>
         */

        TEMPERATURE_OVERRIDE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_WRITE)
        .onInit(channel -> ((IntegerWriteChannel)channel).onSetNextWrite(channel::setNextValue))),

        /**
         * Tells the controller to use the temperature override.
         * <ul>
         * <li>If the override is active.
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        ACTIVATE_OVERRIDE(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE)
                .onInit(channel -> ((BooleanWriteChannel)channel).onSetNextWrite(channel::setNextValue))),

        /**
         * Heating temperature.
         */
        TEMPERATURE_HEATING(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS).accessMode(AccessMode.READ_ONLY)),

        /**
         * Controller output. If the heater should activate or not.
         * <ul>
         * <li>If the heater should activate.
         * <li>Type: Boolean
         * <li>
         * </ul>
         */

        ACTIVATE_HEATER(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));



        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }


    /**
     * Set the override temperature.
     * Unit is decimal degree celsius.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> setOverrideTemperature() {
        return this.channel(ChannelId.TEMPERATURE_OVERRIDE);
    }

    /**
     * Activate the temperature override.
     *
     * @return the Channel
     */

    default WriteChannel<Boolean> activateTemperatureOverride() {
        return this.channel(ChannelId.ACTIVATE_OVERRIDE);
    }

    /**
     * Controller output. If the heater should activate or not.
     *
     * @return the Channel
     */

    default Channel<Boolean> activateHeater() {
        return this.channel(ChannelId.ACTIVATE_HEATER);
    }


    default Channel<Integer> temperatureHeating() {
        return this.channel(ChannelId.TEMPERATURE_HEATING);
    }


}
