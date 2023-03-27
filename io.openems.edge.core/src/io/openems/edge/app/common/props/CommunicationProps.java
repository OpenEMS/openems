package io.openems.edge.app.common.props;

import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

public final class CommunicationProps {

	private CommunicationProps() {
	}

	/**
	 * Creates a {@link AppDef} for a ip-address.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> ip() {
		return CommonProps.defaultDef() //
				.setTranslatedLabel("ipAddress") //
				.setDefaultValue("192.168.178.85") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.setValidation(JsonFormlyUtil.InputBuilder.Validation.IP));
	}

	/**
	 * Creates a {@link AppDef} for a port.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> port() {
		return CommonProps.defaultDef() //
				.setTranslatedLabel("port") //
				.setTranslatedDescription("port.description") //
				.setDefaultValue(502) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.setInputType(JsonFormlyUtil.InputBuilder.Type.NUMBER) //
						.setMin(0));
	}

	/**
	 * Creates a {@link AppDef} for a modbusUnitId.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> modbusUnitId() {
		return CommonProps.defaultDef() //
				.setTranslatedLabel("modbusUnitId") //
				.setTranslatedDescription("modbusUnitId.description") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.setInputType(JsonFormlyUtil.InputBuilder.Type.NUMBER) //
						.setMin(0));
	}

}
