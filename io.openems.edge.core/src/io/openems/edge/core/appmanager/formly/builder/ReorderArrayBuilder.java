package io.openems.edge.core.appmanager.formly.builder;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;

public class ReorderArrayBuilder extends FormlyBuilder<ReorderArrayBuilder> {

	private boolean allowDuplicates = false;
	private final List<SelectOption> selectOptions = new ArrayList<>();

	public record SelectOption(//
			String label, //
			String value, //
			SelectOptionExpressions expressions //
	) {

		/**
		 * Creates a {@link JsonElement} from this {@link SelectOption}.
		 * 
		 * @return the {@link JsonElement}
		 */
		public JsonElement toJson() {
			return JsonUtils.buildJsonObject() //
					.addProperty("label", this.label()) //
					.addProperty("value", this.value()) //
					.onlyIf(this.expressions != null, b -> b.add("expressions", this.expressions().toJson())) //
					.build();
		}

	}

	public record SelectOptionExpressions(//
			BooleanExpression locked //
	) {

		/**
		 * Creates a {@link JsonElement} from this {@link SelectOption}.
		 * 
		 * @return the {@link JsonElement}
		 */
		public JsonElement toJson() {
			return JsonUtils.buildJsonObject() //
					.onlyIf(this.locked() != null, b -> b.addProperty("lockedString", this.locked().expression())) //
					.build();
		}

	}

	public ReorderArrayBuilder(Nameable property) {
		super(property);
	}

	/**
	 * Adds a {@link SelectOption} option to this input.
	 * 
	 * @param option the option to add
	 * @return this
	 */
	public ReorderArrayBuilder addSelectOption(SelectOption option) {
		this.selectOptions.add(option);
		return this;
	}

	@Override
	protected String getType() {
		return "reorder-array";
	}

	@Override
	public JsonObject build() {
		this.templateOptions.addProperty("allowDuplicates", this.allowDuplicates);
		this.templateOptions.add("selectOptions", this.selectOptions.stream() //
				.map(SelectOption::toJson) //
				.collect(JsonUtils.toJsonArray()));
		return super.build();
	}

}
