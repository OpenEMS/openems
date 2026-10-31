package io.openems.edge.app.integratedsystem;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

public class Pro912Components {

	/**
	 * Creates a default ess component for a FENECON Pro 9-12.
	 *
	 * @param essId    es id of the ess
	 * @param modbusId the id of the modbus bridge
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef ess(//
			String essId, //
			String modbusId //
	) {
		return new ComponentDef(essId, essId, "Fenecon.Pro.Ess", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("modbus.id", modbusId) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default grid meter for a FENECON Pro Hybrid 9-12.
	 *
	 * @param meterId   the id of the meter
	 * @param factoryId the factory id of the meter
	 * @param modbusId  the id of the modbus bridge
	 * @param t         the current {@link ConfigurationTarget}
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef gridMeter(//
			final String meterId, //
			final String factoryId, //
			final String modbusId, //
			final ConfigurationTarget t //
	) {
		return new ComponentDef(meterId, meterId, factoryId, //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("modbus.id", modbusId)) //
						.addProperty("modbusUnitId", 5) //
						.addProperty("invert", false) //
						.addProperty("type", MeterType.GRID) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default modbus component for a FENECON Pro Hybrid 9-12.
	 * 
	 * @param modbusId the id of the modbus component
	 * @param t        the current {@link ConfigurationTarget}
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef modbus(//
			final String modbusId, //
			final ConfigurationTarget t //
	) {
		return new ComponentDef(modbusId, modbusId, "Bridge.Modbus.Serial", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.onlyIf(t == ConfigurationTarget.ADD,
								b -> b.addProperty("invalidateElementsAfterReadErrors", 1)) //
						.addProperty("logVerbosity", LogVerbosity.NONE) //
						.addProperty("parity", Parity.NONE) //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("portName", "/dev/ttyUSB0")) //
						.addProperty("stopbits", "ONE") //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default limit total discharge controller for a FENECON Pro 9-12.
	 *
	 * @param limitTotalDischargeId the id of the limit total discharge component
	 * @param essId                 the id of the ess
	 * @param forceChargeSoc        the soc below which charging is forced
	 * @param minSoc                the soc below which discharging is disabled
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef limitTotalDischarge(//
			final String limitTotalDischargeId, //
			final String essId, //
			final int forceChargeSoc, //
			final int minSoc //
	) {
		return new ComponentDef(limitTotalDischargeId, limitTotalDischargeId, "Controller.Ess.LimitTotalDischarge", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("ess.id", essId) //
						.addProperty("forceChargeSoc", forceChargeSoc) //
						.addProperty("minSoc", minSoc) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	private Pro912Components() {
	}
}
