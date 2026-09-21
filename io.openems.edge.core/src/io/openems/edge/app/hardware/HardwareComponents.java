package io.openems.edge.app.hardware;

import java.util.ResourceBundle;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentDef;
import io.openems.edge.core.appmanager.dependency.aggregatetask.ComponentProperties;

public final class HardwareComponents {

	private HardwareComponents() {
	}

	/**
	 * Creates a {@link ComponentDef} for an IOC of the Fenecon MasterBox2V0.
	 * 
	 * @param bundle   the {@link ResourceBundle}
	 * @param iocId    the id of the IOC component
	 * @param modbusId the modbus id of the IOC component
	 * @return the {@link ComponentDef}
	 */
	public static final ComponentDef ioc(//
			final ResourceBundle bundle, //
			final String iocId, //
			final String modbusId //
	) {
		return new ComponentDef(iocId, //
				TranslationUtil.getTranslation(bundle, "App.Hardware.MasterBox2v0.ioc"), "IOC.Fenecon.MasterBox2V0", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("modbus.id", modbusId) //
						.addProperty("modbusUnitId", 10) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a {@link ComponentDef} for a meter of the Fenecon MasterBox2V0.
	 * 
	 * @param bundle  the {@link ResourceBundle}
	 * @param meterId the id of the meter component
	 * @param iocId   the id of the IOC component
	 * @return the {@link ComponentDef}
	 */
	public static final ComponentDef masterBoxMeter(//
			final ResourceBundle bundle, //
			final String meterId, //
			final String iocId //
	) {
		return new ComponentDef(meterId, //
				TranslationUtil.getTranslation(bundle, "App.Hardware.MasterBox2v0.meter"), "Fenecon.MasterBox2V0.Meter", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("ioc.id", iocId) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a {@link ComponentDef} for an IO of the Fenecon MasterBox2V0.
	 * 
	 * @param bundle the {@link ResourceBundle}
	 * @param ioId   the id of the IO component
	 * @param iocId  the id of the IOC component
	 * @return the {@link ComponentDef}
	 */
	public static final ComponentDef masterBoxIo(//
			final ResourceBundle bundle, //
			final String ioId, //
			final String iocId //
	) {
		return new ComponentDef(ioId, //
				TranslationUtil.getTranslation(bundle, "App.Hardware.MasterBox2v0.io"), "IO.Fenecon.MasterBox2V0.Relay", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("ioc.id", iocId) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}

	/**
	 * Creates a {@link ComponentDef} for an AnalogOutput of the Fenecon
	 * MasterBox2V0.
	 * 
	 * @param bundle         the {@link ResourceBundle}
	 * @param analogOutputId the id of the AnalogOutput component
	 * @param iocId          the id of the IOC component
	 * @return the {@link ComponentDef}
	 */
	public static final ComponentDef masterBoxAnalogOutput(//
			final ResourceBundle bundle, //
			final String analogOutputId, //
			final String iocId //
	) {
		return new ComponentDef(analogOutputId, //
				TranslationUtil.getTranslation(bundle, "App.Hardware.MasterBox2v0.analogOutput"),
				"IO.Fenecon.MasterBox2V0.AO", //
				ComponentProperties.fromJson(JsonUtils.buildJsonObject() //
						.addProperty("ioc.id", iocId) //
						.build()),
				ComponentDef.Configuration.defaultConfig());
	}
}
