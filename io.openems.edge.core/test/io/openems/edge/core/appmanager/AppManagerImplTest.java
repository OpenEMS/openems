package io.openems.edge.core.appmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.TreeMap;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.evcs.KebaEvcs;
import io.openems.edge.app.integratedsystem.FeneconHome;
import io.openems.edge.app.timeofusetariff.AwattarHourly;
import io.openems.edge.app.timeofusetariff.StromdaoCorrently;
import io.openems.edge.common.host.Host;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

public class AppManagerImplTest {

	private AppManagerTestBundle appManagerTestBundle;

	private FeneconHome homeApp;

	private KebaEvcs kebaEvcsApp;
	private AwattarHourly awattarApp;
	private StromdaoCorrently stromdao;

	@Before
	public void beforeEach() throws Exception {

		final var essId = "ess0";
		final var modbusIdInternal = "modbus0";
		final var modbusIdExternal = "modbus1";

		final var meterId = "meter0";

		final var emergencyReserveEnabled = false;

		final var shadowManagmentDisabled = false;

		// Battery-Inverter Settings
		final var safetyCountry = "AUSTRIA";
		final var maxFeedInPower = 10000;
		final var feedInSetting = "LAGGING_0_95";

		var componentConfig = JsonUtils.buildJsonObject() //
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
				.add(meterId, JsonUtils.buildJsonObject() //
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
								.addProperty("controlMode", "SMART") //
								.addProperty("setfeedInPowerSettings", "LAGGING_0_95") //
								.addProperty("mpptForShadowEnable", shadowManagmentDisabled ? "DISABLE" : "ENABLE") //
								.addProperty("rcrEnable", "DISABLE") //
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
								.addProperty("mode", "AUTOMATIC") //
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
				.add("ctrlPrepareBatteryExtension0", JsonUtils.buildJsonObject() //
						.addProperty("factoryId", "Controller.Ess.PrepareBatteryExtension") //
						.addProperty("alias", "Batterie Erweiterung vorbereitung") //
						.add("properties", JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.addProperty("targetSoc", 30) //
								.addProperty("enabled", true) //
								.addProperty("ess_id", "ess0") //
								.addProperty("isRunning", false) //
								.addProperty("targetDate", "01.01.2000") //
								.addProperty("targetTime", "08:00") //
								.addProperty("targetTimeSpecified", false) //
								.addProperty("targetTimeBuffer", 30) //
								.addProperty("selfTermination", true) //
								.addProperty("terminationBuffer", 120) //
								.addProperty("conditionalTermination", true) //
								.addProperty("endCondition", "CAPACITY_CHANGED") //
								.build()) //
						.build()) //
				.add("scheduler0", JsonUtils.buildJsonObject() //
						.addProperty("factoryId", "Scheduler.AllAlphabetically") //
						.add("properties", JsonUtils.buildJsonObject() //
								.addProperty("enabled", true) //
								.add("controllers.ids", JsonUtils.buildJsonArray() //
										.add("ctrlPrepareBatteryExtension0") //
										.add("ctrlGridOptimizedCharge0") //
										.add("ctrlEssSurplusFeedToGrid0") //
										.add("ctrlBalancing0") //
										.build()) //
								.build()) //
						.build()) //
				.add("_power", JsonUtils.buildJsonObject() //
						.addProperty("factoryId", "Ess.Power") //
						.add("properties", JsonUtils.buildJsonObject() //
								.addProperty("enablePid", false) //
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
				.build();

		var initialConfig = MyConfig.create() //
				.setApps(JsonUtils.buildJsonArray() //
						.add(JsonUtils.buildJsonObject() //
								.addProperty("appId", "App.FENECON.Home") //
								.addProperty("alias", "FENECON Home") //
								.addProperty("instanceId", UUID.randomUUID().toString()) //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("SAFETY_COUNTRY", safetyCountry) //
										.addProperty("MAX_FEED_IN_POWER", maxFeedInPower) //
										.addProperty("FEED_IN_SETTING", feedInSetting) //
										.addProperty("HAS_AC_METER", false) //
										.addProperty("HAS_DC_PV1", false) //
										.addProperty("HAS_DC_PV2", false) //
										.addProperty("EMERGENCY_RESERVE_ENABLED", emergencyReserveEnabled) //
										.addProperty("SHADOW_MANAGEMENT_DISABLED", shadowManagmentDisabled) //
										.build()) //
								.build()) //
						.add(JsonUtils.buildJsonObject() //
								.addProperty("appId", "App.PvSelfConsumption.GridOptimizedCharge") //
								.addProperty("alias", "") //
								.addProperty("instanceId", UUID.randomUUID().toString()) //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("SELL_TO_GRID_LIMIT_ENABLED", true) //
										.addProperty("MAXIMUM_SELL_TO_GRID_POWER", maxFeedInPower) //
										.addProperty("MODE", "AUTOMATIC") //
										.build()) //
								.build())
						.add(JsonUtils.buildJsonObject() //
								.addProperty("appId", "App.PvSelfConsumption.SelfConsumptionOptimization") //
								.addProperty("alias", "") //
								.addProperty("instanceId", UUID.randomUUID().toString()) //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("ESS_ID", essId) //
										.addProperty("METER_ID", meterId) //
										.build()) //
								.build())
						.add(JsonUtils.buildJsonObject() //
								.addProperty("appId", "App.Ess.PrepareBatteryExtension") //
								.addProperty("alias", "") //
								.addProperty("instanceId", UUID.randomUUID().toString()) //
								.add("properties", JsonUtils.buildJsonObject() //
										.addProperty("TARGET_SOC", 30) //
										.build()) //
								.build())
						.build().toString()) //
				.build();

		this.appManagerTestBundle = new AppManagerTestBundle(componentConfig, initialConfig, t -> {
			return ImmutableList.of(//
					this.homeApp = Apps.feneconHome(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.prepareBatteryExtension(t), //

					this.kebaEvcsApp = Apps.kebaEvcs(t), //
					this.awattarApp = Apps.awattarHourly(t), //
					this.stromdao = Apps.stromdaoCorrently(t) //
			);
		});
	}

	@Test
	public void testAppValidateWorker() throws OpenemsException, Exception {
		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);

		assertEquals(this.appManagerTestBundle.sut.instantiatedApps.size(), 4);

		this.appManagerTestBundle.assertNoValidationErrors();
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testGetInstantiatedApps() {
		this.appManagerTestBundle.sut.getInstantiatedApps().add(null);
	}

	@Test
	public void testFindAppById() {
		assertEquals(this.homeApp, this.appManagerTestBundle.sut.findAppById("App.FENECON.Home").orElse(null));
	}

	@Test
	public void testCheckCardinalitySingle() throws Exception {
		var checkable = this.appManagerTestBundle.checkablesBundle.checkCardinality();
		checkable.setProperties(new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
				.put("openemsApp", this.homeApp) //
				.build());
		assertFalse(checkable.check());
		assertNotNull(checkable.getErrorMessage(Language.DEFAULT));
	}

	@Test
	public void testCheckCardinalityMultiple() throws Exception {
		this.appManagerTestBundle.sut.instantiatedApps.add(new OpenemsAppInstance(this.kebaEvcsApp.getAppId(), "alias",
				UUID.randomUUID(), JsonUtils.buildJsonObject().build(), null));
		this.appManagerTestBundle.sut.instantiatedApps.add(new OpenemsAppInstance(this.kebaEvcsApp.getAppId(), "alias",
				UUID.randomUUID(), JsonUtils.buildJsonObject().build(), null));
		var checkable = this.appManagerTestBundle.checkablesBundle.checkCardinality();
		checkable.setProperties(new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
				.put("openemsApp", this.kebaEvcsApp) //
				.build());
		assertTrue(checkable.check());
		assertNull(checkable.getErrorMessage(Language.DEFAULT));
	}

	@Test
	public void testCheckCardinalitySingleInCategorie() throws Exception {
		this.appManagerTestBundle.sut.instantiatedApps.add(new OpenemsAppInstance(this.awattarApp.getAppId(), "alias",
				UUID.randomUUID(), JsonUtils.buildJsonObject().build(), null));
		var checkable = this.appManagerTestBundle.checkablesBundle.checkCardinality();
		checkable.setProperties(new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
				.put("openemsApp", this.stromdao) //
				.build());
		assertFalse(checkable.check());
		assertNotNull(checkable.getErrorMessage(Language.DEFAULT));
	}

}
