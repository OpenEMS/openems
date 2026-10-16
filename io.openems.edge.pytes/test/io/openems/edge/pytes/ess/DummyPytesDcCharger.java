package io.openems.edge.pytes.ess;

import org.osgi.service.event.Event;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.pytes.dccharger.PytesDcCharger;

/**
 * Fake DC charger (PV) for {@link ApplyPowerHandlerTest}.
 */
public class DummyPytesDcCharger extends AbstractDummyOpenemsComponent<DummyPytesDcCharger> implements PytesDcCharger {

	public DummyPytesDcCharger(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				PytesDcCharger.ChannelId.values() //
		);
	}

	@Override
	protected DummyPytesDcCharger self() {
		return this;
	}

	DummyPytesDcCharger withActualPower(int value) {
		TestUtils.withValue(this, EssDcCharger.ChannelId.ACTUAL_POWER, value);
		return this;
	}

	@Override
	public void handleEvent(Event event) {
		// not used
	}
}
