package io.openems.edge.core.componentmanager.tracker;

import java.util.List;
import java.util.Optional;

import io.openems.edge.common.component.OpenemsComponent;

public sealed interface TrackedComponentsView permits TrackedComponents {

	/**
	 * Get all tracked components.
	 *
	 * @return a list of tracked components
	 */
	public List<OpenemsComponent> getAllComponents();

	/**
	 * Get all enabled tracked components.
	 *
	 * @return a list of enabled tracked components
	 */
	public List<OpenemsComponent> getEnabledComponents();

	/**
	 * Get a tracked component by its ID.
	 *
	 * @param componentId the ID of the component
	 * @return an Optional containing the component if found, otherwise empty
	 */
	public Optional<OpenemsComponent> getComponentById(String componentId);

	/**
	 * Get all IDs of the tracked components.
	 *
	 * @return a collection of IDs of the tracked components
	 */
	public List<String> getAllComponentIds();

	/**
	 * Get all IDs of the components that have duplicates.
	 *
	 * @return a collection of IDs of the components that have duplicates
	 */
	public List<String> getDuplicateIds();

	/**
	 * Check if there are any duplicate components.
	 *
	 * @return true if there are duplicate components, false otherwise
	 */
	public boolean hasDuplicates();

}
