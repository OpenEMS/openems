package io.openems.edge.core.appmanager;

public enum ConfigurationTarget {

	/**
	 * Configuration will be used to Add an instance of the App.
	 */
	ADD,
	/**
	 * Configuration will be used to Update/Repair an instance of the App.
	 */
	UPDATE,
	/**
	 * Configuration will be used to validate an instance of the App.
	 */
	VALIDATE,
	/**
	 * Configuration will be used to delete. Only cares about the Component-IDs
	 */
	DELETE,

	/**
	 * Configuration will be used to test which ids can be replaced even though it
	 * has errors e. g. not all passed values can be converted to enums but still
	 * get the {@link AppConfiguration} an not an exception.
	 */
	TEST;

	public boolean isDeleteOrTest() {
		return this == DELETE || this == TEST;
	}

	public boolean isAddOrUpdate() {
		return this == ADD || this == UPDATE;
	}
}
