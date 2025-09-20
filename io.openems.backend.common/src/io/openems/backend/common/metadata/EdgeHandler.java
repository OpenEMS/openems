package io.openems.backend.common.metadata;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.EdgeConfig;

public interface EdgeHandler {

	/**
	 * Gets the {@link EdgeConfig} for an Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the {@link EdgeConfig}
	 * @throws OpenemsNamedException on error
	 */
	public EdgeConfig getEdgeConfig(String edgeId) throws OpenemsNamedException;

}