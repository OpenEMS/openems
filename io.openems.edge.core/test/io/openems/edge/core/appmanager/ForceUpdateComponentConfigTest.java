package io.openems.edge.core.appmanager;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.jsonrpc.type.CreateComponentConfig;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.TestForceUpdatingConfigComponent;
import io.openems.edge.app.TestForceUpdatingConfigProperties;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.aggregatetask.DependencyProperties;
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

	@Test
	public void testForceUpdateIgnoreInitialProperties() throws Exception {
		this.testBundle = new AppManagerTestBundle(null, null, t -> {
			return ImmutableList.of(Apps.testForceUpdatingConfigProperties(t),
					this.app = Apps.testForceUpdatingConfigComponent(t));
		}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		var propertiesForFirstApp = JsonUtils.buildJsonObject() //
				.addProperty(TestForceUpdatingConfigComponent.Property.PHASE_ROTATION.name(), PhaseRotation.L2_L3_L1)
				.build();

		var propertiesForSecondApp = JsonUtils.buildJsonObject() //
				.addProperty(TestForceUpdatingConfigProperties.Property.ID.name(), "test1") //
				.addProperty(TestForceUpdatingConfigProperties.Property.PHASE_ROTATION.name(), PhaseRotation.L1_L2_L3) //
				.addProperty(TestForceUpdatingConfigProperties.Property.MIN_POWER.name(), 500) //
				.addProperty(TestForceUpdatingConfigProperties.Property.MAX_POWER.name(), 5000) //
				.build();

		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request(this.app.getAppId(), "testApp1", "testApp1", propertiesForFirstApp));

		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN, new AddAppInstance.Request(
				"App.Test.TestForceUpdatingConfigProperties", "testApp2", "testApp2", propertiesForSecondApp));

		assertEquals(2, this.testBundle.sut.getInstantiatedApps().size());

		ForceUpdateComponentConfig.checkForceUpdating(//
				this.testBundle.sut, //
				this.testBundle.appManagerUtil, //
				this.testBundle.componentManger //
		);

		var dependencyInstance = this.testBundle.sut.getInstantiatedApps().stream()
				.filter(app -> app.appId.equals("App.Test.TestForceUpdatingConfigProperties")).findFirst();

		assertTrue(dependencyInstance.isPresent());

		assertEquals(PhaseRotation.L1_L2_L3.name(),
				dependencyInstance.get().properties.get("PHASE_ROTATION").getAsString());
		assertEquals(500, dependencyInstance.get().properties.get("MIN_POWER").getAsInt());
		assertEquals(5000, dependencyInstance.get().properties.get("MAX_POWER").getAsInt());
	}

	@Test
	public void testForceUpdateDependencyProperties() throws Exception {
		this.testBundle = new AppManagerTestBundle(null, MyConfig.create() //
				.setApps("""
						[
						    {
						        "appId": "App.Test.DummyWithForceDependency",
						        "alias": "",
						        "instanceId": "77e0ac49-e12e-46e2-83cb-8ac25e9c6579",
						        "properties": { },
						        "dependencies": [
						            {
						                "key": "DEPENDENCY",
						                "instanceId": "07a74c08-806f-457c-8efe-bc32b8db672b"
						            }
						        ]
						    },
						    {
						        "appId": "App.Test.TestForceUpdatingConfigProperties",
						        "alias": "",
						        "instanceId": "07a74c08-806f-457c-8efe-bc32b8db672b",
						        "properties": {
						            "MIN_POWER": 500
						        }
						    }
						]
						""".stripIndent()).build(), t -> {
					return ImmutableList.of(Apps.testForceUpdatingConfigProperties(t), this.app = DummyApp.create() //
							.setAppId("App.Test.DummyWithForceDependency") //
							.setConfiguration((configurationTarget, jsonObject, language) -> {
								return AppConfiguration.create() //
										.addDependency(new DependencyDeclaration("DEPENDENCY",
												DependencyDeclaration.CreatePolicy.NEVER,
												DependencyDeclaration.UpdatePolicy.ALWAYS,
												DependencyDeclaration.DeletePolicy.NEVER,
												DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES,
												DependencyDeclaration.DependencyDeletePolicy.NOT_ALLOWED,
												DependencyDeclaration.AppDependencyConfig.create() //
														.setAppId("App.Test.TestForceUpdatingConfigProperties") //
														.setProperties(DependencyProperties
																.fromJson(JsonUtils.buildJsonObject() //
																		.addProperty("MIN_POWER", 1000) //
																		.build(), "MIN_POWER"))
														.build()))
										.build();
							}) //
							.build());
				}, null, new AppManagerTestBundle.PseudoComponentManagerFactory());

		var dependencyInstance = this.testBundle.sut.getInstantiatedApps().stream()
				.filter(app -> app.appId.equals("App.Test.TestForceUpdatingConfigProperties")).findFirst();
		assertTrue(dependencyInstance.isPresent());
		assertEquals(500, dependencyInstance.get().properties.get("MIN_POWER").getAsInt());

		ForceUpdateComponentConfig.checkForceUpdating(//
				this.testBundle.sut, //
				this.testBundle.appManagerUtil, //
				this.testBundle.componentManger //
		);

		dependencyInstance = this.testBundle.sut.getInstantiatedApps().stream()
				.filter(app -> app.appId.equals("App.Test.TestForceUpdatingConfigProperties")).findFirst();
		assertTrue(dependencyInstance.isPresent());
		assertEquals(1000, dependencyInstance.get().properties.get("MIN_POWER").getAsInt());
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
