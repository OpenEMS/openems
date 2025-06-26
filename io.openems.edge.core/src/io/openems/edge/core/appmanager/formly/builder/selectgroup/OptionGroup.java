package io.openems.edge.core.appmanager.formly.builder.selectgroup;

import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.util.List;

import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

public record OptionGroup(//
		/**
		 * Non-null.
		 */
		String group, //
		/**
		 * Non-null.
		 */
		String title, //
		/**
		 * Non-null.
		 */
		List<Option> options //
) {

	/**
	 * Creates a {@link OptionGroupBuilder}.
	 * 
	 * @param value the value of the option
	 * @return the {@link OptionGroupBuilder}
	 */
	public static OptionGroupBuilder buildOptionGroup(String group, String title) {
		return new OptionGroupBuilder(group, title);
	}

	/**
	 * Creates a {@link JsonElement} from this {@link OptionGroup}.
	 * 
	 * @return the {@link JsonElement}
	 */
	public JsonElement toJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("group", this.group) //
				.addProperty("title", this.title) //
				.add("options", this.options.stream() //
						.map(Option::toJson) //
						.collect(toJsonArray())) //
				.build();
	}

}