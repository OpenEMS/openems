package com.ed.openems.centurio.ess;

import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.component.OpenemsComponent;

public class CenturioErrorChannel extends StateChannel {

	public CenturioErrorChannel(OpenemsComponent component, ChannelId channelId) {
		super(component, channelId);
	}

	public String getErrorCode() {
		return this.channelId().name();
	}
}
