package io.openems.edge.pytes.ess;

import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.pytes.battery.PytesBattery;

/**
 * Fake battery for {@link ApplyPowerHandlerTest}.
 */
public class DummyPytesBattery extends AbstractDummyOpenemsComponent<DummyPytesBattery> implements PytesBattery {

	public DummyPytesBattery(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				Battery.ChannelId.values(), //
				PytesBattery.ChannelId.values() //
		);
	}

	@Override
	protected DummyPytesBattery self() {
		return this;
	}

	DummyPytesBattery withDcDischargePower(int value) {
		TestUtils.withValue(this, PytesBattery.ChannelId.DC_DISCHARGE_POWER, value);
		return this;
	}

	DummyPytesBattery withBackupLoadPower(int value) {
		TestUtils.withValue(this, PytesBattery.ChannelId.BACKUP_LOAD_POWER, value);
		return this;
	}

	@Override
	public int getConfiguredMaxChargeCurrent() {
		return 40;
	}

	@Override
	public int getConfiguredMaxDischargeCurrent() {
		return 40;
	}

	@Override
	public void setStartStop(StartStop value) {
		// not used
	}
}
