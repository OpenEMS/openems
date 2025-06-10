package io.openems.edge.common.jsonapi;

public final class DummyJsonApi implements JsonApi {

	private final JsonApiBuilder builder;

	public DummyJsonApi(JsonApiBuilder builder) {
		this.builder = builder;
	}

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.addBuilder(this.builder);
	}

}