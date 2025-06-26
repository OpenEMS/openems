package io.openems.edge.core.appmanager.formly.builder;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.Nameable;

public final class RangeBuilder extends FormlyBuilder<RangeBuilder> {

	public RangeBuilder(Nameable property) {
		super(property);
	}

	/**
	 * Sets the min value of the input.
	 *
	 * @param min the min number that can be set
	 * @return this
	 */
	public RangeBuilder setMin(int min) {
		this.templateOptions.addProperty("min", min);
		return this;
	}

	/**
	 * Sets the max value of the input.
	 *
	 * @param max the max number that can be set
	 * @return this
	 */
	public RangeBuilder setMax(int max) {
		this.templateOptions.addProperty("max", max);
		return this;
	}

	@Override
	public JsonObject build() {
		this.templateOptions.add("attributes", JsonUtils.buildJsonObject() //
				.addProperty("pin", true) //
				.build());
		return super.build();
	}

	@Override
	protected String getType() {
		return "range";
	}

}