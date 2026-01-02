package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.TestForceUpdatingConfigProperties;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;
import io.openems.edge.meter.api.PhaseRotation;

public class ForceUpdateComponentConfigTest {

	private AppManagerTestBundle testBundle;
	private OpenemsApp app;

	@Test
	public void testForceUpdateComponent() throws Exception {
		this.testUpdatingOrCreatingComponent(true);
	}

	@Test
	public void testForceCreatingComponent() throws Exception {
		this.testUpdatingOrCreatingComponent(false);
	}

	@Test
	public void testForceUpdateProperties() throws Exception {
		var component = this.getComponentForUpdatingProperties(true);
		assertNotNull(component);
		assertEquals(0, component.getProperties().get("minPower").getAsInt());
		assertEquals(2000, component.getProperties().get("maxPower").getAsInt());
		assertEquals(PhaseRotation.L1_L2_L3.name(), component.getProperties().get("phaseRotation").getAsString());
	}

	@Test
	public void testForceUpdatePropertiesWithoutComponentExisting() throws Exception {
		var component = this.getComponentForUpdatingProperties(false);
		assertNull(component);
	}

	private void testUpdatingOrCreatingComponent(boolean existsComponent) throws Exception {
		this.testBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(this.app = Apps.testForceUpdatingConfigComponent(t));
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		var properties = JsonUtils.buildJsonObject() //
				.addProperty(TestForceUpdatingConfigProperties.Property.PHASE_ROTATION.name(), PhaseRotation.L2_L3_L1)
				.build();

		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.app.getAppId(), "key", "alias", properties));

		assertEquals(1, this.testBundle.sut.getInstantiatedApps().size());

		if (existsComponent) {
			var componentProperties = List.of(//
					new UpdateComponentConfigRequest.Property("id", "test0"), //
					new UpdateComponentConfigRequest.Property("phaseRotation", PhaseRotation.L1_L2_L3.name()) //
			);

			this.testBundle.componentManger.handleCreateComponentConfigRequest(DUMMY_ADMIN,
					new CreateComponentConfig.Request("Test.Force.Updating.Config", componentProperties));
		}

		ForceUpdateComponentConfig.checkForceUpdating(//
				this.testBundle.sut, //
				this.testBundle.appManagerUtil, //
				this.testBundle.componentManger //
		);

		var component = this.testBundle.componentManger.getEdgeConfig().getComponent("test0").orElse(null);
		assertNotNull(component);
		assertEquals(PhaseRotation.L2_L3_L1.name(), component.getProperties().get("phaseRotation").getAsString());
	}

	private EdgeConfig.Component getComponentForUpdatingProperties(boolean existsComponent) throws Exception {
		this.testBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(this.app = Apps.testForceUpdatingConfigProperties(t));
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		var properties = JsonUtils.buildJsonObject() //
				.addProperty(TestForceUpdatingConfigProperties.Property.MIN_POWER.name(), 0)
				.addProperty(TestForceUpdatingConfigProperties.Property.MAX_POWER.name(), 2000)
				.addProperty(TestForceUpdatingConfigProperties.Property.PHASE_ROTATION.name(), PhaseRotation.L2_L3_L1)
				.build();

		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.app.getAppId(), "key", "alias", properties));

		assertEquals(1, this.testBundle.sut.getInstantiatedApps().size());

		if (existsComponent) {
			var componentProperties = List.of(//
					new UpdateComponentConfigRequest.Property("id", "test0"), //
					new UpdateComponentConfigRequest.Property("minPower", -1000), //
					new UpdateComponentConfigRequest.Property("maxPower", 1000), //
					new UpdateComponentConfigRequest.Property("phaseRotation", PhaseRotation.L1_L2_L3.name()) //
			);

			this.testBundle.componentManger.handleCreateComponentConfigRequest(DUMMY_ADMIN,
					new CreateComponentConfig.Request("Test.Force.Updating.Config", componentProperties));
		}

		ForceUpdateComponentConfig.checkForceUpdating(//
				this.testBundle.sut, //
				this.testBundle.appManagerUtil, //
				this.testBundle.componentManger //
		);

		return this.testBundle.componentManger.getEdgeConfig().getComponent("test0").orElse(null);
	}
}
