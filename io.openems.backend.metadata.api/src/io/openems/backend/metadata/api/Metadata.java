package io.openems.backend.metadata.api;

import java.util.Collection;
import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsException;

@ProviderType
public interface Metadata {

	public abstract User authenticate() throws OpenemsException;

	public abstract User authenticate(String sessionId) throws OpenemsException;

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
		Optional<Edge> edgeOpt = this.getEdge(edgeId);
		if (edgeOpt.isPresent()) {
			return edgeOpt.get();
		} else {
			throw new OpenemsException("Unable to get Edge for id [" + edgeId + "]");
		}
	}

	public abstract Optional<User> getUser(String userId);

	public abstract Collection<Edge> getAllEdges();
}
