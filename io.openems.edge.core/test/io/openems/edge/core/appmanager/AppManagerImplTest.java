package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.evcs.KebaEvcs;
import io.openems.edge.app.integratedsystem.FeneconHome;
import io.openems.edge.app.timeofusetariff.AwattarHourly;
import io.openems.edge.app.timeofusetariff.StromdaoCorrently;
import io.openems.edge.common.host.Host;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentContext;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.validator.CheckCardinality;
import io.openems.edge.core.appmanager.validator.Validator;

public class AppManagerImplTest {

	private DummyConfigurationAdmin cm;
	private DummyComponentManager componentManger;
	private ComponentUtil componentUtil;

	private FeneconHome homeApp;
	private KebaEvcs kebaEvcsApp;
	private AwattarHourly awattarApp;
	private StromdaoCorrently stromdao;

	private AppManagerImpl sut;

	@Before
	public void beforeEach() throws Exception {

		this.cm = new DummyConfigurationAdmin();
		this.cm.getOrCreateEmptyConfiguration(AppManager.SINGLETON_SERVICE_PID);

		final var essId = "ess0";
		final var modbusIdInternal = "modbus0";
		final var modbusIdExternal = "modbus1";

		final var emergencyReserveEnabled = false;

		// Battery-Inverter Settings
		final var safetyCountry = "AUSTRIA";
		final var maxFeedInPower = 10000;
		final var feedInSetting = "LAGGING_0_95";

		this.componentManger = new DummyComponentManager();
		this.componentManger.setConfigJson(JsonUtils.buildJsonObject() //
				.add("components", JsonUtils.buildJsonObject() //
						.add(modbusIdInternal, JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Bridge.Modbus.Serial") //
								.addProperty("alias", "Kommunikation mit der Batterie") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("portName", "/dev/busUSB1") //
										.addProperty("baudRate", 19200) //
										.addProperty("databits", 8) //
										.addProperty("stopbits", "ONE") //
										.addProperty("parity", "NONE") //
										.addProperty("logVerbosity", "NONE") //
										.addProperty("invalidateElementsAfterReadErrors", 1) //
										.build()) //
								.build()) //
						.add(modbusIdExternal, JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Bridge.Modbus.Serial") //
								.addProperty("alias", "Kommunikation mit dem Batterie-Wechselrichter") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("portName", "/dev/busUSB2") //
										.addProperty("baudRate", 9600) //
										.addProperty("databits", 8) //
										.addProperty("stopbits", "ONE") //
										.addProperty("parity", "NONE") //
										.addProperty("logVerbosity", "NONE") //
										.addProperty("invalidateElementsAfterReadErrors", 1) //
										.build()) //
								.build()) //
						.add("meter0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "GoodWe.Grid-Meter") //
								.addProperty("alias", "Netzzähler") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("modbus.id", modbusIdExternal) //
										.addProperty("modbusUnitId", 247) //
										.build()) //
								.build()) //
						.add("io0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "IO.KMtronic.4Port") //
								.addProperty("alias", "Relaisboard") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("modbus.id", modbusIdInternal) //
										.addProperty("modbusUnitId", 2) //
										.build()) //
								.build()) //
						.add("battery0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Battery.Fenecon.Home") //
								.addProperty("alias", "Batterie") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("startStop", "AUTO") //
										.addProperty("modbus.id", modbusIdInternal) //
										.addProperty("modbusUnitId", 1) //
										.addProperty("batteryStartUpRelay", "io0/Relay4") //
										.build()) //
								.build()) //
						.add(essId, JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Ess.Generic.ManagedSymmetric") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("startStop", "START") //
										.addProperty("batteryInverter.id", "batteryInverter0") //
										.addProperty("battery.id", "battery0") //
										.build()) //
								.build()) //
						.add("batteryInverter0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "GoodWe.BatteryInverter") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("modbus.id", modbusIdExternal) //
										.addProperty("modbusUnitId", 247) //
										.addProperty("safetyCountry", safetyCountry) //
										.addProperty("backupEnable", "DISABLE") //
										.addProperty("feedPowerEnable", "ENABLE") //
										.addProperty("feedPowerPara", 10000) //
										.addProperty("setfeedInPowerSettings", "LAGGING_0_95") //
										.build()) //
								.build()) //
						.add("predictor0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Predictor.PersistenceModel") //
								.addProperty("alias", "Prognose") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.add("channelAddresses", JsonUtils.buildJsonArray() //
												.add("_sum/ProductionActivePower") //
												.add("_sum/ConsumptionActivePower") //
												.build()) //
										.build()) //
								.build()) //
						.add("ctrlGridOptimizedCharge0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Controller.Ess.GridOptimizedCharge") //
								.addProperty("alias", "Netzdienliche Beladung") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("ess.id", essId) //
										.addProperty("meter.id", "meter0") //
										.addProperty("sellToGridLimitEnabled", true) //
										.addProperty("maximumSellToGridPower", maxFeedInPower) //
										.build()) //
								.build()) //
						.add("ctrlEssSurplusFeedToGrid0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Controller.Ess.Hybrid.Surplus-Feed-To-Grid") //
								.addProperty("alias", "Überschusseinspeisung") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("ess.id", essId) //
										.build()) //
								.build()) //
						.add("ctrlBalancing0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Controller.Symmetric.Balancing") //
								.addProperty("alias", "Eigenverbrauchsoptimierung") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.addProperty("ess.id", essId) //
										.addProperty("meter.id", "meter0") //
										.addProperty("targetGridSetpoint", 0) //
										.build()) //
								.build()) //
						.add("scheduler0", JsonUtils.buildJsonObject() //
								.addProperty("factoryId", "Scheduler.AllAlphabetically") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("enabled", true) //
										.add("controllers.ids", JsonUtils.buildJsonArray() //
												.add("ctrlGridOptimizedCharge0") //
												.add("ctrlEssSurplusFeedToGrid0") //
												.add("ctrlBalancing0") //
												.build()) //
										.build()) //
								.build()) //
						.add(Host.SINGLETON_COMPONENT_ID, JsonUtils.buildJsonObject() //
								.addProperty("factoryId", Host.SINGLETON_SERVICE_PID) //
								.addProperty("alias", "") //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("networkConfiguration", //
												"{\n" //
														+ "  \"interfaces\": {\n" //
														+ "    \"enx*\": {\n" //
														+ "      \"dhcp\": false,\n" //
														+ "      \"addresses\": [\n" //
														+ "        \"10.4.0.1/16\",\n" //
														+ "        \"192.168.1.9/29\"\n" //
														+ "      ]\n" //
														+ "    },\n" //
														+ "    \"eth0\": {\n" //
														+ "      \"dhcp\": true,\n" //
														+ "      \"linkLocalAddressing\": true,\n" //
														+ "      \"addresses\": [\n" //
														+ "        \"192.168.100.100/24\"\n" //
														+ "      ]\n" //
														+ "    }\n" //
														+ "  }\n" //
														+ "}") //
										.build()) //
								.build()) //
						.build()) //
				.add("factories", JsonUtils.buildJsonObject() //
						.build()) //
				.build() //
		);
		this.componentUtil = new ComponentUtilImpl(this.componentManger, this.cm);

		this.homeApp = new FeneconHome(this.componentManger, getComponentContext("App.FENECON.Home"), this.cm,
				this.componentUtil);
		this.kebaEvcsApp = new KebaEvcs(this.componentManger, getComponentContext("App.Evcs.Keba"), this.cm,
				this.componentUtil);
		this.awattarApp = new AwattarHourly(this.componentManger, getComponentContext("App.TimeVariablePrice.Awattar"),
				this.cm, this.componentUtil);
		this.stromdao = new StromdaoCorrently(this.componentManger,
				getComponentContext("App.TimeVariablePrice.Stromdao"), this.cm, this.componentUtil);

		this.sut = new AppManagerImpl();
		new ComponentTest(this.sut) //
				.addReference("cm", this.cm) //
				.addReference("componentManager", this.componentManger) //
				.addReference("availableApps",
						ImmutableList.of(this.homeApp, this.kebaEvcsApp, this.awattarApp, this.stromdao)) //
				.activate(MyConfig.create() //
						.setApps(JsonUtils.buildJsonArray() //
								.add(JsonUtils.buildJsonObject() //
										.addProperty("appId", "App.FENECON.Home") //
										.addProperty("alias", "FENECON Home") //
										.addProperty("instanceId", "ef13f394-1a3c-43ed-b726-ef1efaf23fdf") //
										.add("properties", JsonUtils.buildJsonObject() //
												.addProperty("SAFETY_COUNTRY", safetyCountry) //
												.addProperty("MAX_FEED_IN_POWER", maxFeedInPower) //
												.addProperty("FEED_IN_SETTING", feedInSetting) //
												.addProperty("HAS_AC_METER", false) //
												.addProperty("HAS_DC_PV1", false) //
												.addProperty("HAS_DC_PV2", false) //
												.addProperty("EMERGENCY_RESERVE_ENABLED", emergencyReserveEnabled) //
												.build()) //
										.build()) //
								.build().toString()) //
						.build());
	}

	@Test
	public void testAppValidateWorker() throws OpenemsException, Exception {
		var worker = new AppValidateWorker(this.sut);
		worker.validateApps();

		assertEquals(this.sut.instantiatedApps.size(), 1);

		// should not have found defective Apps
		for (Entry<String, String> entry : worker.defectiveApps.entrySet()) {
			throw new Exception(entry.getKey() + ": " + entry.getValue());
		}
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testGetInstantiatedApps() {
		this.sut.getInstantiatedApps().add(null);
	}

	@Test
	public void testGetReplaceableComponentIds() throws Exception {
		var replaceableIds = this.sut.getReplaceableComponentIds(this.kebaEvcsApp, JsonUtils.buildJsonObject().build());

		assertEquals(replaceableIds.size(), 2);
		assertEquals("EVCS_ID", replaceableIds.get("evcs0"));
		assertEquals("CTRL_EVCS_ID", replaceableIds.get("ctrlEvcs0"));
	}

	@Test
	public void testFindAppById() {
		assertEquals(this.homeApp, this.sut.findAppById("App.FENECON.Home"));
	}

	@Test
	public void testCheckCardinalitySingle() throws Exception {
		var checkable = new CheckCardinality(this.sut);
		checkable.setProperties(new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
				.put("openemsApp", this.homeApp) //
				.build());
		assertFalse(checkable.check());
		assertNotNull(checkable.getErrorMessage());
	}

	@Test
	public void testCheckCardinalityMultiple() throws Exception {
		this.sut.instantiatedApps.add(new OpenemsAppInstance(this.kebaEvcsApp.getAppId(), "alias", UUID.randomUUID(),
				JsonUtils.buildJsonObject().build(), null));
		this.sut.instantiatedApps.add(new OpenemsAppInstance(this.kebaEvcsApp.getAppId(), "alias", UUID.randomUUID(),
				JsonUtils.buildJsonObject().build(), null));
		var checkable = new CheckCardinality(this.sut);
		checkable.setProperties(new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
				.put("openemsApp", this.kebaEvcsApp) //
				.build());
		assertTrue(checkable.check());
		assertNull(checkable.getErrorMessage());
	}

	@Test
	public void testCheckCardinalitySingleInCategorie() throws Exception {
		this.sut.instantiatedApps.add(new OpenemsAppInstance(this.awattarApp.getAppId(), "alias", UUID.randomUUID(),
				JsonUtils.buildJsonObject().build(), null));
		var checkable = new CheckCardinality(this.sut);
		checkable.setProperties(new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
				.put("openemsApp", this.stromdao) //
				.build());
		assertFalse(checkable.check());
		assertNotNull(checkable.getErrorMessage());
	}

	private static ComponentContext getComponentContext(String appId) {
		Dictionary<String, Object> properties = new Hashtable<>();
		properties.put(ComponentConstants.COMPONENT_NAME, appId);
		return new DummyComponentContext(properties);
	}

}
