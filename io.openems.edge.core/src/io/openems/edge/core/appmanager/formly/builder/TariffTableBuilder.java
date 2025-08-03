package io.openems.edge.core.appmanager.formly.builder;

import io.openems.edge.core.appmanager.Nameable;

/**
 * A Builder for a Formly tariff-table.
 *
 * <pre>
 * {
 * 	"key": "key",
 * 	"type": "tariff-table",
 * 	"templateOptions": {
 * 		"label": "label",
 * 		"required": true
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
public final class TariffTableBuilder extends FormlyBuilder<TariffTableBuilder> {

	public TariffTableBuilder(Nameable property) {
		super(property);
	}

	@Override
	protected String getType() {
		return "tariff-table";
	}

}