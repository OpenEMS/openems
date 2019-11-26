package io.openems.edge.bridge.spi;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Debounce;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface SpiBridgeChannel extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT) //
                .debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
        CYCLE_TIME_IS_TOO_SHORT(Doc.of(Level.WARNING) //
                .debounce(10, Debounce.TRUE_VALUES_IN_A_ROW_TO_SET_TRUE)), //
        EXECUTION_DURATION(Doc.of(OpenemsType.LONG)),

        GENERAL_KENOBI(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).text("Hello There")); //


        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return doc;
        }
    }

    public default Channel<String> getDangerous() {

        return this.channel(ChannelId.GENERAL_KENOBI);
    }
}
