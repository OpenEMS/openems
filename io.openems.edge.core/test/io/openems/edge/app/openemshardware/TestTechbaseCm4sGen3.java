package io.openems.edge.app.openemshardware;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.hardware.MasterBox2v0;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.Apps;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

class TestTechbaseCm4sGen3 {

	private AppManagerTestBundle appManagerTestBundle;
	private TechbaseCm4sGen3 techbaseCm4sGen3;
	private MasterBox2v0 masterBox2v0;

	@BeforeEach
	void beforeEach() throws Exception {
		this.appManagerTestBundle = new AppManagerTestBundle(null, null, t -> {
			return List.of(//
					this.techbaseCm4sGen3 = Apps.techbaseCm4sGen3(t), //
					this.masterBox2v0 = Apps.masterBox2v0(t) //
			);
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		final var componentTask = this.appManagerTestBundle.addComponentAggregateTask();
		this.appManagerTestBundle.addSchedulerByCentralOrderAggregateTask(componentTask);
		this.appManagerTestBundle.addPersistencePredictorAggregateTask();
	}

	@Test
	void testMasterBoxDependency() throws Exception {
		this.appManagerTestBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				this.techbaseCm4sGen3.getAppId(), "key", "alias", JsonUtils.buildJsonObject().build()));

		assertEquals(2, this.appManagerTestBundle.sut.getInstantiatedApps().size());
		assertNotNull(this.appManagerTestBundle.sut.getInstantiatedApps().stream()
				.filter(app -> app.appId.equals(this.masterBox2v0.getAppId())).findFirst().orElse(null));
		var componentIdsOfMasterBox = List.of("ioc0", "meter1", "io0", "analogOutput0");
		var allComponentIds = this.appManagerTestBundle.componentManger.getAllComponents().stream() //
				.map(OpenemsComponent::id) //
				.toList();

		for (var expectedId : componentIdsOfMasterBox) {
			assertTrue(allComponentIds.contains(expectedId));
		}
	}

}
