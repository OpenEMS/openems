package io.openems.edge.common.test;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.GetEdgeConfigRequest;
import io.openems.common.jsonrpc.response.GetEdgeConfigResponse;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;

/**
 * Simulates a ComponentManager for the OpenEMS Component test framework.
 */
public class DummyComponentManager implements ComponentManager {

	private final List<OpenemsComponent> components = new ArrayList<>();
	private final Clock clock;
	private JsonObject edgeConfigJson;

	public DummyComponentManager() {
		this(Clock.systemDefaultZone());
	}

	public DummyComponentManager(Clock clock) {
		this.clock = clock;
	}

	@Override
	public List<OpenemsComponent> getEnabledComponents() {
		return Collections.unmodifiableList(this.components);
	}

	@Override
	public List<OpenemsComponent> getAllComponents() {
		return Collections.unmodifiableList(this.components);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends OpenemsComponent> List<T> getEnabledComponentsOfType(Class<T> clazz) {
		List<T> result = new ArrayList<>();
		for (OpenemsComponent component : this.components) {
			if (component.getClass().isInstance(clazz)) {
				result.add((T) component);
			}
		}
		return result;
	}

	/**
	 * Specific for this Dummy implementation.
	 *
	 * @param component
	 */
	public DummyComponentManager addComponent(OpenemsComponent component) {
		if (component != this) {
			this.components.add(component);
		}
		return this;
	}

	/**
	 * Sets a {@link EdgeConfig} json.
	 *
	 * @param the {@link EdgeConfig} json
	 */
	public void setConfigJson(JsonObject json) {
		this.edgeConfigJson = json;
	}

	@Override
	public EdgeConfig getEdgeConfig() {
		if (this.edgeConfigJson == null) {
			return new EdgeConfig();
		}
		try {
			return EdgeConfig.fromJson(this.edgeConfigJson);
		} catch (OpenemsNamedException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage());
		}
	}

	@Override
	public String id() {
		return ComponentManager.SINGLETON_COMPONENT_ID;
	}

	@Override
	public String alias() {
		return ComponentManager.SINGLETON_COMPONENT_ID;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}

	@Override
	public ComponentContext getComponentContext() {
		return null;
	}

	@Deprecated()
	@Override
	public Channel<?> _channel(String channelName) {
		return null;
	}

	@Override
	public Collection<Channel<?>> channels() {
		return new ArrayList<>();
	}

	@Override
	public CompletableFuture<JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		user.assertRoleIsAtLeast("handleJsonrpcRequest", Role.GUEST);

		switch (request.getMethod()) {

		case GetEdgeConfigRequest.METHOD:
			return this.handleGetEdgeConfigRequest(user, GetEdgeConfigRequest.from(request));

		default:
			throw OpenemsError.JSONRPC_UNHANDLED_METHOD.exception(request.getMethod());
		}
	}

	/**
	 * Handles a {@link GetEdgeConfigRequest}.
	 *
	 * @param user    the {@link User}
	 * @param request the {@link GetEdgeConfigRequest}
	 * @return the Future JSON-RPC Response
	 * @throws OpenemsNamedException on error
	 */
	private CompletableFuture<JsonrpcResponseSuccess> handleGetEdgeConfigRequest(User user,
			GetEdgeConfigRequest request) throws OpenemsNamedException {
		var config = this.getEdgeConfig();
		var response = new GetEdgeConfigResponse(request.getId(), config);
		return CompletableFuture.completedFuture(response);
	}

	@Override
	public Clock getClock() {
		return this.clock;
	}

}