package io.openems.edge.heater;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

public interface Storage extends OpenemsComponent {
    enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        /**
         * SetPoint Temperature Min. This Temperature is the Minimum Threshold
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Dezidegree Celsius
         * </ul>
         */

        SET_POINT_TEMPERATURE_MIN(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.DEZIDEGREE_CELSIUS).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                }
        )),
        /**
         * SetPoint Temperature Max. This Temperature is the Maximum Threshold
         *
         * <ul>
         * <li>Interface: HeatnetworkPerformanceBooster
         * <li>Type: Integer
         * <li> Unit: Dezidegree Celsius
         * </ul>
         */
        SET_POINT_TEMPERATURE_MAX(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.DEZIDEGREE_CELSIUS).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                }
        )),
        SET_POINT_BUFFER_MAX_PERCENT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT).onInit(
                channel -> {
                    ((IntegerWriteChannel) channel).onSetNextWrite(channel::setNextValue);
                }
        )),

        STORAGE_LEVEL_PERCENT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT)),
        STORAGE_LITRES_MAX(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.LITRES)),
        STORAGE_LITRES_CURRENT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.LITRES)),
        STORAGE_ENERGY(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.KILOWATT_HOURS)),
        MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS)),
        MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS)),
        AVERAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
                .unit(Unit.DEZIDEGREE_CELSIUS));


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }


    }



    default WriteChannel<Integer> bufferSetPointMaxPercent() {
        return this.channel(ChannelId.SET_POINT_BUFFER_MAX_PERCENT);
    }

    default Channel<Integer> storagePercent() {
        return this.channel(ChannelId.STORAGE_LEVEL_PERCENT);
    }

    default Channel<Integer> storageLitreMax() {
        return this.channel(ChannelId.STORAGE_LITRES_MAX);
    }

    default Channel<Integer> storageLitresCurrent() {
        return this.channel(ChannelId.STORAGE_LITRES_CURRENT);
    }

    default Channel<Integer> storageEnergy() {
        return this.channel(ChannelId.STORAGE_ENERGY);
    }

    default Channel<Integer> maxTemperature() {
        return this.channel(ChannelId.MAX_TEMPERATURE);
    }

    default Channel<Integer> minTemperature() {
        return this.channel(ChannelId.MIN_TEMPERATURE);
    }

    default Channel<Integer> averageTemperature() {
        return this.channel(ChannelId.AVERAGE_TEMPERATURE);
    }


}
