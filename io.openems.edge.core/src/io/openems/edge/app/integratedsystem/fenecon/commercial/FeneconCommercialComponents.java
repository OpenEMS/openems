package io.openems.edge.app.integratedsystem.fenecon.commercial;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.util.ResourceBundle;

import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.meter.KdkMeter;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;

public final class FeneconCommercialComponents {

	/**
	 * Creates a default battery inverter component for a FENECON Commercial 92.
	 * 
	 * @param bundle            the translation bundle
	 * @param batteryInverterId the id of the battery inverter
	 * @param modbusId          the id of the modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component batteryInverter(//
			final ResourceBundle bundle, //
			final String batteryInverterId, //
			final String modbusId //
	) {
		return new EdgeConfig.Component(batteryInverterId,
				translate(bundle, "App.IntegratedSystem.batteryInverter0.alias"),
				"Battery-Inverter.Kaco.BlueplanetGridsave", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.addProperty("startStop", "AUTO") //
						.build());
	}

	/**
	 * Creates a default modbus bridge component to the battery inverter for a
	 * FENECON Commercial 92.
	 * 
	 * @param bundle   the translation bundle
	 * @param t        the current {@link ConfigurationTarget}
	 * @param modbusId the id of the modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusToBatteryInverter(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusId //
	) {
		return new EdgeConfig.Component(modbusId, translate(bundle, "App.IntegratedSystem.modbus1.alias"),
				"Bridge.Modbus.Tcp", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("ip", "172.16.0.100") //
						.addProperty("port", 502) //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b//
								.addProperty("invalidateElementsAfterReadErrors", 1) //
								.addProperty("logVerbosity", "NONE"))
						.build());
	}

	/**
	 * Creates a default modbus bridge component to the grid meter for a FENECON
	 * Commercial 92.
	 * 
	 * @param bundle   the translation bundle
	 * @param t        the current {@link ConfigurationTarget}
	 * @param modbusId the id of the external modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusToGridMeter(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusId //
	) {
		return new EdgeConfig.Component(modbusId, translate(bundle, "App.IntegratedSystem.modbusToGridMeter.alias"),
				"Bridge.Modbus.Serial", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/busUSB2") //
						.addProperty("stopbits", "ONE") //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b//
								.addProperty("invalidateElementsAfterReadErrors", 1) //
								.addProperty("logVerbosity", "NONE"))
						.build());
	}

	/**
	 * Creates a default gridMeter dependency for a FENECON Commercial 92.
	 * 
	 * @param bundle      the translation bundle
	 * @param gridMeterId the id of the grid meter
	 * @param modbusId    the id of the modbus bridge
	 * @return the {@link DependencyDeclaration}
	 */
	public static DependencyDeclaration gridMeter(//
			final ResourceBundle bundle, //
			final String gridMeterId, //
			final String modbusId //
	) {
		return new DependencyDeclaration("GRID_METER", //
				DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.Meter.Kdk") //
						.setAlias(translate(bundle, "App.Meter.gridMeter")) //
						.setProperties(JsonUtils.buildJsonObject() //
								.addProperty(KdkMeter.Property.METER_ID.name(), gridMeterId) //
								.addProperty(KdkMeter.Property.MODBUS_ID.name(), modbusId) //
								.addProperty(KdkMeter.Property.MODBUS_UNIT_ID.name(), 5) //
								.addProperty(KdkMeter.Property.TYPE.name(), MeterType.GRID) //
								.build())
						.build());
	}

	private FeneconCommercialComponents() {
	}

}
