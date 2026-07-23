package io.openems.edge.ess.saxpower;

import io.openems.edge.common.channel.Doc;

public interface SaxPower {
    public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
        ;

        private final Doc doc;

        private ChannelId(Doc doc) {
            this.doc = doc;
        }

        @Override
        public Doc doc() {
            return this.doc;
        }
    }
}
