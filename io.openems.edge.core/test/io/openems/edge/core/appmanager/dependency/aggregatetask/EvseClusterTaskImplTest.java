package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;

class EvseClusterTaskImplTest {

	private DummyPseudoComponentManager componentManager;
	private EvseClusterTaskImpl task;
	private final String ctrlId1 = "evseCtrlSingle0";
	private final String ctrlId2 = "evseCtrlSingle1";

	private void addCluster(String... evseIds) {
		this.componentManager.addComponent(new Component("evseCluster0", "test-cluster", "Evse.Controller.Cluster",
				JsonUtils.buildJsonObject()//
						.add("ctrl.ids",
								JsonUtils.generateJsonArray(List.of(evseIds),
										s -> new JsonPrimitive(Objects.requireNonNull(s)))) //
						.build()));
	}

	private List<String> getClusterIds() {
		var cluster = this.componentManager.getEdgeConfig().getComponentsByFactory("Evse.Controller.Cluster")
				.getFirst();
		return cluster.getProperty("ctrl.ids")
				.map(ids -> JsonSerializerUtil.stringSerializer().toListSerializer().deserialize(ids))
				.orElse(List.of());
	}

	@BeforeEach
	void setUp() {
		this.componentManager = new DummyPseudoComponentManager();

		this.componentManager.setConfigurationAdmin(new DummyConfigurationAdmin());
		this.task = new EvseClusterTaskImpl(this.componentManager);
		this.task.reset();
	}

	@Test
	void testAggregate() {
		final var config = new ClusterConfiguration(this.ctrlId1, this.ctrlId2);
		// should be able to handle null values
		this.task.aggregate(config, null);
		this.task.aggregate(null, config);
		this.task.aggregate(null, null);
		this.task.aggregate(config, config);
		assertEquals(JsonNull.INSTANCE, this.task.getExecutionConfiguration().toJson());
	}

	@Test
	void testCreate() throws Exception {
		final var config = new ClusterConfiguration(this.ctrlId1, this.ctrlId2);
		this.addCluster();
		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());
		List<String> errors = new ArrayList<>();
		this.task.validate(errors, null, config, emptyMap());
		assertEquals(0, errors.size());
	}

	@Test
	void testCreateFailsIfClusterControllerIsMissing() {
		final var config = new ClusterConfiguration(this.ctrlId1, this.ctrlId2);

		this.task.aggregate(config, null);

		var exception = assertThrows(io.openems.common.exceptions.OpenemsException.class,
				() -> this.task.create(DUMMY_ADMIN, emptyList()));

		assertEquals("No Cluster Controller!", exception.getMessage());
		assertEquals(0, this.componentManager.getEdgeConfig().getComponentsByFactory("Evse.Controller.Cluster").size());
	}

	@Test
	void testDelete() throws Exception {
		final var config = new ClusterConfiguration(this.ctrlId1, this.ctrlId2);
		this.addCluster(this.ctrlId1, this.ctrlId2);
		this.task.aggregate(null, config);
		this.task.delete(DUMMY_ADMIN, emptyList());
		List<String> errors = new ArrayList<>();
		this.task.validate(errors, null, new ClusterConfiguration(), emptyMap());
		assertEquals(0, errors.size());
	}

	@Test
	void testUpdateWithSameControllerIdKeepsClusterEntry() throws Exception {
		final var config = new ClusterConfiguration(this.ctrlId1);
		this.addCluster(this.ctrlId1);

		this.task.aggregate(config, config);
		this.task.create(DUMMY_ADMIN, emptyList());

		List<String> errors = new ArrayList<>();
		this.task.validate(errors, null, config, emptyMap());
		assertEquals(0, errors.size());
		assertEquals(List.of(this.ctrlId1), this.getClusterIds());
	}

	@Test
	void testDeleteAndCreateSameControllerIdNetNoChange() throws Exception {
		final var config = new ClusterConfiguration(this.ctrlId1);
		this.addCluster(this.ctrlId1);

		this.task.aggregate(null, config);
		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		List<String> errors = new ArrayList<>();
		this.task.validate(errors, null, config, emptyMap());
		assertEquals(0, errors.size());
		assertEquals(List.of(this.ctrlId1), this.getClusterIds());
	}

	@Test
	void testDuplicationProtectionSameControllerIdNetNoChange() throws Exception {
		this.addCluster(this.ctrlId1);
		final var config = new ClusterConfiguration(this.ctrlId1);
		// create both apps
		this.task.aggregate(config, null);
		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		// delete one
		this.task.aggregate(null, config);
		this.task.delete(DUMMY_ADMIN, emptyList());

		List<String> errors = new ArrayList<>();
		this.task.validate(errors, null, config, emptyMap());
		assertEquals(0, errors.size());
		assertEquals(List.of(this.ctrlId1), this.getClusterIds());
	}
}
