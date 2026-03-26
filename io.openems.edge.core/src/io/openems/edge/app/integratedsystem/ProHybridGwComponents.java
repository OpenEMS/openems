package io.openems.edge.app.integratedsystem;

import java.util.ResourceBundle;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

public class ProHybridGwComponents {

	/**
	 * Creates a default ess component for a FENECON Pro Hybrid GW.
	 * 
	 * @param bundle the translation bundle
	 * @param essId  es id of the ess
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef ess(//
			final ResourceBundle bundle, //
			String essId //
	) {
		return new ComponentDef(essId, //
				TranslationUtil.getTranslation(bundle, "App.FENECON.ProHybrid.GW.Name"), "GoodWe.Ess", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("capacity", 9000) //
						.addProperty("controlMode", ControlMode.INTERNAL) //
						.addProperty("maxBatteryPower", 5200) //
						.addProperty("modbus.id", "modbus0") //
						.addProperty("modbusUnitId", 247) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default grid meter for a FENECON Pro Hybrid GW.
	 * 
	 * @param bundle       the translation bundle
	 * @param meterId      the id of the meter
	 * @param modbusId     the id of the modbus bridge
	 * @param modbusUnitId the id of the modbus unit
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef gridMeter(//
			final ResourceBundle bundle, //
			final String meterId, //
			final String modbusId, //
			final int modbusUnitId //
	) {
		return new ComponentDef(meterId, //
				TranslationUtil.getTranslation(bundle, "gridMeter"), "GoodWe.Grid-Meter", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("modbus.id", modbusId) //
						.addProperty("modbusUnitId", modbusUnitId) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default modbus component for a FENECON Pro Hybrid GW.
	 * 
	 * @param modbusId the id of the modbus component
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef modbus(//
			final String modbusId //
	) {
		return new ComponentDef(modbusId, modbusId, "Bridge.Modbus.Serial", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.addProperty("invalidateElementsAfterReadErrors", 1) //
						.addProperty("logVerbosity", LogVerbosity.NONE) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/ttyUSB0") //
						.addProperty("stopbits", "ONE") //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default DC PV charger component for a FENECON Pro Hybrid GW.
	 * 
	 * @param pvId         the id of the charger
	 * @param pvAlias      the alias of the pv charger
	 * @param factoryId    the factoryId of the component
	 * @param essId        the id of the ess
	 * @param modbusId     the id of the modbus bridge
	 * @param modbusUnitId the id of the modbus unit
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef dcPv(//
			final String pvId, //
			final String pvAlias, //
			final String factoryId, //
			final String essId, //
			final String modbusId, //
			final int modbusUnitId //
	) {
		return new ComponentDef(pvId, pvAlias, factoryId, //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("essOrBatteryInverter.id", essId) //
						.addProperty("modbus.id", modbusId) //
						.addProperty("modbusUnitId", modbusUnitId) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}
}
