package io.openems.edge.core.appmanager.formly.builder.selectgroup;

import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

public record Option(//
		/**
		 * Non-null.
		 */
		String value, //
		/**
		 * Nullable.
		 */
		String title, //
		/**
		 * Nullable.
		 */
		Boolean hide, //
		/**
		 * Nullable.
		 */
		OptionExpressions expressions //
) {

	/**
	 * Creates a {@link OptionBuilder}.
	 * 
	 * @param value the value of the option
	 * @return the {@link OptionBuilder}
	 */
	public static OptionBuilder buildOption(String value) {
		return new OptionBuilder(value);
	}

	/**
	 * Creates a {@link JsonElement} from this {@link Option}.
	 * 
	 * @return the {@link JsonElement}
	 */
	public JsonElement toJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("value", this.value) //
				.addPropertyIfNotNull("title", this.title) //
				.addPropertyIfNotNull("hide", this.hide) //
				.onlyIf(this.expressions != null, b -> b.add("expressions", this.expressions.toJson())) //
				.build();
	}

}
