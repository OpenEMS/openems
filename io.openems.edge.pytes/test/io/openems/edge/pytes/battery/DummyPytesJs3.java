package io.openems.edge.pytes.battery;

import org.osgi.service.event.Event;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.ess.PytesJs3;

/**
 * Minimal ESS stand-in so that {@link PytesBatteryImpl} can be activated in a
 * {@link io.openems.edge.common.test.ComponentTest}.
 */
public class DummyPytesJs3 extends AbstractDummyOpenemsComponent<DummyPytesJs3> implements PytesJs3 {

	private PytesBattery battery;
	private PytesDcCharger charger;

	public DummyPytesJs3(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				PytesJs3.ChannelId.values() //
		);
	}

	@Override
	protected DummyPytesJs3 self() {
		return this;
	}

	@Override
	public void handleEvent(Event event) {
		// nothing to do
	}

	@Override
	public void addBattery(PytesBattery battery) {
		this.battery = battery;
	}

	@Override
	public void removeBattery(PytesBattery battery) {
		if (this.battery == battery) {
			this.battery = null;
		}
	}

	@Override
	public void addCharger(PytesDcCharger charger) {
		this.charger = charger;
	}

	@Override
	public void removeCharger(PytesDcCharger charger) {
		if (this.charger == charger) {
			this.charger = null;
		}
	}

	@Override
	public String getModbusBridgeId() {
		return "modbus0";
	}

	@Override
	public Integer getUnitId() {
		return 1;
	}

	/**
	 * Gets the battery that registered itself.
	 *
	 * @return the battery or null
	 */
	public PytesBattery getBattery() {
		return this.battery;
	}

	/**
	 * Gets the charger that registered itself.
	 *
	 * @return the charger or null
	 */
	public PytesDcCharger getCharger() {
		return this.charger;
	}
}
