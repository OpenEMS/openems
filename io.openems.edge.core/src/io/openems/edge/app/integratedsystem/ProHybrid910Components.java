package io.openems.edge.app.integratedsystem;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.energy.api.LogVerbosity;

public class ProHybrid910Components {

	/**
	 * Crrates a default ess component for a FENECON Pro Hybrid 9-10.
	 *
	 * @param essId    the id of the ess
	 * @param modbusId the id of the modbus bridge
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef ess(//
			final String essId, //
			final String modbusId //
	) {
		return new ComponentDef(essId, essId, "Fenecon.Dess.Ess",
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default grid meter component for a FENECON Pro Hybrid 9-10.
	 *
	 * @param meterId  the id of the grid meter
	 * @param modbusId the id of the modbus bridge
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef gridMeter(//
			final String meterId, //
			final String modbusId //
	) {
		return new ComponentDef(meterId, meterId, "Fenecon.Dess.GridMeter", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default grid meter component for a FENECON Pro Hybrid 9-10.
	 *
	 * @param pvId     the id of the grid meter
	 * @param modbusId the id of the modbus bridge
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef pvMeter(//
			final String pvId, //
			final String modbusId //
	) {
		return new ComponentDef(pvId, pvId, "Fenecon.Dess.PvMeter", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default modbus component for a FENECON Pro Hybrid 9-10.
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
	 * @param pvId      the id of the charger
	 * @param pvAlias   the alias of the pv charger
	 * @param factoryId the factoryId of the component
	 * @param essId     the id of the ess
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef dcPv(//
			final String pvId, //
			final String pvAlias, //
			final String factoryId, //
			final String essId //
	) {
		return new ComponentDef(pvId, pvAlias, factoryId, //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("ess.id", essId) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}
}
