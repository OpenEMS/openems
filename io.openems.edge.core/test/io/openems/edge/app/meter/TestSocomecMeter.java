package io.openems.edge.app.meter;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.enums.ModbusType;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

class TestSocomecMeter {

	private AppManagerTestBundle appManagerTestBundle;
	private SocomecMeter socomecMeter;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> List.of(//
				this.socomecMeter = Apps.socomecMeter(t) //
		), TestSocomecMeter::addSerialModbusBridge, new AppManagerTestBundle.PseudoComponentManagerFactory());
		this.appManagerTestBundle.addComponentAggregateTask();
	}

	@Test
	void testIntegrationTypeDefaultsToRtu() {
		this.appManagerTestBundle //
				.withApp(this.socomecMeter) //
				.assertThatProperty(SocomecMeter.Property.INTEGRATION_TYPE) //
				.hasDefaultValue(ModbusType.RTU);
	}

	@Test
	void testMeterIdDefaultsToMeter1() {
		this.appManagerTestBundle //
				.withApp(this.socomecMeter) //
				.assertThatProperty(SocomecMeter.Property.METER_ID) //
				.hasDefaultValue("meter1");
	}

	@Test
	void testHasOnlyExpectedProperties() {
		this.appManagerTestBundle //
				.withApp(this.socomecMeter) //
				.hasOnlyProperties(//
						SocomecMeter.Property.METER_ID, //
						SocomecMeter.Property.TCP_MODBUS_ID, //
						SocomecMeter.Property.ALIAS, //
						SocomecMeter.Property.TYPE, //
						SocomecMeter.Property.INTEGRATION_TYPE, //
						SocomecMeter.Property.IP, //
						SocomecMeter.Property.PORT, //
						SocomecMeter.Property.MODBUS_ID, //
						SocomecMeter.Property.MODBUS_UNIT_ID, //
						SocomecMeter.Property.INVERT, //
						SocomecMeter.Property.MODBUS_GROUP);
	}

	@Test
	void testIpFieldOnlyShownForTcp() {
		this.appManagerTestBundle //
				.withApp(this.socomecMeter) //
				.assertThatProperty(SocomecMeter.Property.IP) //
				.isVisibleWhen(SocomecMeter.Property.INTEGRATION_TYPE, ModbusType.TCP) //
				.isHiddenWhen(SocomecMeter.Property.INTEGRATION_TYPE, ModbusType.RTU);
	}

	@Test
	void testModbusIdFieldOnlyShownForRtu() {
		this.appManagerTestBundle //
				.withApp(this.socomecMeter) //
				.assertThatProperty(SocomecMeter.Property.MODBUS_ID) //
				.isVisibleWhen(SocomecMeter.Property.INTEGRATION_TYPE, ModbusType.RTU) //
				.isHiddenWhen(SocomecMeter.Property.INTEGRATION_TYPE, ModbusType.TCP);
	}

	@Test
	void testRtuCreatesOnlyMeterComponent() throws Exception {
		this.installSocomecMeterAppWithProps(JsonUtils.buildJsonObject() //
				.addProperty("TYPE", "PRODUCTION") //
				.addProperty("MODBUS_ID", "modbus0") //
				.addProperty("MODBUS_UNIT_ID", 6) //
				.addProperty("INVERT", false) //
				.build());

		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component("meter1", null, "Meter.Socomec.Threephase", JsonUtils.buildJsonObject() //
						.addProperty("modbus.id", "modbus0") //
						.addProperty("modbusUnitId", 6) //
						.addProperty("type", "PRODUCTION") //
						.addProperty("invert", false) //
						.build()) //
		);

		assertFalse(this.appManagerTestBundle.componentManger.getEdgeConfig()
				.getComponentIdsByFactory("Bridge.Modbus.Tcp").contains("modbus2"));
	}

	@Test
	void testTcpCreatesBridgeAndMeterComponent() throws Exception {
		this.installSocomecMeterAppWithProps(JsonUtils.buildJsonObject() //
				.addProperty("TYPE", "PRODUCTION") //
				.addProperty("INTEGRATION_TYPE", "TCP") //
				.addProperty("IP", "10.4.0.20") //
				.addProperty("PORT", 502) //
				.addProperty("MODBUS_UNIT_ID", 1) //
				.addProperty("INVERT", true) //
				.build());

		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component("modbus3", null, "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
						.addProperty("ip", "10.4.0.20") //
						.addProperty("port", 502) //
						.build()), //
				new EdgeConfig.Component("meter1", null, "Meter.Socomec.Threephase", JsonUtils.buildJsonObject() //
						.addProperty("modbus.id", "modbus3") //
						.addProperty("modbusUnitId", 1) //
						.addProperty("type", "PRODUCTION") //
						.addProperty("invert", true) //
						.build()) //
		);
	}

	@Test
	void testTcpDoesNotReuseExistingSerialModbusId() throws Exception {
		var usedModbus2 = "modbus2";
		var newModbus3 = "modbus3";
		// modbus0-2 are already reserved. See addSerialModbusBridge
		this.installSocomecMeterAppWithProps(JsonUtils.buildJsonObject() //
				.addProperty("TYPE", "PRODUCTION") //
				.addProperty("INTEGRATION_TYPE", "TCP") //
				.addProperty("MODBUS_ID", usedModbus2) //
				.addProperty("IP", "10.4.0.20") //
				.addProperty("PORT", 502) //
				.addProperty("MODBUS_UNIT_ID", 1) //
				.addProperty("INVERT", true) //
				.build());

		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component(newModbus3, null, "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
						.addProperty("ip", "10.4.0.20") //
						.addProperty("port", 502) //
						.build()), //
				new EdgeConfig.Component("meter1", null, "Meter.Socomec.Threephase", JsonUtils.buildJsonObject() //
						.addProperty("modbus.id", newModbus3) //
						.addProperty("modbusUnitId", 1) //
						.addProperty("type", "PRODUCTION") //
						.addProperty("invert", true) //
						.build()) //
		);
	}

	private void installSocomecMeterAppWithProps(JsonObject properties) throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.socomecMeter.getAppId(), "key", "alias", properties));
	}

	/**
	 * Adds serial Modbus bridges so the RTU {@code MODBUS_ID} field has a component
	 * to fall back to as a default value.
	 *
	 * @param builder builder
	 */
	private static void addSerialModbusBridge(JsonUtils.JsonObjectBuilder builder) {
		builder.add("modbus0", serialModbusBridge("/dev/busUSB0"));
		builder.add("modbus1", serialModbusBridge("/dev/busUSB1"));
		builder.add("modbus2", serialModbusBridge("/dev/busUSB2"));
	}

	private static JsonObject serialModbusBridge(String portName) {
		return JsonUtils.buildJsonObject() //
				.addProperty("factoryId", "Bridge.Modbus.Serial") //
				.add("properties", JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("portName", portName) //
						.addProperty("baudRate", 9600) //
						.addProperty("databits", 8) //
						.addProperty("stopbits", "ONE") //
						.addProperty("parity", "NONE") //
						.addProperty("logVerbosity", "NONE") //
						.addProperty("invalidateElementsAfterReadErrors", 1) //
						.build()) //
				.build();
	}

}
