package io.openems.backend.metadata.api;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsException;

@ProviderType
public interface MetadataService {

	public abstract User getUserWithSession(String sessionId) throws OpenemsException;

	public abstract int[] getEdgeIdsForApikey(String apikey);

	public abstract Optional<Edge> getEdgeOpt(int edgeId);

	public default Edge getEdge(int edgeId) throws OpenemsException {
		Optional<Edge> edgeOpt = this.getEdgeOpt(edgeId);
		if (edgeOpt.isPresent()) {
			return edgeOpt.get();
		} else {
			throw new OpenemsException("Unable to get Edge for id [" + edgeId + "]");
		}
	}

	public abstract Optional<User> getUser(int userId);
}
