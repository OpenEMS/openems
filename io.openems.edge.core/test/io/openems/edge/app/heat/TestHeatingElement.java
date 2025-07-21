package io.openems.edge.app.heat;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.integratedsystem.FeneconHome20;
import io.openems.edge.app.integratedsystem.TestFeneconHome20;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.core.appmanager.jsonrpc.UpdateAppInstance;

public class TestHeatingElement {
	private AppManagerTestBundle appManagerTestBundle;
	private HeatingElement heatingElement;
	private FeneconHome20 homeApp;

	@Before
	public void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(//
					Apps.eastronMeter(t), //
					this.heatingElement = Apps.heatingElement(t), //
					this.homeApp = Apps.feneconHome20(t), //
					Apps.gridOptimizedCharge(t), //
					Apps.selfConsumptionOptimization(t), //
					Apps.socomecMeter(t) //
			);
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		this.appManagerTestBundle.addComponentAggregateTask();
	}

	@Test
	public void testInternMeterDependencies() throws Exception {
		// install home
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.homeApp.getAppId(), "key", "alias", TestFeneconHome20.fullSettings()));

		assertEquals(4, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		final var result = this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.heatingElement.getAppId(), "key", "alias",
						this.getHeatingWithInternMeterJson()));

		assertEquals(6, this.appManagerTestBundle.sut.getInstantiatedApps().size());

		this.appManagerTestBundle.sut.handleUpdateAppInstanceRequest(DUMMY_ADMIN, new UpdateAppInstance.Request(
				result.instance().instanceId, "alias", this.getHeatingWithoutMeterJson()));

		assertEquals(5, this.appManagerTestBundle.sut.getInstantiatedApps().size());

	}

	private JsonObject getHeatingWithInternMeterJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("OUTPUT_CHANNEL_PHASE_L1", "io0/InputOutput0") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L2", "io0/InputOutput1") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L3", "io0/InputOutput2") //
				.addProperty("POWER_PER_PHASE", 2000) //
				.addProperty("HYSTERESIS", 60) //
				.addProperty("IS_ELEMENT_MEASURED", true) //
				.addProperty("HOW_MEASURED", "INTERN") //
				.build();
	}

	private JsonObject getHeatingWithoutMeterJson() {
		return JsonUtils.buildJsonObject() //
				.addProperty("OUTPUT_CHANNEL_PHASE_L1", "io0/InputOutput0") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L2", "io0/InputOutput1") //
				.addProperty("OUTPUT_CHANNEL_PHASE_L3", "io0/InputOutput2") //
				.addProperty("POWER_PER_PHASE", 2000) //
				.addProperty("HYSTERESIS", 60) //
				.addProperty("IS_ELEMENT_MEASURED", false) //
				.build();
	}
}
