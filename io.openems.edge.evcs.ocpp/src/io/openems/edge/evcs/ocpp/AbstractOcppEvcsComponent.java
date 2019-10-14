package io.openems.edge.evcs.ocpp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import io.openems.edge.common.component.AbstractOpenemsComponent;

public abstract class AbstractOcppEvcsComponent extends AbstractOpenemsComponent{

	private final Set<OcppProfileType> profiles;
	
	protected AbstractOcppEvcsComponent(OcppProfileType[] profiles,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.profiles = new HashSet<OcppProfileType>(Arrays.asList(profiles));
	}

	
	
}
