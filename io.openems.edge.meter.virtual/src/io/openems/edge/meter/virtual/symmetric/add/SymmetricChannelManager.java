package io.openems.edge.meter.virtual.symmetric.add;

import java.util.List;

import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.meter.virtual.common.ChannelManager;

public class SymmetricChannelManager extends ChannelManager {

	public SymmetricChannelManager(SymmetricMeter parent) {
		super(parent);
	}

	@Override
	public void activate(List<? extends SymmetricMeter> meters) {
		super.activate(meters);
	}
}