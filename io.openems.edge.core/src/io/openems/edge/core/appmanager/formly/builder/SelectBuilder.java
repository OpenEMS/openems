package io.openems.edge.core.appmanager.formly.builder;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.Nameable;

/**
 * A Builder for a Formly Select.
 *
 * <pre>
 * {
 * 	"key": "key",
 * 	"type": "select",
 * 	"templateOptions": {
 * 		"label": "label",
 * 		"required": true,
 * 		"multiple": true,
 * 		"options": [
 * 			{
 * 				"label": "label",
 * 				"value": "value"
 * 			}, ...
 * 		]
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
public final class SelectBuilder extends FormlyBuilder<SelectBuilder> {

	public static final Function<OpenemsComponent, JsonElement> DEFAULT_COMPONENT_2_LABEL = t -> new JsonPrimitive(
			t.alias() == null || t.alias().isEmpty() ? t.id() : t.id() + ": " + t.alias());
	public static final Function<OpenemsComponent, JsonElement> DEFAULT_COMPONENT_2_VALUE = t -> new JsonPrimitive(
			t.id());

	public SelectBuilder(Nameable property) {
		super(property);
	}

	public SelectBuilder setOptions(JsonArray options) {
		this.templateOptions.add("options", options);
		return this;
	}

	/**
	 * Note the {@link Map#entry(Object, Object)} does not return a
	 * {@link Comparable} Object so the {@link Set} can not be a {@link TreeSet}.
	 *
	 * @param items the options
	 * @return this
	 */
	public SelectBuilder setOptions(Set<Entry<String, String>> items) {
		return this.setOptions(items, t -> t, t -> t);
	}

	public <T, C> SelectBuilder setOptions(Set<Entry<T, C>> items, Function<T, String> item2Label,
			Function<C, String> item2Value) {
		var options = JsonUtils.buildJsonArray();
		items.stream().forEach(t -> {
			options.add(JsonUtils.buildJsonObject() //
					.addProperty("label", item2Label.apply(t.getKey())) //
					.addProperty("value", item2Value.apply(t.getValue())) //
					.build());
		});
		return this.setOptions(options.build());
	}

	public SelectBuilder setOptions(List<String> items) {
		return this.setOptions(items, JsonPrimitive::new, JsonPrimitive::new);
	}

	public <T> SelectBuilder setOptions(List<? extends T> items, Function<T, JsonElement> item2Label,
			Function<T, JsonElement> item2Value) {
		var options = JsonUtils.buildJsonArray();
		for (var item : items) {
			options.add(JsonUtils.buildJsonObject() //
					.add("label", item2Label.apply(item)) //
					.add("value", item2Value.apply(item)) //
					.build());
		}
		return this.setOptions(options.build());
	}

	public SelectBuilder setOptions(OptionsFactory factory, Language l) {
		return this.setOptions(factory.options(l));
	}

	/**
	 * Sets if more than one options can be selected.
	 *
	 * @param isMulti if more options can be selected
	 * @return this
	 */
	public SelectBuilder isMulti(boolean isMulti) {
		if (isMulti) {
			this.templateOptions.addProperty("multiple", isMulti);
		} else if (this.templateOptions.has("multiple")) {
			this.templateOptions.remove("multiple");
		}
		return this;
	}

	@Override
	protected String getType() {
		return "select";
	}

}