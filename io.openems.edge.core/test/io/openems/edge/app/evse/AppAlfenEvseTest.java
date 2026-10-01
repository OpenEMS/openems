package io.openems.edge.app.evse;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

class AppAlfenEvseTest {

	private AppManagerTestBundle appManagerTestBundle;
	private AppAlfenEvse alfenEvse;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> List.of(//
				this.alfenEvse = Apps.alfenEvse(t), //
				Apps.genericVehicle(t), //
				Apps.clusterEvse(t) //
		), null, new AppManagerTestBundle.PseudoComponentManagerFactory());
		this.appManagerTestBundle.addComponentAggregateTask();
	}

	@Test
	void testHasOnlyExpectedProperties() {
		assertEquals(12, AppAlfenEvse.Property.values().length);
		this.appManagerTestBundle //
				.withApp(this.alfenEvse) //
				.hasOnlyProperties(//
						AppAlfenEvse.Property.EVSE_ID, //
						AppAlfenEvse.Property.CTRL_SINGLE_ID, //
						AppAlfenEvse.Property.MODBUS_ID, //
						AppAlfenEvse.Property.ALIAS, //
						AppAlfenEvse.Property.IP, //
						AppAlfenEvse.Property.MODBUS_UNIT_ID, //
						AppAlfenEvse.Property.ELECTRIC_VEHICLE_ID, //
						AppAlfenEvse.Property.CONFIGURE_VEHICLE, //
						AppAlfenEvse.Property.WIRING, //
						AppAlfenEvse.Property.PHASE_ROTATION, //
						AppAlfenEvse.Property.READ_ONLY, //
						AppAlfenEvse.Property.NAVIGATION_MIGRATION_ACKNOWLEDGEMENT);
	}

	@Test
	void testCreateApp() throws Exception {
		var vehicle = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.Evse.ElectricVehicle.Generic", "key", "Vehicle",
						JsonUtils.buildJsonObject() //
								.build()))
				.instance();

		var instance = this.createApp(vehicle.instanceId.toString());

		assertEquals(3, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertEquals(AppAlfenEvse.APP_EVSE_ALFEN, instance.appId);

		this.appManagerTestBundle.assertComponentsExist(//
				new EdgeConfig.Component("modbus0", null, "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
						.addProperty("ip", "192.168.25.11") //
						.addProperty("port", 502) //
						.build()), //
				new EdgeConfig.Component("evseChargePoint0", null, "Evse.ChargePoint.Alfen", JsonUtils.buildJsonObject() //
						.addProperty("readOnly", true) //
						.addProperty("phaseRotation", "L1_L2_L3") //
						.addProperty("wiring", "THREE_PHASE") //
						.addProperty("modbus.id", "modbus0") //
						.addProperty("modbusUnitId", 1) //
						.build()), //
				new EdgeConfig.Component("ctrlEvseSingle0", null, "Evse.Controller.Single", JsonUtils.buildJsonObject() //
						.addProperty("electricVehicle.id", "evseElectricVehicle0") //
						.addProperty("chargePoint.id", "evseChargePoint0") //
						.build()));
	}

	private OpenemsAppInstance createApp(String vehicleId) throws Exception {
		return this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.alfenEvse.getAppId(), "key", "Alfen", properties(vehicleId)))
				.instance();
	}

	private static JsonObject properties(String vehicleId) {
		return JsonUtils.buildJsonObject() //
				.addProperty("IP", "192.168.25.11") //
				.addProperty("ELECTRIC_VEHICLE_ID", vehicleId) //
				.addProperty("WIRING", "THREE_PHASE") //
				.addProperty("PHASE_ROTATION", "L1_L2_L3") //
				.addProperty("READ_ONLY", true) //
				.build();
	}
}



