package io.openems.edge.io.revpi.bsp.core;

import java.io.IOException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Core extends OpenemsComponent {

    public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	;

	private final Doc doc;

	private ChannelId(Doc doc) {
	    this.doc = doc;
	}

	public Doc doc() {
	    return this.doc;
	}

    }

    /**
     * Toggles the Hardware watchdog. 
     * @throws IOException on any error
     */
    public void toggleWatchdog() throws IOException;
}
