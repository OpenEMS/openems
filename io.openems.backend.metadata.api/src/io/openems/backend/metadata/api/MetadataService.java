package io.openems.backend.metadata.api;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.exceptions.OpenemsException;

@ProviderType
public interface MetadataService {

	public abstract Optional<User> getUserWithSession(String sessionId) throws OpenemsException;
	
	public abstract int[] getEdgeIdsForApikey(String apikey);
	
	public abstract Optional<Edge> getEdge(int edgeId);
	
	public abstract Optional<User> getUser(int userId);
}
