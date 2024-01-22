package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.ComponentUtilImpl;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;

public class SchedulerAggregateTaskImplTest {

	private SchedulerAggregateTask task;
	private DummyPseudoComponentManager componentManager;
	private DummyConfigurationAdmin cm;
	private ComponentUtilImpl componentUtil;
	private ComponentAggregateTask aggregateTask;

	@Before
	public void setUp() throws Exception {
		this.componentManager = new DummyPseudoComponentManager();
		this.cm = new DummyConfigurationAdmin();
		this.componentUtil = new ComponentUtilImpl(this.componentManager, this.cm);
		this.componentManager.setConfigurationAdmin(this.cm);
		this.aggregateTask = new ComponentAggregateTaskImpl(this.componentManager);
		this.aggregateTask.reset();
		this.task = new SchedulerAggregateTaskImpl(this.aggregateTask, this.componentUtil);
		this.task.reset();

		this.componentManager.addComponent(new EdgeConfig.Component("scheduler0", "", "Scheduler.AllAlphabetically",
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.add("controllers.ids", JsonUtils.buildJsonArray() //
								.build())
						.build()));
		// create config for scheduler
		this.cm.getOrCreateEmptyConfiguration(
				this.componentManager.getEdgeConfig().getComponent("scheduler0").get().getPid());
	}

	@Test
	public void testAggregate() {
		final var config = new SchedulerConfiguration("test0", "test1");

		this.task.aggregate(config, null);
		this.task.aggregate(null, config);
		this.task.aggregate(null, null);
		this.task.aggregate(config, config);
	}

	@Test
	public void testCreate() throws Exception {
		final var config = new SchedulerConfiguration("test0", "test1");
		this.componentManager.addComponent(
				new EdgeConfig.Component("test0", "test", "Test.test", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("test1", "test", "Test.test", JsonUtils.buildJsonObject().build()));

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());
		this.assertExactSchedulerOrder("Ids in scheduler do not match!", config.componentOrder());
	}

	@Test
	public void testDelete() throws Exception {
		final var config = new SchedulerConfiguration("test0", "test1");
		this.setSchedulerIds(config.componentOrder());
		this.task.aggregate(null, config);
		this.task.delete(DUMMY_ADMIN, emptyList());
		this.assertExactSchedulerOrder("Ids in scheduler got not removed!");
	}

	@Test
	public void testGetGeneralFailMessage() {
		final var dt = TranslationUtil.enableDebugMode();

		for (var l : Language.values()) {
			this.task.getGeneralFailMessage(l);
		}
		assertTrue(dt.getMissingKeys().isEmpty());
	}

	@Test
	public void testValidate() throws Exception {
		final var config = AppConfiguration.create() //
				.addTask(Tasks.scheduler("test0", "test1")) //
				.build();
		this.setSchedulerIds(config.getConfiguration(SchedulerAggregateTask.class).componentOrder());

		final var errors = new ArrayList<String>();
		this.task.validate(errors, config, config.getConfiguration(SchedulerAggregateTask.class));
		assertTrue(errors.isEmpty());
	}

	private void assertExactSchedulerOrder(String message, List<String> orderIds) throws IOException {
		this.assertExactSchedulerOrder(message, orderIds.toArray(String[]::new));
	}

	private void assertExactSchedulerOrder(String message, String... orderIds) throws IOException {
		final var config = this.cm.getConfiguration(
				this.componentManager.getEdgeConfig().getComponent("scheduler0").get().getPid(), null);
		final var ids = (String[]) config.getProperties().get("controllers.ids");
		assertArrayEquals(message, orderIds, ids);
	}

	private void setSchedulerIds(List<String> ids)
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		this.setSchedulerIds(ids.toArray(String[]::new));
	}

	private void setSchedulerIds(String... ids) throws OpenemsNamedException, InterruptedException, ExecutionException {
		this.componentManager.handleJsonrpcRequest(DUMMY_ADMIN, new UpdateComponentConfigRequest("scheduler0", List.of(//
				new UpdateComponentConfigRequest.Property("controllers.ids", Arrays.stream(ids) //
						.map(JsonPrimitive::new) //
						.collect(toJsonArray())) //
		))).get();
	}

}
