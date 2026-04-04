package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import org.osgi.service.cm.ConfigurationAdmin;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.api.FixStateOfCharge;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.test.DummyManagedSymmetricEss;
import io.openems.edge.timedata.api.Timedata;

public class DummyFixStateOfChargeController extends AbstractFixStateOfCharge {

	private ManagedSymmetricEss ess;
	private boolean referenceCycleEnabled = false;

	public DummyFixStateOfChargeController() {
		super(
				OpenemsComponent.ChannelId.values(),
				FixStateOfCharge.ChannelId.values());
		this.ess = new DummyManagedSymmetricEss("ess0");
	}

	DummyFixStateOfChargeController withCapacity(int capacityWh) {
		this.ess = new DummyManagedSymmetricEss("ess0").withCapacity(capacityWh);
		return this;
	}

	@Override
	public ComponentManager getComponentManager() {
		return null;
	}

	@Override
	public Sum getSum() {
		return null;
	}

	@Override
	public ManagedSymmetricEss getEss() {
		return this.ess;
	}

	@Override
	public Timedata getTimedata() {
		return null;
	}

	@Override
	public ConfigurationAdmin getConfigurationAdmin() {
		return null;
	}

	@Override
	public boolean isReferenceCycleEnabled() {
		return this.referenceCycleEnabled;
	}

	void withReferenceCycle() {
		this.referenceCycleEnabled = true;
	}

	public Long getExpectedStartEpochSecondsValue() {
		return (Long) this.channel(FixStateOfCharge.ChannelId.EXPECTED_START_EPOCH_SECONDS).getNextValue().get();
	}
}
