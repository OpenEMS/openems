package io.openems.edge.ess.mr.gridcon.onoffgrid.helper;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.ess.mr.gridcon.EssGridcon;
import io.openems.edge.ess.mr.gridcon.StateObject;
import io.openems.edge.ess.power.api.Power;

public class DummyEss extends EssGridcon  {

	
	
	public DummyEss(io.openems.edge.common.channel.ChannelId[] otherChannelIds) {
		super(otherChannelIds);
	}

	@Override
	public Power getPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ComponentManager getComponentManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected StateObject getFirstStateObjectUndefined() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void initializeStateController(String gridconPCS, String b1, String b2, String b3) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void calculateGridMode() throws IllegalArgumentException, OpenemsNamedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void writeStateMachineToChannel() {
		// TODO Auto-generated method stub
		
	}
}
