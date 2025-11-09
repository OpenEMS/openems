package io.openems.edge.app.integratedsystem.fenecon.industrial.l;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.util.ResourceBundle;
import java.util.stream.IntStream;

import com.google.gson.JsonPrimitive;

import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.ess.power.api.SolverStrategy;

public final class FeneconIndustrialLComponents {

	/**
	 * Creates a default battery for FENECON Industrial L.
	 * 
	 * @param bundle                 the translation bundle
	 * @param batteryId              the id of the battery
	 * @param offset                 the alias offset of the id
	 * @param batteryModbusId        the Modbus id of the battery
	 * @param batteryFirmwareVersion the firmware version of the battery
	 * @param version                the version of the battery (Enfas/Wuerth). Can
	 *                               be removed in future
	 * @return the {@link Component}
	 */
	public static Component battery(//
			final ResourceBundle bundle, //
			final String batteryId, //
			final int offset, //
			final String batteryModbusId, //
			final String batteryFirmwareVersion, //
			final String version //
	) {
		return new Component(batteryId, translate(bundle, "App.IntegratedSystem.batteryN.alias", offset), //
				version, JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("modbus.id", batteryModbusId) //
						.addProperty("modbusUnitId", 1) //
						.addProperty("startStop", "AUTO") //
						.addProperty("version", batteryFirmwareVersion) //
						.build());
	}

	/**
	 * Creates a default battery-inverter component for FENECON Industrial L.
	 * 
	 * @param bundle                  the translation bundle
	 * @param number                  the number of the Battery-Inverter
	 * @param batteryInverterModbusId the Modbus id of the inverter
	 * @return the {@link Component}
	 */
	public static Component batteryInverter(//
			final ResourceBundle bundle, //
			final int number, //
			final String batteryInverterModbusId) {
		return new Component("batteryInverter" + number,
				translate(bundle, "App.IntegratedSystem.batteryInverterN.alias", number),
				"Battery-Inverter.Kaco.BlueplanetGridsave", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("activateWatchdog", true) //
						.addProperty("modbus.id", batteryInverterModbusId) //
						.addProperty("startStop", "AUTO") //
						.build());
	}

	/**
	 * Creates a default cycle component for a FENECON Industrial L.
	 * 
	 * @return the {@link Component}
	 */
	public static Component cycle() {
		return new Component(Cycle.SINGLETON_COMPONENT_ID, Cycle.SINGLETON_SERVICE_PID, Cycle.SINGLETON_SERVICE_PID, //
				JsonUtils.buildJsonObject() //
						.addProperty("cycleTime", 200) //
						.build());
	}

	/**
	 * Creates a default ess cluster component for FENECON Industrial L.
	 * 
	 * @param bundle            the translation bundle
	 * @param clusterId         the id of the ess-cluster
	 * @param numberOfBatteries the number of batteries
	 * @return the {@link Component}
	 */
	public static Component essCluster(//
			final ResourceBundle bundle, //
			final String clusterId, //
			final int numberOfBatteries) {
		return new Component(clusterId, translate(bundle, "App.IntegratedSystem.essCluster0.alias"), "Ess.Cluster", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("startStop", "AUTO") //
						.add("ess.ids", IntStream.range(0, numberOfBatteries) //
								.mapToObj(i -> new JsonPrimitive("ess" + (i + 1))) //
								.collect(toJsonArray())) //
						.build()); //
	}

	/**
	 * Creates a default generic managed symmetric ess component for FENECON
	 * Industrial L.
	 * 
	 * @param bundle            the translation bundle
	 * @param essId             the id of the ess
	 * @param alias             the alias of the ess
	 * @param batteryId         the batteryId controlled by this ess
	 * @param batteryInverterId the battery inverter id controlled by this ess
	 * @return the {@link Component}
	 */
	public static Component essGenericManagedSymmetric(//
			final ResourceBundle bundle, //
			final String essId, //
			final int alias, //
			final String batteryId, //
			final String batteryInverterId //
	) {
		final String c = translate(bundle, "App.IntegratedSystem.essN.alias", alias);
		return new Component(essId, c, "Ess.Generic.ManagedSymmetric", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("battery.id", batteryId) //
						.addProperty("batteryInverter.id", batteryInverterId) //
						.addProperty("startStop", "AUTO") //
						.build());
	}

	/**
	 * Creates a default io component for FENECON Industrial L.
	 * 
	 * @param ioId          the io id
	 * @param bundle        the translation bundle
	 * @param isNewHardware boolean to choose between new and old hardware
	 * @return the {@link Component}
	 */
	public static Component io(//
			final String ioId, //
			final ResourceBundle bundle, //
			final boolean isNewHardware //
	) {
		return new Component(ioId, translate(bundle, "App.Hardware.IoGpio.Name"), "IO.Gpio", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("gpioPath", "/sys/class") //
						.addProperty("hardwareType",
								isNewHardware ? "MODBERRY_X500_M40804_MAX" : "MODBERRY_X500_M40804_WB")
						.build());
	}

	/**
	 * Creates a default internal modbus bridge component for FENECON Industrial L.
	 * 
	 * @param bundle           the translation bundle
	 * @param t                the {@link ConfigurationTarget} of the current
	 *                         configuration
	 * @param modbusIdInternal the id of the internal modbus bridge
	 * @return the {@link Component}
	 */
	public static Component modbusInternal(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusIdInternal //
	) {
		return new Component(modbusIdInternal, //
				translate(bundle, "App.FENECON.Industrial.L.modbus0.alias"), "Bridge.Modbus.Serial", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b //
								.addProperty("logVerbosity", "NONE") //
								.addProperty("invalidateElementsAfterReadErrors", 1)) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/ttySC0") //
						.addProperty("stopbits", "ONE") //
						.build()); //
	}

	/**
	 * Creates a default modbus tcp bridge component to a battery for a FENECON
	 * Industrial S.
	 * 
	 * @param bundle          the translation bundle
	 * @param t               the {@link ConfigurationTarget} of the current
	 *                        configuration
	 * @param batteryModbusId the id suffix of the bridge
	 * @param number          the number of the modbus bridge to a Battery
	 * @return the {@link Component}
	 */
	public static Component modbusToBattery(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final int batteryModbusId, //
			final int number //
	) {
		return new Component("modbus" + batteryModbusId,
				translate(bundle, "App.IntegratedSystem.modbusToBatteryN.alias", number), //
				"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("ip", "10.4.0.2" + number) //
						.addProperty("port", 502) //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b //
								.addProperty("logVerbosity", "NONE") //
								.addProperty("invalidateElementsAfterReadErrors", 3)) //
						.build());
	}

	/**
	 * Creates a default modbus bridge to a battery-inverter component for a FENECON
	 * Industrial L.
	 * 
	 * @param bundle                  the translation bundle
	 * @param t                       the {@link ConfigurationTarget} of the current
	 *                                configuration
	 * @param batteryInverterModbusId the id suffix of the bridge
	 * @param number                  the number of the modbus bridge to a
	 *                                Battery-Inverter
	 * @return the {@link Component}
	 */
	public static Component modbusToBatteryInverter(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final int batteryInverterModbusId, //
			final int number //
	) {
		return new Component("modbus" + batteryInverterModbusId,
				translate(bundle, "App.IntegratedSystem.modbus1N.alias", number), //
				"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("port", 502) //
						.addProperty("ip", "10.4.0.1" + number) //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b //
								.addProperty("logVerbosity", "NONE") //
								.addProperty("invalidateElementsAfterReadErrors", 5)) //
						.build());
	}

	/**
	 * Creates a default power component for FENECON Industrial L.
	 * 
	 * @return the {@link Component}
	 */
	public static Component power() {
		return new Component("_power", "Ess.Power", "Ess.Power", //
				JsonUtils.buildJsonObject() //
						.addProperty("strategy", SolverStrategy.OPTIMIZE_BY_KEEPING_ALL_EQUAL) //
						.build());
	}

	/**
	 * Creates a default system component for FENECON Industrial L.
	 * 
	 * @param t                         the {@link ConfigurationTarget} of the
	 *                                  current configuration
	 * @param essClusterId              the ess cluster id
	 * @param coolingUnitModbusId       the modbus id of the cooling unit
	 * @param isSmokeDetectionInstalled boolean whether smoke detection is installed
	 * @param numberOfBatteries         the number of batteries
	 * @return the {@link Component}
	 */
	public static Component system(//
			final ConfigurationTarget t, //
			final String essClusterId, //
			final String coolingUnitModbusId, //
			final boolean isSmokeDetectionInstalled, //
			final int numberOfBatteries) {
		return new Component("system0", "System Industrial L (ILK710)", "System.Fenecon.Industrial.L", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("ess.id", essClusterId) //
						.addProperty("coolingUnitCoolingSetPoint", 25) //
						.addProperty("coolingUnitHeatingSetPoint", 24) //
						.addProperty("coolingUnitModbus.id", coolingUnitModbusId) //
						.addProperty("coolingUnitModbusUnitId", 1) //
						.addProperty("coolingUnitMode", "ENABLED") //
						.addProperty("acknowledgeEmergencyStop", "io0/DigitalOutput2") //
						.addProperty("emergencyStopState", "io0/DigitalInput3") //
						.addProperty("spdTripped", "io0/DigitalInput2") //
						.addProperty("fuseTripped", "io0/DigitalInput4") //
						.addProperty("psuTriggered", "io0/DigitalInput1") //
						.addProperty("isSmokeDetectionInstalled", isSmokeDetectionInstalled) //
						.addProperty("smokeDetection", "io0/DigitalInputOutput1") //
						.addProperty("smokeDetectionFailure", "io0/DigitalInputOutput2") //
						.addProperty("bmsHardReset", "io0/DigitalOutput1") //
						.onlyIf(t == ConfigurationTarget.ADD, b -> b.addProperty("startStop", "STOP")) //
						.add("battery.ids", IntStream.range(0, numberOfBatteries) //
								.mapToObj(i -> new JsonPrimitive("battery" + (i + 1))) //
								.collect(toJsonArray())) //
						.build()); //
	}

	private FeneconIndustrialLComponents() {
	}

}
