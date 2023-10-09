package io.openems.edge.app.api;

import java.util.List;

import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.ReorderArrayBuilder.SelectOptionExpressions;

public final class ModbusTcpApiProps {

	/**
	 * Creates a {@link AppDef} to select {@link ModbusSlave} Components for a
	 * ModbusTcpApi.
	 * 
	 * @param <APP> the type of the {@link OpenemsApp}
	 * @return the {@link AppDef}
	 */
	public static <APP extends OpenemsApp & ComponentUtilSupplier> AppDef<APP, Nameable, BundleProvider> pickModbusIds() {
		return AppDef.copyOfGeneric(ComponentProps.pickOrderedArrayIds(ModbusSlave.class, component -> {
			if ("_meta".equals(component.id())) {
				return false;
			}
			return true;
		}, (app, property, l, parameter, component) -> {
			if ("_sum".equals(component.id())) {
				final var lockedExpression = Exp.currentModelValue(property).asArray() //
						.elementAt(0).equal(Exp.staticValue(component.id()));
				return new SelectOptionExpressions(lockedExpression);
			}
			return null;
		}, List.of((app, property, language, parameter) -> {
			return JsonFormlyUtil.buildText() //
					.setText(TranslationUtil.getTranslation(parameter.bundle(),
							"App.Api.ModbusTcp.changeComponentHint"));
		})), def -> def //
				.setTranslatedLabel("component.id.plural") //
		);
	}

	private ModbusTcpApiProps() {
	}

}
