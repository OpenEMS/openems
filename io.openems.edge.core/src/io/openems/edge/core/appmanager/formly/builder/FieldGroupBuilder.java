package io.openems.edge.core.appmanager.formly.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.enums.DisplayType;
import io.openems.edge.core.appmanager.formly.enums.Wrappers;

public final class FieldGroupBuilder extends FormlyBuilder<FieldGroupBuilder> {

	private JsonArray fieldGroup;

	public FieldGroupBuilder(Nameable property) {
		super(property);
	}

	public FieldGroupBuilder setFieldGroup(JsonArray fieldGroup) {
		this.fieldGroup = fieldGroup;
		return this.self();
	}

	public FieldGroupBuilder setPopupInput(Nameable displayValue, DisplayType displayType) {
		this.addWrapper(Wrappers.SAFE_INPUT);
		this.templateOptions.addProperty("pathToDisplayValue", displayValue.name());
		this.templateOptions.addProperty("displayType", displayType.getTypeName());
		return this;
	}

	@Override
	protected String getType() {
		return null;
	}

	@Override
	public JsonObject build() {
		final var object = super.build();
		final var templateOptions = object.get("templateOptions").getAsJsonObject();
		templateOptions.remove("required");
		JsonUtils.getAsOptionalJsonObject(object, "expressionProperties") //
				.map(t -> t.remove("templateOptions.required"));
		object.add("fieldGroup", this.fieldGroup);
		return JsonUtils.buildJsonObject() //
				.add("hideExpression", object.remove("hideExpression")) //
				.add("fieldGroup", JsonUtils.buildJsonArray() //
						.add(object) //
						.build())
				.build();
	}

}