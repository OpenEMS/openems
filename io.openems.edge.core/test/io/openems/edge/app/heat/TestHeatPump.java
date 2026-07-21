package io.openems.edge.app.heat;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.common.test.DummyUser.DUMMY_OWNER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.ModbusTcpApiReadOnly;
import io.openems.edge.app.api.RestJsonApiReadOnly;
import io.openems.edge.app.integratedsystem.FeneconHome10;
import io.openems.edge.app.integratedsystem.TestFeneconHome10;
import io.openems.edge.app.integratedsystem.TestFeneconHome20;
import io.openems.edge.app.meter.EastronMeter;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.io.test.DummyInputOutput;

public class TestHeatPump {

	private AppManagerTestBundle appManagerTestBundle;

	private HeatPump heatPump;

	private FeneconHome10 homeApp;

	private EastronMeter internMeter;

	private SocomecMeter externMeter;

	private ModbusTcpApiReadOnly modbusTcpApiReadOnly;
	private RestJsonApiReadOnly restJsonApiReadOnly;

	@BeforeEach
	public void beforeEach() throws Exception {
		final var componentFactory = new AppManagerTestBundle.PseudoComponentManagerFactory();
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return List.of(//
					this.heatPump = Apps.heatPump(t), //
					this.homeApp = Apps.feneconHome10(t), //
					Apps.feneconHome20(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.socomecMeter(t), //
					this.modbusTcpApiReadOnly = Apps.modbusTcpApiReadOnly(t), //
					this.restJsonApiReadOnly = Apps.restJsonApiReadOnly(t), //
					this.internMeter = Apps.eastronMeter(t), //
					this.externMeter = Apps.socomecMeter(t) //
			);
		}, null, componentFactory);

		// create relay to make sure heat pump can be installed
		final var dummyRelay = new DummyInputOutput("io0");
		new ComponentTest(dummyRelay) //
				.activate(null);
		this.appManagerTestBundle.cm.getOrCreateEmptyConfiguration("io0");
		componentFactory.getComponentManager().addComponent(dummyRelay);
	}

	@Test
	public void testNotRemovingDependenciesFromRelay() throws Exception {
		// install usual free apps
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				this.modbusTcpApiReadOnly.getAppId(), "key", "alias", JsonUtils.buildJsonObject().build()));
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				this.restJsonApiReadOnly.getAppId(), "key", "alias", JsonUtils.buildJsonObject().build()));
		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// install home
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.homeApp.getAppId(), "key", "alias", TestFeneconHome10.fullSettings()));

		assertEquals(6, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// create heat pump
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				this.heatPump.getAppId(), "key", "alias", JsonUtils.buildJsonObject().build()));

		assertEquals(7, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var heatPumpInstance = this.appManagerTestBundle.findFirst(this.heatPump.getAppId());
		var home = this.appManagerTestBundle.findFirst(this.homeApp.getAppId());

		// dependency to relay(in this example home)
		assertEquals(1, heatPumpInstance.dependencies.size());
		assertEquals(heatPumpInstance.dependencies.get(0).instanceId, home.instanceId);

		// make sure home still has its dependencies
		assertEquals(3, home.dependencies.size());

		// update heat pump
		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN, new UpdateAppInstance.Request(
				heatPumpInstance.instanceId, "alias", JsonUtils.buildJsonObject().build()));

		// if exceptions occurs here heat pump also deleted dependencies from home
		assertEquals(7, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		heatPumpInstance = this.appManagerTestBundle.findFirst(this.heatPump.getAppId());
		home = this.appManagerTestBundle.findFirst(this.homeApp.getAppId());

		assertEquals(3, home.dependencies.size());
		assertEquals(1, heatPumpInstance.dependencies.size());
	}

	@Test
	public void testInternMeterDependency() throws Exception {

		this.createHome20();

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.heatPump.getAppId(), "key2", "alias2", getHeatPumpWithInternMeter()));

		this.testInitiatedAppsSizeWithMeter();
		this.testDependencySizeWithMeter();
		this.testCorrectMeterDependencyExists(this.internMeter.getAppId());
	}

	@Test
	public void testExternMeterDependency() throws Exception {

		this.createHome20();
		this.createExternMeterComponentAndApp();

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.heatPump.getAppId(), "key3", "alias3", getHeatPumpWithExternMeter()));

		this.testInitiatedAppsSizeWithMeter();
		this.testDependencySizeWithMeter();
		this.testCorrectMeterDependencyExists(this.externMeter.getAppId());
	}

	private static JsonObject getHeatPumpWithInternMeter() {
		return JsonUtils.buildJsonObject() //
				.addProperty("OUTPUT_CHANNEL_1", "io0/InputOutput0") //
				.addProperty("OUTPUT_CHANNEL_2", "io0/InputOutput1") //
				.addProperty("IS_ELEMENT_MEASURED", true) //
				.addProperty("HOW_MEASURED", "INTERN") //
				.build();
	}

	private static JsonObject getHeatPumpWithExternMeter() {
		return JsonUtils.buildJsonObject() //
				.addProperty("OUTPUT_CHANNEL_1", "io0/InputOutput0") //
				.addProperty("OUTPUT_CHANNEL_2", "io0/InputOutput1") //
				.addProperty("IS_ELEMENT_MEASURED", true) //
				.addProperty("HOW_MEASURED", "EXTERN") //
				.addProperty("METER_ID", "meter4") //
				.build();
	}

	private void createHome20() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("App.FENECON.Home.20", "key", "alias", TestFeneconHome20.fullSettings()));
	}

	private void createExternMeterComponentAndApp() throws Exception {
		final var meterProperties = List.of(//
				new UpdateComponentConfigRequest.Property("id", "meter4") //
		);
		this.appManagerTestBundle.componentManger.handleCreateComponentConfigRequest(DUMMY_OWNER,
				new CreateComponentConfig.Request("Meter.Socomec.Threephase", meterProperties));

		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.externMeter.getAppId(), "key2", "key2",
						JsonUtils.buildJsonObject().addProperty("METER_ID", "meter4").build()));
	}

	private void testInitiatedAppsSizeWithMeter() {
		assertEquals(6, this.appManagerTestBundle.sut.getInstantiatedApps().size());
	}

	private void testDependencySizeWithMeter() {
		var heatPumpInstance = this.appManagerTestBundle.findFirst(this.heatPump.getAppId());
		assertEquals(2, heatPumpInstance.dependencies.size());
	}

	private void testCorrectMeterDependencyExists(//
			final String meterAppId //
	) {
		var heatPumpInstance = this.appManagerTestBundle.findFirst(this.heatPump.getAppId());
		var meterInstance = this.appManagerTestBundle.findFirst(meterAppId);
		var meterDependency = heatPumpInstance.dependencies.stream()
				.filter(d -> d.instanceId.equals(meterInstance.instanceId)).findFirst();
		assertNotNull(meterDependency);
	}
}
