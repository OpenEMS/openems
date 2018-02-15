package io.openems.backend.metadata.api;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;

@ProviderType
public interface MetadataService {

	public abstract User getUserWithSession(String sessionId) throws OpenemsException;
	
	public abstract int[] getEdgeIdsForApikey(String apikey);
	
	public abstract Optional<Edge> getEdge(int edgeId);
			
	public abstract void updateEdgeConfig(int edgeId, JsonObject jConfig);

}
