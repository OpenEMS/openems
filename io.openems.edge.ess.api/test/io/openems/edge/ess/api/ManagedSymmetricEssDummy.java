package io.openems.edge.ess.api;


import java.util.Collection;

import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.symmetric.api.ManagedSymmetricEss;

public class ManagedSymmetricEssDummy implements ManagedSymmetricEss {

	@Override
	public String id() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String servicePid() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ComponentContext componentContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Channel<?> _channel(String channelName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Channel<?>> channels() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Power getPower() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
//		check.act(activePowerExpected, reactivePowerEexpected, activePower, reactivePower);	
	}

	@Override
	public int getPowerPrecision() {
		// TODO Auto-generated method stub
		return 1;
	}

}
