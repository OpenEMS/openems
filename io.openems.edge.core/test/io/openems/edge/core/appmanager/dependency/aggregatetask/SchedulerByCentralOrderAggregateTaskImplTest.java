package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppManagerTestBundle;
import io.openems.edge.core.appmanager.AppManagerTestBundle.PseudoComponentManagerFactory;
import io.openems.edge.core.appmanager.DummyApp;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderAggregateTaskImpl.SchedulerOrderDefinition;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.jsonrpc.AddAppInstance;

public class SchedulerByCentralOrderAggregateTaskImplTest {

	private SchedulerByCentralOrderAggregateTask task;

	private AppManagerTestBundle testBundle;
	private DummyPseudoComponentManager componentManager;
	private ComponentAggregateTask componentTask;

	@Before
	public void setUp() throws Exception {
		final var componentManagerFactory = new PseudoComponentManagerFactory();
		this.testBundle = new AppManagerTestBundle(null, null, tb -> {
			return ImmutableList.of(DummyApp.create() //
					.setAppId("appId") //
					.setConfiguration((t, u, s) -> AppConfiguration.create() //
							.addTask(Tasks.component(new EdgeConfig.Component("id0", "alias", "factoryId",
									JsonUtils.buildJsonObject().build()))) //
							.addTask(Tasks.schedulerByCentralOrder(new SchedulerComponent("id0", "factoryId", "appId"))) //
							.build())
					.build(),
					DummyApp.create() //
							.setAppId("appId2") //
							.setConfiguration((t, u, s) -> AppConfiguration.create() //
									.addTask(Tasks.component(new EdgeConfig.Component("id1", "alias", "factoryId1",
											JsonUtils.buildJsonObject().build()))) //
									.addTask(Tasks.schedulerByCentralOrder(
											new SchedulerComponent("id1", "factoryId1", "appId2"))) //
									.build())
							.build());
		}, null, componentManagerFactory);
		this.componentManager = componentManagerFactory.getComponentManager();

		this.componentTask = this.testBundle.addComponentAggregateTask();
		this.componentTask.reset();
		this.task = new SchedulerByCentralOrderAggregateTaskImpl(//
				this.testBundle.componentManger, //
				this.testBundle.componentUtil, //
				this.testBundle.appManagerUtil, //
				this.componentTask, //
				new SchedulerOrderDefinition() //
						.thenByFactoryId("factoryId") //
						.thenByFactoryId("factoryId1") //
						.thenByFactoryId("factoryId2") //
						.thenByFactoryId("factoryId3") //
		);
		this.task.reset();
		this.testBundle.appHelper.addAggregateTask(this.task);
	}

	@Test
	public void testAggregate() {
		final var config = new SchedulerByCentralOrderConfiguration();

		this.task.aggregate(null, null);
		this.task.aggregate(config, null);
		this.task.aggregate(null, config);
		this.task.aggregate(config, config);
	}

	@Test
	public void testCreate() throws Exception {
		this.componentManager.addComponent(
				new EdgeConfig.Component("id0", "alias", "factoryId1", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id1", "alias", "factoryId2", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id2", "alias", "factoryId3", JsonUtils.buildJsonObject().build()));

		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id0", "factoryId1", "appId"), //
				new SchedulerComponent("id2", "factoryId3", "appId"), //
				new SchedulerComponent("id1", "factoryId2", "appId") //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		this.testBundle.scheduler.assertExactSchedulerOrder("Ids got not added in Scheduler", "id0", "id1", "id2");
	}

	@Test
	public void testCreateInsertIntoExistingTop() throws Exception {
		this.componentManager.addComponent(
				new EdgeConfig.Component("id0", "alias", "factoryId1", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id1", "alias", "factoryId2", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id2", "alias", "factoryId3", JsonUtils.buildJsonObject().build()));

		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id1", "id2");

		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id0", "factoryId1", "appId") //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		this.testBundle.scheduler.assertExactSchedulerOrder("Ids got not added in Scheduler", "id0", "id1", "id2");
	}

	@Test
	public void testCreateInsertIntoExistingCenter() throws Exception {
		this.componentManager.addComponent(
				new EdgeConfig.Component("id0", "alias", "factoryId1", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id1", "alias", "factoryId2", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id2", "alias", "factoryId3", JsonUtils.buildJsonObject().build()));

		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id0", "id2");

		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id1", "factoryId2", "appId") //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		this.testBundle.scheduler.assertExactSchedulerOrder("Ids got not added in Scheduler", "id0", "id1", "id2");
	}

	@Test
	public void testCreateInsertIntoExistingBottom() throws Exception {
		this.componentManager.addComponent(
				new EdgeConfig.Component("id0", "alias", "factoryId1", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id1", "alias", "factoryId2", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id2", "alias", "factoryId3", JsonUtils.buildJsonObject().build()));

		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id0", "id1");

		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id2", "factoryId3", "appId") //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		this.testBundle.scheduler.assertExactSchedulerOrder("Ids got not added in Scheduler", "id0", "id1", "id2");
	}

	@Test
	public void testCreateInsertIntoExistingWithUndefinedIds() throws Exception {
		this.componentManager.addComponent(
				new EdgeConfig.Component("id0", "alias", "factoryId1", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id1", "alias", "undefinedFactoryId", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id2", "alias", "factoryId2", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id3", "alias", "factoryId3", JsonUtils.buildJsonObject().build()));

		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id0", "id1", "id3");

		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id2", "factoryId2", "appId") //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		this.testBundle.scheduler.assertExactSchedulerOrder("Ids got not added in Scheduler", "id0", "id1", "id2",
				"id3");
	}

	@Test
	public void testCreateInsertIntoExistingWithNotExistingComponent() throws Exception {
		this.componentManager.addComponent(
				new EdgeConfig.Component("id0", "alias", "factoryId1", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id2", "alias", "factoryId2", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id3", "alias", "factoryId3", JsonUtils.buildJsonObject().build()));

		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id0", "id1", "id3");

		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id2", "factoryId2", "appId") //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		this.testBundle.scheduler.assertExactSchedulerOrder("Ids got not added in Scheduler", "id0", "id1", "id2",
				"id3");
	}

	@Test
	public void testCreateRescheduleWronlyConfigured() throws Exception {
		this.componentManager.addComponent(
				new EdgeConfig.Component("id0", "alias", "factoryId1", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id1", "alias", "factoryId2", JsonUtils.buildJsonObject().build()));
		this.componentManager.addComponent(
				new EdgeConfig.Component("id2", "alias", "factoryId3", JsonUtils.buildJsonObject().build()));

		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id2", "id0", "id1");

		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id2", "factoryId3", "appId") //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		this.testBundle.scheduler.assertExactSchedulerOrder("Ids got not added in Scheduler", "id0", "id1", "id2");
	}

	@Test
	public void testDelete() throws Exception {
		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id0", "factoryId1", "appId") //
		);
		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id0");
		this.task.aggregate(null, config);
		this.task.delete(DUMMY_ADMIN, emptyList());
		this.testBundle.scheduler.assertExactSchedulerOrder("Ids in scheduler got not removed!");
	}

	@Test
	public void testDeleteOfStillUsedId() throws Exception {
		final var config = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id0", "factoryId1", "appId") //
		);
		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id0");
		this.task.aggregate(null, config);
		this.task.delete(DUMMY_ADMIN, List.of(AppConfiguration.create() //
				.addTask(Tasks.schedulerByCentralOrder(new SchedulerComponent("id0", "factoryId1", "otherAppId"))) //
				.build()));
		this.testBundle.scheduler.assertExactSchedulerOrder("Ids in scheduler got not removed!", "id0");
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
		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("appId", "key", "alias", JsonUtils.buildJsonObject().build()));

		final var schedulerConfig = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id0", "factoryId", "appId") //
		);
		final var config = AppConfiguration.create() //
				.addTask(Tasks.schedulerByCentralOrder(schedulerConfig.componentOrder())) //
				.build();

		final var errors = new ArrayList<String>();
		this.task.validate(errors, config, schedulerConfig);

		assertTrue("Validation should be successful but got: " + String.join(", ", errors), errors.isEmpty());
	}

	@Test
	public void testValidateMissingIds() throws Exception {
		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("appId", "key", "alias", JsonUtils.buildJsonObject().build()));

		// remove ids from scheduler
		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN);

		final var schedulerConfig = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id0", "factoryId", "appId") //
		);
		final var config = AppConfiguration.create() //
				.addTask(Tasks.schedulerByCentralOrder(schedulerConfig.componentOrder())) //
				.build();

		final var errors = new ArrayList<String>();
		this.task.validate(errors, config, schedulerConfig);

		assertFalse("Validation should not be successful but got: " + String.join(", ", errors), errors.isEmpty());
	}

	@Test
	public void testValidateWronglyConfiguredIds() throws Exception {
		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("appId", "key", "alias", JsonUtils.buildJsonObject().build()));
		this.testBundle.sut.handleAddAppInstanceRequest(DUMMY_ADMIN,
				new AddAppInstance.Request("appId2", "key", "alias", JsonUtils.buildJsonObject().build()));

		this.testBundle.scheduler.setSchedulerIds(DUMMY_ADMIN, "id1", "id0");

		final var schedulerConfig = new SchedulerByCentralOrderConfiguration(//
				new SchedulerComponent("id0", "factoryId", "appId") //
		);
		final var config = AppConfiguration.create() //
				.addTask(Tasks.schedulerByCentralOrder(schedulerConfig.componentOrder())) //
				.build();

		final var errors = new ArrayList<String>();
		this.task.validate(errors, config, schedulerConfig);

		assertFalse("Validation should not be successful but got: " + String.join(", ", errors), errors.isEmpty());
	}

}
