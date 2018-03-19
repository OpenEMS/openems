package io.openems.common.types;

public interface OpenemsComponent {

	/**
	 * Returns a unique ID for this Thing (i.e. the OSGi service.pid)
	 * 
	 * @return
	 */
	String id();

	/**
	 * Returns whether this component is active (i.e. the OSGi components'
	 * activator() was executed, but deactivate() was not)
	 * 
	 * @return
	 */
	boolean isActive();

	/**
	 * Returns whether this component is enabled
	 * 
	 * @return
	 */
	boolean isEnabled();

	/**
	 * Enables or disables this component
	 */
	void setEnabled(boolean isEnabled);
}
