package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.jsonrpc.type.UpdateComponentConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.evcs.SwitchArchitecture;
import io.openems.edge.app.evcs.readonly.AppGoeEvcsReadOnly;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance.Request;

@RunWith(MockitoJUnitRunner.class)
public class SwitchEvcsEvseTest {

	private OpenemsApp kebaApp;
	private AppManagerTestBundle amtb;
	private SwitchArchitecture sa;
	private AppGoeEvcsReadOnly goeApp;

	@Spy
	private ComponentManager cmSpy;

	@Before
	public void setup() throws Exception {
		this.amtb = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.kebaApp = Apps.kebaEvcs(t), //
					this.goeApp = Apps.goeEvcs(t), //
					Apps.genericVehicle(t), //
					Apps.clusterEvse(t), //
					Apps.evcsCluster(t));
		}, null, new PseudoComponentManagerFactory());
		this.amtb.addComponentAggregateTask();
		this.amtb.addStaticIpAggregateTask();

		this.cmSpy = spy(this.amtb.componentManger);
		doNothing().when(this.cmSpy).handleUpdateComponentConfigRequest(//
				any(User.class), //
				any(UpdateComponentConfig.Request.class));

		this.sa = new SwitchArchitecture(this.amtb.appManagerUtil, this.cmSpy, this.amtb.sut, new DummyOpenemsEdgeOem());
	}

	@Test
	public void testCanSwitch() throws Exception {
		this.amtb.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new Request(this.kebaApp.getAppId(), null, "testApp", JsonUtils.buildJsonObject()//
						.addProperty("ARCHITECTURE_TYPE", "EVCS") //
						.addProperty("HARDWARE_TYPE", "P40") //
						.addProperty("IP", "192.168.25.11") //
						.addProperty("PHASE_ROTATION", "L1_L2_L3") //
						.addProperty("MODBUS_UNIT_ID", 255) //
						.addProperty("READ_ONLY", false) //
						.build()));

		this.amtb.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new Request(this.goeApp.getAppId(), null, "testApp2", JsonUtils.buildJsonObject()//
						.build()));

		this.amtb.assertInstalledApps(2);

		var response = this.sa.handleCanSwitch(DUMMY_ADMIN);
		assertFalse(response.canSwitch());
	}

	@Test
	public void testEvseToEvcs() throws Exception {

		var vehicleAppInstance = this.amtb.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.Evse.ElectricVehicle.Generic",
						// TODO: make vehicle generic app free of charge
						"0000-0000-0000", "EV1", //
						JsonUtils.buildJsonObject()// default values
								.build()));

		this.amtb.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new Request(this.kebaApp.getAppId(), null, "testApp", JsonUtils.buildJsonObject()//
						.addProperty("ARCHITECTURE_TYPE", "EVSE") //
						.addProperty("HARDWARE_TYPE", "P40") //
						.addProperty("IP", "192.168.25.11") //
						.addProperty("PHASE_ROTATION", "L1_L2_L3") //
						.addProperty("ELECTRIC_VEHICLE_ID", vehicleAppInstance.instance().instanceId.toString())
						.addProperty("MODBUS_UNIT_ID", 255) //
						.addProperty("READ_ONLY", false) //
						.build()));

		vehicleAppInstance = this.amtb.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.Evse.ElectricVehicle.Generic",
						// TODO: make vehicle generic app free of charge
						"0000-0000-0000", "EV2", //
						JsonUtils.buildJsonObject()// default values
								.build()));

		this.amtb.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new Request(this.kebaApp.getAppId(), null, "testApp2", JsonUtils.buildJsonObject()//
						.addProperty("ARCHITECTURE_TYPE", "EVSE") //
						.addProperty("HARDWARE_TYPE", "P40") //
						.addProperty("IP", "192.168.25.12") //
						.addProperty("ELECTRIC_VEHICLE_ID", vehicleAppInstance.instance().instanceId.toString())
						.addProperty("PHASE_ROTATION", "L1_L2_L3") //
						.addProperty("MODBUS_UNIT_ID", 255) //
						.addProperty("READ_ONLY", false) //
						.build()));

		this.amtb.assertInstalledApps(5);

		var responseCanHandle = this.sa.handleCanSwitch(DUMMY_ADMIN);
		assertTrue(responseCanHandle.canSwitch());

		var response = this.sa.handleSwitchEmobilityArchitecture(DUMMY_ADMIN);

		// Assert update version of EnergyScheduler
		var captor = ArgumentCaptor.forClass(UpdateComponentConfig.Request.class);
		verify(this.cmSpy).handleUpdateComponentConfigRequest(eq(DUMMY_ADMIN), captor.capture());
		var capturedRequest = captor.getValue();
		boolean hasV1Version = capturedRequest.properties().stream()//
				.anyMatch(p -> p.getName().equals("version")
						&& p.getValue().getAsString().equals(io.openems.edge.energy.api.Version.V1_ESS_ONLY.name()));
		assertTrue(hasV1Version);

		assertEquals(2, response.apps().stream().filter(t -> t.appId.equals("App.Evcs.Keba")).toList().size());
		assertEquals(1, response.apps().stream().filter(t -> t.appId.equals("App.Evcs.Cluster")).toList().size());

	}

	@Test
	public void testEvcsToEvse() throws OpenemsNamedException, IOException {
		this.amtb.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new Request(this.kebaApp.getAppId(), null, "testApp", JsonUtils.buildJsonObject()//
						.addProperty("ARCHITECTURE_TYPE", "EVCS") //
						.addProperty("HARDWARE_TYPE", "P40") //
						.addProperty("IP", "192.168.25.11") //
						.addProperty("PHASE_ROTATION", "L1_L2_L3") //
						.addProperty("MODBUS_UNIT_ID", 255) //
						.addProperty("READ_ONLY", false) //
						.build()));

		this.amtb.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new Request(this.kebaApp.getAppId(), null, "testApp2", JsonUtils.buildJsonObject()//
						.addProperty("ARCHITECTURE_TYPE", "EVCS") //
						.addProperty("HARDWARE_TYPE", "P40") //
						.addProperty("IP", "192.168.25.12") //
						.addProperty("PHASE_ROTATION", "L1_L2_L3") //
						.addProperty("MODBUS_UNIT_ID", 255) //
						.addProperty("READ_ONLY", false) //
						.build()));

		this.amtb.assertInstalledApps(3);
		var responseCanHandle = this.sa.handleCanSwitch(DUMMY_ADMIN);
		assertTrue(responseCanHandle.canSwitch());

		var response = this.sa.handleSwitchEmobilityArchitecture(DUMMY_ADMIN);

		// Assert update version of EnergyScheduler
		var captor = ArgumentCaptor.forClass(UpdateComponentConfig.Request.class);
		verify(this.cmSpy).handleUpdateComponentConfigRequest(eq(DUMMY_ADMIN), captor.capture());
		var capturedRequest = captor.getValue();
		boolean hasV2Version = capturedRequest.properties().stream()//
				.anyMatch(p -> p.getName().equals("version") && p.getValue().getAsString()
						.equals(io.openems.edge.energy.api.Version.V2_ENERGY_SCHEDULABLE.name()));
		assertTrue(hasV2Version);

		assertEquals(2, response.apps().stream().filter(t -> t.appId.equals("App.Evse.ElectricVehicle.Generic"))
				.toList().size());
		assertEquals(2, response.apps().stream().filter(t -> t.appId.equals("App.Evcs.Keba")).toList().size());
		assertEquals(1,
				response.apps().stream().filter(t -> t.appId.equals("App.Evse.Controller.Cluster")).toList().size());
	}

}
