package io.openems.edge.core.appmanager.formly;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.Nameable;

public class DefaultValueOptions {

	private final Nameable field;
	private final List<Case> cases;

	public DefaultValueOptions(Nameable field, Case... cases) {
		super();
		this.field = field;
		this.cases = Arrays.stream(cases).collect(Collectors.toList());
	}

	/**
	 * Creates a {@link JsonObject} from this {@link DefaultValueOptions}.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.addProperty("field", this.field.name()) //
				.add("cases", this.cases.stream().map(Case::toJsonObject).collect(JsonUtils.toJsonArray())) //
				.build();
	}

}