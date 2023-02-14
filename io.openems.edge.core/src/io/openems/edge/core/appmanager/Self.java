package io.openems.edge.core.appmanager;

public interface Self<T extends Self<T>> {

	/**
	 * Gets itself.
	 * 
	 * @return this
	 */
	public T self();

}
