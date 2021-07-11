package io.openems.edge.bridge.lucidcontrol.api;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The Nature for a LucidControlInput. The real devices are attached to a LucidControlModule.
 * They get the Voltage of a connected Device and convert them into the Pressure Channel, depending on the max Voltage.
 */
public interface LucidControlDeviceInput extends OpenemsComponent {

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * The Voltage Input.
         * <ul>
         *     <li>Interface: LucidControlDeviceInput
         *     <li>Unit: Volt
         * </ul>
         */
        INPUT_VOLTAGE(Doc.of(OpenemsType.DOUBLE).unit(Unit.VOLT)),
        /**
         * The Pressure Input.
         * <ul>
         *     <li>Interface: LucidControlDeviceInput
         *     <li>Unit: BAR
         * </ul>
         */
        PRESSURE(Doc.of(OpenemsType.DOUBLE).unit(Unit.BAR));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the InputVoltage Channel.
     *
     * @return the Channel
     */

    default Channel<Double> getVoltageChannel() {
        return this.channel(ChannelId.INPUT_VOLTAGE);
    }

    /**
     * Get the Value of the Voltage Channel.
     *
     * @return the ValueWrapper (Double) of the Channel Input_Voltage.
     */

    default Value<Double> getVoltage() {
        return this.getVoltageChannel().value();
    }

    /**
     * Get the Pressure Channel.
     *
     * @return the Channel
     */

    default Channel<Double> getPressureChannel() {
        return this.channel(ChannelId.PRESSURE);
    }

    /**
     * Get the Wrapper Value of the Pressure Channel.
     *
     * @return the ValueWrapper (Double) of Pressure.
     */

    default Value<Double> getPressure() {
        return this.getPressureChannel().value();
    }

}
