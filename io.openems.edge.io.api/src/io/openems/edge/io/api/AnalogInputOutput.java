package io.openems.edge.io.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a Aio (Analog-Input-Output) Module.
 *
 * <p>
 * The Content of the Channels is dependent on the Configuration
 * <ul>
 * <li>AIO_READ contains the Digital Output
 * <li>AIO_PERCENT contains the Percent Value of the Configured Value
 * <li>AIO_CHECK_WRITE contains the Value that is written as a debug read
 * </ul>
 */

public interface AnalogInputOutput extends OpenemsComponent {

    int AIO_MINIMUM_THOUSANDTH = 0;
    int AIO_MAXIMUM_THOUSANDTH = 1000;

    enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Status of Aio, depends on Configuration.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * </ul>
         */
        AIO_INPUT(Doc.of(OpenemsType.INTEGER)),
        /**
         * Set Status of Aio in thousandth.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * <li>Unit: %
         * <li>Range: 0..100
         * </ul>
         */
        AIO_THOUSANDTH_WRITE(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)),
        /**
         * Status of Aio in thousandth.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * <li>Unit: %
         * <li>Range: 0..1000
         * </ul>
         */
        AIO_CHECK_THOUSANDTH(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH)),
        /**
         * Set Value that is being written to Aio.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * </ul>
         */
        AIO_WRITE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        /**
         * Value that is being written to Aio.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * </ul>
         */
        AIO_CHECK_WRITE(Doc.of(OpenemsType.INTEGER));
        private final Doc doc;

        ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }

    }

    /**
     * Get the Channel for {@link ChannelId#AIO_INPUT}.
     *
     * @return the channel.
     */
    default Channel<Integer> getInputChannel() {
        return this.channel(ChannelId.AIO_INPUT);
    }

    /**
     * Get the Value of the {@link ChannelId#AIO_INPUT} or else -1.
     *
     * @return the Value.
     */
    default int getInputValue() {
        if (this.getInputChannel().value().isDefined()) {
            return this.getInputChannel().value().get();
        } else if (this.getInputChannel().getNextValue().isDefined()) {
            return this.getInputChannel().getNextValue().get();
        }
        return -1;
    }

    /**
     * Get the Channel for {@link ChannelId#AIO_CHECK_THOUSANDTH}.
     *
     * @return the channel.
     */
    default Channel<Integer> getThousandthCheckChannel() {
        return this.channel(ChannelId.AIO_CHECK_THOUSANDTH);
    }

    /**
     * Get the Value of the {@link ChannelId#AIO_CHECK_THOUSANDTH} in Percent or else -1.
     *
     * @return the Value in Percent.
     */
    default float getPercentValue() {
        if (this.getThousandthCheckChannel().value().isDefined()) {
            return this.getThousandthCheckChannel().value().get().floatValue() / 10;
        } else if (this.getThousandthCheckChannel().getNextValue().isDefined()) {
            return this.getThousandthCheckChannel().getNextValue().get().floatValue() / 10;
        }
        return -1;
    }

    /**
     * Get the Channel for {@link ChannelId#AIO_WRITE}.
     *
     * @return the channel.
     */
    default WriteChannel<Integer> getWriteChannel() {
        return this.channel(ChannelId.AIO_WRITE);
    }

    /**
     * Get the Channel for {@link ChannelId#AIO_CHECK_WRITE}.
     *
     * @return the channel.
     */
    default Channel<Integer> getCheckWriteChannel() {
        return this.channel(ChannelId.AIO_CHECK_WRITE);
    }

    /**
     * Get the Value of the {@link ChannelId#AIO_CHECK_WRITE} or else -1.
     *
     * @return the Value in Percent.
     */
    default int getWriteValue() {
        if (this.getCheckWriteChannel().value().isDefined()) {
            return this.getCheckWriteChannel().value().get();
        } else if (this.getCheckWriteChannel().getNextValue().isDefined()) {
            return this.getCheckWriteChannel().getNextValue().get();
        }
        return -1;
    }

    /**
     * Sets the Value of the Write Channel.
     *
     * @param value the value that has to be set
     */
    default void setWrite(int value) throws OpenemsError.OpenemsNamedException {
        this.getWriteChannel().setNextWriteValue(value);
    }

    /**
     * Returns Channel for the Output Percent Register.
     *
     * @return the Channel
     */
    default WriteChannel<Integer> setWriteThousandthChannel() {
        return this.channel(ChannelId.AIO_THOUSANDTH_WRITE);
    }

    /**
     * Returns the Value of the Percent Output Channel.
     *
     * @return the Value
     */
    default int getWriteThousandthValue() {
        if (this.setWriteThousandthChannel().value().isDefined()) {
            return this.setWriteThousandthChannel().value().get();
        } else if (this.setWriteThousandthChannel().getNextValue().isDefined()) {
            return this.setWriteThousandthChannel().getNextValue().get();
        }
        return -1;
    }

    /**
     * Sets the Value of the Percent Channel.
     *
     * @param thousandth the value that has to be set
     */
    default void setWriteThousandth(int thousandth) throws OpenemsError.OpenemsNamedException {
        this.setWriteThousandthChannel().setNextWriteValue(Math.max(AIO_MINIMUM_THOUSANDTH, Math.min(thousandth, AIO_MAXIMUM_THOUSANDTH)));
    }

    /**
     * Sets the Value for the Output Channel in Percent.
     *
     * @param percent the percentage value
     * @throws OpenemsError.OpenemsNamedException is thrown on an internal error.
     */
    default void setWritePercent(int percent) throws OpenemsError.OpenemsNamedException {
        this.setWriteThousandth(percent * 10);
    }

    /**
     * Sets the Value for the Output Channel in Percent.
     *
     * @param percent the percentage value
     * @throws OpenemsError.OpenemsNamedException is thrown on an internal error.
     */
    default void setWritePercent(double percent) throws OpenemsError.OpenemsNamedException {
        this.setWriteThousandth((int) (percent * 10));
    }


}
