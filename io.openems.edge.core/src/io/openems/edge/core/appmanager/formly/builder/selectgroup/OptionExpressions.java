package io.openems.edge.core.appmanager.formly.builder.selectgroup;

import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;

public record OptionExpressions(//
		/**
		 * Nullable.
		 */
		BooleanExpression hide, //
		/**
		 * Nullable.
		 */
		BooleanExpression disabled, //
		/**
		 * Nullable.
		 */
		StringExpression title //
) {

	/**
	 * Creates a {@link JsonElement} from this {@link OptionExpressions}.
	 * 
	 * @return the {@link JsonElement}
	 */
	public JsonElement toJson() {
		return JsonUtils.buildJsonObject() //
				.onlyIf(this.hide() != null, b -> b.addProperty("hideString", this.hide().expression()))
				.onlyIf(this.disabled() != null, b -> b.addProperty("disabledString", this.disabled().expression()))
				.onlyIf(this.title() != null, b -> b.addProperty("titleString", this.title().expression())) //
				.build();
	}

}