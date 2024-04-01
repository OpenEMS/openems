package io.openems.edge.core.appmanager.dependency.aggregatetask;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import com.google.gson.JsonPrimitive;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.request.UpdateComponentConfigRequest;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.DummyPseudoComponentManager;

public class TestScheduler {

	/**
	 * Adds a scheduler to the current components and creates a
	 * {@link TestScheduler} object.
	 * 
	 * @param componentManager the {@link DummyPseudoComponentManager} to add the
	 *                         scheduler
	 * @return the {@link TestScheduler}
	 */
	public static TestScheduler create(DummyPseudoComponentManager componentManager) {
		componentManager.addComponent(new EdgeConfig.Component("scheduler0", "", "Scheduler.AllAlphabetically",
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.add("controllers.ids", JsonUtils.buildJsonArray() //
								.build())
						.build()));
		return new TestScheduler(componentManager);
	}

	private final ComponentManager componentManager;

	public TestScheduler(ComponentManager componentManager) {
		super();
		this.componentManager = componentManager;
	}

	/**
	 * Checks if the given order of ids matches the order in the scheduler.
	 * 
	 * @param message     the identifying message for the {@link AssertionError}
	 * @param expectedIds the expected ids in the scheduler
	 * @throws OpenemsNamedException on error
	 */
	public void assertExactSchedulerOrder(String message, List<String> expectedIds) throws OpenemsNamedException {
		this.assertExactSchedulerOrder(message, expectedIds.toArray(String[]::new));
	}

	/**
	 * Checks if the given order of ids matches the order in the scheduler.
	 * 
	 * @param message  the identifying message for the {@link AssertionError}
	 * @param orderIds the ids of components in the scheduler
	 * @throws OpenemsNamedException on error
	 */
	public void assertExactSchedulerOrder(String message, String... orderIds) throws OpenemsNamedException {
		final var scheduler = this.componentManager.getComponent("scheduler0");
		final var ids = (Object[]) scheduler.getComponentContext().getProperties().get("controllers.ids");
		if (ids == null) {
			assertTrue(orderIds.length == 0);
			return;
		}
		assertArrayEquals(message + ": was [" + Stream.of(ids).map(Object::toString).collect(joining(", "))
				+ "] expected [" + String.join(", ", orderIds) + "]", orderIds, ids);
	}

	public void setSchedulerIds(User user, List<String> ids)
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		this.setSchedulerIds(user, ids.toArray(String[]::new));
	}

	public void setSchedulerIds(User user, String... ids)
			throws OpenemsNamedException, InterruptedException, ExecutionException {
		this.componentManager.handleJsonrpcRequest(user, new UpdateComponentConfigRequest("scheduler0", List.of(//
				new UpdateComponentConfigRequest.Property("controllers.ids", Arrays.stream(ids) //
						.map(JsonPrimitive::new) //
						.collect(toJsonArray())) //
		))).get();
	}

}
