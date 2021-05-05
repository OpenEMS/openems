package io.openems.edge.meter.watermeter.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This is the water meter interface, containing the shared channels of M-Bus and Wireless M-Bus water meters.
 */

public interface WaterMeter extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Counted volume.
         *
         * <ul>
         * <li>Interface: WaterMeter
         * <li>Type: Double
         * <li>Unit: cubic meters (m³)
         * </ul>
         */
        TOTAL_CONSUMED_WATER(Doc.of(OpenemsType.DOUBLE).unit(Unit.CUBIC_METER).accessMode(AccessMode.READ_ONLY)),

        /**
         * The last timestamp. Unit is seconds since Epoch (1. 1. 1970). Granularity is 60 seconds if you use the meter
         * readout. If you let OpenEMS generate the timestamp you get 1 second granularity.
         *
         * <ul>
         * <li>Interface: WaterMeter
         * <li>Type: Long
         * <li>Unit: seconds
         * </ul>
         */
        TIMESTAMP_SECONDS(Doc.of(OpenemsType.LONG).unit(Unit.SECONDS).accessMode(AccessMode.READ_ONLY)),

        /**
         * The last timestamp, as a string. Format is "dd-MM-yyyy HH:mm". Calculated from TIMESTAMP_SECONDS, so meter
         * created or OpenEMS created depending on what you choose there.
         *
         * <ul>
         * <li>Interface: WaterMeter
         * <li>Type: String
         * </ul>
         */
        TIMESTAMP_STRING(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY)),

        /**
         * Error message. Will contain "No error" when there is no error. Otherwise a description of the error will be
         * given.
         *
         * <ul>
         * <li>Interface: WaterMeter
         * <li>Type: String
         * </ul>
         */
        ERROR_MESSAGE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY));

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Gets the Channel for {@link ChannelId#TOTAL_CONSUMED_WATER}.
     *
     * @return the Channel
     */
    default DoubleReadChannel getTotalConsumedWaterChannel() {
        return this.channel(ChannelId.TOTAL_CONSUMED_WATER);
    }

    /**
     * Gets the total consumed water in [m³]. See {@link ChannelId#TOTAL_CONSUMED_WATER}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Double> getTotalConsumedWater() {
        return this.getTotalConsumedWaterChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TOTAL_CONSUMED_WATER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTotalConsumedWater(Double value) {
        this.getTotalConsumedWaterChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TOTAL_CONSUMED_WATER}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTotalConsumedWater(double value) {
        this.getTotalConsumedWaterChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#TIMESTAMP_SECONDS}.
     *
     * @return the Channel
     */
    default LongReadChannel getTimestampChannel() {
        return this.channel(ChannelId.TIMESTAMP_SECONDS);
    }

    /**
     * Gets the last timestamp in [s since Epoch]. See {@link ChannelId#TIMESTAMP_SECONDS}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<Long> getTimestamp() {
        return this.getTimestampChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TIMESTAMP_SECONDS}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTimestamp(Long value) {
        this.getTimestampChannel().setNextValue(value);
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TIMESTAMP_SECONDS}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTimestamp(long value) {
        this.getTimestampChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#TIMESTAMP_STRING}.
     *
     * @return the Channel
     */
    default StringReadChannel getTimestampStringChannel() {
        return this.channel(ChannelId.TIMESTAMP_STRING);
    }

    /**
     * Gets the last timestamp as a String. Format is DD-MM-YYYY HH:MM. See {@link ChannelId#TIMESTAMP_STRING}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<String> getTimestampString() {
        return this.getTimestampStringChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#TIMESTAMP_STRING}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setTimestamp(String value) {
        this.getTimestampStringChannel().setNextValue(value);
    }

    /**
     * Gets the Channel for {@link ChannelId#ERROR_MESSAGE}.
     *
     * @return the Channel
     */
    default StringReadChannel getErrorMessageChannel() {
        return this.channel(ChannelId.ERROR_MESSAGE);
    }

    /**
     * Gets the error message. Will contain "No error" when there is no error. See {@link ChannelId#ERROR_MESSAGE}.
     *
     * @return the Channel {@link Value}
     */
    public default Value<String> getErrorMessage() {
        return this.getErrorMessageChannel().value();
    }

    /**
     * Internal method to set the 'nextValue' on {@link ChannelId#ERROR_MESSAGE}
     * Channel.
     *
     * @param value the next value
     */
    public default void _setErrorMessage(String value) {
        this.getErrorMessageChannel().setNextValue(value);
    }
}
