package io.openems.edge.core.appmanager.formly.builder;

import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.selectgroup.OptionGroup;
import io.openems.edge.core.appmanager.formly.enums.DisplayType;

/**
 * A Builder for a Formly Select Group.
 *
 * <pre>
 * {
 * 	"key": "key",
 * 	"type": "formly-option-group-picker",
 * 	"templateOptions": {
 * 		"label": "label",
 * 		"required": true,
 * 		"options": OptionGroup[]
 * 	},
 * 	"expressionProperties": {
 * 		"templateOptions.required": "model.PROPERTY"
 * 	},
 * 	"hideExpression": "!model.PROPERTY",
 * 	"defaultValue": "defaultValue"
 * }
 * </pre>
 *
 */
public final class SelectGroupBuilder extends FormlyBuilder<SelectGroupBuilder> {

	private final Nameable property;
	private final List<OptionGroup> optionGroups = new ArrayList<>();

	public SelectGroupBuilder(Nameable property) {
		super(property);
		this.property = property;
	}

	@Override
	protected String getType() {
		return "formly-option-group-picker";
	}

	/**
	 * Adds a {@link OptionGroup} to this {@link SelectGroupBuilder}.
	 * 
	 * @param optionGroup the {@link OptionGroup} to add
	 * @return this
	 */
	public SelectGroupBuilder addOption(OptionGroup optionGroup) {
		this.optionGroups.add(optionGroup);
		return this;
	}

	@Override
	public JsonObject build() {
		// wrap input field into a popup input
		final var fieldGroup = JsonFormlyUtil.buildFieldGroupFromNameable(this.property);
		// copy my settings into parent field
		this.templateOptions.entrySet()
				.forEach(entry -> fieldGroup.templateOptions.add(entry.getKey(), entry.getValue()));
		this.jsonObject.entrySet().forEach(entry -> fieldGroup.jsonObject.add(entry.getKey(), entry.getValue()));

		// set options
		this.templateOptions.add("options", this.optionGroups.stream() //
				.map(OptionGroup::toJson) //
				.collect(toJsonArray()));
		fieldGroup.setFieldGroup(JsonUtils.buildJsonArray() //
				.add(super.build()) //
				.build()) //
				.setDefaultValue(this.getDefaultValue());

		fieldGroup.setPopupInput(this.property, DisplayType.OPTION_GROUP);

		return fieldGroup.build();
	}

}