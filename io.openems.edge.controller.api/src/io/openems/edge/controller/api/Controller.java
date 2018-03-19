package io.openems.edge.controller.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsComponent;

@ProviderType
public interface Controller extends OpenemsComponent {

	/**
	 * Execute the Controller logic
	 */
	public void run();
}
