package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.edge.common.test.DummyUser.DUMMY_ADMIN;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;

public class ComponentAggregateTaskImplTest {

	private ComponentAggregateTask task;
	private DummyPseudoComponentManager componentManager;

	@Before
	public void setUp() throws Exception {
		this.componentManager = new DummyPseudoComponentManager();
		this.componentManager.setConfigurationAdmin(new DummyConfigurationAdmin());
		this.task = new ComponentAggregateTaskImpl(this.componentManager);
		this.task.reset();
	}

	@Test
	public void testAggregate() {
		final var config = new ComponentConfiguration(//
				new EdgeConfig.Component("test0", "test", "Test.test", JsonUtils.buildJsonObject() //
						.build()) //
		);

		// should be able to handle null values
		this.task.aggregate(config, null);
		this.task.aggregate(null, config);
		this.task.aggregate(null, null);
		this.task.aggregate(config, config);
	}

	@Test
	public void testCreate() throws Exception {
		final var dummyComponentId = "test0";
		final var config = new ComponentConfiguration(//
				new EdgeConfig.Component(dummyComponentId, "test", "Test.test", JsonUtils.buildJsonObject() //
						.build()) //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());
		final var component = this.componentManager.getComponent(dummyComponentId);
		assertNotNull(component);
	}

	@Test
	public void testCreateWithExistingComponentNotFromOtherAppSameConfig() throws Exception {
		final var config = new ComponentConfiguration(//
				new EdgeConfig.Component("test0", "test", "Test.test", JsonUtils.buildJsonObject() //
						.build()) //
		);
		assertEquals(0, this.componentManager.getAllComponents().size());

		// create component
		this.componentManager.addComponent(config.components().get(0));
		assertEquals(1, this.componentManager.getAllComponents().size());

		// not failing even if component already exist
		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		// not creating the same component twice
		assertEquals(1, this.componentManager.getAllComponents().size());
	}

	@Test
	public void testCreateWithExistingComponentNotFromOtherAppDifferentConfig() throws Exception {
		final var dummyComponentId = "test0";
		final var config = new ComponentConfiguration(//
				new EdgeConfig.Component(dummyComponentId, "test", "Test.test", JsonUtils.buildJsonObject() //
						.addProperty("testProperty", "test_updated_value") //
						.build()) //
		);
		assertEquals(0, this.componentManager.getAllComponents().size());

		// create component
		this.componentManager.addComponent(new EdgeConfig.Component(dummyComponentId, "test", "Test.test",
				JsonUtils.buildJsonObject() //
						.addProperty("testProperty", "test_first_value") //
						.build()));
		assertEquals(1, this.componentManager.getAllComponents().size());

		// not failing even if component already exist
		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		// not creating the same component twice
		assertEquals(1, this.componentManager.getAllComponents().size());
		final var component = this.componentManager.getComponent(dummyComponentId);
		assertEquals("test_updated_value", component.getComponentContext().getProperties().get("testProperty"));
	}

	@Test
	public void testCreateWithExistingComponentFromOtherAppSameConfig() throws Exception {
		final var config = new ComponentConfiguration(//
				new EdgeConfig.Component("test0", "test", "Test.test", JsonUtils.buildJsonObject() //
						.build()) //
		);
		assertEquals(0, this.componentManager.getAllComponents().size());

		// creating components of 1st config
		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());
		assertEquals(1, this.componentManager.getAllComponents().size());

		this.task.reset();

		// creating components of 2nd config with same properties
		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, List.of(AppConfiguration.create() //
				.addTask(Tasks.component(config.components())) //
				.build()));

		// not creating the same component twice
		assertEquals(1, this.componentManager.getAllComponents().size());
	}

	@Test(expected = OpenemsNamedException.class)
	public void testCreateWithExistingComponentFromOtherAppDifferentConfig() throws Exception {
		final var dummyComponentId = "test0";
		final var config = AppConfiguration.create() //
				.addTask(Tasks.component(new EdgeConfig.Component(dummyComponentId, "test", "Test.test",
						JsonUtils.buildJsonObject() //
								.addProperty("testProperty", "test_first_value") //
								.build())))
				.build();
		assertEquals(0, this.componentManager.getAllComponents().size());

		// creating components of 1st config
		this.task.aggregate(config.getConfiguration(ComponentAggregateTask.class), null);
		this.task.create(DUMMY_ADMIN, emptyList());
		assertEquals(1, this.componentManager.getAllComponents().size());

		// creating components of 2nd config with different properties
		this.task.aggregate(new ComponentConfiguration(//
				new EdgeConfig.Component(dummyComponentId, "test", "Test.test", JsonUtils.buildJsonObject() //
						.addProperty("testProperty", "test_updated_value") //
						.build()) //
		), null);
		this.task.create(DUMMY_ADMIN, List.of(config));
	}

	@Test(expected = OpenemsNamedException.class)
	public void testDelete() throws Exception {
		final var dummyComponentId = "test0";
		try {
			final var config = new ComponentConfiguration(//
					new EdgeConfig.Component(dummyComponentId, "test", "Test.test", JsonUtils.buildJsonObject() //
							.build()) //
			);

			this.componentManager.addComponent(config.components().get(0));

			this.task.aggregate(null, config);
			this.task.delete(DUMMY_ADMIN, emptyList());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		this.componentManager.getComponent(dummyComponentId);
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
				.addTask(Tasks
						.component(new EdgeConfig.Component("test0", "test", "Test.test", JsonUtils.buildJsonObject() //
								.build())))
				.build();

		this.task.aggregate(config.getConfiguration(ComponentAggregateTask.class), null);
		this.task.create(DUMMY_ADMIN, emptyList());

		final var errors = new ArrayList<String>();
		this.task.validate(errors, config, config.getConfiguration(ComponentAggregateTask.class));
		assertTrue(String.join(", ", errors), errors.isEmpty());
	}

	@Test
	public void testValidateDetectMissingComponent() throws Exception {
		final var config = AppConfiguration.create() //
				.addTask(Tasks
						.component(new EdgeConfig.Component("test0", "test", "Test.test", JsonUtils.buildJsonObject() //
								.build())))
				.build();

		final var errors = new ArrayList<String>();
		this.task.validate(errors, config, config.getConfiguration(ComponentAggregateTask.class));
		assertFalse("No errors while validating configuration", errors.isEmpty());
	}

	@Test
	public void testGetCreatedComponents() throws Exception {
		assertTrue(this.task.getCreatedComponents().isEmpty());
		final var dummyComponentId = "test0";
		final var config = new ComponentConfiguration(//
				new EdgeConfig.Component(dummyComponentId, "test", "Test.test", JsonUtils.buildJsonObject() //
						.build()) //
		);

		this.task.aggregate(config, null);
		this.task.create(DUMMY_ADMIN, emptyList());

		assertFalse(this.task.getCreatedComponents().isEmpty());
		assertEquals(1, this.task.getCreatedComponents().size());
		assertEquals(dummyComponentId, this.task.getCreatedComponents().get(0).getId());
	}

	@Test
	public void testGetDeletedComponents() throws Exception {
		assertTrue(this.task.getDeletedComponents().isEmpty());
		final var dummyComponentId = "test0";
		final var config = new ComponentConfiguration(//
				new EdgeConfig.Component(dummyComponentId, "test", "Test.test", JsonUtils.buildJsonObject() //
						.build()) //
		);

		this.componentManager.addComponent(config.components().get(0));

		this.task.aggregate(null, config);
		this.task.delete(DUMMY_ADMIN, emptyList());

		assertFalse(this.task.getDeletedComponents().isEmpty());
		assertEquals(1, this.task.getDeletedComponents().size());
		assertEquals(dummyComponentId, this.task.getDeletedComponents().get(0));
	}

}
