package io.openems.edge.app.pvinverter;

import java.util.List;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.JsonUtils.JsonObjectBuilder;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

public final class CommonPvInverterConfiguration {

	private CommonPvInverterConfiguration() {
	}

	private static final AppDef<OpenemsApp, Nameable, BundleParameter> defaultDef() {
		return AppDef.<OpenemsApp, Nameable, BundleParameter>of() //
				.setTranslationBundleSupplier(BundleParameter::getBundle);
	}

	/**
	 * Creates a {@link AppDef} for a PV-Inverter ip-address.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> ip() {
		return defaultDef() //
				.setTranslatedLabel("ipAddress") //
				.setTranslatedDescription("App.PvInverter.ip.description") //
				.setDefaultValue("192.168.178.85") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.isRequired(true) //
						.setValidation(JsonFormlyUtil.InputBuilder.Validation.IP));
	}

	/**
	 * Creates a {@link AppDef} for a PV-Inverter port.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> port() {
		return defaultDef() //
				.setTranslatedLabel("port") //
				.setTranslatedDescription("App.PvInverter.port.description") //
				.setDefaultValue(502) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.setInputType(JsonFormlyUtil.InputBuilder.Type.NUMBER) //
						.setMin(0));
	}

	/**
	 * Creates a {@link AppDef} for a PV-Inverter modbusUnitId.
	 * 
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleParameter> modbusUnitId() {
		return defaultDef() //
				.setTranslatedLabel("modbusUnitId") //
				.setTranslatedDescription("App.PvInverter.modbusUnitId.description") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, prop, l, param, f) -> //
				f.setInputType(JsonFormlyUtil.InputBuilder.Type.NUMBER) //
						.setMin(0));
	}

	/**
	 * Creates the commonly used components for a PV-Inverter.
	 * 
	 * @param factoryId                    the factoryId of the PV-Inverter
	 * @param pvInverterId                 the id of the PV-Inverter
	 * @param modbusId                     the id of the modbus component
	 * @param alias                        the alias of the inverter
	 * @param ip                           the ip of the modbus connection
	 * @param port                         the port of the modbus connection
	 * @param additionalInverterProperties consumer for additional configuration for
	 *                                     the inverter
	 * @param additionalBridgeProperties   consumer for additional configuration for
	 *                                     the modbus component
	 * @return the components
	 */
	public static final List<Component> getComponents(//
			final String factoryId, //
			final String pvInverterId, //
			final String modbusId, //
			final String alias, //
			final String ip, //
			final int port, //
			final Consumer<JsonObjectBuilder> additionalInverterProperties, //
			final Consumer<JsonObjectBuilder> additionalBridgeProperties //
	) {
		final var inverterProperties = JsonUtils.buildJsonObject() //
				.addProperty("modbus.id", modbusId);
		if (additionalInverterProperties != null) {
			additionalInverterProperties.accept(inverterProperties);
		}
		final var bridgeProperties = JsonUtils.buildJsonObject() //
				.addProperty("ip", ip) //
				.addProperty("port", port);
		if (additionalBridgeProperties != null) {
			additionalBridgeProperties.accept(bridgeProperties);
		}

		return Lists.newArrayList(//
				new EdgeConfig.Component(pvInverterId, alias, factoryId, inverterProperties.build()), //
				new EdgeConfig.Component(modbusId, alias, "Bridge.Modbus.Tcp", bridgeProperties.build())//
		);
	}

}
