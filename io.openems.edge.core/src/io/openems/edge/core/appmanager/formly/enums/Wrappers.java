package io.openems.edge.core.appmanager.formly.enums;

public enum Wrappers {
	/**
	 * Wrapper for setting the default value dynamically based on the different
	 * {@link Case Cases}.
	 */
	DEFAULT_OF_CASES("formly-wrapper-default-of-cases"), //

	/**
	 * Wrapper for a panel.
	 */
	PANEL("panel"), //

	/**
	 * Input with a popup.
	 */
	SAFE_INPUT("formly-safe-input-wrapper"), //

	/**
	 * Input with unit.
	 */
	INPUT_WITH_UNIT("input-with-unit"), //
	;

	private final String wrapperClass;

	private Wrappers(String wrapperClass) {
		this.wrapperClass = wrapperClass;
	}

	public String getWrapperClass() {
		return this.wrapperClass;
	}

}