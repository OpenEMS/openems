package io.openems.edge.utility.virtualchannel;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.internal.AbstractDoc;

public class DynamicChannelId implements ChannelId {
    private final String name;
    private final Doc doc;

    public DynamicChannelId(String name, Doc doc) {
        this.name = name;
        this.doc = doc;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Doc doc() {
        return this.doc;
    }
}
