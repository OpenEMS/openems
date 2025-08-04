package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonPrimitive;

import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;

public class EvseClusterTaskImplTest {

	private DummyPseudoComponentManager componentManager;
	private EvseClusterTaskImpl task;
	private final String ctrlId1 = "evseCtrlSingle0";
	private final String ctrlId2 = "evseCtrlSingle1";

	private void addCluster(String... evseIds) {
		this.componentManager.addComponent(new Component("evseCluster0", "test-cluster", "Evse.Controller.Cluster",
				JsonUtils.buildJsonObject()//
						.add("ctrl.ids",
								JsonUtils.generateJsonArray(Stream.of(evseIds).toList(), v -> new JsonPrimitive(v)))
						.build()));
	}

	@Before
	public void setUp() throws Exception {
		this.componentManager = new DummyPseudoComponentManager();

		this.componentManager.setConfigurationAdmin(new DummyConfigurationAdmin());
		this.task = new EvseClusterTaskImpl(this.componentManager);
		this.task.reset();
	}

	@Test
	public void testAggregate() {
		final var config = new ClusterConfiguration(this.ctrlId1, this.ctrlId2);
		// should be able to handle null values
		this.task.aggregate(config, null);
		this.task.aggregate(null, config);
		this.task.aggregate(null, null);
		this.task.aggregate(config, config);
	}

	@Test
	public void testCreate() throws Exception {
		final var config = new ClusterConfiguration(this.ctrlId1, this.ctrlId2);
		this.addCluster();
		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());
		List<String> errors = new ArrayList<String>();
		this.task.validate(errors, null, config);
		assertEquals(0, errors.size());
	}

	@Test
	public void testDelete() throws Exception {
		final var config = new ClusterConfiguration(this.ctrlId1, this.ctrlId2);
		this.addCluster(this.ctrlId1, this.ctrlId2);
		this.task.aggregate(null, config);
		this.task.delete(DUMMY_ADMIN, emptyList());
		List<String> errors = new ArrayList<String>();
		this.task.validate(errors, null, new ClusterConfiguration());
		assertEquals(0, errors.size());
	}
}
