package io.openems.edge.heat.test;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;

public class DummyManagedHeatElement extends AbstractOpenemsComponent implements OpenemsComponent {

	private String id;

	/**
	 * Instantiates a disabled {@link DummyManagedHeatElement}.
	 * 
	 * @param id the Component-ID
	 * @return a new {@link DummyManagedHeatElement}
	 */
	public static DummyManagedHeatElement ofDisabled(String id) {
		return new DummyManagedHeatElement(id);
	}

	public DummyManagedHeatElement(String id) {
		this(id, true);
	}

	private DummyManagedHeatElement(String id, boolean isEnabled) {
		super(//
				OpenemsComponent.ChannelId.values() //
		);
		this.id = id;
		super.activate(null, id, "", isEnabled);
	}

	@Override
	public String id() {
		return this.id;
	}

	@Override
	public boolean isEnabled() {
		return false;
	}

	public double getCurTemperature() {
		return 0;
	}

	public int getStatus() {
		return 0;
	}

	public boolean getIsReadOnly() {
		return false;
	}

	@Override
	public String alias() {
		return null;
	}
}
