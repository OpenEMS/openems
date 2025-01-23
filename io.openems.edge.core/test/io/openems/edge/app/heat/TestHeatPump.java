package io.openems.edge.app.heat;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.ModbusTcpApiReadOnly;
import io.openems.edge.app.api.RestJsonApiReadOnly;
import io.openems.edge.app.integratedsystem.FeneconHome10;
import io.openems.edge.app.integratedsystem.TestFeneconHome10;
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

	private ModbusTcpApiReadOnly modbusTcpApiReadOnly;
	private RestJsonApiReadOnly restJsonApiReadOnly;

	@Before
	public void beforeEach() throws Exception {
		final var componentFactory = new AppManagerTestBundle.DefaultComponentManagerFactory();
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					this.heatPump = Apps.heatPump(t), //
					this.homeApp = Apps.feneconHome10(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.socomecMeter(t), //
					this.modbusTcpApiReadOnly = Apps.modbusTcpApiReadOnly(t), //
					this.restJsonApiReadOnly = Apps.restJsonApiReadOnly(t) //
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

}
