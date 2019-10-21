package io.openems.edge.bridge.mbus.api;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractOpenemsMbusComponent extends AbstractOpenemsComponent{

	private Integer primaryAddress;
	
	protected List<ChannelRecord> channelDataRecordsList = new ArrayList<ChannelRecord>();
		
	protected AbstractOpenemsMbusComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}
	
	public List<ChannelRecord> getChannelDataRecordsList() {
		return channelDataRecordsList;
	}

	public Integer getPrimaryAddress() {
		return primaryAddress;
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, int primaryAddress,
			ConfigurationAdmin cm, String mbusReference, String mbusId, String service_pid) {
		super.activate(context, service_pid, id, enabled);
		this.primaryAddress = primaryAddress;
		
	}
}
