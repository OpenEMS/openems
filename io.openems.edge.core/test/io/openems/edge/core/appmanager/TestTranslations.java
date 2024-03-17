package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.TestFeneconHome;
import io.openems.edge.app.integratedsystem.TestFeneconHome20;
import io.openems.edge.app.integratedsystem.TestFeneconHome30;
import io.openems.edge.app.integratedsystem.TestFeneconIndustrial;

public class TestTranslations {

	private record TestTranslation(OpenemsApp app, boolean validateAppAssistant, JsonObject config) {

	}

	private List<TestTranslation> apps;

	@Before
	public void beforeEach() throws Exception {
		this.apps = new ArrayList<>();
		new AppManagerTestBundle(null, null, t -> {
			this.apps.add(new TestTranslation(Apps.feneconHome(t), true, TestFeneconHome.fullSettings()));
			this.apps.add(new TestTranslation(Apps.feneconHome20(t), true, TestFeneconHome20.fullSettings()));
			this.apps.add(new TestTranslation(Apps.feneconHome30(t), true, TestFeneconHome30.fullSettings()));
			this.apps.add(new TestTranslation(Apps.feneconIsk010(t), true, TestFeneconIndustrial.fullSettings()));
			this.apps.add(new TestTranslation(Apps.feneconIsk110(t), true, TestFeneconIndustrial.fullSettings()));
			this.apps.add(new TestTranslation(Apps.feneconIsk011(t), true, TestFeneconIndustrial.fullSettings()));
			this.apps.add(new TestTranslation(Apps.awattarHourly(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.entsoE(t), true, JsonUtils.buildJsonObject() //
					.addProperty("BIDDING_ZONE", "GERMANY") //
					.build()));
			this.apps.add(new TestTranslation(Apps.stromdaoCorrently(t), true, JsonUtils.buildJsonObject() //
					.addProperty("ZIP_CODE", "123456789") //
					.build()));
			this.apps.add(new TestTranslation(Apps.tibber(t), true, JsonUtils.buildJsonObject() //
					.addProperty("ACCESS_TOKEN", "123456789") //
					.build()));
			this.apps.add(new TestTranslation(Apps.modbusTcpApiReadOnly(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.modbusTcpApiReadWrite(t), true, JsonUtils.buildJsonObject() //
					.addProperty("API_TIMEOUT", 60) //
					.add("COMPONENT_IDS", new JsonArray()) //
					.build()));
			this.apps.add(new TestTranslation(Apps.restJsonApiReadOnly(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.restJsonApiReadWrite(t), true, JsonUtils.buildJsonObject() //
					.addProperty("API_TIMEOUT", 60) //
					.build()));
			this.apps.add(new TestTranslation(Apps.hardyBarthEvcs(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.kebaEvcs(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.iesKeywattEvcs(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.alpitronicEvcs(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.webastoNext(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.webastoUnite(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.evcsCluster(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.heatPump(t), false, JsonUtils.buildJsonObject() //
					.addProperty("OUTPUT_CHANNEL_1", "io0/Relay1") //
					.addProperty("OUTPUT_CHANNEL_2", "io0/Relay2") //
					.build()));
			this.apps.add(new TestTranslation(Apps.combinedHeatAndPower(t), false, JsonUtils.buildJsonObject() //
					.addProperty("OUTPUT_CHANNEL", "io0/Relay1") //
					.build()));
			this.apps.add(new TestTranslation(Apps.heatingElement(t), false, JsonUtils.buildJsonObject() //
					.addProperty("OUTPUT_CHANNEL_PHASE_L1", "io0/Relay1") //
					.addProperty("OUTPUT_CHANNEL_PHASE_L2", "io0/Relay2") //
					.addProperty("OUTPUT_CHANNEL_PHASE_L3", "io0/Relay3") //
					.build()));
			this.apps.add(new TestTranslation(Apps.gridOptimizedCharge(t), true, JsonUtils.buildJsonObject() //
					.addProperty("MAXIMUM_SELL_TO_GRID_POWER", 60) //
					.build()));
			this.apps.add(new TestTranslation(Apps.selfConsumptionOptimization(t), true, JsonUtils.buildJsonObject() //
					.addProperty("ESS_ID", "ess0") //
					.addProperty("METER_ID", "meter0") //
					.build()));
			this.apps.add(new TestTranslation(Apps.manualRelayControl(t), false, JsonUtils.buildJsonObject() //
					.addProperty("OUTPUT_CHANNEL", "io0/Relay1") //
					.build()));
			this.apps.add(new TestTranslation(Apps.thresholdControl(t), false, JsonUtils.buildJsonObject() //
					.add("OUTPUT_CHANNELS", JsonUtils.buildJsonArray().add("io0/Relay1").build()) //
					.build()));
			this.apps.add(new TestTranslation(Apps.socomecMeter(t), false, JsonUtils.buildJsonObject() //
					.addProperty("MODBUS_ID", "modbus0") //
					.build()));
			this.apps.add(new TestTranslation(Apps.carloGavazziMeter(t), false, JsonUtils.buildJsonObject() //
					.addProperty("MODBUS_ID", "modbus0") //
					.addProperty("MODBUS_UNIT_ID", 5) //
					.build()));
			this.apps.add(new TestTranslation(Apps.janitzaMeter(t), false, JsonUtils.buildJsonObject() //
					.addProperty("MODBUS_ID", "modbus0") //
					.build()));
			this.apps.add(new TestTranslation(Apps.froniusPvInverter(t), false, JsonUtils.buildJsonObject() //
					.addProperty("MODBUS_ID", "modbus0") //
					.addProperty("PORT", 502) //
					.addProperty("MODBUS_UNIT_ID", 1) //
					.build()));
			this.apps.add(new TestTranslation(Apps.kacoPvInverter(t), false, JsonUtils.buildJsonObject() //
					.addProperty("MODBUS_ID", "modbus0") //
					.addProperty("PORT", 502) //
					.build()));
			this.apps.add(new TestTranslation(Apps.kostalPvInverter(t), false, JsonUtils.buildJsonObject() //
					.addProperty("MODBUS_ID", "modbus0") //
					.addProperty("PORT", 502) //
					.addProperty("MODBUS_UNIT_ID", 1) //
					.build()));
			this.apps.add(new TestTranslation(Apps.smaPvInverter(t), false, JsonUtils.buildJsonObject() //
					.addProperty("MODBUS_ID", "modbus0") //
					.build()));
			this.apps.add(new TestTranslation(Apps.solarEdgePvInverter(t), false, JsonUtils.buildJsonObject() //
					.addProperty("MODBUS_ID", "modbus0") //
					.addProperty("PORT", 502) //
					.build()));
			this.apps.add(new TestTranslation(Apps.peakShaving(t), true, JsonUtils.buildJsonObject() //
					.addProperty("ESS_ID", "ess0") //
					.addProperty("METER_ID", "meter0") //
					.build()));
			this.apps.add(new TestTranslation(Apps.phaseAccuratePeakShaving(t), true, JsonUtils.buildJsonObject() //
					.addProperty("ESS_ID", "ess0") //
					.addProperty("METER_ID", "meter0") //
					.build()));
			this.apps.add(new TestTranslation(Apps.fixActivePower(t), true, JsonUtils.buildJsonObject() //
					.addProperty("ESS_ID", "ess0") //
					.build()));
			this.apps.add(new TestTranslation(Apps.powerPlantController(t), true, new JsonObject()));
			this.apps.add(new TestTranslation(Apps.prepareBatteryExtension(t), true, new JsonObject()));
			return this.apps.stream().map(TestTranslation::app).toList();
		});
	}

	@Test
	public void testGermanTranslation() throws Exception {
		this.testTranslations(Language.DE);
	}

	@Test
	public void testEnglishTranslation() throws Exception {
		this.testTranslations(Language.EN);
	}

	@Test
	// TODO this is certainly not the best place for this test, but it holds the
	// most testable Apps.
	public void testOemWebsiteUrl() throws Exception {
		var dummyOem = new DummyOpenemsEdgeOem();
		var missing = this.apps.stream() //
				.map(TestTranslation::app) //
				.map(OpenemsApp::getAppId) //
				.filter(appId -> dummyOem.getAppWebsiteUrl(appId) == null) //
				.toList();
		assertTrue("Missing Website-URLs in Edge-OEM for [" + String.join(", ", missing) + "]", missing.isEmpty());
	}

	private void testTranslations(Language l) throws Exception {
		final var debugTranslator = TranslationUtil.enableDebugMode();

		for (var entry : this.apps) {
			final var app = entry.app();
			if (entry.validateAppAssistant()) {
				app.getAppAssistant(DUMMY_ADMIN);
			}
			if (entry.config() != null) {
				app.getAppConfiguration(ConfigurationTarget.ADD, entry.config(), l);
			}
		}

		assertTrue(
				"Missing Translation Keys for Language " + l + " ["
						+ String.join(", ", debugTranslator.getMissingKeys()) + "]",
				debugTranslator.getMissingKeys().isEmpty());
	}

}
