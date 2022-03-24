package io.openems.edge.core.appmanager.validator;

import io.openems.edge.common.component.ComponentManager;

public class CheckHome implements Checkable {

	private final ComponentManager componentManager;

	public CheckHome(ComponentManager componentManager) {
		this.componentManager = componentManager;
	}

	@Override
	public boolean check() {
		var batteries = this.componentManager.getEdgeConfig().getComponentsByFactory("Battery.Fenecon.Home");
		return !batteries.isEmpty();
	}

	@Override
	public String getErrorMessage() {
		return "No Home installed";
	}

}
