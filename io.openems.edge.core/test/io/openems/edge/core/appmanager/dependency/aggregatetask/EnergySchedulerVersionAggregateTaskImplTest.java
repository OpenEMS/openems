package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.EnumSet;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.openems.common.session.Language;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.energy.api.EnergyScheduler;
import io.openems.edge.energy.api.Version;

class EnergySchedulerVersionAggregateTaskImplTest {

	private DummyPseudoComponentManager componentManager;
	private EnergySchedulerVersionAggregateTaskImpl task;

	@BeforeEach
	void setUp() {
		this.componentManager = new DummyPseudoComponentManager();
		this.componentManager.setConfigurationAdmin(new DummyConfigurationAdmin());
		this.componentManager.addComponent(new EdgeConfig.Component(//
				EnergyScheduler.SINGLETON_COMPONENT_ID, //
				"Energy Scheduler", //
				EnergyScheduler.SINGLETON_SERVICE_PID, //
				JsonUtils.buildJsonObject() //
						.addProperty("version", Version.V1_ESS_ONLY.name()) //
						.build()));
		this.task = new EnergySchedulerVersionAggregateTaskImpl(this.componentManager);
		this.task.reset();
	}

	@Test
	void testCreateUpdatesEnergySchedulerVersionToV2() throws Exception {
		this.task.aggregate(new EnergySchedulerVersionConfiguration(Version.V2_ENERGY_SCHEDULABLE), null);
		this.task.create(DUMMY_ADMIN, emptyList());

		assertEquals(Version.V2_ENERGY_SCHEDULABLE.name(), this.getEnergySchedulerVersion());
	}

	@Test
	void testAggregateKeepsHighestVersion() throws Exception {
		this.task.aggregate(new EnergySchedulerVersionConfiguration(Version.V1_ESS_ONLY), null);
		this.task.aggregate(new EnergySchedulerVersionConfiguration(Version.V2_ENERGY_SCHEDULABLE), null);
		this.task.create(DUMMY_ADMIN, emptyList());

		assertEquals(Version.V2_ENERGY_SCHEDULABLE.name(), this.getEnergySchedulerVersion());
	}

	@Test
	void testGetExecutionConfiguration() {
		assertTrue(this.task.getExecutionConfiguration().toJson().isJsonNull());

		this.task.aggregate(new EnergySchedulerVersionConfiguration(Version.V1_ESS_ONLY), null);
		final var executionConfiguration = this.task.getExecutionConfiguration().toJson();
		assertTrue(executionConfiguration.isJsonObject());

		assertEquals(Version.V1_ESS_ONLY.name(),
				executionConfiguration.getAsJsonObject().get("targetVersion").getAsString());
	}

	@Test
	void testGetHighestRequiredVersionUsesOwnAndOtherConfigurations() {
		final var requiredVersions = EnumSet.of(Version.V1_ESS_ONLY);
		final var otherConfigurations = List.of(
				AppConfiguration.create().addTask(Tasks.energySchedulerVersion(Version.V2_ENERGY_SCHEDULABLE)).build());

		final var highestRequiredVersion = EnergySchedulerVersionAggregateTaskImpl
				.getHighestRequiredVersion(requiredVersions, otherConfigurations);

		assertEquals(Version.V2_ENERGY_SCHEDULABLE, highestRequiredVersion);
	}

	@Test
	void testGetHighestRequiredVersionReturnsNullIfNoVersionsAreRequired() {
		final var highestRequiredVersion = EnergySchedulerVersionAggregateTaskImpl
				.getHighestRequiredVersion(EnumSet.noneOf(Version.class), emptyList());

		assertNull(highestRequiredVersion);
	}

	@Test
	void testGetGeneralFailMessage() {
		final var dt = TranslationUtil.enableDebugMode();

		for (var l : Language.values()) {
			this.task.getGeneralFailMessage(l);
		}
		assertTrue(dt.getMissingKeys().isEmpty());
	}

	private String getEnergySchedulerVersion() {
		return this.componentManager.getEdgeConfig() //
				.getComponent(EnergyScheduler.SINGLETON_COMPONENT_ID).orElseThrow() //
				.getProperties().get("version").getAsString();
	}
}
