package io.openems.edge.common.jsonapi;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public final class Subrequest {

	public record Subroute(String[] path, JsonApiBuilder builder, Supplier<JsonArray> endpointSupplier) {

	}

	private final JsonElement baseRequest;
	private final List<Subroute> subrouteToBuilder = new ArrayList<>();

	public Subrequest(JsonElement baseRequest) {
		this.baseRequest = baseRequest;
	}

	/**
	 * Adds a {@link JsonApiBuilder} for the {@link Subrequest} associated to the
	 * provided path.
	 * 
	 * @param builder the {@link JsonApiBuilder} to add at the path
	 * @param path    the path which the builder gets associated to
	 */
	public void addRpcBuilderFor(JsonApiBuilder builder, String... path) {
		this.subrouteToBuilder.add(new Subroute(path, builder, null));
	}

	public JsonElement getBaseRequest() {
		return this.baseRequest;
	}

	public List<Subroute> getSubrouteToBuilder() {
		return this.subrouteToBuilder;
	}

}
