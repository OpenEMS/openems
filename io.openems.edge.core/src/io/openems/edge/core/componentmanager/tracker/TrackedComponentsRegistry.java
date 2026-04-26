package io.openems.edge.core.componentmanager.tracker;

import io.openems.edge.common.component.OpenemsComponent;

public sealed interface TrackedComponentsRegistry permits TrackedComponents {

	/**
	 * Notify the registry that a component has been activated.
	 * 
	 * @param servicePid the service PID of the activated component
	 * @param component  the activated component
	 */
	public void activatedComponent(Long servicePid, OpenemsComponent component);

	/**
	 * Notify the registry that a component has been modified.
	 * 
	 * @param servicePid the service PID of the modified component
	 * @param component  the modified component
	 */
	public void modifiedComponent(Long servicePid, OpenemsComponent component);

	/**
	 * Notify the registry that a component has been deactivated.
	 * 
	 * @param servicePid the service PID of the deactivated component
	 * @param component  the deactivated component
	 */
	public void deactivatedComponent(Long servicePid, OpenemsComponent component);

}
