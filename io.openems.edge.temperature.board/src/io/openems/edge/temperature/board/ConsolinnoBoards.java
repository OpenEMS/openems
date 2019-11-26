package io.openems.edge.temperature.board;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface ConsolinnoBoards extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        DANGEROUS_TO_GO_ALONE(Doc.of(OpenemsType.STRING).accessMode(AccessMode.READ_ONLY).text("Take this.")); //
        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        public Doc doc() {
            return this.doc;
        }
    }

    public default Channel<String> getDangerous() {
        return this.channel(ChannelId.DANGEROUS_TO_GO_ALONE);
    }


}
