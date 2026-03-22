package io.openems.edge.core.appmanager.formly.builder.accordiongroup;

import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.formly.builder.FormlyBuilder;

public class AccordionGroupBuilder extends FormlyBuilder<AccordionGroupBuilder> {

	private JsonArray fieldGroup;

	public AccordionGroupBuilder(Nameable property) {
		super(property);
	}

	/**
	 * Sets the field group of the Accordion Group.
	 * 
	 * @param accordions the accordions for the group as a JsonArray
	 * @return the Builder
	 */
	public AccordionGroupBuilder setFieldGroup(JsonArray accordions) {
		this.fieldGroup = accordions;
		return this;
	}

	/**
	 * Sets the accordions that should be expanded on initialization.
	 * 
	 * @param accordions the accordions that should be expanded
	 * @return the Builder
	 */
	public AccordionGroupBuilder setOpenAccordions(Nameable... accordions) {
		if (accordions.length == 0) {
			this.templateOptions.remove("openAccordions");
		}

		var jsonAccordions = Arrays.stream(accordions) //
				.map(Nameable::name) //
				.map(JsonPrimitive::new) //
				.collect(JsonUtils.toJsonArray());

		this.templateOptions.add("openAccordions", jsonAccordions);
		return this;
	}

	/**
	 * Sets the 'Multiple' parameter to unfold multiple accordions simultaneously.
	 *
	 * @param isMulti isMulti
	 * @return this builder
	 */
	public AccordionGroupBuilder setMulti(boolean isMulti) {
		if (isMulti) {
			this.templateOptions.addProperty("isMulti", true);
		} else {
			this.templateOptions.remove("isMulti");
		}
		return this;
	}

	/**
	 * Sets the missing text if no accordions were added to the
	 * AccordionGroupBuilder.
	 *
	 * @param text the missing text
	 * @return this builder
	 */
	public AccordionGroupBuilder setMissingOptionsText(String text) {
		if (text != null) {
			this.templateOptions.addProperty("missingOptionsText", text);
		} else {
			this.templateOptions.remove("missingOptionsText");
		}
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
		return "accordion-group";
	}
}
