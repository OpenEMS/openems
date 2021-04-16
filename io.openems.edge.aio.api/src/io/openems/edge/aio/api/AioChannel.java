package io.openems.edge.aio.api;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * Represents a Aio (Analog-Input-Output) Module.
 *
 * <p>
 *
 *
 *
 * <ul>
 * <li>
 * <li>
 *
 * <li>
 *
 * </ul>
 */

public interface AioChannel extends OpenemsComponent {
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

        /**
         * Status of Aio, depends on Configuration.
         *
         * <ul>
         * <li>Interface: AioChannel
         * <li>Type: Integer
         * <li>Unit: %
         * <li>Range: 0..100
         * </ul>
         */
        AIO_READ(Doc.of(OpenemsType.INTEGER)),
        AIO_PERCENT(Doc.of(OpenemsType.INTEGER)),
        AIO_WRITE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)),
        AIO_CHECK_WRITE(Doc.of(OpenemsType.INTEGER));
        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }

    }

    default Channel<Integer> getReadChannel() {
        return this.channel(ChannelId.AIO_READ);
    }

    default int getReadValue() {
        if (this.getReadChannel().value().isDefined()) {
            return this.getReadChannel().value().get();
        } else if (this.getReadChannel().getNextValue().isDefined()) {
            return this.getReadChannel().getNextValue().get();
        }
        return -1;
    }
    default Channel<Integer> getPercentChannel() {
        return this.channel(ChannelId.AIO_PERCENT);
    }

    default int getPercentValue() {
        if (this.getPercentChannel().value().isDefined()) {
            return this.getPercentChannel().value().get();
        } else if (this.getPercentChannel().getNextValue().isDefined()) {
            return this.getPercentChannel().getNextValue().get();
        }
        return -1;
    }
    default WriteChannel<Integer> getWriteChannel() {
        return this.channel(ChannelId.AIO_WRITE);
    }

    default Channel<Integer> getCheckWriteChannel() {
        return this.channel(ChannelId.AIO_CHECK_WRITE);
    }
}
