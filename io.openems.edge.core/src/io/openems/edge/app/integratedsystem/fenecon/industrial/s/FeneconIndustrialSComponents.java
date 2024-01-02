package io.openems.edge.app.integratedsystem.fenecon.industrial.s;

import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import java.util.List;
import java.util.ResourceBundle;

import com.google.gson.JsonPrimitive;

import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.Parity;
import io.openems.edge.app.meter.KdkMeter;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.ess.power.api.SolverStrategy;

public final class FeneconIndustrialSComponents {

	/**
	 * Creates a default bmw battery component for a FENECON Industrial S.
	 * 
	 * @param bundle           the translation bundle
	 * @param batteryId        the id of the battery
	 * @param alias            the alias of the battery
	 * @param modbusIdInternal the id of the internal modbus bridge
	 * @param modbusUnitId     the modbus unit id of the battery
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component batteryBmw(//
			final ResourceBundle bundle, //
			final String batteryId, //
			final String alias, //
			final String modbusIdInternal, //
			final int modbusUnitId //
	) {
		return new EdgeConfig.Component(batteryId, translate(bundle, "App.IntegratedSystem.batteryN.alias", alias),
				"Battery.Fenecon.F2B.BMW", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("startStop", "AUTO") //
						.addProperty("modbus.id", modbusIdInternal) //
						.addProperty("modbusUnitId", modbusUnitId) //
						.build());
	}

	/**
	 * Creates a default f2b parallel battery cluster component for a FENECON
	 * Industrial S.
	 * 
	 * @param bundle     the translation bundle
	 * @param batteryId  the id of the battery
	 * @param alias      the alias of the battery
	 * @param batteryIds the ids of the batteries
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component batteryf2bClusterParallel(//
			final ResourceBundle bundle, //
			final String batteryId, //
			final String alias, //
			final List<String> batteryIds //
	) {
		return new EdgeConfig.Component(batteryId,
				translate(bundle, "App.IntegratedSystem.batteryParallelClusterN.alias", alias),
				"Battery.Fenecon.F2B.Cluster.Parallel", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("startStop", "AUTO") //
						.add("battery.ids", batteryIds.stream() //
								.map(JsonPrimitive::new) //
								.collect(JsonUtils.toJsonArray())) //
						.build());
	}

	/**
	 * Creates a default f2b serial battery cluster component for a FENECON
	 * Industrial S.
	 * 
	 * @param bundle     the translation bundle
	 * @param batteryId  the id of the battery
	 * @param alias      the alias of the battery
	 * @param batteryIds the ids of the batteries which are controlled by this
	 *                   cluster
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component batteryf2bClusterSerial(//
			final ResourceBundle bundle, //
			final String batteryId, //
			final String alias, //
			final List<String> batteryIds //
	) {
		return new EdgeConfig.Component(batteryId,
				translate(bundle, "App.IntegratedSystem.batterySerialClusterN.alias", alias),
				"Battery.Fenecon.F2B.Cluster.Serial", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("startStop", "AUTO") //
						.add("battery.ids", batteryIds.stream() //
								.map(JsonPrimitive::new) //
								.collect(JsonUtils.toJsonArray())) //
						.build());
	}

	/**
	 * Creates a default battery-inverter component for a FENECON Industrial S.
	 * 
	 * @param bundle the translation bundle
	 * @param number the number of the Battery-Inverter
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component batteryInverter(//
			final ResourceBundle bundle, //
			final int number //
	) {
		return new EdgeConfig.Component("batteryInverter" + number,
				translate(bundle, "App.IntegratedSystem.batteryInverterN.alias", number),
				"Battery-Inverter.Kaco.BlueplanetGridsave", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("activateWatchdog", true) //
						.addProperty("modbus.id", "modbus" + number) //
						.addProperty("startStop", "AUTO") //
						.build());
	}

	/**
	 * Creates a default cycle component for a FENECON Industrial S.
	 * 
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component cycle() {
		return new EdgeConfig.Component("_cycle", "Core.Cycle", "Core.Cycle", //
				JsonUtils.buildJsonObject() //
						.addProperty("cycleTime", 200) //
						.build());
	}

	/**
	 * Creates a default ess cluster component for a FENECON Industrial S.
	 * 
	 * @param bundle    the translation bundle
	 * @param clusterId the id of the ess-cluster
	 * @param essIds    the ess ids which are controlled by this ess
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component essCluster(//
			final ResourceBundle bundle, //
			final String clusterId, //
			final List<String> essIds //
	) {
		return new EdgeConfig.Component(clusterId, translate(bundle, "App.FENECON.Industrial.S.essCluster0.alias"),
				"Ess.Cluster", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("startStop", "AUTO") //
						.add("ess.ids", essIds.stream() //
								.map(JsonPrimitive::new) //
								.collect(JsonUtils.toJsonArray())) //
						.build());
	}

	/**
	 * Creates a default generic managed symmetric ess component for a FENECON
	 * Industrial S.
	 * 
	 * @param bundle            the translation bundle
	 * @param essId             the id of the ess
	 * @param batteryId         the id of the battery which is controlled by this
	 *                          ess
	 * @param batteryInverterId the id of the battery-inverter which is controlled
	 *                          by this ess
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component essGenericManagedSymmetric(//
			final ResourceBundle bundle, //
			final String essId, //
			final String batteryId, //
			final String batteryInverterId //
	) {
		return essGenericManagedSymmetric(bundle, essId, null, batteryId, batteryInverterId);
	}

	/**
	 * Creates a default generic managed symmetric ess component for a FENECON
	 * Industrial S.
	 * 
	 * @param bundle            the translation bundle
	 * @param essId             the id of the ess
	 * @param alias             the alias of the ess
	 * @param batteryId         the id of the battery which is controlled by this
	 *                          ess
	 * @param batteryInverterId the id of the battery-inverter which is controlled
	 *                          by this ess
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component essGenericManagedSymmetric(//
			final ResourceBundle bundle, //
			final String essId, //
			final String alias, //
			final String batteryId, //
			final String batteryInverterId //
	) {
		final String c;
		if (alias == null) {
			c = translate(bundle, "App.FENECON.Industrial.S.ess0.alias");
		} else {
			c = translate(bundle, "App.FENECON.Industrial.S.essN.alias", alias);
		}
		return new EdgeConfig.Component(essId, c, "Ess.Generic.ManagedSymmetric", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("battery.id", batteryId) //
						.addProperty("batteryInverter.id", batteryInverterId) //
						.addProperty("startStop", "AUTO") //
						.build());
	}

	/**
	 * Creates a default io component for a FENECON Industrial S.
	 * 
	 * @param bundle the translation bundle
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component io(//
			final ResourceBundle bundle //
	) {
		return new EdgeConfig.Component("io0", translate(bundle, "App.FENECON.Industrial.S.io0"), "IO.Modberry", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("gpioPath", "/sys/class") //
						.addProperty("modberryType", "MODBERRY_X500_M40804_WB") //
						.build());
	}

	/**
	 * Creates a default internal modbus bridge component for a FENECON Industrial
	 * S.
	 * 
	 * @param bundle           the translation bundle
	 * @param t                the {@link ConfigurationTarget} of the current
	 *                         configuration
	 * @param modbusIdInternal the id of the internal modbus bridge
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusInternal(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusIdInternal //
	) {
		return new EdgeConfig.Component(modbusIdInternal, translate(bundle, "App.IntegratedSystem.modbus0N.alias"),
				"Bridge.Modbus.Serial", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.onlyIf(t != ConfigurationTarget.VALIDATE, b -> {
							b.addProperty("invalidateElementsAfterReadErrors", 3) //
									.addProperty("logVerbosity", "NONE");
						}) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/ttySC0") //
						.addProperty("stopbits", "ONE") //
						.build());
	}

	/**
	 * Creates a default modbus bridge to a battery-inverter component for a FENECON
	 * Industrial S.
	 * 
	 * @param bundle the translation bundle
	 * @param t      the {@link ConfigurationTarget} of the current configuration
	 * @param number the number of the modbus bridge to a Battery-Inverter
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusToBatteryInverter(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final int number //
	) {
		return new EdgeConfig.Component("modbus" + number,
				translate(bundle, "App.IntegratedSystem.modbus1N.alias", number), "Bridge.Modbus.Tcp",
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.onlyIf(t != ConfigurationTarget.VALIDATE, b -> {
							b.addProperty("invalidateElementsAfterReadErrors", 3) //
									.addProperty("logVerbosity", "NONE");
						}) //
						.addProperty("port", 502) //
						.addProperty("ip", "172.23.22.1" + number) //
						.build());
	}

	/**
	 * Creates a default modbus bridge to a grid-meter component for a FENECON
	 * Industrial S.
	 * 
	 * @param bundle   the translation bundle
	 * @param t        the {@link ConfigurationTarget} of the current configuration
	 * @param modbusId the component id
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component modbusToGridMeter(//
			final ResourceBundle bundle, //
			final ConfigurationTarget t, //
			final String modbusId //
	) {
		return new EdgeConfig.Component(modbusId, //
				TranslationUtil.getTranslation(bundle, "App.FENECON.Industrial.S.modbusToGridMeter.alias"),
				"Bridge.Modbus.Serial", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.onlyIf(t != ConfigurationTarget.VALIDATE, b -> {
							b.addProperty("invalidateElementsAfterReadErrors", 3) //
									.addProperty("logVerbosity", "NONE");
						}) //
						.addProperty("parity", Parity.NONE) //
						.addProperty("portName", "/dev/ttyAMA0") //
						.addProperty("stopbits", "ONE") //
						.build());
	}

	/**
	 * Creates a default power component for a FENECON Industrial S.
	 * 
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component power() {
		return new EdgeConfig.Component("_power", "Ess.Power", "Ess.Power", //
				JsonUtils.buildJsonObject() //
						.addProperty("strategy", SolverStrategy.OPTIMIZE_BY_KEEPING_ALL_EQUAL) //
						.build());
	}

	/**
	 * Creates a default system component for a FENECON Industrial S.
	 * 
	 * @param t         the {@link ConfigurationTarget} of the current configuration
	 * @param alias     the alias of the component
	 * @param batteries the battery ids
	 * @return the {@link Component}
	 */
	public static EdgeConfig.Component system(//
			final ConfigurationTarget t, //
			final String alias, //
			final List<String> batteries //
	) {
		return new EdgeConfig.Component("system0", alias, "System.Fenecon.Industrial.S", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.add("battery.ids", batteries.stream() //
								.map(JsonPrimitive::new) //
								.collect(JsonUtils.toJsonArray())) //
						.addProperty("acknowledgeEmergencyStop", "io0/DigitalOutput2") //
						.addProperty("emergencyStopState", "io0/DigitalInput3") //
						.addProperty("ess.id", "ess0") //
						.addProperty("fuseTripped", "io0/DigitalInput4") //
						.addProperty("coolingUnitEnable", "io0/DigitalOutput1") //
						.addProperty("coolingUnitError", "io0/DigitalInput1") //
						.addProperty("coolingUnitMode", "AUTO") //
						.addProperty("spdTripped", "io0/DigitalInput2") //
						.onlyIf(t == ConfigurationTarget.ADD, b -> {
							b.addProperty("startStop", "STOP");
						}) //
						.build());
	}

	/**
	 * Creates a default grid-meter dependency for a FENECON Industrial S.
	 * 
	 * @param bundle           the translation bundle
	 * @param modbusIdInternal the id of the internal modbus bridge
	 * @param gridMeterId      the grid-meter component id
	 * @return the {@link DependencyDeclaration}
	 */
	public static final DependencyDeclaration gridMeter(//
			final ResourceBundle bundle, //
			final String modbusIdInternal, //
			final String gridMeterId //
	) {
		return new DependencyDeclaration("GRID_METER", //
				DependencyDeclaration.CreatePolicy.ALWAYS, //
				DependencyDeclaration.UpdatePolicy.ALWAYS, //
				DependencyDeclaration.DeletePolicy.IF_MINE, //
				DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
				DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED, //
				DependencyDeclaration.AppDependencyConfig.create() //
						.setAppId("App.Meter.Kdk") //
						.setAlias(TranslationUtil.getTranslation(bundle, "App.Meter.gridMeter")) //
						.setProperties(JsonUtils.buildJsonObject() //
								.addProperty(KdkMeter.Property.TYPE.name(), MeterType.GRID) //
								.addProperty(KdkMeter.Property.MODBUS_ID.name(), modbusIdInternal) //
								.addProperty(KdkMeter.Property.METER_ID.name(), gridMeterId) //
								.addProperty(KdkMeter.Property.MODBUS_UNIT_ID.name(), 6) //
								.build())
						.build());
	}

	private FeneconIndustrialSComponents() {
	}

}
