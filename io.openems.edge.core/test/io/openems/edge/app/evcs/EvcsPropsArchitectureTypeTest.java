package io.openems.edge.app.evcs;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class EvcsPropsArchitectureTypeTest {

	private AppManagerTestBundle testBundle;
	private OpenemsApp hardyBarthEvcs;
	private OpenemsApp kebaEvcs;
	private OpenemsApp mennekesEvse;

	@BeforeEach
	public void before() throws Exception {
		this.testBundle = new AppManagerTestBundle(null, null, t -> List.of(//
				this.hardyBarthEvcs = Apps.hardyBarthEvcs(t), //
				this.kebaEvcs = Apps.kebaEvcs(t), //
				this.mennekesEvse = Apps.mennekesEvse(t), //
				Apps.genericVehicle(t), //
				Apps.clusterEvse(t), //
				Apps.evcsCluster(t) //
		));
	}

	@Test
	public void architectureTypeShowsAllSupportedOptionsWithoutInstalledArchitecture() {
		var architectureTypeField = this.getArchitectureTypeField(this.hardyBarthEvcs.getAppAssistant(DUMMY_ADMIN));

		assertNotNull(architectureTypeField);
		assertEquals(List.of("EVCS", "EVSE"), this.getOptionValues(architectureTypeField));
		assertEquals("EVCS", architectureTypeField.get("defaultValue").getAsString());
	}

	@Test
	public void architectureTypeUsesConfiguredArchitectureTypeOfInstalledApp() throws Exception {
		var vehicleInstance = this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.Evse.ElectricVehicle.Generic", null, "EV1", //
						JsonUtils.buildJsonObject().build()));

		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.kebaEvcs.getAppId(), null, "Keba", //
						JsonUtils.buildJsonObject() //
								.addProperty("ARCHITECTURE_TYPE", "EVSE") //
								.addProperty("ELECTRIC_VEHICLE_ID", vehicleInstance.instance().instanceId.toString()) //
								.build()));

		var architectureTypeField = this.getArchitectureTypeField(this.hardyBarthEvcs.getAppAssistant(DUMMY_ADMIN));

		assertNotNull(architectureTypeField);
		assertEquals(List.of("EVSE"), this.getOptionValues(architectureTypeField));
		assertEquals("EVSE", architectureTypeField.get("defaultValue").getAsString());
	}

	@Test
	public void architectureTypeFallsBackToSingleSupportedTypeOfInstalledEmobilityApp() throws Exception {
		var vehicleInstance = this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.Evse.ElectricVehicle.Generic", null, "EV1", //
						JsonUtils.buildJsonObject().build()));

		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.mennekesEvse.getAppId(), null, "Mennekes", //
						JsonUtils.buildJsonObject() //
								.addProperty("ELECTRIC_VEHICLE_ID", vehicleInstance.instance().instanceId.toString()) //
								.build()));

		var architectureTypeField = this.getArchitectureTypeField(this.hardyBarthEvcs.getAppAssistant(DUMMY_ADMIN));

		assertNotNull(architectureTypeField);
		assertEquals(List.of("EVSE"), this.getOptionValues(architectureTypeField));
		assertEquals("EVSE", architectureTypeField.get("defaultValue").getAsString());
	}

	private JsonObject getArchitectureTypeField(AppAssistant appAssistant) {
		for (var field : appAssistant.fields) {
			var fieldObject = field.getAsJsonObject();
			if ("ARCHITECTURE_TYPE".equals(fieldObject.get("key").getAsString())) {
				return fieldObject;
			}
		}
		return null;
	}

	private List<String> getOptionValues(JsonObject architectureTypeField) {
		return architectureTypeField //
				.getAsJsonObject("templateOptions") //
				.getAsJsonArray("options") //
				.asList().stream() //
				.map(option -> option //
						.getAsJsonObject().get("value").getAsString())
				.toList();
	}
}
