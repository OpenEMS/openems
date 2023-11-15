package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.DummyUser;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.Tasks;

public class PersistencePredictorAggregateTaskImplTest {

	private final User user = new DummyUser("1", "password", Language.DEFAULT, Role.ADMIN);

	private PersistencePredictorAggregateTask task;

	private DummyPseudoComponentManager componentManager;

	@Before
	public void setUp() throws Exception {
		this.componentManager = new DummyPseudoComponentManager();
		this.task = new PersistencePredictorAggregateTaskImpl(this.componentManager);
		this.task.reset();
	}

	@Test
	public void testAggregate() {
		final var config = new PersistencePredictorConfiguration("test/Test");
		this.task.aggregate(null, null);
		this.task.aggregate(config, null);
		this.task.aggregate(null, config);
		this.task.aggregate(config, config);
	}

	@Test(expected = OpenemsNamedException.class)
	public void testCreateWithoutPredictor() throws Exception {
		final var config = new PersistencePredictorConfiguration("test/Test");
		this.task.aggregate(config, null);
		this.task.create(this.user, emptyList());
	}

	@Test
	public void testCreate() throws Exception {
		this.createPredictor();
		final var config = new PersistencePredictorConfiguration("test/Test");
		this.task.aggregate(config, null);
		this.task.create(this.user, emptyList());
		this.assertChannelsInPredictor("test/Test");
	}

	@Test
	public void testDelete() throws Exception {
		this.createPredictor("test/Test");
		final var config = new PersistencePredictorConfiguration("test/Test");
		this.task.aggregate(null, config);
		this.task.create(this.user, emptyList());
		this.assertChannelsNotInPredictor("test/Test");
	}

	@Test
	public void testDeleteWithUsageOfOtherConfig() throws Exception {
		this.createPredictor("test/Test");
		final var config = new PersistencePredictorConfiguration("test/Test");
		this.task.aggregate(null, config);
		this.task.create(this.user, emptyList());
		this.assertChannelsNotInPredictor("test/Test");
	}

	@Test
	public void testDeleteWithoutPredictor() throws Exception {
		final var config = new PersistencePredictorConfiguration("test/Test");
		this.task.aggregate(null, config);
		this.task.create(this.user, emptyList());
	}

	@Test
	public void testValidate() {
		this.createPredictor("test/Test");

		final var config = AppConfiguration.create() //
				.addTask(Tasks.persistencePredictor("test/Test")) //
				.build();

		final var errors = new ArrayList<String>();
		this.task.validate(errors, config, config.getConfiguration(PersistencePredictorAggregateTask.class));
		assertTrue("Validation has error but all channels got added.", errors.isEmpty());
	}

	@Test
	public void testValidateMissingChannel() {
		this.createPredictor();

		final var config = AppConfiguration.create() //
				.addTask(Tasks.persistencePredictor("test/Test")) //
				.build();

		final var errors = new ArrayList<String>();
		this.task.validate(errors, config, config.getConfiguration(PersistencePredictorAggregateTask.class));
		assertFalse("Validation has no error but channels are missing.", errors.isEmpty());
	}

	@Test
	public void testGetGeneralFailMessage() {
		final var dt = TranslationUtil.enableDebugMode();

		for (var l : Language.values()) {
			this.task.getGeneralFailMessage(l);
		}
		assertTrue(dt.getMissingKeys().isEmpty());
	}

	private void createPredictor(String... channels) {
		this.componentManager.addComponent(new EdgeConfig.Component("predictor0", "Predictor",
				"Predictor.PersistenceModel", JsonUtils.buildJsonObject() //
						.add("channelAddresses", Stream.of(channels) //
								.map(JsonPrimitive::new) //
								.collect(toJsonArray()))
						.build()));
	}

	private void assertChannelsInPredictor(String... channels) throws OpenemsNamedException {
		final var existingAddresses = this.getChannelsInPredictor();
		final var expectedChannels = Stream.of(channels).collect(toSet());
		expectedChannels.removeAll(existingAddresses);
		assertTrue("Missing channels [" + String.join(", ", existingAddresses) + "]", expectedChannels.isEmpty());
	}

	private void assertChannelsNotInPredictor(String... channels) throws OpenemsNamedException {
		final var existingAddresses = this.getChannelsInPredictor();
		final var expectedChannels = Stream.of(channels).collect(toSet());
		assertFalse("Missing channels [" + String.join(", ", existingAddresses) + "]",
				expectedChannels.removeAll(existingAddresses));
	}

	private Set<String> getChannelsInPredictor() throws OpenemsNamedException {
		final var predictor = this.componentManager.getComponent("predictor0");
		final var existingAddressesArray = (Object[]) predictor.getComponentContext().getProperties()
				.get("channelAddresses");
		return Optional.ofNullable(existingAddressesArray) //
				.map(Stream::of) //
				.map(t -> t.map(String.class::cast)) //
				.orElse(Stream.empty()).collect(toSet());
	}

}
