package io.openems.edge.ess.api;

import java.util.Collection;

import org.osgi.service.component.ComponentContext;

import io.openems.edge.common.channel.Channel;
import io.openems.edge.ess.asymmetric.api.ManagedAsymmetricEss;
import io.openems.edge.ess.power.api.Power;

public class ManagedAsymmetricEssDummy implements ManagedAsymmetricEss {

	@Override
	public Power getPower() {
		// TODO Auto-generated method stub
		return null;
	}

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
	public void applyPower(int activePowerL1, int reactivePowerL1, int activePowerL2, int reactivePowerL2,
			int activePowerL3, int reactivePowerL3) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getPowerPrecision() {
		// TODO Auto-generated method stub
		return 1;
	}

}
