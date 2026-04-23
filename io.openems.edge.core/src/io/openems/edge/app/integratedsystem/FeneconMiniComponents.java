package io.openems.edge.app.integratedsystem;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.enums.Phase;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;
import io.openems.edge.predictor.api.prediction.LogVerbosity;

public class FeneconMiniComponents {

	/**
	 * Creates a default ess component for a FENECON Pro Hybrid.
	 *
	 * @param essId    the id of the ess
	 * @param modbusId the id of the modbus bridge
	 * @param phase    the phase of the ess
	 * @param readOnly is the ess read only
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef ess(//
			final String essId, //
			final String modbusId, //
			final Phase phase, //
			final boolean readOnly //
	) {
		return new ComponentDef(essId, essId, "Fenecon.Mini.Ess",
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.addProperty("phase", phase.toString()) //
						.addProperty("readOnly", readOnly) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default grid meter component for a FENECON Pro Hybrid.
	 *
	 * @param meterId  the id of the grid meter
	 * @param modbusId the id of the modbus bridge
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef gridMeter(//
			final String meterId, //
			final String modbusId //
	) {
		return new ComponentDef(meterId, meterId, "Fenecon.Mini.GridMeter", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default charger component for a FENECON Pro Hybrid.
	 *
	 * @param pvMeterId the id of the charger
	 * @param modbusId  the id of the modbus bridge
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef pvMeter(//
			final String pvMeterId, //
			final String modbusId //
	) {
		return new ComponentDef(pvMeterId, pvMeterId, "Fenecon.Mini.PvMeter", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", modbusId) //
						.build()), //
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a default modbus component for a FENECON Pro Hybrid GW.
	 *
	 * @param modbusId the id of the modbus component
	 * @return the {@link ComponentDef}
	 */
	public static ComponentDef modbus(final String modbusId) {
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
}
