package io.openems.edge.core.appmanager.formly.builder.accordiongroup;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.builder.FormlyBuilder;

public class AccordionBuilder extends FormlyBuilder<AccordionBuilder> {

	private JsonArray fieldGroup;

	public AccordionBuilder(Nameable property) {
		super(property);
	}

	/**
	 * Sets the field group of the Accordion.
	 * 
	 * @param fieldGroup the Properties as a JsonArray
	 * @return the Builder
	 */
	public AccordionBuilder setFieldGroup(JsonArray fieldGroup) {
		this.fieldGroup = fieldGroup;
		return this;
	}

	@Override
	public JsonObject build() {
		var result = super.build();
		result.add("fieldGroup", this.fieldGroup);
		return result;
	}

	@Override
	protected String getType() {
		return "accordion";
	}
}
