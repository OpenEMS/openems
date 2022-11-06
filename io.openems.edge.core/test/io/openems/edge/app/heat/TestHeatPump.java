package io.openems.edge.app.heat;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.ModbusTcpApiReadOnly;
import io.openems.edge.app.api.RestJsonApiReadOnly;
import io.openems.edge.app.integratedsystem.FeneconHome;
import io.openems.edge.app.integratedsystem.TestFeneconHome;
import io.openems.edge.app.meter.SocomecMeter;
import io.openems.edge.app.pvselfconsumption.GridOptimizedCharge;
import io.openems.edge.app.pvselfconsumption.SelfConsumptionOptimization;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;
import io.openems.edge.io.test.DummyInputOutput;

public class TestHeatPump {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	private AppManagerTestBundle appManagerTestBundle;

	private HeatPump heatPump;

	private FeneconHome homeApp;

	private ModbusTcpApiReadOnly modbusTcpApiReadOnly;
	private RestJsonApiReadOnly restJsonApiReadOnly;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(
					this.heatPump = new HeatPump(t.componentManger,
							AppManagerTestBundle.getComponentContext("App.Heat.HeatPump"), t.cm, t.componentUtil), //
					this.homeApp = new FeneconHome(t.componentManger,
							AppManagerTestBundle.getComponentContext("App.FENECON.Home"), t.cm, t.componentUtil), //
					new GridOptimizedCharge(t.componentManger,
							AppManagerTestBundle.getComponentContext("App.PvSelfConsumption.GridOptimizedCharge"), t.cm,
							t.componentUtil), //
					new SelfConsumptionOptimization(t.componentManger,
							AppManagerTestBundle.getComponentContext(
									"App.PvSelfConsumption.SelfConsumptionOptimization"),
							t.cm, t.componentUtil), //
					new SocomecMeter(t.componentManger, AppManagerTestBundle.getComponentContext("App.Meter.Socomec"),
							t.cm, t.componentUtil), //
					this.modbusTcpApiReadOnly = new ModbusTcpApiReadOnly(t.componentManger,
							AppManagerTestBundle.getComponentContext("App.Api.ModbusTcp.ReadOnly"), t.cm,
							t.componentUtil), //
					this.restJsonApiReadOnly = new RestJsonApiReadOnly(t.componentManger,
							AppManagerTestBundle.getComponentContext("App.Api.RestJson.ReadOnly"), t.cm,
							t.componentUtil) //
			);
		});

		// create relay to make sure heat pump can be installed
		final var dummyRelay = new DummyInputOutput("io0");
		new ComponentTest(dummyRelay) //
				.activate(null);
		this.appManagerTestBundle.cm.getOrCreateEmptyConfiguration("io0");
		this.appManagerTestBundle.componentManger.addComponent(dummyRelay);
	}

	@Test
	public void testNotRemovingDependenciesFromRelay() throws Exception {
		// install usual free apps
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(
				this.modbusTcpApiReadOnly.getAppId(), "alias", JsonUtils.buildJsonObject().build()));
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user, new AddAppInstance.Request(
				this.restJsonApiReadOnly.getAppId(), "alias", JsonUtils.buildJsonObject().build()));
		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// install home
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.homeApp.getAppId(), "alias", TestFeneconHome.fullSettings()));
		assertEquals(6, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		// create heat pump
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(this.user,
				new AddAppInstance.Request(this.heatPump.getAppId(), "alias", JsonUtils.buildJsonObject().build()));

		assertEquals(7, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		var heatPumpInstance = this.appManagerTestBundle.findFirst(this.heatPump.getAppId());
		var home = this.appManagerTestBundle.findFirst(this.homeApp.getAppId());

		// dependency to relay(in this example home)
		assertEquals(1, heatPumpInstance.dependencies.size());
		assertEquals(heatPumpInstance.dependencies.get(0).instanceId, home.instanceId);

		// make sure home still has its dependencies
		assertEquals(3, home.dependencies.size());

		// update heat pump
		this.appManagerTestBundle.sut.handleJsonrpcRequest(this.user, new UpdateAppInstance.Request(
				heatPumpInstance.instanceId, "alias", JsonUtils.buildJsonObject().build()));

		// if exceptions occurs here heat pump also deleted dependencies from home
		assertEquals(7, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		heatPumpInstance = this.appManagerTestBundle.findFirst(this.heatPump.getAppId());
		home = this.appManagerTestBundle.findFirst(this.homeApp.getAppId());

		assertEquals(3, home.dependencies.size());
		assertEquals(1, heatPumpInstance.dependencies.size());
	}

}
