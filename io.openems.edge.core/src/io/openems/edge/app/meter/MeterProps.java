package io.openems.edge.app.meter;

import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter;

public final class MeterProps {

	private MeterProps() {
	}

	/**
	 * Creates a {@link AppDef} for a meter type.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, Parameter.BundleParameter> type() {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("App.Meter.mountType.label") //
						.setDefaultValue(MeterType.PRODUCTION) //
						.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
							field.setOptions(OptionsFactory.of(MeterType.class), l);
						}));
	}

	/**
	 * Creates a {@link AppDef} for a modbusUnitId for a meter.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, Parameter.BundleParameter> modbusUnitId() {
		return AppDef.copyOfGeneric(CommunicationProps.modbusUnitId(), def -> {
			def.setTranslatedDescription("App.Meter.modbusUnitId.description");
		});
	}

}
