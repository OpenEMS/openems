package io.openems.edge.app.integratedsystem;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static io.openems.edge.core.appmanager.AssertOpenemsAppPropertyDefinition.assertPropertyDefaultValue;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.jsonrpc.request.DeleteComponentConfigRequest;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.heat.CombinedHeatAndPower;
import io.openems.edge.app.heat.HeatPump;
import io.openems.edge.app.heat.HeatingElement;
import io.openems.edge.app.loadcontrol.ManualRelayControl;
import io.openems.edge.app.loadcontrol.ThresholdControl;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;
import io.openems.edge.core.appmanager.OpenemsAppInstance;
import io.openems.edge.io.test.DummyInputOutput;

public class TestFeneconHomeDefaultRelays {

	private AppManagerTestBundle appManagerTestBundle;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return Apps.of(t, //
					Apps::feneconHome, //
					Apps::gridOptimizedCharge, //
					Apps::selfConsumptionOptimization, //
					Apps::socomecMeter, //
					Apps::prepareBatteryExtension, //
					Apps::heatPump, //
					Apps::heatingElement, //
					Apps::combinedHeatAndPower, //
					Apps::manualRelayControl, //
					Apps::thresholdControl //
			);
		}, null, new PseudoComponentManagerFactory());
		this.createFullHomeWithDummyIo();
	}

	@Test
	public void testDefaultRelayValuesChp() throws Exception {
		final var app = this.appManagerTestBundle.sut.findAppByIdOrError("App.Heat.CHP");
		final var props = app.getProperties();

		assertPropertyDefaultValue(props, CombinedHeatAndPower.Property.OUTPUT_CHANNEL, "io0/Relay1");
	}

	@Test
	public void testDefaultRelayValuesHeatingElement() throws Exception {
		final var app = this.appManagerTestBundle.sut.findAppByIdOrError("App.Heat.HeatingElement");
		final var props = app.getProperties();

		assertPropertyDefaultValue(props, HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L1, "io0/Relay1");
		assertPropertyDefaultValue(props, HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L2, "io0/Relay2");
		assertPropertyDefaultValue(props, HeatingElement.Property.OUTPUT_CHANNEL_PHASE_L3, "io0/Relay3");
	}

	@Test
	public void testDefaultRelayValuesHeatPump() throws Exception {
		final var app = this.appManagerTestBundle.sut.findAppByIdOrError("App.Heat.HeatPump");
		final var props = app.getProperties();

		assertPropertyDefaultValue(props, HeatPump.Property.OUTPUT_CHANNEL_1, "io0/Relay2");
		assertPropertyDefaultValue(props, HeatPump.Property.OUTPUT_CHANNEL_2, "io0/Relay3");
	}

	@Test
	public void testDefaultRelayValuesManuelRelayControl() throws Exception {
		final var app = this.appManagerTestBundle.sut.findAppByIdOrError("App.LoadControl.ManualRelayControl");
		final var props = app.getProperties();

		assertPropertyDefaultValue(props, ManualRelayControl.Property.OUTPUT_CHANNEL, "io0/Relay1");
	}

	@Test
	public void testDefaultRelayValuesThresholdControl() throws Exception {
		final var app = this.appManagerTestBundle.sut.findAppByIdOrError("App.LoadControl.ThresholdControl");
		final var props = app.getProperties();

		assertPropertyDefaultValue(props, ThresholdControl.Property.OUTPUT_CHANNELS, JsonUtils.buildJsonArray() //
				.add("io0/Relay1") //
				.build());
	}

	private final OpenemsAppInstance createFullHomeWithDummyIo() throws Exception {
		final var instance = TestFeneconHome.createFullHome(this.appManagerTestBundle, DUMMY_ADMIN);
		this.appManagerTestBundle.componentManger.handleDeleteComponentConfigRequest(DUMMY_ADMIN,
				new DeleteComponentConfigRequest("io0"));
		final var dummyRelay = new DummyInputOutput("io0", "RELAY", 1, 4);
		this.appManagerTestBundle.cm.getOrCreateEmptyConfiguration(dummyRelay.id());
		((DummyPseudoComponentManager) this.appManagerTestBundle.componentManger).addComponent(dummyRelay);
		final var dummyRelay1 = new DummyInputOutput("io1", "RELAY", 1, 4);
		this.appManagerTestBundle.cm.getOrCreateEmptyConfiguration(dummyRelay1.id());
		((DummyPseudoComponentManager) this.appManagerTestBundle.componentManger).addComponent(dummyRelay1);
		return instance;
	}

}
