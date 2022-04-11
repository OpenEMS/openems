package io.openems.backend.common.metadata;

import java.util.Collection;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsException;

@ProviderType
public interface EdgeMetadata {
	
	/**
	 * Gets the Edge-ID for an API-Key, i.e. authenticates the API-Key.
	 *
	 * @param apikey the API-Key
	 * @return the Edge-ID or Empty
	 */
	public abstract Optional<String> getEdgeIdForApikey(String apikey);
	
	/**
	 * Get an Edge by its unique Edge-ID.
	 *
	 * @param edgeId the Edge-ID
	 * @return the Edge as Optional
	 */
	public abstract Optional<Edge> getEdge(String edgeId);

	/**
	 * Get an Edge by its unique Edge-ID. Throws an Exception if there is no Edge
	 * with this ID.
	 *
	 * @param edgeId the Edge-ID
	 * @return the Edge
	 * @throws OpenemsException on error
	 */
	public default Edge getEdgeOrError(String edgeId) throws OpenemsException {
		var edgeOpt = this.getEdge(edgeId);
		if (edgeOpt.isPresent()) {
			return edgeOpt.get();
		}
		throw new OpenemsException("Unable to get Edge for id [" + edgeId + "]");
	}
	
	/**
	 * Get an Edge by Edge-SetupPassword.
	 *
	 * @param setupPassword to find Edge
	 * @return Edge as a Optional
	 */
	public abstract Optional<Edge> getEdgeBySetupPassword(String setupPassword);
	
	/**
	 * Gets all Edges.
	 *
	 * @return collection of Edges.
	 */
	public abstract Collection<Edge> getAllEdges();

}
